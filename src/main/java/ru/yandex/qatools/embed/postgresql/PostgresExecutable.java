package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import de.flapdoodle.embed.process.runtime.ProcessControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * postgres executable
 */
public class PostgresExecutable extends AbstractPGExecutable<PostgresConfig, PostgresProcess> {
    public PostgresExecutable(Distribution distribution,
                              PostgresConfig config, IRuntimeConfig runtimeConfig, IExtractedFileSet exe) {
        super(distribution, config, runtimeConfig, exe);
    }

    @Override
    protected PostgresProcess start(Distribution distribution, PostgresConfig config, IRuntimeConfig runtime)
            throws IOException {
        addShutdownHook(runtime, distribution);
        return new PostgresProcess(distribution, config, runtime, this);
    }

    private void addShutdownHook(IRuntimeConfig runtimeConfig, Distribution distribution) throws IOException {
        ProcessControl.addShutdownHook(
                new CleanerRunner(runtimeConfig.getArtifactStore().extractFileSet(distribution).baseDir())
        );
    }

    static class CleanerRunner implements Runnable {

        private File fileOrDirectory;

        CleanerRunner(File fileOrDirectory) {
            this.fileOrDirectory = fileOrDirectory;
        }

        @Override
        public void run() {
            DirectoryCleaner.getInstance().clean(this.fileOrDirectory);
        }
    }

    static class DirectoryCleaner {
        private static Logger logger = LoggerFactory.getLogger(DirectoryCleaner.class);
        private static final DirectoryCleaner instance = new DirectoryCleaner();

        static DirectoryCleaner getInstance() {
            return instance;
        }

        void clean(File cleanupTarget) {
            synchronized (instance) {
                if (!cleanupTarget.exists()) {
                    return;
                }

                try (Stream<Path> stream = Files.walk(cleanupTarget.toPath())) {
                    stream.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .filter(File::exists)
                            .forEach(File::delete);
                } catch (IOException e) {
                    logger.warn("Could not delete {}.", cleanupTarget.getAbsolutePath(), e);
                }
            }
        }
    }
}
