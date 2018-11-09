.. _sec:flinklink:

Flinklink: Code Examples
========================


* `Input/output`_

	- `Reading data`_

	- `Write data`_

* `Parametric Learning`_

	- `Parallel Maximum Likelihood`_

	- `Distributed Variational Message Pasing`_

	- `Distributed VI`_

	- `Stochastic VI`_

* `Extensions and applications`_

	- `Latent variable models with Flink`_

	- `Concept drift`_  


.. _`Input/output`:

Input/output
------------

.. _sec:flinklink:io:read:

.. _`Reading data`:

Reading data
~~~~~~~~~~~~

In this example we show how can we read a dataset using Flink. Note that
the process is the same regardless being a single or a distributed file.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/flinklink/examples/io/DataStreamLoaderExample.java
   :language: java

.. raw:: latex

   \hyperref[sec:flinklink]{[Back to Top]}

.. raw:: latex

   \newline 

.. _sec:flinklink:io:write:

.. _`Write data`:

Writing data
~~~~~~~~~~~~

Below we generate a random Flink dataset with 1000 instances, 2 discrete
variables and 3 continuous ones. The seed used is 1234. Eventually, we
save it as a distributed dataset (format ARFF folder).

.. literalinclude:: ../../examples/src/main/java/eu/amidst/flinklink/examples/io/DataStreamWriterExample.java
   :language: java

.. raw:: latex

   \hyperref[sec:flinklink]{[Back to Top]}

.. raw:: latex

   \newline 

.. _sec:flinklink:learning:


Parametric learning
-------------------

Here give examples of the provided algorithms by AMiDST for learning the
probability distributions from a Flink data set. For shake of
simplicity, we will consider the Naive Bayes DAG structure. Note that
the code is almost the same of each of the algoritms, they only differ
on the constructor used (e.g. *new ParallelMaximumLikelihood()*, *new
dVMP()*, etc.)

.. _sec:flinklink:learning:pml:

.. _`Parallel Maximum Likelihood`:

Parallel Maximum Likelihood
~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. literalinclude:: ../../examples/src/main/java/eu/amidst/flinklink/examples/learning/ParallelMLExample.java
   :language: java

.. raw:: latex

   \hyperref[sec:flinklink]{[Back to Top]}

.. raw:: latex

   \newline 

.. _sec:flinklink:learning:dvmp:

.. _`Distributed Variational Message Pasing`:

Distributed Variational Message Passing
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. literalinclude:: ../../examples/src/main/java/eu/amidst/flinklink/examples/learning/dVMPExample.java
   :language: java

.. raw:: latex

   \hyperref[sec:flinklink]{[Back to Top]}

.. raw:: latex

   \newline 

.. _sec:flinklink:learning:dvi:

.. _`Distributed VI`:

Distributed VI
~~~~~~~~~~~~~~

.. literalinclude:: ../../examples/src/main/java/eu/amidst/flinklink/examples/learning/DistributedVIExample.java
   :language: java

.. raw:: latex

   \hyperref[sec:flinklink]{[Back to Top]}

.. raw:: latex

   \newline 

.. _sec:flinklink:learning:svi:

.. _`Stochastic VI`:

Stochastic VI
~~~~~~~~~~~~~

An example of the learning algorithm Stochastic VI is given below. Note
that two specific parameters must be set, namely the *learning factor*
and the *data size*.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/flinklink/examples/learning/StochasticVIExample.java
   :language: java

.. raw:: latex

   \hyperref[sec:flinklink]{[Back to Top]}

.. raw:: latex

   \newline 

.. _sec:flinklink:ext:

.. _`Extensions and applications`:

Extensions and applications
---------------------------

.. _sec:flinklink:ext:models:

.. _`Latent variable models with Flink`_

Latent variable models with Flink
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The module *latent-variable-models* contains a large set of classes that
allow to easily learn some of the standard models with latent variables.
These models can be learnt from not only from local datasets (e.g. a
single ARFF file) but also from distributed ones (e.g. ARFF folder).
These last ones are managed using Flink. In code example shown below the
model *Factor Analysis* is learnt from a distributed dataset.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/flinklink/examples/extensions/LatentModelsFlink.java
   :language: java

.. raw:: latex

   \hyperref[sec:flinklink]{[Back to Top]}

.. raw:: latex

   \newline 

.. _sec:flinklink:ext:conceptdrift:

.. _`Concept drift`:

Concept drift detection
~~~~~~~~~~~~~~~~~~~~~~~

A salient aspect of streaming data is that the domain being modeled is
often *non-stationary*. That is, the distribution governing the data
changes over time. This situation is known as *concept drift* and if not
carefully taken into account, the result can be a failure to capture and
interpret intrinsic properties of the data during data exploration. The
AMIDST toolbox can be used for detecting this situation as shown in the
example below.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/flinklink/examples/reviewMeeting2015/ConceptDriftDetector.java
   :language: java

.. raw:: latex

   \hyperref[sec:flinklink]{[Back to Top]}

.. raw:: latex

   \newline 
