package de.flapdoodle.embed.process.store;

import de.flapdoodle.embed.process.config.store.FileSet;
import de.flapdoodle.embed.process.config.store.FileType;
import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import de.flapdoodle.embed.process.extract.ITempNaming;
import de.flapdoodle.embed.process.extract.ImmutableExtractedFileSet.Builder;
import de.flapdoodle.embed.process.io.directories.IDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static de.flapdoodle.embed.process.config.store.FileType.Executable;
import static de.flapdoodle.embed.process.config.store.FileType.Library;
import static de.flapdoodle.embed.process.extract.ImmutableExtractedFileSet.builder;
import static java.nio.file.Files.exists;
import static java.nio.file.Paths.get;
import static org.apache.commons.io.FileUtils.iterateFiles;
import static org.apache.commons.io.filefilter.TrueFileFilter.TRUE;

public class CachedPostgresArtifactStore extends PostgresArtifactStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(CachedPostgresArtifactStore.class);
    private IDownloadConfig downloadConfig;
    private IDirectory eDir;

    public CachedPostgresArtifactStore(IDownloadConfig downloadConfig, IDirectory eDir,
                                       ITempNaming executableNaming, IDownloader downloader) {
        super(downloadConfig, eDir, executableNaming, downloader);
        this.downloadConfig = downloadConfig;
        this.eDir = eDir;
    }

    @Override
    public void removeFileSet(Distribution distribution, IExtractedFileSet all) {
        // do nothing
    }

    @Override
    public IExtractedFileSet extractFileSet(Distribution distribution) throws IOException {
        try {
            final File dir = this.eDir.asFile();
            final FileSet filesSet = downloadConfig.getPackageResolver().getFileSet(distribution);
            final Path path = get(dir.getPath(),
                                  "pgsql" + "-" + distribution.getVersion().asInDownloadPath(),
                                  "bin");
            if (filesSet.entries().stream().allMatch(entry -> exists(path))
              ) {
                final Builder extracted = builder(dir).baseDirIsGenerated(false);
                iterateFiles(dir, TRUE, TRUE).forEachRemaining(file -> {
                    FileType type = Library;
                    if (filesSet.entries().stream()
                            .anyMatch(entry -> entry.matchingPattern().matcher(file.getPath()).matches())) {
                        type = Executable;
                    }
                    extracted.file(type, file);
                });
                return extracted.build();
            } else {
                return super.extractFileSet(distribution);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to extract file set", e);
            return new EmptyFileSet();
        }
    }
}
