package ru.yandex.qatools.embed.postgresql;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;

import java.io.File;
import java.sql.*;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.qatools.embed.postgresql.distribution.Version.Main.PRODUCTION;
import static ru.yandex.qatools.embed.postgresql.util.SocketUtil.findFreePort;

public class TestPsqlImport {

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
    public void testPsqlImport() throws Exception {
        process.importFromFile(new File("src/test/resources/test.backup"));
        assertThat(conn, not(nullValue()));

        Statement statement = conn.createStatement();

        String expected;
        try (ResultSet res = statement.executeQuery("SELECT * FROM table1;")) {
            assertThat(res, not(nullValue()));
            String tableString = readTable(res);

            assertThat("Missing content in relation 'table1' in dump file!", tableString, not(nullValue()));

            expected = "test\t1\ta\n" +
                    "test\t2\tb\n" +
                    "test\t3\tc\n" +
                    "test\t4\td\n";
            assertEquals(expected, tableString);
        }

        assertThat(conn.createStatement().execute("INSERT INTO table1 VALUES ('test',5,'e');"), is(false));
        assertThat(conn.createStatement().execute("INSERT INTO table1 VALUES ('test',6,'f');"), is(false));

        try (ResultSet res = statement.executeQuery("SELECT * FROM table1;")) {
            assertThat(res, not(nullValue()));
            String tableString = readTable(res);

            assertThat("Missing content in relation 'table1' in dump file!", tableString, not(nullValue()));

            expected += "test\t5\te\n" + "test\t6\tf\n";
            assertEquals(expected, tableString);
        }

    }

    private String readTable(ResultSet res) throws SQLException {
        StringBuilder sb = null;
        while (res.next()) {
            if (null == sb)
                sb = new StringBuilder();
            sb.append(res.getString("col1"));
            sb.append("\t");
            sb.append(res.getInt("col2"));
            sb.append("\t");
            sb.append(res.getString("col3"));
            sb.append("\n");
        }
        return null != sb ? sb.toString() : null;
    }
}