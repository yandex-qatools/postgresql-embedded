package ru.yandex.qatools.embed.postgresql.config;

import de.flapdoodle.embed.process.builder.IProperty;
import de.flapdoodle.embed.process.builder.TypedProperty;
import de.flapdoodle.embed.process.config.store.DownloadConfigBuilder;
import de.flapdoodle.embed.process.config.store.DownloadPath;
import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.config.store.IDownloadPath;
import de.flapdoodle.embed.process.config.store.IPackageResolver;
import de.flapdoodle.embed.process.config.store.IProxyFactory;
import de.flapdoodle.embed.process.config.store.ITimeoutConfig;
import de.flapdoodle.embed.process.extract.ITempNaming;
import de.flapdoodle.embed.process.extract.UUIDTempNaming;
import de.flapdoodle.embed.process.io.directories.IDirectory;
import de.flapdoodle.embed.process.io.directories.UserHome;
import de.flapdoodle.embed.process.io.progress.IProgressListener;
import de.flapdoodle.embed.process.io.progress.StandardConsoleProgressListener;
import ru.yandex.qatools.embed.postgresql.Command;
import ru.yandex.qatools.embed.postgresql.PackagePaths;
import ru.yandex.qatools.embed.postgresql.ext.SubdirTempDir;


/**
 * Download config builder for postgres
 */
public class PostgresDownloadConfigBuilder extends DownloadConfigBuilder {
    private static final TypedProperty<UserAgent> USER_AGENT = TypedProperty.with("UserAgent", UserAgent.class);
    private static final TypedProperty<IProgressListener> PROGRESS_LISTENER = TypedProperty.with("ProgressListener", IProgressListener.class);
    private static final TypedProperty<ITempNaming> FILE_NAMING = TypedProperty.with("FileNaming", ITempNaming.class);
    private static final TypedProperty<IDirectory> ARTIFACT_STORE_PATH = TypedProperty.with("ArtifactStorePath", IDirectory.class);
    private static final TypedProperty<IPackageResolver> PACKAGE_RESOLVER = TypedProperty.with("PackageResolver", IPackageResolver.class);
    private static final TypedProperty<DownloadPrefix> DOWNLOAD_PREFIX = TypedProperty.with("DownloadPrefix", DownloadPrefix.class);
    private static final TypedProperty<IDownloadPath> DOWNLOAD_PATH = TypedProperty.with("DownloadPath", IDownloadPath.class);

    private static final TypedProperty<ITimeoutConfig> TIMEOUT_CONFIG = TypedProperty.with("TimeoutConfig", ITimeoutConfig.class);
    private static final TypedProperty<IProxyFactory> PROXY_FACTORY = TypedProperty.with("ProxyFactory", IProxyFactory.class);

    public PostgresDownloadConfigBuilder defaultsForCommand(Command command) {
        fileNaming().setDefault(new UUIDTempNaming());
        // I've found the only open and easy to use cross platform binaries
        downloadPath().setDefault(new DownloadPath("http://get.enterprisedb.com/postgresql/"));
        packageResolver().setDefault(new PackagePaths(command, SubdirTempDir.defaultInstance()));
        artifactStorePath().setDefault(new UserHome(".embedpostgresql"));
        downloadPrefix().setDefault(new DownloadPrefix("postgresql-download"));
        userAgent().setDefault(new UserAgent("Mozilla/5.0 (compatible; Embedded postgres; +https://github.com/yandex-qatools)"));
        progressListener().setDefault(new StandardConsoleProgressListener() {
            @Override
            public void info(String label, String message) {
                if (label.startsWith("Extract")) {
                    System.out.print(".");//NOSONAR
                } else {
                    super.info(label, message);//NOSONAR
                }
            }
        });
        return this;
    }

    @Override
    public IDownloadConfig build() {
        final IDownloadPath downloadPath = get(DOWNLOAD_PATH);
        final String downloadPrefix = get(DOWNLOAD_PREFIX).value();
        final IPackageResolver packageResolver = get(PACKAGE_RESOLVER);
        final IDirectory artifactStorePath = get(ARTIFACT_STORE_PATH);
        final ITempNaming fileNaming = get(FILE_NAMING);
        final IProgressListener progressListener = get(PROGRESS_LISTENER);
        final String userAgent = get(USER_AGENT).value();
        final ITimeoutConfig timeoutConfig = get(TIMEOUT_CONFIG);
        final IProxyFactory proxyFactory = get(PROXY_FACTORY);

        return new MutableDownloadConfig(downloadPath, downloadPrefix, packageResolver, artifactStorePath, fileNaming,
                progressListener, userAgent, timeoutConfig, proxyFactory);
    }

    @Override
    public DownloadConfigBuilder downloadPath(String path) {
        set(DOWNLOAD_PATH, new DownloadPath(path));
        return this;
    }

    @Override
    protected IProperty<IDownloadPath> downloadPath() {
        return property(DOWNLOAD_PATH);
    }

    @Override
    public DownloadConfigBuilder downloadPrefix(String prefix) {
        set(DOWNLOAD_PREFIX, new DownloadPrefix(prefix));
        return this;
    }

    @Override
    protected IProperty<DownloadPrefix> downloadPrefix() {
        return property(DOWNLOAD_PREFIX);
    }

    @Override
    public DownloadConfigBuilder packageResolver(IPackageResolver packageResolver) {
        set(PACKAGE_RESOLVER, packageResolver);
        return this;
    }

    @Override
    protected IProperty<IPackageResolver> packageResolver() {
        return property(PACKAGE_RESOLVER);
    }

    @Override
    public DownloadConfigBuilder artifactStorePath(IDirectory artifactStorePath) {
        set(ARTIFACT_STORE_PATH, artifactStorePath);
        return this;
    }

    @Override
    protected IProperty<IDirectory> artifactStorePath() {
        return property(ARTIFACT_STORE_PATH);
    }

    @Override
    public DownloadConfigBuilder fileNaming(ITempNaming fileNaming) {
        set(FILE_NAMING, fileNaming);
        return this;
    }

    @Override
    protected IProperty<ITempNaming> fileNaming() {
        return property(FILE_NAMING);
    }

    @Override
    public DownloadConfigBuilder progressListener(IProgressListener progressListener) {
        set(PROGRESS_LISTENER, progressListener);
        return this;
    }

    @Override
    protected IProperty<IProgressListener> progressListener() {
        return property(PROGRESS_LISTENER);
    }

    @Override
    public DownloadConfigBuilder userAgent(String userAgent) {
        set(USER_AGENT, new UserAgent(userAgent));
        return this;
    }

    @Override
    protected IProperty<UserAgent> userAgent() {
        return property(USER_AGENT);
    }

    @Override
    public DownloadConfigBuilder timeoutConfig(ITimeoutConfig timeoutConfig) {
        set(TIMEOUT_CONFIG, timeoutConfig);
        return this;
    }

    @Override
    protected IProperty<ITimeoutConfig> timeoutConfig() {
        return property(TIMEOUT_CONFIG);
    }

    @Override
    public DownloadConfigBuilder proxyFactory(IProxyFactory proxyFactory) {
        set(PROXY_FACTORY, proxyFactory);
        return this;
    }

    @Override
    protected IProperty<IProxyFactory> proxyFactory() {
        return property(PROXY_FACTORY);
    }
}
