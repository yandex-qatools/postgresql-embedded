package ru.yandex.qatools.embed.postgresql.distribution;

import de.flapdoodle.embed.process.distribution.IVersion;

/**
 * PostgreSQL Version enum
 */
public enum Version implements IVersion {

    /**
     * 9.3.5 release
     */
    V9_3_5("9.3.5-1"),
    @Deprecated
    V9_2_4("9.2.4-1"),
    ;

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

    public static enum Main implements IVersion {
        @Deprecated
        V9_2(V9_2_4),
        /**
         * latest production release
         */
        V9_3(V9_3_5),

        PRODUCTION(V9_3_5);

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