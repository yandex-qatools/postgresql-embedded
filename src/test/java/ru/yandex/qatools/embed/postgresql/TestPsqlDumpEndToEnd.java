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
import static org.junit.Assert.assertTrue;

public class TestPsqlDumpEndToEnd extends AbstractPsqlTest {

	@Test
	public void testPsqlDumpEndToEnd() throws Exception {

		// Load dump
		process.importFromFile(new File("src/test/resources/test.backup"));
		assertThat(conn, not(nullValue()));

		assertSchemaAndData();

		// Create binary dump
		File fullExportDump = File.createTempFile("full_", ".dmp");
		process.exportToFile(fullExportDump, "-Fc");
		assertTrue(fullExportDump.exists());
		assertTrue(fullExportDump.length() > 0);

		// Create new connection
		tearDown();
		setUp();

		// Load binary dump into a fresh database
		assertTrue(fullExportDump.exists());
		process.restoreFromFile(fullExportDump);
		assertThat(conn, not(nullValue()));

		assertSchemaAndData();

	}

	private void assertSchemaAndData() throws SQLException {

		String expected;
		try (Statement statement = conn.createStatement();
				ResultSet res = statement.executeQuery("SELECT * FROM table1;")) {
			assertThat(res, not(nullValue()));
			String tableString = readTable(res);

			assertThat("Missing content in relation 'table1' in dump file!", tableString, not(nullValue()));

			expected = "test\t1\ta\n" + "test\t2\tb\n" + "test\t3\tc\n" + "test\t4\td\n";
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