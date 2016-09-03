package ru.yandex.qatools.embed.postgresql.config;

import de.flapdoodle.embed.process.config.store.DownloadPath;
import de.flapdoodle.embed.process.extract.UUIDTempNaming;
import de.flapdoodle.embed.process.io.directories.UserHome;
import de.flapdoodle.embed.process.io.progress.LoggingProgressListener;
import ru.yandex.qatools.embed.postgresql.Command;
import ru.yandex.qatools.embed.postgresql.PackagePaths;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Download config builder for postgres
 */
public class DownloadConfigBuilder extends de.flapdoodle.embed.process.config.store.DownloadConfigBuilder {

    public DownloadConfigBuilder defaultsForCommand(Command command) {
        fileNaming().setDefault(new UUIDTempNaming());
        // I've found the only open and easy to use cross platform binaries
        downloadPath().setDefault(new DownloadPath("http://get.enterprisedb.com/postgresql/"));
        packageResolver().setDefault(new PackagePaths(command));
        artifactStorePath().setDefault(new UserHome(".embedpostgresql"));
        downloadPrefix().setDefault(new DownloadPrefix("posgresql-download"));
        userAgent().setDefault(new UserAgent("Mozilla/5.0 (compatible; Embedded postgres; +https://github.com/yandex-qatools)"));
        progressListener().setDefault(new LoggingProgressListener(Logger.getLogger(getClass().getName()), Level.INFO));
        return this;
    }

}
