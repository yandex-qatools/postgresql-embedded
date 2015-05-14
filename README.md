# Embedded PostgreSQL Server
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/ru.yandex.qatools.embed/postgresql-embedded/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.yandex.qatools.embed/postgresql-embedded) [![covarage](https://img.shields.io/sonar/http/sonar.qatools.ru/ru.yandex.qatools.embed:postgresql-embedded/coverage.svg?style=flat)](http://sonar.qatools.ru/dashboard/index/784)

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
        <version>1.3</version>
    </dependency>
```
## Howto

Here is the example of how to launch and use the embedded PostgreSQL instance
```java

    // starting Postgres
    PostgresStarter<PostgresExecutable, PostgresProcess> runtime = PostgresStarter.getDefaultInstance();
    final PostgresConfig config = PostgresConfig.defaultWithDbName("test");
    PostgresExecutable exec = runtime.prepare(config);
    PostgresProcess process = exec.start();
    
    // connecting to a running Postgres
    String url = format("jdbc:postgresql://%s:%s/%s?user=%s&password=%s",
            config.net().getServerAddress().getHostAddress(),
            config.net().port(),
            config.storage().dbName()
    );
    Connection conn = DriverManager.getConnection(url);
    
    // feeding up the database
    conn.createStatement().execute("CREATE TABLE films (code char(5));");
    conn.createStatement().execute("INSERT INTO films VALUES ('movie');");
    
    // performing some assertions
    final Statement statement = conn.createStatement();
    assertThat(statement.execute("SELECT * FROM films;"), is(true));
    assertThat(statement.getResultSet().next(), is(true));
                
    // stopping Postgres
    conn.close();
    process.stop();
```

### Supported Versions

Versions: 9.4.1, 9.3.5, 9.2.4, any custom
Support for Linux, Windows and MacOSX.

