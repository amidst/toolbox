.. _sec:sparklink:

Sparklink: Code Examples
========================

.. _sec:sparklink:io:

Input/output
------------

.. _sec:sparklink:io:read:

Reading data
~~~~~~~~~~~~

In this example we show how can we read a dataset using sparklink. This
module supports reading files in formats *json* and *parquet*. Note that
the method *DataSparkLoader::open* automatically detects the format of
the file (the indicated path should contain the extension). Finally all
the instances in the data set are printed.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/sparklink/examples/io/DataStreamLoaderExample.java
   :language: java

.. _sec:sparklink:io:write:

Writing data
~~~~~~~~~~~~

Here we show how can we save spark data into a file. First a random data
set is generated using the method *DataSetGenerator::generate*.
Afterwards, such data is save using the method
*DataSparkWriter::writeDataToFolder*

.. literalinclude:: ../../examples/src/main/java/eu/amidst/sparklink/examples/io/DataStreamWriterExample.java
   :language: java

.. _sec:sparklink:learning:

Parameter learning
------------------

AMIDST provides parameter learning using spark with the *Maximum
Likelihood* algorithm. In the following example, we load a data set in
format json and we use it for learning a simple naive bayes (more
complex DAGs can also be learnt).

.. literalinclude:: ../../examples/src/main/java/eu/amidst/sparklink/examples/learning/MaximumLikelihoodLearningExample.java
   :language: java


