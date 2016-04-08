# Getting Started! <a name="architecture"></a>

## Installing AMIDST Toolbox <a name="installation"></a>

The first step is to install [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).  As second step, we strongly recommend to install [IntelliJ](http://www.jetbrains.com/idea/download/) as IDE tool because it has direct support of Maven and Github. 

<!---  Now, we detail two different installation settings based on Maven and IntelliJ. The first installation settings is for those who just want to use the AMIDST toolbox and do not plan to make contributions/extensions to this open software project. The second settings details how to proceed to be a contributor of this project. 

* **Using AMIDST** simply requires to create a new **Maven Project** using IntelliJ where your code will be placed. Then edit the file "pom.xml" and add the following lines referring to the link the AMIDST jar library inside the dependencies plugin (follow this [link](http://books.sonatype.com/mvnex-book/reference/customizing-sect-add-depend.html) for further details) and then you are ready to rock. 

        <dependency>
            <groupId>eu.amidst.toolbox</groupId>
            <artifactId>AMIDST</artifactId>
            <version>1.0</version>
        </dependency>

* **Contributing to AMDIST** is based on the [Fork & Pull](https://help.github.com/articles/using-pull-requests/) 
collaboration model. Read this [guide](https://guides.github.com/activities/forking/) for full details about how 
to fork a project and make a pull request. Once you have forked the project, you can clone to you computer and open 
it with Intellij by pointing at the pom.xml file, and everything will be ready to start. 

-->

AMIDST toolbox is hosted on [GitHub](https://github.com/amidst/toolbox). To work with AMIDST code you should follow 
the [Fork & Pull](https://help.github.com/articles/using-pull-requests/) collaboration model. Read this [guide](https://guides.github.com/activities/forking/) for full details about how 
to fork a project and make a pull request. Once you have forked the project, you can clone to you computer and open 
it with Intellij by pointing at the pom.xml file, and everything will be ready to start. 

Further details about how to contribute to this project are given this [link](http://amidst.github.io/toolbox/ContributingToAMIDST.html). 



## Installing MOALink<a name="installmoa"></a>

To use AMIDST functionality within [MOA](http://moa.cms.waikato.ac.nz) you just have to run the script 
*compileWithDependencies.sh* in the *moalink* directory. A file *moalink-1.0-SNAPSHOT-jar-with-dependencies.jar* 
will be generated in the *[project-root-folder]/moalink/target* directory. Please place this jar file on your library path for 
[MOA](http://moa.cms.waikato.ac.nz). 

With this jar file we can make use of the different learning and inference algorithms in AMIDST to learn more 
expressive Bayesian network models for classification, regression and clustering. AMIDST offers the possibility to 
add latent Gaussian and/or Multinomial variables to a base naive Bayes structure. Normally, the addition of 
these latent variables should provide classifiers with lower bias and higher variance, that is, more sophisticated 
classifiers that are able to lean more complex interdependencies in the data, but also more prone to overfit. 
The user should evaluate the complexity of his/her dataset and choose the number of latent Gaussian variables 
and states of the multinomial latent variable accordingly.

With the following command, [MOA](http://moa.cms.waikato.ac.nz) gui can be invoked e.g 
(remember to place *compileWithDependencies.sh* under the lib folder reference in the *-cp* option):

```
java -Xmx512m -cp "../lib/*" -javaagent:../lib/sizeofag-1.0.0.jar moa.gui.GUI
```

Note that the above example should be slightly adapted to run on a Windows machine: 
e.g. use "^" instead of "\" to escape brackets.

## Installing HuginLink<a name="installhugin"></a>

HuginLink is present in the toolbox as a independent Maven module. To use this module and, in consequence, access some of the functionalities provided by [Hugin](http://www.hugin.com/) commercial software, we need to perform the following steps:

1. **Install Hugin software**. Here we describe how to install [Hugin Lite 8.2](http://www.hugin.com/productsservices/demo) which is a freely available demo version of [Hugin](http://www.hugin.com) software. For those with a full license or who want to update HuginLink to link to a new version of [Hugin](http://www.hugin.com) sofware, just follow the same steps.

2. **Install the binary file**. Inside the installation folder you will find a folder called *Libraries* which contains the binary file needed for the installation. Choose the file that fits with your operating system and copy it to the folder [project-root-folder]/huginlink/huginlib/. For example, for a MAC OS X with 64 bits architecture the file needed is *libhapi82-64.jnilib*. 
    
    Finally, rename this binary the file to match it with the Java jar file already provided by the toolbox, *[project-root-folder]/huginlink/huginlib/hapi82_amidst-64.jar*. The final name of the binary file should be *libhapi82_amidst-64.[extension]*. The extension should not be modify because it depends on the specific operating system.


<!--- 2. **Install the jar file**. Inside the installation folder of [Hugin Lite](http://www.hugin.com/productsservices/demo), you will find a jar file. Copy this file to the folder [project-root-folder]/huginlink/huginlib/. Then, edit the file [project-root-folder]/huginlink/pom.xml to correctly reference to this jar file. The lines to edit are the following:

        <dependency>
            <groupId>com.hugin</groupId>
            <artifactId>hugin</artifactId>
            <version>8.2</version>
            <scope>system</scope>
            <systemPath>${project.basedir}/huginlib/hapi82-64.jar</systemPath>
        </dependency>
-->

Now, you can invoke the following example by using the script *run.sh* :

    ./run.sh eu.amidst.huginlink.examples.inference.HuginInferenceExample


We notice that ror running any code invoking the Hugin API, you have to provide the following option to the JVM  

    -Djava.library.path="./huginlink/huginlib/" 


## Compiling & Running from the command line<a name="compilation"></a>

1. Install Maven: http://maven.apache.org/download.cgi  (follow specific instructions for your OS).

2. Modify the file maven_startup.sh (which you can find in the root project folder, and which is displayed below) 
and fix the path of your Maven and Java installation.


```bash
#!/bin/bash

# Example script for setting up Maven on Mac OS X. May need modifications.

export M2_HOME=/Applications/apache-maven/apache-maven-3.2.3
export M2=$M2_HOME/bin
export MAVEN_OPTS="-Xms256m -Xmx512m"
export PATH=$M2:$PATH
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_20.jdk/Contents/Home
export PATH=${JAVA_HOME}/bin/:$PATH
```

3. Create (or modify if already exists) a file ".profile" or ".bash_profile" in you home directory and add the following line,
which points to file "maven_startup.sh"

        source <project-folder>/maven_startup.sh

 Now after restarting the terminal, *mvn* command should work.


4. The script "compile.sh" (which you can find in the root project folder) just compiles the whole project.


5. The script "run.sh" (which you can find in the root project folder) should be used to run some class. For example,

        ./run.sh eu.amidst.core.examples.learning.ParallelMaximumLikelihoodExample


## Java 8 Integration: Lambdas, streams, and functional-sytle programming<a name="java8"></a>

This toolbox has been specifically designed for using the functional-style features provided by the Java 8 release. This design leverages these new features for developing easy-to-code parallel algorithms on mutli-core CPUs. As commented above, the main scalability properties of this toolbox rely on this functional-style approach introduced in Java 8. Our aim is that future developers can also exploit this specific design of the toolbox for easily developing new methods for dealing with massive data streams using PGMs.  

The following paper provides a deep discussion over the different design issues we had to face and how they were solved using Java 8 functional-style features. 


> Andres R. Masegosa, Ana M. Martinez, and Hanen Borchani. Probabilistic graphical models on multi-core CPUs using Java 8. IEEE Computational Intelligence Magazine, Special Issue on Computational Intelligence Software, Under review, 2015.


## Module's Based Architecture<a name="description"></a>

AMIDST toolbox is an open source project under [Apache Software License 2.0](http://www.apache.org/licenses/LICENSE-2.0). It is written in Java and is based on [Apache Maven](https://en.wikipedia.org/wiki/Apache_Maven) for building and structuring the project. This toolbox is structured as [multi-module Maven project](http://books.sonatype.com/mvnex-book/reference/multimodule.html). Roughly speaking, a **Maven module** is an independent piece of software with explicit dependencies to other modules in the project and to other external libraries. Each module is placed in independent folders and contains an xml file describing its dependencies. In this current version, the toolbox is composed by the following four modules:

* **Core module** contains all the main functionalities for *Static BNs*. It is placed in the *core* folder under the root project folder. 

* **Core-dynamic module** contains all the main functionalities for *Dynamic BNs*. It is placed in the *core-dynamic* folder under the root project folder. 

* **Examples module** contains basic code examples showing how to use the main functionalities of the toolbox. It is placed in the *examples* folder under the root project folder.

* **MoaLink module** contains the code needed to use the AMIDST functionality within [MOA](http://moa.cms.waikato.ac.nz/). It is placed in the *moalink* folder  under the root project folder.

* **HuginLink module** contains the code needed to use [Hugin](www.hugin.com) software within AMIDST. It is placed in the *huginlink* folder under the root project folder. 

