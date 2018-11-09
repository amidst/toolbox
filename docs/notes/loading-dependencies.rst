Loading AMIDST dependencies from a remote maven repository
==========================================================

Here we explain how to add the AMIDST dependencies in a maven project
with Java 8 or higher. Alternatively, you might prefer following the
video-tutorial in this
`link <https://www.youtube.com/watch?v=i_X6cFo91LE>`__ .

In this example, we will use a project containing only one class, though
the procedure here explain could be used in any other maven project. You
can check this
`link <https://www.jetbrains.com/help/idea/2016.1/getting-started-with-maven.html>`__
for getting more information about how to create a new mavenproject.

For using the AMIDST Toolbox, the **pom.xlm** file will be modified.
First, in the Project view (located on the left) select the file pom.xml
of your project and open it:

.. raw:: latex

   \centering 

.. figure:: ../_static/img/use_amidst05.png
   :alt: Capture of Maven project in IntelliJ and the initial pom.xml file
   :width: 8.5cm
   :align: center

   Capture of Maven project in IntelliJ and the initial pom.xml file


Add the AMIDST repository by including the following code to your pom:

.. code-block:: xml

   <repositories>
   <!-- AMIDST repository in github -->
   <repository>
   <id>amidstRepo</id> <!-- local identifier, it can be anything -->
   <url>https://raw.github.com/amidst/toolbox/mvn-repo/</url>
   </repository>
   <!-- ... -->
   </repositories>        

Then, add the dependencies of modules in AMIDST you want to use.  For
each module, add an element **<dependency>…</dependency>** inside the
labels **<dependencies></dependencies>**. For each one, we have to
indicate the following information:

-  **groupId** is an identifier of the project’s module. In this case it
   should containt the value *“eu.amidst”.*

-  **artifactId** is the name of the module we want to use. More
   precisely, it is the name of the jar file containing such module. You
   can see the list of AMIDST modules
   `here <https://github.com/amidst/toolbox/tree/mvn-repo/eu/amidst>`__.

-  **version** is the identifier of  AMIDST Toolbox release. You can see
   `here <mohttps://github.com/amidst/toolbox/blob/master/CHANGELOG.mddules%20here>`__
   the list of all versions available.

-  **scope**  allows you to only include dependencies appropriate for
   the current stage of the build. We will set this to *“compile”.*

.. raw:: latex

   \vspace{2mm}

For example, for using the *core-dynamic* module, include the following
code:

.. parsed-literal::

   <dependencies>
   <!-- Load any of the modules from AMIDST Toolbox -->
   <dependency>
   <groupId>eu.amidst</groupId>
   <artifactId>core-dynamic</artifactId>
   <version> |version| </version>
   <scope>compile</scope>
   </dependency>

   <!-- ... -->
   </dependencies>        

Note that for using another module, simply change the value of the
element artifactId (i.e. the content between the tags <artifactId> and
<artifactId>). Now you can check in the **Maven Projects panel** that
all the dependencies have been loaded:

.. raw:: latex

   \centering

.. figure:: ../_static/img/use_amidst07.png
   :alt: List of loaded dependencies
   :width: 8.5cm
   :align: center

   List of loaded dependencies


.. raw:: latex

   \newpage 

Note that the *core-dynamic module* depends on corethat has been loaded
as well. We recomend you to download the sources and the javadoc:

.. raw:: latex

   \centering

.. figure:: ../_static/img/use_amidst08.png
   :alt: Download the JavaDoc and the source code
   :width: 10cm
   :align: center

   Download the JavaDoc and the source code


Finally, for testing purposes, we can run the code shown below that
generates a random dynamic bayesian network (DBN) and prints its
parameters.

.. code-block:: java

   import eu.amidst.dynamic.models.DynamicBayesianNetwork;
   import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;

   public class TestingAmidst {
     public static void main(String[] args) throws WrongConfigurationException {
       DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(2);
       DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
       DynamicBayesianNetworkGenerator.setNumberOfStates(3);

       DynamicBayesianNetwork extendedDBN = 
       DynamicBayesianNetworkGenerator.generateDynamicBayesianNetwork();    

       System.out.println(extendedDBN.toString());


     }

   }

If everything goes right, the following output will be generated:

.. raw:: latex

   \centering

.. figure:: ../_static/img/use_amidst09.png
   :alt: Ouput of the testing code that generates a random DBN
   :width: 8.5cm
   :align: center

   Ouput of the testing code that generates a random DBN

