package ru.yandex.qatools.embed.postgresql.config;

import de.flapdoodle.embed.process.config.store.IDownloadPath;
import de.flapdoodle.embed.process.config.store.IPackageResolver;
import de.flapdoodle.embed.process.config.store.IProxyFactory;
import de.flapdoodle.embed.process.config.store.ITimeoutConfig;
import de.flapdoodle.embed.process.extract.ITempNaming;
import de.flapdoodle.embed.process.io.directories.IDirectory;
import de.flapdoodle.embed.process.io.progress.IProgressListener;

public class MutableDownloadConfig implements IMutableDownloadConfig {

    private final IDownloadPath downloadPath;
    private final IProgressListener progressListener;
    private final IDirectory artifactStorePath;
    private final ITempNaming fileNaming;
    private final String downloadPrefix;
    private final String userAgent;
    private final ITimeoutConfig timeoutConfig;
    private final IProxyFactory proxyFactory;
    private IPackageResolver packageResolver;

    public MutableDownloadConfig(IDownloadPath downloadPath, String downloadPrefix, IPackageResolver packageResolver,//NOSONAR
                                 IDirectory artifactStorePath, ITempNaming fileNaming, IProgressListener progressListener, String userAgent,//NOSONAR
                                 ITimeoutConfig timeoutConfig, IProxyFactory proxyFactory) { //NOSONAR
        super();
        this.downloadPath = downloadPath;
        this.downloadPrefix = downloadPrefix;
        this.packageResolver = packageResolver;
        this.artifactStorePath = artifactStorePath;
        this.fileNaming = fileNaming;
        this.progressListener = progressListener;
        this.userAgent = userAgent;
        this.timeoutConfig = timeoutConfig;
        this.proxyFactory = proxyFactory;
    }

    @Override
    public IDownloadPath getDownloadPath() {
        return downloadPath;
    }

    @Override
    public IProgressListener getProgressListener() {
        return progressListener;
    }

    @Override
    public IDirectory getArtifactStorePath() {
        return artifactStorePath;
    }

    @Override
    public ITempNaming getFileNaming() {
        return fileNaming;
    }

    @Override
    public String getDownloadPrefix() {
        return downloadPrefix;
    }

    @Override
    public String getUserAgent() {
        return userAgent;
    }

    @Override
    public IPackageResolver getPackageResolver() {
        return packageResolver;
    }

    @Override
    public void setPackageResolver(IPackageResolver packageResolver) {
        this.packageResolver = packageResolver;
    }

    @Override
    public ITimeoutConfig getTimeoutConfig() {
        return timeoutConfig;
    }

    @Override
    public IProxyFactory proxyFactory() {
        return proxyFactory;
    }
}