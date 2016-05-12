package ru.yandex.qatools.embed.postgresql;

import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestPsqlExport extends AbstractPsqlTest {

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