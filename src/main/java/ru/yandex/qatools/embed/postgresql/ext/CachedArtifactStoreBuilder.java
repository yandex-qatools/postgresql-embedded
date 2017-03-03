package ru.yandex.qatools.embed.postgresql.ext;

import de.flapdoodle.embed.process.store.IArtifactStore;

public class CachedArtifactStoreBuilder extends ArtifactStoreBuilder {

    @Override
    public IArtifactStore build() {
        return new CachedPostgresArtifactStore(get(DOWNLOAD_CONFIG), get(TEMP_DIR_FACTORY), get(EXECUTABLE_NAMING), get(DOWNLOADER));
    }
}
