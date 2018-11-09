Getting Started! 
=================

Quick start
-----------

Here we explain how to download and run an example project that uses the
AMIDST functionality. You will need to have *java 8, mvn* and *git*
installed. For more information, read the
`requirements <requirements.html>`__ section. First, download the
example project code:

.. code-block:: bash

   $ git clone https://github.com/amidst/example-project.git

Enter in the downloaded folder:

.. code-block:: bash

   $ cd example-project/

A code example illustrating the use of the toolbox is provided in the
file **./src/main/java/BasicExample.java**.

Compile and build the package:

.. code-block:: bash

   $ mvn clean package

Finally, run the code example previously mentioned:

.. code-block:: bash

   $ java -cp target/example-project-full.jar BasicExample

Each time that our model is updated, the following output is shown:

::

   Processing batch 1:
       Total instance count: 1000
       N Iter: 2, elbo:2781.727395615198
   Processing batch 2:
       Total instance count: 2000
       N Iter: 2, elbo:2763.7884038634625

   . . .

   Processing batch 89:
       Total instance count: 88565
       N Iter: 2, elbo:1524.8632699545146
       
   Bayesian Network:
   P(codrna_X1 | M, Z0) follows a Normal|Multinomial,Normal
   [ alpha = 2.5153648505301884, beta1 = -6.47078042377021, var = 0.012038840802392285 ] | {M = 0}
   [ alpha = 0.0, beta1 = 0.0, var = 1.0 ] | {M = 1}

   P(codrna_X2 | M, Z0) follows a Normal|Multinomial,Normal
   [ alpha = -1.4100844769398433, beta1 = 6.449118564273272, var = 0.09018732085219959 ] | {M = 0}
   [ alpha = 0.0, beta1 = 0.0, var = 1.0 ] | {M = 1}

   P(codrna_X3 | M, Z0) follows a Normal|Multinomial,Normal
   [ alpha = 0.5004820734231348, beta1 = -0.7233270338873005, var = 0.02287282091577493 ] | {M = 0}
   [ alpha = 0.0, beta1 = 0.0, var = 1.0 ] | {M = 1}

   P(codrna_X4 | M, Z0) follows a Normal|Multinomial,Normal
   [ alpha = -3.727658229972866, beta1 = 15.332997451530298, var = 0.035794031399428765 ] | {M = 0}
   [ alpha = 0.0, beta1 = 0.0, var = 1.0 ] | {M = 1}

   P(codrna_X5 | M, Z0) follows a Normal|Multinomial,Normal
   [ alpha = -1.3370521440370204, beta1 = 7.394413026859823, var = 0.028236889224165312 ] | {M = 0}
   [ alpha = 0.0, beta1 = 0.0, var = 1.0 ] | {M = 1}

   P(codrna_X6 | M, Z0) follows a Normal|Multinomial,Normal
   [ alpha = -3.3189931551027154, beta1 = 13.565377369009742, var = 0.007243019620713637 ] | {M = 0}
   [ alpha = 0.0, beta1 = 0.0, var = 1.0 ] | {M = 1}

   P(codrna_X7 | M, Z0) follows a Normal|Multinomial,Normal
   [ alpha = -1.3216192169520564, beta1 = 6.327466251964861, var = 0.01677087665403506 ] | {M = 0}
   [ alpha = 0.0, beta1 = 0.0, var = 1.0 ] | {M = 1}

   P(codrna_X8 | M, Z0) follows a Normal|Multinomial,Normal
   [ alpha = 2.235639811622681, beta1 = -5.927480690695894, var = 0.015383139745907676 ] | {M = 0}
   [ alpha = 0.0, beta1 = 0.0, var = 1.0 ] | {M = 1}

   P(codrna_Y) follows a Multinomial
   [ 0.3333346978625332, 0.6666653021374668 ]
   P(M | codrna_Y) follows a Multinomial|Multinomial
   [ 0.9999877194382871, 1.2280561712892748E-5 ] | {codrna_Y = 0}
   [ 0.9999938596437365, 6.1403562634704065E-6 ] | {codrna_Y = 1}
   P(Z0 | codrna_Y) follows a Normal|Multinomial
   Normal [ mu = 0.2687114577360176, var = 6.897846922968294E-5 ] | {codrna_Y = 0}
   Normal [ mu = 0.2674517087293682, var = 5.872354808764403E-5 ] | {codrna_Y = 1}


   P(codrna_Y|codrna_X1=0.7) = [ 0.49982925627218583, 0.5001707437278141 ]

The output shows: the current batch number; the total number of
instances that has been processed until now; the required number of
iterations for learning from the current batch; and the *elbo (evidence
lower bound)*. Finally, distributions in the learnt Bayesian network are
given.

In general, for start using the AMIDST toolbox, add the following lines
to the pom.xml file of your maven project:


.. parsed-literal::


   <repositories> 
     <repositories>
       <repository>
     <id>amidstRepo</id>
     <url>https://raw.github.com/amidst/toolbox/mvn-repo/</url>
     </repository>
   </repositories>

   <dependencies>
     <dependency>
       <groupId>eu.amidst</groupId>
       <artifactId>module-all</artifactId>
       <version> |version| </version>
       <scope>compile</scope>
     </dependency>
   </dependencies> 
   

Getting started in detail
-------------------------

Before starting using the AMDIST, you might check that your system fits
the `requirements <requirements.html>`__ of the toolbox.

Toolbox users (i.e. those interested in simply using the functionality
provided by AMIDST)  might find useful the following tutorials:

-  `Loading AMIDST dependencies from a remote maven
   repository <remoteDeps.html>`__.

-  `Installing a local AMIDST repository <localDeps.html>`__

-  `Generating the packages for each module and its dependencies
   (command line). <copydep.html>`__

Additionally, for those developers interested in colaborating to AMIDST
toolbox could read the following tutorials:

-  `Basic steps for contributing <amidst_team_modifications.html>`__
