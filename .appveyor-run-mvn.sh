#!/bin/bash
export JAVA_HOME=/cygdrive/c/Program\ Files/Java/jdk1.8.0
export M3_HOME=/cygdrive/c/maven/apache-maven-3.2.5
export TEMP=/cygdrive/c/Users/postgres/AppData/Local/Temp
export TMP=/cygdrive/c/Users/postgres/AppData/Local/Temp
export M2_HOME=
export MAVEN_OPTS=-Xmx4g
export JAVA_OPTS=-Xmx4g
export PATH=/cygdrive/c/maven/apache-maven-3.2.5/bin:$JAVA_HOME/bin:/cygdrive/c/Program\ Files/OpenSSH/bin:$PATH
mvn $@
