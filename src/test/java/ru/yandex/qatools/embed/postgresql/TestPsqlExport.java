package ru.yandex.qatools.embed.postgresql;

import java.nio.file.Files;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
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

    @Test
    public void testPsqlExportWithArgs() throws Exception {
        process.importFromFile(new File("src/test/resources/test.backup"));
        assertThat(conn, not(nullValue()));

        {
            File fullExportDump = Files.createTempDirectory("psql_export_full_dump").toFile();
            try {
                process
                    .exportToFileWithArgs(fullExportDump,
                        "--no-privileges",
                        "--no-owner",
                        "--format",
                        "directory");
                final File[] filesInDumpDir = fullExportDump.listFiles();
                assertNotNull(filesInDumpDir);
                assertNotEquals(0, filesInDumpDir.length);
                for (final File file : filesInDumpDir) {
                    assertNotEquals(0, file.length());
                }
            } finally {
                deleteDir(fullExportDump);
            }
        }

        {
            File schemeDump = Files.createTempDirectory("psql_export_scheme_dump").toFile();
            try {
                process
                    .exportSchemeToFileWithArgs(schemeDump,
                        "--no-privileges",
                        "--no-owner",
                        "--format",
                        "directory");
                final File[] filesInDumpDir = schemeDump.listFiles();
                assertNotNull(filesInDumpDir);
                assertNotEquals(0, filesInDumpDir.length);
                for (final File file : filesInDumpDir) {
                    assertNotEquals(0, file.length());
                }
            } finally {
                deleteDir(schemeDump);
            }
        }

        {
            File dataExportDump = Files.createTempDirectory("psql_export_data_dump").toFile();
            try {
                process
                    .exportSchemeToFileWithArgs(dataExportDump,
                        "--no-privileges",
                        "--no-owner",
                        "--format",
                        "directory");
                final File[] filesInDumpDir = dataExportDump.listFiles();
                assertNotNull(filesInDumpDir);
                assertNotEquals(0, filesInDumpDir.length);
                for (final File file : filesInDumpDir) {
                    assertNotEquals(0, file.length());
                }
            } finally {
                deleteDir(dataExportDump);
            }
        }
    }
}