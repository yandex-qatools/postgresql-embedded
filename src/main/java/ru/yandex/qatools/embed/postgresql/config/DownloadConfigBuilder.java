package ru.yandex.qatools.embed.postgresql.config;

import de.flapdoodle.embed.process.config.store.DownloadPath;
import de.flapdoodle.embed.process.extract.UUIDTempNaming;
import de.flapdoodle.embed.process.io.directories.UserHome;
import de.flapdoodle.embed.process.io.progress.StandardConsoleProgressListener;
import ru.yandex.qatools.embed.postgresql.Command;
import ru.yandex.qatools.embed.postgresql.PackagePaths;
import ru.yandex.qatools.embed.postgresql.ext.SubdirTempDir;


/**
 * Download config builder for postgres
 */
public class DownloadConfigBuilder extends de.flapdoodle.embed.process.config.store.DownloadConfigBuilder {

    public DownloadConfigBuilder defaultsForCommand(Command command) {
        fileNaming().setDefault(new UUIDTempNaming());
        // I've found the only open and easy to use cross platform binaries
        downloadPath().setDefault(new DownloadPath("http://get.enterprisedb.com/postgresql/"));
        packageResolver().setDefault(new PackagePaths(command, SubdirTempDir.defaultInstance()));
        artifactStorePath().setDefault(new UserHome(".embedpostgresql"));
        downloadPrefix().setDefault(new DownloadPrefix("posgresql-download"));
        userAgent().setDefault(new UserAgent("Mozilla/5.0 (compatible; Embedded postgres; +https://github.com/yandex-qatools)"));
        progressListener().setDefault(new StandardConsoleProgressListener() {
            @Override
            public void info(String label, String message) {
                if(label.startsWith("Extract")){
                    System.out.print(".");//NOSONAR
                } else {
                    super.info(label, message);//NOSONAR
                }
            }
        });
        return this;
    }

}
