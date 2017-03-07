# Embedded PostgreSQL Server
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/ru.yandex.qatools.embed/postgresql-embedded/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.yandex.qatools.embed/postgresql-embedded)
[![Build status](https://travis-ci.org/yandex-qatools/postgresql-embedded.svg?branch=master)](https://travis-ci.org/yandex-qatools/postgresql-embedded/)
[![covarage](https://img.shields.io/sonar/http/sonar.qatools.ru/ru.yandex.qatools.embed:postgresql-embedded/coverage.svg?style=flat)](http://sonar.qatools.ru/dashboard/index/784)

Embedded PostgreSQL server provides a platform neutral way for running postgres binaries in unittests.
This library is based on [Flapdoodle OSS's embed process](https://github.com/flapdoodle-oss/de.flapdoodle.embed.process). 

Please consider using the [embedded-services](https://github.com/yandex-qatools/embedded-services) project as well.

## Motivation

* It's much easier than installing specific version manually
* You can choose the version right from the code
* You can start your development environment with the PostgreSQL embedded with the single command

### Maven

Add the following dependency to your pom.xml:
```xml
    <dependency>
        <groupId>ru.yandex.qatools.embed</groupId>
        <artifactId>postgresql-embedded</artifactId>
        <version>1.22</version>
    </dependency>
```
### Gradle

Add a line to build.gradle:
```groovy
    compile 'ru.yandex.qatools.embed:postgresql-embedded:1.22'
```

## Howto

Here is the example of how to launch and use the embedded PostgreSQL instance
```java
    // define of retrieve db name and credentials
    final String name = "yourDbname";
    final String username = "yourUser";
    final String password = "youPassword";

    // starting Postgres
    final PostgresStarter<PostgresExecutable, PostgresProcess> runtime = PostgresStarter.getDefaultInstance();
    final PostgresConfig config = PostgresConfig.defaultWithDbName(name, username, password);
    // pass info regarding encoding, locale, collate, ctype, instead of setting global environment settings
    config.getAdditionalInitDbParams().addAll(asList(
        "-E", "UTF-8",
        "--locale=en_US.UTF-8",
        "--lc-collate=en_US.UTF-8",
        "--lc-ctype=en_US.UTF-8"
    ));
    PostgresExecutable exec = runtime.prepare(config);
    PostgresProcess process = exec.start();
    
    // connecting to a running Postgres
    String url = format("jdbc:postgresql://%s:%s/%s?currentSchema=public&user=%s&password=%s",
            config.net().host(),
            config.net().port(),
            config.storage().dbName(),
            config.credentials().username(),
            config.credentials().password()
    );
    Connection conn = DriverManager.getConnection(url);
    
    // feeding up the database
    conn.createStatement().execute("CREATE TABLE films (code char(5));");
    conn.createStatement().execute("INSERT INTO films VALUES ('movie');");

    // ... or you can execute SQL files...
    //pgProcess.importFromFile(new File("someFile.sql"))
    // ... or even SQL files with PSQL variables in them...
    //pgProcess.importFromFileWithArgs(new File("someFile.sql"), "-v", "tblName=someTable")
    
    // performing some assertions
    final Statement statement = conn.createStatement();
    assertThat(statement.execute("SELECT * FROM films;"), is(true));
    assertThat(statement.getResultSet().next(), is(true));

    // close db connection
    conn.close();

    // stop Postgres
    process.stop();
```

### How to use avoid archive extraction on every run

You can specify the cached artifact store to avoid archives downloading and extraction (in case if a directory remains on every run)
```java
final Command cmd = Command.Postgres;
// the cached directory should contain pgsql folder
final FixedPath cachedDir = new FixedPath("/path/to/my/extracted/postgres");
IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder().defaults(cmd)
                                    .artifactStore(new CachedArtifactStoreBuilder()
                                            .defaults(cmd)
                                            .tempDir(cachedDir)
                                            .download(new DownloadConfigBuilder()
                                                    .defaultsForCommand(cmd)
                                                    .packageResolver(new PackagePaths(cmd, cachedDir))
                                                    .build()))
                                    .build();
```

### How to configure logging

Just configure your own `slf4j` appenders. Here is the example of typical `src/test/resources/log4j.properties` file:

```java
# suppress inspection "UnusedProperty" for whole file
log4j.rootLogger=DEBUG, stdout

# reduce logging for postgresql-embedded
log4j.logger.ru.yandex.qatools.embed=INFO
log4j.logger.de.flapdoodle.embed=INFO

# Direct log messages to stdout
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.Target=System.out
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n
log4j.throwableRenderer=org.apache.log4j.EnhancedThrowableRenderer
```

### How to use your custom version of PostgreSQL

Pass the required `IVersion` interface implementation as a first argument of the `PostgresConfig` object:

```java
final PostgresConfig config =  new PostgresConfig(() -> (IS_OS_WINDOWS) ? "9.6.2-2" : "9.6.2-1", ...);
```

### Important Notes
* PostgreSQL server is known to not start under the privileged user (which means you cannot start it under root/Administrator of your system):  

> `initdb must be run as the user that will own the server process, because the server needs to have access to the files and directories that initdb creates. Since the server cannot be run as root, you must not run initdb as root either. (It will in fact refuse to do so.)` 
  ([link](http://www.postgresql.org/docs/9.5/static/app-initdb.html)).   
  
  However some users have launched it successfully on Windows under Administrator, so you can try anyway. 
  
* It is no longer required to set up the LANG environment variable within your system, just pass that config as additionalInitDbParams.

### Supported Versions
Versions: 9.6.2, 9.5.0, 9.4.4, 9.4.1, 9.3.5, 9.2.4, any custom
Support for Linux, Windows and MacOSX.

