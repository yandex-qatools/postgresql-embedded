package ru.yandex.qatools.embed.postgresql;

import org.junit.Test;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TestPsqlImport extends AbstractPsqlTest {

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