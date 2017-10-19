package ru.yandex.qatools.embed.postgresql.distribution;

import de.flapdoodle.embed.process.distribution.IVersion;

/**
 * PostgreSQL Version enum
 */
public enum Version implements IVersion {
    V10_0("10.0-1"),
    V9_6_5("9.6.5-1"),
    @Deprecated V9_5_7("9.5.7-1"),;

    private final String specificVersion;

    Version(String vName) {
        this.specificVersion = vName;
    }

    @Override
    public String asInDownloadPath() {
        return specificVersion;
    }

    @Override
    public String toString() {
        return "Version{" + specificVersion + '}';
    }

    public enum Main implements IVersion {
        V9_5(V9_5_7),
        V9_6(V9_6_5),
        V10(V10_0),
        PRODUCTION(V10_0);

        private final IVersion _latest;

        Main(IVersion latest) {
            _latest = latest;
        }

        @Override
        public String asInDownloadPath() {
            return _latest.asInDownloadPath();
        }
    }
}
