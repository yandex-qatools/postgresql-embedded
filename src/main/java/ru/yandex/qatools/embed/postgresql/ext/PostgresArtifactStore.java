package ru.yandex.qatools.embed.postgresql.ext;

import de.flapdoodle.embed.process.config.store.FileSet;
import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.config.store.IPackageResolver;
import de.flapdoodle.embed.process.distribution.ArchiveType;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.Extractors;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import de.flapdoodle.embed.process.extract.IExtractor;
import de.flapdoodle.embed.process.extract.ITempNaming;
import de.flapdoodle.embed.process.io.directories.IDirectory;
import de.flapdoodle.embed.process.store.ArtifactStore;
import de.flapdoodle.embed.process.store.IDownloader;

import java.io.File;
import java.io.IOException;

/**
 * @author Ilya Sadykov
 *         Hacky ArtifactStore. Just to override the default FilesToExtract with PostgresFilesToExtract
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
        if (!dir.exists()) {
            if (!dir.mkdirs())
                throw new IllegalArgumentException("Could NOT create Directory " + dir);
        }
        if (!dir.isDirectory())
            throw new IllegalArgumentException("" + dir + " is not a Directory");
    }

    public IDirectory getTempDir() {
        return _tempDirFactory;
    }

    @Override
    public void removeFileSet(Distribution distribution, IExtractedFileSet all) {
        try {
            super.removeFileSet(distribution, all);
        } catch (IllegalArgumentException e) {
            System.err.println("Failed to remove file set: " + e.getMessage());//NOSONAR
        }
    }

    public IDownloadConfig getDownloadConfig() {
        return _downloadConfig;
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
        try {
            final FileSet fileSet = packageResolver.getFileSet(distribution);
            return extractor.extract(_downloadConfig, artifact,
                    new PostgresFilesToExtract(_tempDirFactory, _executableNaming, fileSet));
        } catch (Exception e) {
            e.printStackTrace();//NOSONAR
            System.out.println("Failed to extract file set: " + e.getMessage());//NOSONAR
            return new EmptyFileSet();
        }
    }
}
