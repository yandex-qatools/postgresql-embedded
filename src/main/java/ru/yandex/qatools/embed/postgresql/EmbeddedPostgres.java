package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.IVersion;
import de.flapdoodle.embed.process.io.directories.FixedPath;
import de.flapdoodle.embed.process.store.PostgresArtifactStoreBuilder;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.PostgresDownloadConfigBuilder;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.RuntimeConfigBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static ru.yandex.qatools.embed.postgresql.distribution.Version.Main.PRODUCTION;
import static ru.yandex.qatools.embed.postgresql.util.SocketUtil.findFreePort;

/**
 * Helper class simplifying the start up configuration for embedded postgres
 */
public class EmbeddedPostgres {
    public static final String DEFAULT_USER = "postgres";//NOSONAR
    public static final String DEFAULT_PASSWORD = "postgres";//NOSONAR
    public static final String DEFAULT_DB_NAME = "postgres";//NOSONAR
    public static final String DEFAULT_HOST = "localhost";
    private static final List<String> DEFAULT_ADD_PARAMS = asList(
            "-E", "SQL_ASCII",
            "--locale=C",
            "--lc-collate=C",
            "--lc-ctype=C");
    private final String dataDir;
    private final IVersion version;
    private PostgresProcess process;
    private PostgresConfig config;

    public EmbeddedPostgres() {
        this(PRODUCTION);
    }

    public EmbeddedPostgres(IVersion version) {
        this(version, null);
    }

    public EmbeddedPostgres(String dataDir){
        this(PRODUCTION, dataDir);
    }

    public EmbeddedPostgres(IVersion version, String dataDir){
        this.version = version;
        this.dataDir = dataDir;
    }

    /**
     * Initializes the default runtime configuration using the temporary directory.
     *
     * @return runtime configuration required for postgres to start.
     */
    public static IRuntimeConfig defaultRuntimeConfig() {
        return new RuntimeConfigBuilder()
                .defaults(Command.Postgres)
                .artifactStore(new PostgresArtifactStoreBuilder()
                        .defaults(Command.Postgres)
                        .download(new PostgresDownloadConfigBuilder()
                                .defaultsForCommand(Command.Postgres)
                                .build()))
                .build();
    }

    /**
     * Initializes runtime configuration for cached directory.
     * If a provided directory is empty, postgres will be extracted into it.
     *
     * @param cachedPath path where postgres is supposed to be extracted
     * @return runtime configuration required for postgres to start
     */
    public static IRuntimeConfig cachedRuntimeConfig(Path cachedPath) {
        final Command cmd = Command.Postgres;
        final FixedPath cachedDir = new FixedPath(cachedPath.toString());
        return new RuntimeConfigBuilder()
                .defaults(cmd)
                .artifactStore(new PostgresArtifactStoreBuilder()
                        .defaults(cmd)
                        .tempDir(cachedDir)
                        .download(new PostgresDownloadConfigBuilder()
                                .defaultsForCommand(cmd)
                                .packageResolver(new PackagePaths(cmd, cachedDir))
                                .build()))
                .build();
    }

    public String start() throws IOException {
        return start(DEFAULT_HOST, findFreePort(), DEFAULT_DB_NAME);
    }

    public String start(String host, int port, String dbName) throws IOException {
        return start(host, port, dbName, DEFAULT_USER, DEFAULT_PASSWORD, DEFAULT_ADD_PARAMS);
    }

    public String start(String host, int port, String dbName, String user, String password) throws IOException {
        return start(defaultRuntimeConfig(), host, port, dbName, user, password, DEFAULT_ADD_PARAMS);
    }

    public String start(String host, int port, String dbName, String user, String password, List<String> additionalParams) throws IOException {
        return start(defaultRuntimeConfig(), host, port, dbName, user, password, additionalParams);
    }

    public String start(IRuntimeConfig runtimeConfig) throws IOException {
        return start(runtimeConfig, DEFAULT_HOST, findFreePort(), DEFAULT_DB_NAME, DEFAULT_USER, DEFAULT_PASSWORD, DEFAULT_ADD_PARAMS);
    }

    /**
     * Starts up the embedded postgres
     *
     * @param runtimeConfig    required runtime configuration
     * @param host             host to bind to
     * @param port             port to bind to
     * @param dbName           name of the database to initialize
     * @param user             username to connect
     * @param password         password for the provided username
     * @param additionalParams additional database init params (if required)
     * @return connection url for the initialized postgres instance
     * @throws IOException if an I/O error occurs during the process startup
     */
    public String start(IRuntimeConfig runtimeConfig, String host, int port, String dbName, String user, String password,
                        List<String> additionalParams) throws IOException {
        final PostgresStarter<PostgresExecutable, PostgresProcess> runtime = PostgresStarter.getInstance(runtimeConfig);
        config = new PostgresConfig(version,
                new AbstractPostgresConfig.Net(host, port),
                new AbstractPostgresConfig.Storage(dbName, dataDir),
                new AbstractPostgresConfig.Timeout(),
                new AbstractPostgresConfig.Credentials(user, password)
        );
        config.getAdditionalInitDbParams().addAll(additionalParams);
        PostgresExecutable exec = runtime.prepare(config);
        this.process = exec.start();
        return formatConnUrl(config);
    }

    /**
     * Returns the configuration of started process
     *
     * @return empty if process has not been started yet
     */
    public Optional<PostgresConfig> getConfig() {
        return ofNullable(config);
    }


    /**
     * Returns the process if started
     *
     * @return empty if process has not been started yet
     */
    public Optional<PostgresProcess> getProcess() {
        return ofNullable(process);
    }

    /**
     * Returns the connection url for the running postgres instance
     *
     * @return empty if process has not been started yet
     */
    public Optional<String> getConnectionUrl() {
        return getConfig().map(this::formatConnUrl);
    }

    private String formatConnUrl(PostgresConfig config) {
        return format("jdbc:postgresql://%s:%s/%s?user=%s&password=%s",//NOSONAR
                config.net().host(),
                config.net().port(),
                config.storage().dbName(),
                config.credentials().username(),
                config.credentials().password()
        );
    }

    public void stop() {
        getProcess().orElseThrow(() -> new IllegalStateException("Cannot stop not started instance!")).stop();
    }
}
