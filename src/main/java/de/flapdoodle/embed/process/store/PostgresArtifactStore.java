package de.flapdoodle.embed.process.store;

import de.flapdoodle.embed.process.config.store.FileSet;
import de.flapdoodle.embed.process.config.store.FileType;
import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.config.store.IPackageResolver;
import de.flapdoodle.embed.process.distribution.ArchiveType;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.Extractors;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import de.flapdoodle.embed.process.extract.IExtractor;
import de.flapdoodle.embed.process.extract.ITempNaming;
import de.flapdoodle.embed.process.io.directories.IDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import static org.apache.commons.io.FileUtils.deleteQuietly;

/**
 * @author Ilya Sadykov
 * Hacky ArtifactStore. Just to override the default FilesToExtract with PostgresFilesToExtract
 */
public class PostgresArtifactStore implements IMutableArtifactStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresArtifactStore.class);
    private IDownloadConfig downloadConfig;
    private IDirectory tempDirFactory;
    private ITempNaming executableNaming;
    private IDownloader downloader;

    PostgresArtifactStore(IDownloadConfig downloadConfig, IDirectory tempDirFactory, ITempNaming executableNaming, IDownloader downloader) {
        this.downloadConfig = downloadConfig;
        this.tempDirFactory = tempDirFactory;
        this.executableNaming = executableNaming;
        this.downloader = downloader;
    }

    public IDirectory getTempDir() {
        return tempDirFactory;
    }

    @Override
    public void removeFileSet(Distribution distribution, IExtractedFileSet all) {
        for (FileType type : EnumSet.complementOf(EnumSet.of(FileType.Executable))) {
            for (File file : all.files(type)) {
                if (file.exists() && !deleteQuietly(file))
                    LOGGER.trace("Could not delete {} NOW: {}", type, file);
            }
        }
        File exe = all.executable();
        if (exe.exists() && !deleteQuietly(exe)) {
            LOGGER.trace("Could not delete executable NOW: {}", exe);
        }

        if (all.baseDirIsGenerated() && !deleteQuietly(all.baseDir())) {
            LOGGER.trace("Could not delete generatedBaseDir: {}", all.baseDir());
        }
    }

    @Override
    public boolean checkDistribution(Distribution distribution) throws IOException {
        if (!LocalArtifactStore.checkArtifact(downloadConfig, distribution)) {
            return LocalArtifactStore.store(downloadConfig, distribution, downloader.download(downloadConfig, distribution));
        }
        return true;
    }

    public IDownloadConfig getDownloadConfig() {
        return downloadConfig;
    }

    @Override
    public void setDownloadConfig(IDownloadConfig downloadConfig) {
        this.downloadConfig = downloadConfig;
    }

    @Override
    public IExtractedFileSet extractFileSet(Distribution distribution) throws IOException {
        IPackageResolver packageResolver = downloadConfig.getPackageResolver();
        File artifact = getArtifact(downloadConfig, distribution);
        final ArchiveType archiveType = packageResolver.getArchiveType(distribution);
        IExtractor extractor = Extractors.getExtractor(archiveType);
        try {
            final FileSet fileSet = packageResolver.getFileSet(distribution);
            return extractor.extract(downloadConfig, artifact,
                    new PostgresFilesToExtract(tempDirFactory, executableNaming, fileSet, distribution));
        } catch (Exception e) {
            LOGGER.error("Failed to extract file set:", e);
            return new EmptyFileSet();
        }
    }

    private File getArtifact(IDownloadConfig runtime, Distribution distribution) {
        File dir = createOrGetBaseDir(runtime);
        File artifactFile = new File(dir, runtime.getPackageResolver().getPath(distribution));
        if ((artifactFile.exists()) && (artifactFile.isFile()))
            return artifactFile;
        return null;
    }

    private File createOrGetBaseDir(IDownloadConfig runtime) {
        File dir = runtime.getArtifactStorePath().asFile();
        createOrCheckDir(dir);
        return dir;
    }

    private void createOrCheckDir(File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalArgumentException("Could NOT create Directory " + dir);
        }
        if (!dir.isDirectory())
            throw new IllegalArgumentException("" + dir + " is not a Directory");
    }
}
