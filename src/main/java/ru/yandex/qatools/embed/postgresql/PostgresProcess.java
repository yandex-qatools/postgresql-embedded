package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import de.flapdoodle.embed.process.io.LogWatchStreamProcessor;
import de.flapdoodle.embed.process.io.NullProcessor;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.StreamToLineProcessor;
import de.flapdoodle.embed.process.io.file.Files;
import de.flapdoodle.embed.process.runtime.Executable;
import de.flapdoodle.embed.process.runtime.ProcessControl;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.PostgresqlConfig;
import ru.yandex.qatools.embed.postgresql.config.RuntimeConfigBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.readLines;
import static ru.yandex.qatools.embed.postgresql.Command.CreateDb;
import static ru.yandex.qatools.embed.postgresql.Command.InitDb;
import static ru.yandex.qatools.embed.postgresql.PostgresStarter.getStarter;
import static ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig.Storage;

/**
 * postgres process
 */
public class PostgresProcess extends AbstractPGProcess<PostgresqlConfig, PostgresExecutable, PostgresProcess> {
    private static Logger logger = Logger.getLogger(PostgresProcess.class.getName());

    boolean stopped = false;

    public PostgresProcess(Distribution distribution, PostgresqlConfig config,
                           IRuntimeConfig runtimeConfig, PostgresExecutable executable) throws IOException {
        super(distribution, config, runtimeConfig, executable);
    }

    protected Set<String> knownFailureMessages() {
        HashSet<String> ret = new HashSet<>();
        ret.add("failed errno");
        ret.add("[postgres error]");
        return ret;
    }

    @Override
    protected void stopInternal() {
        synchronized (this) {
            if (!stopped) {
                stopped = true;
                logger.info("try to stop postgresql");
                if (!sendStopToPostgresqlInstance()) {
                    logger.warning("could not stop postgresql with command, try next");
                    if (!sendKillToProcess()) {
                        logger.warning("could not stop postgresql, try next");
                        if (!sendTermToProcess()) {
                            logger.warning("could not stop postgresql, try next");
                            if (!tryKillToProcess()) {
                                logger.warning("could not stop postgresql the second time, try one last thing");
                            }
                        }
                    }
                }
            }
            deleteTempFiles();
        }
    }

    protected final boolean sendStopToPostgresqlInstance() {
        return shutdownPostgres(getConfig());
    }

    public static boolean shutdownPostgres(AbstractPostgresConfig config) {
        return runCmd(PgCtlExecutable.class, config, Command.PgCtl, "server stopped", "stop");
    }

    private static <E extends Executable<C, P>, P extends AbstractPGProcess, C extends AbstractPostgresConfig>
    boolean runCmd(Class<E> executorClass, C config, Command cmd, String waitOutput, String... args) {
        try {
            LogWatchStreamProcessor logWatch = new LogWatchStreamProcessor(waitOutput,
                    Collections.<String>emptySet(), new NullProcessor());
            final RuntimeConfigBuilder rtConfigBuilder = new RuntimeConfigBuilder().defaults(cmd);
            IRuntimeConfig runtimeConfig = rtConfigBuilder
                    .processOutput(new ProcessOutput(logWatch, logWatch, logWatch))
                    .build();
            Executable exec = getStarter(executorClass, runtimeConfig)
                    .prepare((C) new PostgresqlConfig(config).withArgs(args));
            exec.start();
            logWatch.waitForResult(config.timeout().startupTimeout());
            return true;
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
        return false;
    }

    @Override
    protected void onBeforeProcess(IRuntimeConfig runtimeConfig)
            throws IOException {
        super.onBeforeProcess(runtimeConfig);
        PostgresqlConfig config = getConfig();
        runCmd(InitDbExecutable.class, config, InitDb, "Success. You can now start the database server using");
    }

    @Override
    protected List<String> getCommandLine(Distribution distribution, PostgresqlConfig config, IExtractedFileSet exe)
            throws IOException {
        List<String> ret = new ArrayList<>();
        ret.addAll(Arrays.asList(exe.executable().getAbsolutePath(),
                "-h", config.net().getServerAddress().getHostName(),
                "-p", String.valueOf(config.net().port()),
                "-D", config.storage().dbDir().getAbsolutePath()
        ));

        return ret;
    }

    protected void deleteTempFiles() {
        final Storage storage = getConfig().storage();
        if ((storage.dbDir() != null) && (storage.isTmpDir()) && (!Files.forceDelete(storage.dbDir()))) {
            logger.warning("Could not delete temp db dir: " + storage.dbDir());
        }
    }

    @Override
    protected final void onAfterProcessStart(ProcessControl process,
                                             IRuntimeConfig runtimeConfig) throws IOException {
        ProcessOutput outputConfig = runtimeConfig.getProcessOutput();
        LogWatchStreamProcessor logWatch = new LogWatchStreamProcessor(
                "database system is ready to accept connections",
                knownFailureMessages(), StreamToLineProcessor.wrap(outputConfig.getOutput()));
        Processors.connect(process.getReader(), logWatch);
        Processors.connect(process.getError(), StreamToLineProcessor.wrap(outputConfig.getError()));
        logWatch.waitForResult(getConfig().timeout().startupTimeout());
        final Path pidFilePath = Paths.get(getConfig().storage().dbDir().getAbsolutePath(), "postmaster.pid");
        int pid = -1;
        try {
            pid = Integer.valueOf(readLines(pidFilePath.toFile()).get(0));
        } catch (Exception e) {
            logger.log(Level.SEVERE, format("Failed to read PID file (%s)", e.getMessage()));
        }
        if (logWatch.isInitWithSuccess() && pid != -1) {
            setProcessId(pid);
        } else {
            // fallback, try to read pid file. will throw IOException if
            // that
            // fails
            setProcessId(getPidFromFile(pidFile()));
        }
        runCmd(CreateDbExecutable.class, getConfig(), CreateDb, "", getConfig().storage().dbName());
    }

    @Override
    protected void cleanupInternal() {
    }
}