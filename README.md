# Embedded PostgreSQL Server
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/ru.yandex.qatools.embed/postgresql-embedded/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.yandex.qatools.embed/postgresql-embedded)
[![Build status](https://travis-ci.org/yandex-qatools/postgresql-embedded.svg?branch=master)](https://travis-ci.org/yandex-qatools/postgresql-embedded/)
[![Windows build status](https://ci.appveyor.com/api/projects/status/00ov87k6fe2euwvo?svg=true)](https://ci.appveyor.com/project/smecsia/postgresql-embedded)

Embedded PostgreSQL server provides a platform neutral way for running postgres binaries in unittests.
This library is based on [Flapdoodle OSS's embed process](https://github.com/flapdoodle-oss/de.flapdoodle.embed.process). 

## Note: this project is not being actively maintained anymore
Sorry for any inconvinience, but this project needs active maintainers. If anyone is interested in becoming the maintainer - please let me ([@smecsia](https://github.com/smecsia)) know.

## Officially recommended alternative
Please be adviced that the main maintainer of this project has successfuly migrated to the use of [Test Containers project](https://www.testcontainers.org/modules/databases/postgres/). This is the best possible alternative nowadays.

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
    <version>2.10</version>
</dependency>
```
### Gradle

Add a line to build.gradle:
```groovy
compile 'ru.yandex.qatools.embed:postgresql-embedded:2.10'
```

## Howto

Here is the example of how to launch and use the embedded PostgreSQL instance
```java
// starting Postgres
final EmbeddedPostgres postgres = new EmbeddedPostgres(V9_6);
// predefined data directory
// final EmbeddedPostgres postgres = new EmbeddedPostgres(V9_6, "/path/to/predefined/data/directory");
final String url = postgres.start("localhost", 5432, "dbName", "userName", "password");

// connecting to a running Postgres and feeding up the database
final Connection conn = DriverManager.getConnection(url);
conn.createStatement().execute("CREATE TABLE films (code char(5));");
conn.createStatement().execute("INSERT INTO films VALUES ('movie');");

// ... or you can execute SQL files...
//postgres.getProcess().importFromFile(new File("someFile.sql"))
// ... or even SQL files with PSQL variables in them...
//postgres.getProcess().importFromFileWithArgs(new File("someFile.sql"), "-v", "tblName=someTable")
// ... or even restore database from dump file
//postgres.getProcess().restoreFromFile(new File("src/test/resources/test.binary_dump"))

// performing some assertions
final Statement statement = conn.createStatement();
assertThat(statement.execute("SELECT * FROM films;"), is(true));
assertThat(statement.getResultSet().next(), is(true));
assertThat(statement.getResultSet().getString("code"), is("movie"));

// close db connection
conn.close();
// stop Postgres
postgres.stop();
```

Note that EmbeddedPostgres implements [java.lang.AutoCloseable](https://docs.oracle.com/javase/7/docs/api/java/lang/AutoCloseable.html), 
which means that you can use it with a [try-with-resources](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) 
statement (in Java >= 7) to have it automatically stopped.

### How to avoid archive extraction on every run

You can specify the cached artifact store to avoid archives downloading and extraction (in case if a directory remains on every run).
```java
final EmbeddedPostgres postgres = new EmbeddedPostgres();
postgres.start(cachedRuntimeConfig("/path/to/my/extracted/postgres"));
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

Pass the required `IVersion` interface implementation as a first argument of the `EmbeddedPostgres` object:

```java
final EmbeddedPostgres postgres = new EmbeddedPostgres(() -> (IS_OS_WINDOWS) ? "9.6.2-2" : "9.6.2-1");
```

### Known issues
* A lot of issues have been reported for this library under Windows. Please try to use the suggested way of start up and use
the cached artifact storage (to avoid extraction of the archive as extraction is extremely slow under Windows): 
```java
postgres.start(cachedRuntimeConfig("C:\\Users\\vasya\\pgembedded-installation"));
```

* PostgreSQL server is known to not start under the privileged user (which means you cannot start it under root/Administrator of your system):  

> `initdb must be run as the user that will own the server process, because the server needs to have access to the files and directories that initdb creates. Since the server cannot be run as root, you must not run initdb as root either. (It will in fact refuse to do so.)` 
  ([link](http://www.postgresql.org/docs/9.5/static/app-initdb.html)).   
  
  However some users have launched it successfully on Windows under Administrator, so you can try anyway. 
  
### Supported Versions

* 11.2: on Mac OS X and Windows 64 bit
* 10.7, 9.6.12, 9.5.16: on Linux, Windows, Mac OS X
* any custom version
