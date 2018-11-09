.. _requirements-amidst-toolbox:

Requirements for AMIDST Toolbox
===============================

For toolbox users 
------------------

This toolbox has been specifically designed for using the
functional-style features provided by the Java 8 release. You can check
the Java version installed in your system with the following command:

.. code-block:: bash

   $ java -version    

| If everything is right, the following output (or similar) will be
  generated:

.. code-block:: bash

   java version "1.8.0_73"
   Java(TM) SE Runtime Environment (build 1.8.0_73-b02)
   Java HotSpot(TM) 64-Bit Server VM (build 25.73-b02, mixed mode)   

Make sure that the Java version is 1.8 or higher. Otherwise you must
install a `more recent release of
Java. <http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html>`__

The second requirement consists of having maven installed your system:
`http://maven.apache.org/download.cgi <http://maven.apache.org/download.cgi%20%28follow%20specific%20instructions%20for%20your%20OS%29>`__ 
(follow specific instructions for your OS). Use the following command
for verifying that everything works properly:

.. code-block:: bash

   $ mvn -v

| which should generate an output similar to

.. code-block:: bash

   Apache Maven 3.2.3 (33f8c3e1027c3ddde99d3cdebad2656a31e8fdf4; 2014-08-11T22:58:10+02:00)
   Maven home: /sw/share/maven
   Java version: 1.8.0_73, vendor: Oracle Corporation
   Java home: /Library/Java/JavaVirtualMachines/jdk1.8.0_73.jdk/Contents/Home/jre
   Default locale: en_US, platform encoding: UTF-8
   OS name: "mac os x", version: "10.11.3", arch: "x86_64", family: "mac"    

Having git installed is advisable for downloading relevant material
(i.e., source code, example project, datasets, etc.) Further information
can be found
`here <https://git-scm.com/book/en/v2/Getting-Started-Installing-Git>`__.

For AMIDST developers 
----------------------

AMIDST toolbox is hosted on
`GitHub <https://github.com/amidst/toolbox>`__. To work with AMIDST code
you should follow the `Fork &
Pull <https://help.github.com/articles/using-pull-requests/>`__
collaboration model. Read this
`guide <https://guides.github.com/activities/forking/>`__ for full
details about how to fork a project and make a pull request. Once you
have forked the project, you can clone to you computer and open it with
Intellij by pointing at the pom.xml file, and everything will be ready
to start. Further details about how to contribute to this project are
given this
`link <http://amidst.github.io/toolbox/ContributingToAMIDST.html>`__.
