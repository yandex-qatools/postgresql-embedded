package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.distribution.BitSize;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.distribution.IVersion;
import de.flapdoodle.embed.process.distribution.Platform;
import de.flapdoodle.embed.process.store.IArtifactStore;
import ru.yandex.qatools.embed.postgresql.store.PostgresArtifactStoreBuilder;
import org.junit.Test;
import ru.yandex.qatools.embed.postgresql.distribution.Version;

import java.io.IOException;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TestDownloads {

    /** Version 11 binary downloads are available for OS X and Windows 64 bit only */
    private boolean supported(Distribution distribution) {
        if (! distribution.getVersion().asInDownloadPath().startsWith("11.")) {
            return true;
        }
        switch (distribution.getPlatform()) {
        case OS_X:
            return true;
        case Windows:
            return distribution.getBitsize() == BitSize.B64;
        default:
            return false;
        }
    }

    @Test
    public void testDownloads() throws IOException {
        IArtifactStore artifactStore = new PostgresArtifactStoreBuilder().defaults(Command.Postgres).build();

        for (Platform p : asList(Platform.OS_X, Platform.Linux, Platform.Windows)) {
            for (BitSize b : BitSize.values()) {
                for (IVersion version : Version.Main.values()) {
                    Distribution distribution = new Distribution(version, p, b);
                    if (! supported(distribution)) {
                        continue;
                    }
                    assertThat("Distribution: " + distribution + " should be accessible", artifactStore.checkDistribution(distribution), is(true));
                }
            }
        }
    }
}
