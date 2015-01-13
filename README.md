core
====

This is the repository for the open source code in the amidst project. 

Install Java 8 and IntelliJ

http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

http://www.jetbrains.com/idea/download/

In IntelliJ open maven project and point to the pom file. Also you need to point to the Java 8 installation and you ready to rock.


========================
Compiling & Running from the command line
========================


1- Install Maven: http://maven.apache.org/download.cgi
(Download binaries and copy to Applications folder)

2- Modify the file maven_startup.sh (which you find it in the root project folder) and fix the path of your maven (Line 5) and java installation (Line 9).

3- Create (or modify if already exist) a file ".profile" or ".bash_profile" in you home directory and add the following line,
which points to file "maven_startup.sh"

        source <project-folder>/maven_startup.sh

 Now after restarting the terminal, mvn should work.


4- Put the hugin files ("hgapi82_amidst-64.jar" and "libhgapi82_amidst-64.jnilib") in a new folder in the project
folder called "huginlib". I.e. we should have the two following files:

        <project-folder>/huginlib/hgapi82_amidst-64.jar
        <project-folder>/huginlib/libhgapi82_amidst-64.jnilib


5- The script "compile.sh" (which you find it in the root project folder) just compile the whole project and create a .jar file in the ./target folder.


6- The script "run.sh" (which you find it in the root project folder) should be used to run some class. For example,

        ./run.sh eu.amidst.examples.ParallelTANDemo
