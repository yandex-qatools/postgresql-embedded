package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import de.flapdoodle.embed.process.io.LoggingOutputStreamProcessor;
import de.flapdoodle.embed.process.io.directories.IDirectory;
import de.flapdoodle.embed.process.io.progress.LoggingProgressListener;
import de.flapdoodle.embed.process.runtime.Executable;
import de.flapdoodle.embed.process.runtime.ProcessControl;
import de.flapdoodle.embed.process.store.IArtifactStore;
import org.apache.commons.lang3.ArrayUtils;
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
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.logging.Logger.getLogger;
import static org.apache.commons.io.FileUtils.readLines;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.yandex.qatools.embed.postgresql.Command.CreateDb;
import static ru.yandex.qatools.embed.postgresql.Command.InitDb;
import static ru.yandex.qatools.embed.postgresql.Command.PgDump;
import static ru.yandex.qatools.embed.postgresql.Command.Psql;
import static ru.yandex.qatools.embed.postgresql.PostgresStarter.getCommand;
import static ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig.Storage;
import static ru.yandex.qatools.embed.postgresql.util.ReflectUtil.setFinalField;

/**
 * postgres process
 */
public class PostgresProcess extends AbstractPGProcess<PostgresExecutable, PostgresProcess> {
    public static final int MAX_CREATEDB_TRIALS = 3;
    private static Logger logger = getLogger(PostgresProcess.class.getName());
    private final IRuntimeConfig runtimeConfig;

    volatile boolean processReady = false;
    boolean stopped = false;

    public PostgresProcess(Distribution distribution, PostgresConfig config,
                           IRuntimeConfig runtimeConfig, PostgresExecutable executable) throws IOException {
        super(distribution, config, runtimeConfig, executable);
        this.runtimeConfig = runtimeConfig;
    }

    /**
     * @deprecated consider using {@link #stop()} method instead
     */
    @Deprecated
    public static boolean shutdownPostgres(PostgresConfig config) {
        return shutdownPostgres(config, new RuntimeConfigBuilder().defaults(Command.PgCtl).build());
    }

    private static String runCmd(
            PostgresConfig config, IRuntimeConfig runtimeConfig, Command cmd, String successOutput, int timeout, String... args) {
        return runCmd(config, runtimeConfig, cmd, successOutput, Collections.<String>emptySet(), timeout, args);
    }

    private static String runCmd(
            PostgresConfig config, IRuntimeConfig runtimeConfig, Command cmd, String successOutput, Set<String> failOutput, long timeout, String... args) {
        try {
            LogWatchStreamProcessor logWatch = new LogWatchStreamProcessor(successOutput,
                    failOutput, new LoggingOutputStreamProcessor(logger, Level.ALL));

            IArtifactStore artifactStore = runtimeConfig.getArtifactStore();
            IDownloadConfig downloadCfg = ((PostgresArtifactStore) artifactStore).getDownloadConfig();

            // TODO: very hacky and unreliable way to respect the parent command's configuration
            try { //NOSONAR
                setFinalField(downloadCfg, "_packageResolver", new PackagePaths(cmd));
                setFinalField(artifactStore, "_downloadConfig", downloadCfg);
            } catch (Exception e) {
                // fallback to the default config
                logger.log(Level.SEVERE, "Could not use the configured artifact store for cmd, " +
                        "falling back to default " + cmd, e);
                downloadCfg = new DownloadConfigBuilder().defaultsForCommand(cmd)
                        .progressListener(new LoggingProgressListener(logger, Level.ALL)).build();
                artifactStore = new ArtifactStoreBuilder().defaults(cmd).download(downloadCfg).build();
            }

            final IRuntimeConfig runtimeCfg = new RuntimeConfigBuilder().defaults(cmd)
                    .processOutput(new ProcessOutput(logWatch, logWatch, logWatch))
                    .artifactStore(artifactStore)
                    .commandLinePostProcessor(runtimeConfig.getCommandLinePostProcessor()).build();


            final PostgresConfig postgresConfig = new PostgresConfig(config).withArgs(args);
            if (Command.InitDb == cmd) {
                postgresConfig.withAdditionalInitDbParams(config.getAdditionalInitDbParams());
            }
            Executable<?, ? extends AbstractPGProcess> exec = getCommand(cmd, runtimeCfg)
                    .prepare(postgresConfig);
            AbstractPGProcess proc = exec.start();
            logWatch.waitForResult(timeout);
            proc.waitFor();
            return logWatch.getOutput();
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
        return null;
    }

    private static boolean shutdownPostgres(PostgresConfig config, IRuntimeConfig runtimeConfig) {
        try {
            return isEmpty(runCmd(config, runtimeConfig, Command.PgCtl, "server stopped", 1000, "stop"));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to stop postgres by pg_ctl!");
        }
        return false;
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
        final boolean result = shutdownPostgres(getConfig(), runtimeConfig);
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
        runCmd(config, runtimeConfig, InitDb, "Success. You can now start the database server using", 1000);
    }

    @Override
    protected List<String> getCommandLine(Distribution distribution, PostgresConfig config, IExtractedFileSet exe)
            throws IOException {
        List<String> ret = new ArrayList<>();
        switch (config.supportConfig().getName()) {
            case "postgres": //NOSONAR
                ret.addAll(asList(exe.executable().getAbsolutePath(),
                        "-p", String.valueOf(config.net().port()),
                        "-h", config.net().host(),
                        "-D", config.storage().dbDir().getAbsolutePath()
                ));
                break;
            case "pg_ctl": //NOSONAR
                ret.addAll(asList(exe.executable().getAbsolutePath(),
                        String.format("-o \"-p %s\" \"-h %s\"", config.net().port(), config.net().host()),
                        "-D", config.storage().dbDir().getAbsolutePath(),
                        "-w",
                        "start"
                ));
                break;
            default:
                throw new RuntimeException("Failed to launch Postgres: Unknown command " +
                        config.supportConfig().getName() + "!");
        }
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
            // fallback, try to read pid file. will throw IOException if that fails
            setProcessId(getPidFromFile(pidFile()));
        }
        int trial = 0;
        do {
            String output = runCmd(getConfig(), runtimeConfig, CreateDb, "",
                    new HashSet<>(singleton("database creation failed")), 3000, getConfig().storage().dbName());
            try {
                if (isEmpty(output) || !output.contains("could not connect to database")) {
                    break;
                }
                logger.log(Level.WARNING,
                        format("Could not create database first time (%s of %s trials)", trial, MAX_CREATEDB_TRIALS));
                sleep(100);
            } catch (InterruptedException ie) { /* safe to ignore */ }
        } while (trial++ < MAX_CREATEDB_TRIALS);
    }

