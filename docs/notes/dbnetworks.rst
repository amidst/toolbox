.. _sec:dbns:

Dynamic Bayesian Networks: Code Examples
========================================

* `Data Streams`_

* `Dynamic Random Variables`_

* `Dynamic Bayesian networks`_

	- `Creating Dynamic Bayesian networks`_

	- `Creating Dynamic Bayesian Networks with Latent Variables`_

	- `Modifying Dynamic Bayesian Networks`_

* `Sampling from Dynamic Bayesian Networks`_

* `Inference Algorithms for Dynamic Bayesian Networks`_

	- `The Dynamic MAP Inference`_

	- `The Dynamic Variational Message Passing`_

	- `The Dynamic Importance sampling`_

* `Dynamic Learning Algorithms`_

	- `Maximum Likelihood for DBNs`_
	- `Streaming Variational Bayes for DBNS`_ 


.. _`Data Streams`:


Data Streams
------------

In this example we show how to use the main features of a *DataStream*
object. More precisely, we show how to load a dynamic data stream and
how to iterate over the *DynamicDataInstance* objects.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/dynamic/examples/datastream/DataStreamsExample.java
   :language: java

.. _`Dynamic Random Variables`:

Dynamic Random Variables
------------------------

This example show the basic functionalities related to dynamic
variables.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/dynamic/examples/variables/DynamicVariablesExample.java
   :language: java

.. _`Dynamic Bayesian networks`:

Dynamic Bayesian networks
-------------------------

.. _`Creating Dynamic Bayesian networks`:

Creating Dynamic Bayesian networks
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This example creates a dynamic BN, from a dynamic data stream, with
randomly generated probability distributions, then saves it to a file.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/dynamic/examples/models/CreatingDBNs.java
   :language: java

.. _`Creating Dynamic Bayesian Networks with Latent Variables`:

Creating Dynamic Bayesian Networks with Latent Variables
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This example shows how to create a BN model with hidden variables. We
simply create a BN for clustering, i.e., a naive Bayes like structure
with a single hidden variable acting as parant of all the remaining
observable variables.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/dynamic/examples/models/CreatingDBNsWithLatentVariables.java
   :language: java

.. _`Modifying Dynamic Bayesian Networks`:

Modifying Dynamic Bayesian Networks
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This example shows how to create a BN model with hidden variables. We
This example shows how to access and modify the conditional
probabilities of a Dynamic Bayesian network model.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/dynamic/examples/models/ModifyingDBNs.java
   :language: java

.. _`Sampling from Dynamic Bayesian Networks`:

Sampling from Dynamic Bayesian Networks
---------------------------------------

This example shows how to use the *DynamicBayesianNetworkSampler* class
to randomly generate a dynamic data stream from a given Dynamic Bayesian
network.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/dynamic/examples/utils/DynamicBayesianNetworkSamplerExample.java
   :language: java

.. _`Inference Algorithms for Dynamic Bayesian Networks`:

Inference Algorithms for Dynamic Bayesian Networks
--------------------------------------------------

.. _`The Dynamic MAP Inference`:

The Dynamic MAP Inference
~~~~~~~~~~~~~~~~~~~~~~~~~

This example shows how to use the Dynamic MAP Inference algorithm.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/dynamic/examples/inference/DynamicMAPInference.java
   :language: java

.. _`The Dynamic Variational Message Passing`:

The Dynamic Variational Message Passing
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This example shows how to use the Factored Frontier algorithm with
Variational Message Passing for running inference on dynamic Bayesian
networks.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/dynamic/examples/inference/DynamicVMP_FactoredFrontier.java
   :language: java

.. _`The Dynamic Importance Sampling`:


The Dynamic Importance Sampling
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This example shows how to use the Factored Frontier algorithm with
Importance Sampling for running inference in dynamic Bayesian networks.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/dynamic/examples/inference/DynamicIS_FactoredFrontier.java
   :language: java


.. _`Dynamic Learning Algorithms`:


Dynamic Learning Algorithms
---------------------------

.. _`Maximum Likelihood for DBNs`:

Maximum Likelihood for DBNs
~~~~~~~~~~~~~~~~~~~~~~~~~~~

This example shows how to learn the parameters of a dynamic Bayesian
network using maximum likelihood from a randomly sampled data stream.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/dynamic/examples/learning/MLforDBNsampling.java
   :language: java

.. _`Streaming Variational Bayes for DBNs`:

Streaming Variational Bayes for DBNs
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This example shows how to learn the parameters of a dynamic Bayesian
network using streaming variational Bayes from a randomly sampled data
stream.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/dynamic/examples/learning/SVBforDBN.java
   :language: java


