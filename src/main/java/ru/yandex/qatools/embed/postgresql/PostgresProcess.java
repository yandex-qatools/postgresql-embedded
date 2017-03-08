package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import de.flapdoodle.embed.process.io.Slf4jLevel;
import de.flapdoodle.embed.process.io.Slf4jStreamProcessor;
import de.flapdoodle.embed.process.io.directories.IDirectory;
import de.flapdoodle.embed.process.io.progress.Slf4jProgressListener;
import de.flapdoodle.embed.process.runtime.Executable;
import de.flapdoodle.embed.process.runtime.ProcessControl;
import de.flapdoodle.embed.process.store.IArtifactStore;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import ru.yandex.qatools.embed.postgresql.config.DownloadConfigBuilder;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.RuntimeConfigBuilder;
import ru.yandex.qatools.embed.postgresql.ext.ArtifactStoreBuilder;
import ru.yandex.qatools.embed.postgresql.ext.LogWatchStreamProcessor;
import ru.yandex.qatools.embed.postgresql.ext.PostgresArtifactStore;
import ru.yandex.qatools.embed.postgresql.ext.SubdirTempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static de.flapdoodle.embed.process.io.file.Files.forceDelete;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.io.FileUtils.readLines;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.slf4j.LoggerFactory.getLogger;
import static ru.yandex.qatools.embed.postgresql.Command.CreateDb;
import static ru.yandex.qatools.embed.postgresql.Command.InitDb;
import static ru.yandex.qatools.embed.postgresql.Command.PgDump;
import static ru.yandex.qatools.embed.postgresql.Command.PgRestore;
import static ru.yandex.qatools.embed.postgresql.Command.Psql;
import static ru.yandex.qatools.embed.postgresql.PostgresStarter.getCommand;
import static ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig.Storage;
import static ru.yandex.qatools.embed.postgresql.util.ReflectUtil.setFinalField;

/**
 * postgres process
 */
public class PostgresProcess extends AbstractPGProcess<PostgresExecutable, PostgresProcess> {
    public static final int MAX_CREATEDB_TRIALS = 3;
    private static Logger LOGGER = getLogger(PostgresProcess.class);
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
                    failOutput, new Slf4jStreamProcessor(LOGGER, Slf4jLevel.TRACE));

            IArtifactStore artifactStore = runtimeConfig.getArtifactStore();
            IDownloadConfig downloadCfg = ((PostgresArtifactStore) artifactStore).getDownloadConfig();

            // TODO: very hacky and unreliable way to respect the parent command's configuration
            try { //NOSONAR
                IDirectory tempDir = SubdirTempDir.defaultInstance();
                if (downloadCfg.getPackageResolver() instanceof PackagePaths) {
                    tempDir = ((PackagePaths) downloadCfg.getPackageResolver()).getTempDir();
                }
                setFinalField(downloadCfg, "_packageResolver", new PackagePaths(cmd, tempDir));
                setFinalField(artifactStore, "_downloadConfig", downloadCfg);
            } catch (Exception e) {
                // fallback to the default config
                LOGGER.error("Could not use the configured artifact store for cmd, " +
                        "falling back to default " + cmd, e);
                downloadCfg = new DownloadConfigBuilder().defaultsForCommand(cmd)
                        .progressListener(new Slf4jProgressListener(LOGGER)).build();
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
            LOGGER.warn("Failed to run command {}", cmd.commandName(), e);
        }
        return null;
    }

    private static boolean shutdownPostgres(PostgresConfig config, IRuntimeConfig runtimeConfig) {
        try {
            return isEmpty(runCmd(config, runtimeConfig, Command.PgCtl, "server stopped", 2000, "stop"));
        } catch (Exception e) {
            LOGGER.warn("Failed to stop postgres by pg_ctl!", e);
        }
        return false;
    }

    @Override
    protected void stopInternal() {
        synchronized (this) {
            if (!stopped) {
                stopped = true;
                LOGGER.info("trying to stop postgresql");
                if (!sendStopToPostgresqlInstance() && !sendTermToProcess() && waitUntilProcessHasStopped(2000)) {
                    LOGGER.warn("could not stop postgresql with pg_ctl/SIGTERM, trying to kill it...");
                    if (!sendKillToProcess() && !tryKillToProcess() && waitUntilProcessHasStopped(3000)) {
                        LOGGER.warn("could not kill postgresql within 4s!");
                    }
                }
            }
            if (waitUntilProcessHasStopped(5000)) {
                LOGGER.error("Postgres has not been stopped within 10s! Something's wrong!");
            }
            deleteTempFiles();
        }
    }

    private boolean waitUntilProcessHasStopped(int timeoutMillis) {
        long started = currentTimeMillis();
        while (currentTimeMillis() - started < timeoutMillis && isProcessRunning()) {
            try {
                sleep(50);
            } catch (InterruptedException e) {
                LOGGER.warn("Failed to wait with timeout until the process has been killed", e);
            }
        }
        return isProcessRunning();
    }

    protected final boolean sendStopToPostgresqlInstance() {
        final boolean result = shutdownPostgres(getConfig(), runtimeConfig);
        if (runtimeConfig.getArtifactStore() instanceof PostgresArtifactStore) {
            final IDirectory tempDir = ((PostgresArtifactStore) runtimeConfig.getArtifactStore()).getTempDir();
            if (tempDir != null && tempDir.asFile() != null && tempDir.isGenerated()) {
                LOGGER.info("Cleaning up after the embedded process (removing {})...", tempDir.asFile().getAbsolutePath());
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
            LOGGER.warn("Could not delete temp db dir: {}", storage.dbDir());
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
            LOGGER.error("Failed to read PID file ({})", e.getMessage(), e);
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
                LOGGER.warn("Could not create database first time ({} of {} trials)", trial, MAX_CREATEDB_TRIALS);
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

    /**
     * Import into database from file with additional args
     *
     * @param file
     * @param cliArgs additional arguments for psql (be sure to separate args from their values)
     */
    public void restoreFromFile(File file, String... cliArgs) {
        if (file.exists()) {
            String[] args = {
                    "-U", getConfig().credentials().username(),
                    "-d", getConfig().storage().dbName(),
                    "-h", getConfig().net().host(),
                    "-p", String.valueOf(getConfig().net().port()),
                    file.getAbsolutePath()};
            if (cliArgs != null && cliArgs.length != 0) {
                args = ArrayUtils.addAll(args, cliArgs);
            }
            runCmd(getConfig(), runtimeConfig, PgRestore, "", new HashSet<>(singletonList("restore into " + getConfig().storage().dbName() + " failed")), 1000, args);
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