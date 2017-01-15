package ru.yandex.qatools.embed.postgresql.config;

import de.flapdoodle.embed.process.config.ExecutableProcessConfig;
import de.flapdoodle.embed.process.distribution.IVersion;
import de.flapdoodle.embed.process.io.file.Files;
import ru.yandex.qatools.embed.postgresql.Command;
import ru.yandex.qatools.embed.postgresql.ext.SubdirTempDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static de.flapdoodle.embed.process.runtime.Network.getFreeServerPort;
import static de.flapdoodle.embed.process.runtime.Network.getLocalHost;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Common postgres config
 */
public abstract class AbstractPostgresConfig<C extends AbstractPostgresConfig> extends ExecutableProcessConfig {

    private final Storage storage;
    protected final Net network;
    protected final Timeout timeout;
    protected final Credentials credentials;
    protected List<String> args = new ArrayList<>();
    protected List<String> additionalInitDbParams = new ArrayList<>();

    protected AbstractPostgresConfig(AbstractPostgresConfig config, Command postgres) {
        this(config.version, config.net(), config.storage, config.timeout(), config.credentials, new SupportConfig(postgres));
    }

    protected AbstractPostgresConfig(AbstractPostgresConfig config) {
        this(config, Command.Postgres);
    }

    public AbstractPostgresConfig(IVersion version, Net network, Storage storage, Timeout timeout, Credentials cred, SupportConfig supportConfig) {
        super(version, supportConfig);
        this.network = network;
        this.timeout = timeout;
        this.storage = storage;
        this.credentials = cred;
    }

    public AbstractPostgresConfig(IVersion version, Net network, Storage storage, Timeout timeout) {
        this(version, network, storage, timeout, null, new SupportConfig(Command.Postgres));
    }

    public Net net() {
        return network;
    }

    public Timeout timeout() {
        return timeout;
    }

    public Storage storage() {
        return storage;
    }

    public Credentials credentials() {
        return credentials;
    }

    public List<String> args() {
        return args;
    }

    public C withArgs(String... args) {
        args().addAll(asList(args));
        return (C) this;
    }

    public C withAdditionalInitDbParams(List<String> additionalInitDbParams) {
        this.additionalInitDbParams.addAll(additionalInitDbParams);
        return (C) this;
    }


    /**
     * You may add here additional arguments for the {@code initdb} executable.<br/>
     * <p>
     * Example.<br>
     * to support german umlauts you would add here this additional arguments.<br/>
     * <pre>
     * getAdditionalInitDbParams().addAll(
     *      java.util.Arrays.asList(
     *          "-E", "'UTF-8'",
     *          "--lc-collate='de_DE.UTF-8'",
     *          "--lc-ctype=locale='de_DE.UTF-8'")
     * )
     * </pre>
     *
     * @return The list of additional parameters for the {@code initdb} executable.<br/>
     * Not {@code null}.<br/>
     */
    public List<String> getAdditionalInitDbParams() {
        return additionalInitDbParams;
    }

    public static class Storage {
        private final File dbDir;
        private final String dbName;
        private final boolean isTmpDir;

        public Storage(String dbName) throws IOException {
            this(dbName, null);
        }

        public Storage(String dbName, String databaseDir) throws IOException {
            this.dbName = dbName;
            if (isEmpty(databaseDir)) {
                isTmpDir = true;
                dbDir = Files.createTempDir(SubdirTempDir.defaultInstance(), "db-content");
            } else {
                dbDir = Files.createOrCheckDir(databaseDir);
                isTmpDir = false;
            }
        }

        public File dbDir() {
            return dbDir;
        }

        public boolean isTmpDir() {
            return isTmpDir;
        }

        public String dbName() {
            return dbName;
        }

        @Override
        public String toString() {
            return "Storage{" +
                    "dbDir=" + dbDir +
                    ", dbName='" + dbName + '\'' +
                    ", isTmpDir=" + isTmpDir +
                    '}';
        }
    }

    public static class Credentials {

        private final String username;
        private final String password;


        public Credentials(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String username() {
            return username;
        }

        public String password() {
            return password;
        }

        @Override
        public String toString() {
            return "Credentials{" +
                    "username='" + username + '\'' +
                    ", password='" + password + '\'' + //NOSONAR
                    '}';
        }
    }

    public static class Net {

        private final String host;
        private final int port;

        public Net() throws IOException {
            this(getLocalHost().getHostAddress(), getFreeServerPort());
        }

        public Net(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public int port() {
            return port;
        }

        public String host() {
            return host;
        }

        @Override
        public String toString() {
            return "Net{" +
                    "host='" + host + '\'' +
                    ", port=" + port +
                    '}';
        }
    }

    public static class Timeout {

        private final long startupTimeout;

        public Timeout() {
            this(15000);
        }

        public Timeout(long startupTimeout) {
            this.startupTimeout = startupTimeout;
        }

        public long startupTimeout() {
            return startupTimeout;
        }

        @Override
        public String toString() {
            return "Timeout{" +
                    "startupTimeout=" + startupTimeout +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "AbstractPostgresConfig{" +
                "storage=" + storage +
                ", network=" + network +
                ", timeout=" + timeout +
                ", credentials=" + credentials +
                ", args=" + args +
                ", additionalInitDbParams=" + additionalInitDbParams +
                '}';
    }
}