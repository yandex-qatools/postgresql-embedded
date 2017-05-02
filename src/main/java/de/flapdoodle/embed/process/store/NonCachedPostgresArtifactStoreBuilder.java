package de.flapdoodle.embed.process.store;

public class NonCachedPostgresArtifactStoreBuilder extends PostgresArtifactStoreBuilder {

    @Override
    public IArtifactStore build() {
        return new PostgresArtifactStore(get(DOWNLOAD_CONFIG), get(TEMP_DIR_FACTORY), get(EXECUTABLE_NAMING), get(DOWNLOADER));
    }
}