    /**
     * Import into database from file
     *
     * @param file The file to import into database
     */
    public void importFromFile(File file) {
        importFromFileWithArgs(file);
    }

    /**
     * Import into database from file with additional args
     *
     * @param file
     * @param cliArgs additional arguments for psql (be sure to separate args from their values)
     */
    public void importFromFileWithArgs(File file, String... cliArgs) {
        if (file.exists()) {
            String[] args = {
                    "-U", getConfig().credentials().username(),
                    "-d", getConfig().storage().dbName(),
                    "-h", getConfig().net().host(),
                    "-p", String.valueOf(getConfig().net().port()),
                    "-f", file.getAbsolutePath()};
            if (cliArgs != null && cliArgs.length != 0) {
                args = ArrayUtils.addAll(args, cliArgs);
            }
            runCmd(getConfig(), runtimeConfig, Psql, "", new HashSet<>(singletonList("import into " + getConfig().storage().dbName() + " failed")), 1000, args);
        }
    }

    public void exportToFile(File file) {
        runCmd(getConfig(), runtimeConfig, PgDump, "", new HashSet<>(singletonList("export from " + getConfig().storage().dbName() + " failed")),
                1000,
                "-U", getConfig().credentials().username(),
                "-d", getConfig().storage().dbName(),
                "-h", getConfig().net().host(),
                "-p", String.valueOf(getConfig().net().port()),
                "-f", file.getAbsolutePath()
        );
    }

    public void exportSchemeToFile(File file) {
        runCmd(getConfig(), runtimeConfig, PgDump, "", new HashSet<>(singletonList("export from " + getConfig().storage().dbName() + " failed")),
                1000,
                "-U", getConfig().credentials().username(),
                "-d", getConfig().storage().dbName(),
                "-h", getConfig().net().host(),
                "-p", String.valueOf(getConfig().net().port()),
                "-f", file.getAbsolutePath(),
                "-s"
        );
    }

    public void exportDataToFile(File file) {
        runCmd(getConfig(), runtimeConfig, PgDump, "", new HashSet<>(singletonList("export from " + getConfig().storage().dbName() + " failed")),
                1000,
                "-U", getConfig().credentials().username(),
                "-d", getConfig().storage().dbName(),
                "-h", getConfig().net().host(),
                "-p", String.valueOf(getConfig().net().port()),
                "-f", file.getAbsolutePath(),
                "-a"
        );
    }

    public boolean isProcessReady() {
        return processReady;
    }

    @Override
    protected void cleanupInternal() {
    }
}