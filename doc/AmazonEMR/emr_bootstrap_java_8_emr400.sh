# Check java version
JAVA_VER=$(java -version 2>&1 | sed 's/java version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q')

if [ "$JAVA_VER" -lt 18 ]
then
    # Figure out how many versions of Java and javac we currently have
    NR_OF_JRE_OPTIONS=$(echo 0 | alternatives --config java 2>/dev/null | grep 'There ' | awk '{print $3}' | tail -1)
    NR_OF_SDK_OPTIONS=$(echo 0 | alternatives --config javac 2>/dev/null | grep 'There ' | awk '{print $3}' | tail -1)

    # Silent install javac (includes jre)
    sudo yum -y install java-1.8.0-devel

    echo "Found $NR_OF_JRE_OPTIONS existing versions of java. Adding new version."
    echo "Found $NR_OF_SDK_OPTIONS existing versions of javac. Adding new version."

    # Make java 8 the default
    echo $(($NR_OF_JRE_OPTIONS + 1)) | sudo alternatives --config java
    echo $(($NR_OF_SDK_OPTIONS + 1)) | sudo alternatives --config javac

    # Fix wrong links
    sudo rm /etc/alternatives/java_sdk_openjdk;sudo ln -s /usr/lib/jvm/java-1.8.0-openjdk.x86_64 /etc/alternatives/java_sdk_openjdk
    sudo rm /etc/alternatives/java_sdk_openjdk_exports;sudo ln -s /usr/lib/jvm-exports/java-1.8.0-openjdk.x86_64 /etc/alternatives/java_sdk_openjdk_exports

fi

# Check java version again
JAVA_VER=$(java -version 2>&1 | sed 's/java version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q')

echo ""
echo "Java version is $JAVA_VER!"
echo "JAVA_HOME: $JAVA_HOME"
echo "JRE_HOME: $JRE_HOME"
echo "PATH: $PATH"
