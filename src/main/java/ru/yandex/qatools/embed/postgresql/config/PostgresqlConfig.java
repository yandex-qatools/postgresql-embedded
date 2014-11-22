package ru.yandex.qatools.embed.postgresql.config;

import de.flapdoodle.embed.process.distribution.IVersion;
import ru.yandex.qatools.embed.postgresql.distribution.Version;

import java.io.IOException;

/**
 * Configuration for postgres
 */
public class PostgresqlConfig extends AbstractPostgresConfig<PostgresqlConfig> {

    public PostgresqlConfig(AbstractPostgresConfig config) {
        super(config);
    }

    public PostgresqlConfig(IVersion version, String dbName) throws IOException {
        this(version, new Net(), new Storage(dbName), new Timeout());
    }

    public PostgresqlConfig(IVersion version, String host, int port, String dbName) throws IOException {
        this(version, new Net(host, port), new Storage(dbName), new Timeout());
    }

    public PostgresqlConfig(IVersion version, Net networt, Storage storage, Timeout timeout, Credentials cred) {
        super(version, networt, storage, timeout, cred);
    }

    public PostgresqlConfig(IVersion version, Net network, Storage storage, Timeout timeout) {
        super(version, network, storage, timeout);
    }

    public static PostgresqlConfig defaultWithDbName(String dbName) throws IOException {
        return new PostgresqlConfig(Version.Main.PRODUCTION, dbName);
    }
}
