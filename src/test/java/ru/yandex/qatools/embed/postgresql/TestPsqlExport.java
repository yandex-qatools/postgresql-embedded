package ru.yandex.qatools.embed.postgresql;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.qatools.embed.postgresql.distribution.Version.Main.PRODUCTION;
import static ru.yandex.qatools.embed.postgresql.util.SocketUtil.findFreePort;

public class TestPsqlExport {

    private PostgresProcess process;
    private Connection conn;

    @Before
    public void setUp() throws Exception {
        PostgresStarter<PostgresExecutable, PostgresProcess> runtime = PostgresStarter.getDefaultInstance();
        final PostgresConfig config = new PostgresConfig(PRODUCTION, new AbstractPostgresConfig.Net(
                "localhost", findFreePort()
        ), new AbstractPostgresConfig.Storage("test"), new AbstractPostgresConfig.Timeout(),
                new AbstractPostgresConfig.Credentials("user", "password"));
        PostgresExecutable exec = runtime.prepare(config);
        process = exec.start();
        String url = format("jdbc:postgresql://%s:%s/%s?user=%s&password=%s",
                config.net().host(),
                config.net().port(),
                config.storage().dbName(),
                config.credentials().username(),
                config.credentials().password()
        );
        conn = DriverManager.getConnection(url);
    }

    @After
    public void tearDown() throws Exception {
        conn.close();
        process.stop();
    }

    @Test
    public void testPsqlExport() throws Exception {
        process.importFromFile(new File("src/test/resources/test.backup"));
        assertThat(conn, not(nullValue()));

        File fullExportDump = File.createTempFile("full_", ".dmp");
        try {
            process.exportToFile(fullExportDump);
            assertTrue(fullExportDump.length() > 0);
        } finally {
            assertTrue(fullExportDump.delete());
        }

        File schemeDump = File.createTempFile("scheme_", ".dmp");
        try {
            process.exportSchemeToFile(schemeDump);
            assertTrue(schemeDump.length() > 0);
        } finally {
            assertTrue(schemeDump.delete());
        }

        File dataExportDump = File.createTempFile("data_", ".dmp");
        try {
            process.exportToFile(dataExportDump);
            assertTrue(dataExportDump.length() > 0);
        } finally {
            assertTrue(dataExportDump.delete());
        }
    }
}