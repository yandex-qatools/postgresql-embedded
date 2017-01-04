package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.DownloadConfigBuilder;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.RuntimeConfigBuilder;
import ru.yandex.qatools.embed.postgresql.ext.ArtifactStoreBuilder;

import java.sql.Connection;
import java.sql.DriverManager;

import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static ru.yandex.qatools.embed.postgresql.distribution.Version.Main.PRODUCTION;
import static ru.yandex.qatools.embed.postgresql.util.SocketUtil.findFreePort;

/**
 * @author Ilya Sadykov
 */
public class PostgresqlService {

    private PostgresProcess process;
    private Connection conn;

    public void start() throws Exception {
        IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
                .defaults(Command.Postgres)
                .artifactStore(new ArtifactStoreBuilder()
                        .defaults(Command.Postgres)
                        .download(new DownloadConfigBuilder()
                                .defaultsForCommand(Command.Postgres).build()
                        )
                ).build();
        PostgresStarter<PostgresExecutable, PostgresProcess> runtime = PostgresStarter.getInstance(runtimeConfig);
        final PostgresConfig config = new PostgresConfig(PRODUCTION, new AbstractPostgresConfig.Net("localhost", findFreePort()),
                new AbstractPostgresConfig.Storage("test"), new AbstractPostgresConfig.Timeout(),
                new AbstractPostgresConfig.Credentials("user", "password"));
        config.getAdditionalInitDbParams().addAll(asList(
                "-E", "SQL_ASCII",
                "--locale=C",
                "--lc-collate=C",
                "--lc-ctype=C"
        ));
        PostgresExecutable exec = runtime.prepare(config);
        process = exec.start();
        for (int trial = 0; trial < 10 && !process.isProcessReady(); ++trial) {
            sleep(100);
        }
        String url = format("jdbc:postgresql://%s:%s/%s?user=%s&password=%s",
                config.net().host(),
                config.net().port(),
                config.storage().dbName(),
                config.credentials().username(),
                config.credentials().password()
        );
        conn = DriverManager.getConnection(url);
    }

    public PostgresProcess getProcess() {
        return process;
    }

    public Connection getConn() {
        return conn;
    }

    public void stop() throws Exception {
        conn.close();
        process.stop();
    }
}
