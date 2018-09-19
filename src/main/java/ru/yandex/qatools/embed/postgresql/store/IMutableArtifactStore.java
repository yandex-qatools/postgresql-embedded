package ru.yandex.qatools.embed.postgresql.store;

import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.store.IArtifactStore;

public interface IMutableArtifactStore extends IArtifactStore {
    void setDownloadConfig(IDownloadConfig downloadConfig);
}
