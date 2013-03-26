orm4j
=====

O/R Mapping framework for Java

Supported databases:
 - SQLite

To install in maven repository:
mvn install:install-file -Dfile={SQLITE_JAR_PATH_HERE} -DgroupId=sqlite -DartifactId=sqlite-jdbc -Dversion=3.7.2 -Dpackaging=jar
mvn install:install-file -Dfile={ORM_FILE_JAR_HERE} -DgroupId=orm4j -DartifactId=orm4j -Dversion={version} -Dpackaging=jar

