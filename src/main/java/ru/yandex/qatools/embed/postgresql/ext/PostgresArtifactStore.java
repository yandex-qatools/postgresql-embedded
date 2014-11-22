package ru.yandex.qatools.embed.postgresql.ext;

import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.config.store.IPackageResolver;
import de.flapdoodle.embed.process.distribution.ArchiveType;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.*;
import de.flapdoodle.embed.process.io.directories.IDirectory;
import de.flapdoodle.embed.process.store.ArtifactStore;
import de.flapdoodle.embed.process.store.IDownloader;

import java.io.File;
import java.io.IOException;

/**
 * @author Ilya Sadykov
 * Hacky ArtifactStore. Just to override the default FilesToExtract with PostgresFilesToExtract
 */
public class PostgresArtifactStore extends ArtifactStore {
    private IDownloadConfig _downloadConfig;
    private IDirectory _tempDirFactory;
    private ITempNaming _executableNaming;

    public PostgresArtifactStore(IDownloadConfig downloadConfig, IDirectory tempDirFactory, ITempNaming executableNaming, IDownloader downloader) {
        super(downloadConfig, tempDirFactory, executableNaming, downloader);
        _downloadConfig = downloadConfig;
        _tempDirFactory = tempDirFactory;
        _executableNaming = executableNaming;
    }

    public IDirectory getTempDir() {
        return _tempDirFactory;
    }

    private static File getArtifact(IDownloadConfig runtime, Distribution distribution) {
        File dir = createOrGetBaseDir(runtime);
        File artifactFile = new File(dir, runtime.getPackageResolver().getPath(distribution));
        if ((artifactFile.exists()) && (artifactFile.isFile()))
            return artifactFile;
        return null;
    }

    private static File createOrGetBaseDir(IDownloadConfig runtime) {
        File dir = runtime.getArtifactStorePath().asFile();
        createOrCheckDir(dir);
        return dir;
    }

    private static void createOrCheckDir(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs())
                throw new IllegalArgumentException("Could NOT create Directory " + dir);
        }
        if (!dir.isDirectory())
            throw new IllegalArgumentException("" + dir + " is not a Directory");
    }

    /**
     * Actually this entirely class does exist because of this method only!
     * TODO: Look for the more native way to override the default FilesToExtract strategy
     */
    @Override
    public IExtractedFileSet extractFileSet(Distribution distribution) throws IOException {
        IPackageResolver packageResolver = _downloadConfig.getPackageResolver();
        File artifact = getArtifact(_downloadConfig, distribution);
        final ArchiveType archiveType = packageResolver.getArchiveType(distribution);
        IExtractor extractor = Extractors.getExtractor(archiveType);

        return extractor.extract(_downloadConfig, artifact,
                new PostgresFilesToExtract(_tempDirFactory, _executableNaming, packageResolver.getFileSet(distribution)));
    }
}
