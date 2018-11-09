Generating the packages for each module and for its dependencies
================================================================

Here we explain how can we generate the packages (i.e. jar files) for
each module and also those corresponding to the dependencies. This will
allow to use AMIDST toolbox in any java project. Alternatively, you can
watch this
`video-tutorial <https://www.youtube.com/watch?v=6iLG17UPzX0>`__. First,
you can download the source code using the following command:

.. code-block:: bash

   $ git clone https://github.com/amidst/toolbox.git      

Once the download has finished, enter into the downloaded folder:

.. code-block:: bash

   $ cd toolbox     

For generating the packages for all the modules, use the following
command:

.. code-block:: bash

   $ mvn clean dependency:copy-dependencies package -Dmaven.test.skip=true    

Note that the argument **-Dmaven.test.skip=true** is optional (its
default value is false). If omitted, maven will run the unitary tests
and hence the process will take much more time. If everything goes
right, you will eventually obtain an output similar to the following
one:

.. code-block:: bash

   [INFO] Reactor Summary:
   [INFO] 
   [INFO] AmidstToolbox ...................................... SUCCESS [  1.177 s]
   [INFO] core ............................................... SUCCESS [ 11.891 s]
   [INFO] core-dynamic ....................................... SUCCESS [  5.081 s]
   [INFO] huginlink .......................................... SUCCESS [  3.031 s]
   [INFO] standardmodels ..................................... SUCCESS [  2.976 s]
   [INFO] examples ........................................... SUCCESS [  4.563 s]
   [INFO] moalink ............................................ SUCCESS [  2.609 s]
   [INFO] wekalink ........................................... SUCCESS [  2.515 s]
   [INFO] ------------------------------------------------------------------------
   [INFO] BUILD SUCCESS
   [INFO] ------------------------------------------------------------------------
   [INFO] Total time: 34.504 s
   [INFO] Finished at: 2016-05-10T16:41:23+02:00
   [INFO] Final Memory: 68M/670M
   [INFO] ------------------------------------------------------------------------  

Now, at each module folder, a new folder called **target** containing
the generated packages has been created. In addition the sub-folder
target/dependencycontains the dependencies for the given package. Now
simply copy all these packages into a folder of your classpath and you
will be able to use the functionality provided by AMIDST.
