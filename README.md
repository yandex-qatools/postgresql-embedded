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
        <version>1.16</version>
    </dependency>
```
### Gradle

Add a line to build.gradle:
```groovy
    compile 'ru.yandex.qatools.embed:postgresql-embedded:1.16'
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

### Important Notes
* PostgreSQL server is known to not start under the privileged user (which means you cannot start it under root/Administrator of your system):  

> `initdb must be run as the user that will own the server process, because the server needs to have access to the files and directories that initdb creates. Since the server cannot be run as root, you must not run initdb as root either. (It will in fact refuse to do so.)` 
  ([link](http://www.postgresql.org/docs/9.5/static/app-initdb.html)).   
  
  However some users have launched it successfully on Windows under Administrator, so you can try anyway. 
  
* It is no longer required to set up the LANG environment variable within your system, just pass that config as additionalInitDbParams.

### Supported Versions
Versions: 9.5.0, 9.4.4, 9.4.1, 9.3.5, 9.2.4, any custom
Support for Linux, Windows and MacOSX.

