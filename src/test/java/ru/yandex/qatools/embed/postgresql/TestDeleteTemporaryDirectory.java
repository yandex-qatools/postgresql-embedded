package ru.yandex.qatools.embed.postgresql;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;

@RunWith(MockitoJUnitRunner.class)
public class TestDeleteTemporaryDirectory {

    private static final Logger logger = LoggerFactory.getLogger(TestDeleteTemporaryDirectory.class);
    private Path testDirectory;

    @Before
    public void setup() {
        testDirectory = Paths.get(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
    }

    @After
    public void cleanup() {
        if (!testDirectory.toFile().exists()) {
            return;
        }

        try (Stream<Path> stream = Files.walk(testDirectory)) {
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .filter(File::exists)
                    .forEach(File::delete);
        } catch (IOException e) {
            logger.warn("Could not delete {}.", testDirectory.toFile().getAbsolutePath(), e);
        }
    }

    @Test
    public void callCleanMethodWhenCleanerRunnerIsRun() {
        // Mock DirectoryCleaner
        PostgresExecutable.DirectoryCleaner cleaner = Mockito.spy(PostgresExecutable.DirectoryCleaner.class);
        Mockito.doNothing().when(cleaner).clean(Mockito.any(File.class));

        // Run CleanerRunner
        PostgresExecutable.CleanerRunner runner =
                Mockito.spy(new PostgresExecutable.CleanerRunner(testDirectory.toFile()));
        Mockito.doReturn(cleaner).when(runner).getCleaner();
        runner.run();

        // Clean method was called by CleanerRunner
        Mockito.verify(cleaner, Mockito.times(1)).clean(testDirectory.toFile());
    }

    @Test
    public void directoryCleanerDeleteFilesInDirectoryAndItself() throws IOException {
        createTestDirectories(testDirectory);

        // Run clean method
        new PostgresExecutable.DirectoryCleaner().clean(testDirectory.toFile());

        // Removed test directory
        assertFalse("Directory should be removed. " + testDirectory, testDirectory.toFile().exists());
    }

    private void createTestDirectories(Path root) throws IOException {
        // foo/
        //   ├── file1
        //   ├── file2
        //   ├── bar/
        //   │   ├── fileA
        //   │   └── fileB
        //   └── baz/
        Path foo = Paths.get(root.toFile().getAbsolutePath(), "foo");
        Path bar = Paths.get(foo.toFile().getAbsolutePath(), "bar");
        Path baz = Paths.get(foo.toFile().getAbsolutePath(), "baz");
        Files.createDirectories(foo);
        Files.createDirectories(bar);
        Files.createDirectories(baz);

        Files.createFile(Paths.get(foo.toFile().getAbsolutePath(), "file1"));
        Files.createFile(Paths.get(foo.toFile().getAbsolutePath(), "file2"));
        Files.createFile(Paths.get(bar.toFile().getAbsolutePath(), "fileA"));
        Files.createFile(Paths.get(bar.toFile().getAbsolutePath(), "fileB"));
    }
}
