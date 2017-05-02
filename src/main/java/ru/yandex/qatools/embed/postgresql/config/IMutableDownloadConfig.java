package ru.yandex.qatools.embed.postgresql.config;

import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.config.store.IPackageResolver;

public interface IMutableDownloadConfig extends IDownloadConfig {
    void setPackageResolver(IPackageResolver packageResolver);
}
