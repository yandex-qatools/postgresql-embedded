package ru.yandex.qatools.embed.postgresql.ext;

import de.flapdoodle.embed.process.io.directories.PropertyOrPlatformTempDir;

import java.io.File;
import java.io.IOException;

import static de.flapdoodle.embed.process.io.file.Files.createTempDir;

/**
 * @author Ilya Sadykov
 * Temporary dir creating the temp dir inside of the system temp dir.
 * TODO: Be careful: the path is ThreadLocal. This might lead to some side effects
 */
public class SubdirTempDir extends PropertyOrPlatformTempDir {

    private static final ThreadLocal<File> tempDir = new ThreadLocal<>();

    static {
        try {
            String customTempDir = System.getProperty("de.flapdoodle.embed.io.tmpdir");
            if (customTempDir != null) {
                tempDir.set(new File(customTempDir));
            } else {
                tempDir.set(createTempDir(new File(System.getProperty("java.io.tmpdir")), "postgresql-embed"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp dir", e);
        }
    }

    @Override
    public File asFile() {
        return tempDir.get();
    }
}
