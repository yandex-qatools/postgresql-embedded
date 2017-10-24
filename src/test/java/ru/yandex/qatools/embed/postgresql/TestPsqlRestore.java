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

/**
 * This test does the same than {@link TestPsqlImport} but the initial dump is restored using pg_restore. The dump file
 * is a binary dump {@link /test.binary_dump} built as a copy of text dump {@link /test.backup}.
 *
 * Except initial "import" which is replaced by "restore", the test is identical
 *
 * @author Arnaud Thimel (Code Lutin)
 */
public class TestPsqlRestore extends AbstractPsqlTest{

    @Test
    public void testPsqlRestore() throws Exception {
        process.restoreFromFile(new File("src/test/resources/test.binary_dump"));
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