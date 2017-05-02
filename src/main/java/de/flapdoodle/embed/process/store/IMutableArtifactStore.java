package de.flapdoodle.embed.process.store;

import de.flapdoodle.embed.process.config.store.IDownloadConfig;

public interface IMutableArtifactStore extends IArtifactStore {
    void setDownloadConfig(IDownloadConfig downloadConfig);
}
