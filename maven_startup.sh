#!/bin/bash

# Example script for setting up Maven on Mac OS X. May need modifications.

export M2_HOME=/Applications/apache-maven/apache-maven-3.2.3
export M2=$M2_HOME/bin
export MAVEN_OPTS="-Xms256m -Xmx512m"
export PATH=$M2:$PATH
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_20.jdk/Contents/Home
export PATH=${JAVA_HOME}/bin/:$PATH
