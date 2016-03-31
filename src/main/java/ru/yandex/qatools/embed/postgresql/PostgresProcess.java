package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import de.flapdoodle.embed.process.io.LoggingOutputStreamProcessor;
import de.flapdoodle.embed.process.io.directories.IDirectory;
import de.flapdoodle.embed.process.io.progress.LoggingProgressListener;
import de.flapdoodle.embed.process.runtime.Executable;
import de.flapdoodle.embed.process.runtime.ProcessControl;
import ru.yandex.qatools.embed.postgresql.config.DownloadConfigBuilder;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.RuntimeConfigBuilder;
import ru.yandex.qatools.embed.postgresql.ext.ArtifactStoreBuilder;
import ru.yandex.qatools.embed.postgresql.ext.LogWatchStreamProcessor;
import ru.yandex.qatools.embed.postgresql.ext.PostgresArtifactStore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.flapdoodle.embed.process.io.file.Files.forceDelete;
import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.logging.Logger.getLogger;
import static org.apache.commons.io.FileUtils.readLines;
import static ru.yandex.qatools.embed.postgresql.Command.*;
import static ru.yandex.qatools.embed.postgresql.PostgresStarter.getCommand;
import static ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig.Storage;

/**
 * postgres process
 */
public class PostgresProcess extends AbstractPGProcess<PostgresExecutable, PostgresProcess> {
    private static Logger logger = getLogger(PostgresProcess.class.getName());
    private final IRuntimeConfig runtimeConfig;

    boolean stopped = false;

    public PostgresProcess(Distribution distribution, PostgresConfig config,
                           IRuntimeConfig runtimeConfig, PostgresExecutable executable) throws IOException {
        super(distribution, config, runtimeConfig, executable);
        this.runtimeConfig = runtimeConfig;
    }

    public static boolean shutdownPostgres(PostgresConfig config) {
        try {
            return runCmd(config, Command.PgCtl, "server stopped", 1000, "stop");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to stop postgres by pg_ctl!");
        }
        return false;
    }

    private static <P extends AbstractPGProcess> boolean runCmd(
            PostgresConfig config, Command cmd, String successOutput, int timoeut, String... args) {
        return runCmd(config, cmd, successOutput, Collections.<String>emptySet(), timoeut, args);
    }

    private static <P extends AbstractPGProcess> boolean runCmd(
            PostgresConfig config, Command cmd, String successOutput, Set<String> failOutput, long timeout, String... args) {
        try {
            LogWatchStreamProcessor logWatch = new LogWatchStreamProcessor(successOutput,
                    failOutput, new LoggingOutputStreamProcessor(logger, Level.ALL));
            final RuntimeConfigBuilder rtConfigBuilder = new RuntimeConfigBuilder().defaults(cmd);
            IRuntimeConfig runtimeConfig = rtConfigBuilder
                    .processOutput(new ProcessOutput(logWatch, logWatch, logWatch))
                    .artifactStore(new ArtifactStoreBuilder().defaults(cmd)
                            .download(new DownloadConfigBuilder().defaultsForCommand(cmd)
                                    .progressListener(
                                            new LoggingProgressListener(logger, Level.ALL)).build()))
                    .build();
            Executable exec = getCommand(cmd, runtimeConfig)
                    .prepare(new PostgresConfig(config).withArgs(args));
            P proc = (P) exec.start();
            logWatch.waitForResult(timeout);
            proc.waitFor();
            return true;
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
        return false;
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
                logger.info("trying to stop postgresql");
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
        final boolean result = shutdownPostgres(getConfig());
        if (runtimeConfig.getArtifactStore() instanceof PostgresArtifactStore) {
            final IDirectory tempDir = ((PostgresArtifactStore) runtimeConfig.getArtifactStore()).getTempDir();
            if (tempDir != null && tempDir.asFile() != null) {
                logger.log(Level.INFO, format("Cleaning up after the embedded process (removing %s)...",
                        tempDir.asFile().getAbsolutePath()));
                forceDelete(tempDir.asFile());
            }
        }
        return result;
    }

    @Override
    protected void onBeforeProcess(IRuntimeConfig runtimeConfig)
            throws IOException {
        super.onBeforeProcess(runtimeConfig);
        PostgresConfig config = getConfig();
        runCmd(config, InitDb, "Success. You can now start the database server using", 1000);
    }

    @Override
    protected List<String> getCommandLine(Distribution distribution, PostgresConfig config, IExtractedFileSet exe)
            throws IOException {
        List<String> ret = new ArrayList<>();
        ret.addAll(asList(exe.executable().getAbsolutePath(),
                "start",
                "-o",
                String.format("\"-p %s\"", String.valueOf(config.net().port())),
                "-D", config.storage().dbDir().getAbsolutePath(),
                "-w"
        ));

        return ret;
    }

    protected void deleteTempFiles() {
        final Storage storage = getConfig().storage();
        if ((storage.dbDir() != null) && (storage.isTmpDir()) && (!forceDelete(storage.dbDir()))) {
            logger.warning("Could not delete temp db dir: " + storage.dbDir());
        }
    }

    @Override
    protected final void onAfterProcessStart(ProcessControl process,
                                             IRuntimeConfig runtimeConfig) throws IOException {
        final Path pidFilePath = Paths.get(getConfig().storage().dbDir().getAbsolutePath(), "postmaster.pid");
        final File pidFile = new File(pidFilePath.toAbsolutePath().toString());
        int timeout = TIMEOUT;
        while (!pidFile.exists() && ((timeout = timeout - 100) > 0)) {
            try {
                sleep(100);
            } catch (InterruptedException ie) { /* safe to ignore */ }
        }
        int pid = -1;
        try {
            pid = Integer.valueOf(readLines(pidFilePath.toFile()).get(0));
        } catch (Exception e) {
            logger.log(Level.SEVERE, format("Failed to read PID file (%s)", e.getMessage()));
        }
        if (pid != -1) {
            setProcessId(pid);
        } else {
            // fallback, try to read pid file. will throw IOException if
            // that
            // fails
            setProcessId(getPidFromFile(pidFile()));
        }
        runCmd(getConfig(), CreateDb, "", new HashSet<>(singletonList("database creation failed")),
                1000, getConfig().storage().dbName());
    }

    /**
     * Import into database from file
     *
     * @param file The file to import into database
     */
    public void importFromFile(File file) {
        if (file.exists()) {
            runCmd(getConfig(), Psql, "", new HashSet<>(singletonList("import into " + getConfig().storage().dbName() + " failed")),
                    1000,
                    "-U", getConfig().credentials().username(),
                    "-d", getConfig().storage().dbName(),
                    "-h", getConfig().net().host(),
                    "-f", file.getAbsolutePath()
            );
        }
    }

    @Override
    protected void cleanupInternal() {
    }
}