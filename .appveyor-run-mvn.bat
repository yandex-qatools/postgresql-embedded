SET JAVA_HOME=C:\Program Files\Java\jdk1.8.0
SET M3_HOME=C:\maven\apache-maven-3.2.5
SET TEMP=C:\Users\postgres\AppData\Local\Temp
SET TMP=C:\Users\postgres\AppData\Local\Temp
SET M2_HOME=
SET MAVEN_OPTS=-XX:MaxPermSize=2g -Xmx4g
SET JAVA_OPTS=-XX:MaxPermSize=2g -Xmx4g
SET PATH=C:\maven\apache-maven-3.2.5\bin;%JAVA_HOME%\bin;C:\Program Files\OpenSSH\bin;%PATH%
mvn %*
if %ERRORLEVEL% == 0 (
   echo Build success (%ERRORLEVEL%)
) else (
   echo Build failed (%ERRORLEVEL%)
   %ERRORLEVEL% > target/exit-code.txt
)