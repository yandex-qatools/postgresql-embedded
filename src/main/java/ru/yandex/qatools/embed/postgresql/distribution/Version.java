package ru.yandex.qatools.embed.postgresql.distribution;

import de.flapdoodle.embed.process.distribution.IVersion;

/**
 * PostgreSQL Version enum
 */
public enum Version implements IVersion {
    /**
     * 11 for Mac OS X and Windows x86-64 only because EnterpriseDB reduced the
     * <a href="https://www.enterprisedb.com/docs/en/11.0/PG_Inst_Guide_v11/PostgreSQL_Installation_Guide.1.04.html">supported platforms</a>
     * on their
     * <a href="https://www.enterprisedb.com/downloads/postgres-postgresql-downloads">binary download site</a>.
     */
    V11_2("11.2-1"),
    V10_7("10.7-1"),
    V9_6_12("9.6.12-1"),
    @Deprecated V9_5_16("9.5.16-1"),;

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
        @Deprecated V9_5(V9_5_16),
        V9_6(V9_6_12),
        V10(V10_7),
        PRODUCTION(V10_7),
        /**
         * 11 for Mac OS X and Windows x86-64 only because EnterpriseDB reduced the
         * <a href="https://www.enterprisedb.com/docs/en/11.0/PG_Inst_Guide_v11/PostgreSQL_Installation_Guide.1.04.html">supported platforms</a>
         * on their
         * <a href="https://www.enterprisedb.com/downloads/postgres-postgresql-downloads">binary download site</a>.
         */
        V11(V11_2);

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
