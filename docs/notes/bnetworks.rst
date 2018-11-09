.. _sec:bns:

Bayesian Networks: Code Examples
================================

* `Data Streams`_

* `Models`_

	- `Creating BNs`_

	- `Creating Bayesian networks with latent variables`_

	- `Modifying Bayesian Networks`_

* `Input/Output`_

	- `I/O of data streams`_

	- `I/O of BNs`_

* `Inference`_

	- `The inference engine`_

	- `Variational Message Passing`_

	- `Importance Sampling`_

* `Learning Algorithms`_

	- `Maximum Likelihood`_

	- `Parallel Maximum Likelihood`_

	- `Streaming Variational Bayes`_
	
	- `Parallel Streaming Variational Bayes`_ 

* `Concept Drift Methods`_

	- `Naive Bayes with Virtual Concept Drift Detection`_

* `HuginLink`_

	- `Models conversion between AMIDST and Hugin`_

	- `I/O of Bayesian Networks with Hugin net format`_

	- `Invoking Hugin's inference engine`_
	
	- `Invoking Hugin's Parallel TAN`_

* `MoaLink`_

	- `AMIDST Classifiers from MOA`_  

 


.. _`Data Streams`:

Data Streams
------------

In this example we show how to use the main features of a DataStream
object. More precisely, we show six different ways of iterating over the
data samples of a DataStream object.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/core/examples/datastream/DataStreamsExample.java
   :language: java

.. _sec:bns:randomvars:

Data Streams
------------

This example show the basic functionality of the classes Variables and
Variable.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/core/examples/variables/VariablesExample.java
   :language: java

.. raw:: latex

   \hyperref[sec:bns]{[Back to Top]}

.. raw:: latex

   \newline 

.. _sec:bns:models:

.. _`Models`:

Models
------

.. _`Creating BNs`:

Creating BNs
~~~~~~~~~~~~

In this example, we take a data set, create a BN and we compute the
log-likelihood of all the samples of this data set. The numbers defining
the probability distributions of the BN are randomly fixed.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/core/examples/models/CreatingBayesianNetworks.java
   :language: java

.. raw:: latex

   \hyperref[sec:bns]{[Back to Top]}

.. raw:: latex

   \newline 

.. _`Creating Bayesian networks with latent variables`:

Creating Bayesian networks with latent variables
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In this example, we simply show how to create a BN model with hidden
variables. We simply create a BN for clustering, i.e., a naive-Bayes
like structure with a single common hidden variable acting as parant of
all the observable variables.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/core/examples/models/CreatingBayesianNetworksWithLatentVariables.java
   :language: java

.. raw:: latex

   \hyperref[sec:bns]{[Back to Top]}

.. raw:: latex

   \newline 

.. _`Modifying Bayesian networks`:


Modifying Bayesian networks
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In this example we show how to access and modify the conditional
probabilities of a Bayesian network model.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/core/examples/models/ModifiyingBayesianNetworks.java
   :language: java

.. raw:: latex

   \hyperref[sec:bns]{[Back to Top]}

.. raw:: latex

   \newline 

.. _`Input/Output`:

Input/Output
------------

.. _`I/O of data streams`:

I/O of data streams
~~~~~~~~~~~~~~~~~~~

In this example we show how to load and save data sets from .arff files.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/core/examples/io/DataStreamIOExample.java
   :language: java

.. raw:: latex

   \hyperref[sec:bns]{[Back to Top]}

.. raw:: latex

   \newline 

.. _`I/O of BNs`:

I/O of BNs
~~~~~~~~~~

In this example we show how to load and save Bayesian networks models
for a binary file with ".bn" extension. In this toolbox Bayesian
networks models are saved as serialized objects.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/core/examples/io/BayesianNetworkIOExample.java
   :language: java

.. raw:: latex

   \hyperref[sec:bns]{[Back to Top]}

.. raw:: latex

   \newline 

.. _`Inference`:

Inference
---------

.. _`The inference engine`:

The inference engine
~~~~~~~~~~~~~~~~~~~~

This example show how to perform inference in a Bayesian network model
using the InferenceEngine static class. This class aims to be a
straigthfoward way to perform queries over a Bayesian network model. By
the default the *VMP* inference method is invoked.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/core/examples/inference/InferenceEngineExample.java
   :language: java

.. raw:: latex

   \hyperref[sec:bns]{[Back to Top]}

.. raw:: latex

   \newline 

.. _sec:bns:inference:

Inference
---------

.. _`Variational Message Passing`:

Variational Message Passing
~~~~~~~~~~~~~~~~~~~~~~~~~~~

This example we show how to perform inference on a general Bayesian
network using the Variational Message Passing (VMP) algorithm detailed
in

   Winn, J. M., Bishop, C. M. (2005). Variational message passing. In
   Journal of Machine Learning Research (pp. 661-694).

.. literalinclude:: ../../examples/src/main/java/eu/amidst/core/examples/inference/VMPExample.java
   :language: java

.. raw:: latex

   \hyperref[sec:bns]{[Back to Top]}

.. raw:: latex

   \newline 

.. _`Importance Sampling`:

Importance Sampling
~~~~~~~~~~~~~~~~~~~

This example we show how to perform inference on a general Bayesian
network using an importance sampling algorithm detailed in

   Fung, R., Chang, K. C. (2013). Weighing and integrating evidence for
   stochastic simulation in Bayesian networks. arXiv preprint
   arXiv:1304.1504.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/core/examples/inference/ImportanceSamplingExample.java
   :language: java

.. raw:: latex

   \hyperref[sec:bns]{[Back to Top]}

.. raw:: latex

   \newline 

.. _`Learning Algorithms`:

Learning Algorithms
-------------------

.. _`Maximum Likelihood`:

Maximum Likelihood
~~~~~~~~~~~~~~~~~~

This other example shows how to learn incrementally the parameters of a
Bayesian network using data batches,

.. literalinclude:: ../../examples/src/main/java/eu/amidst/core/examples/learning/MaximimumLikelihoodByBatchExample.java
   :language: java


.. raw:: latex

   \hyperref[sec:bns]{[Back to Top]}

.. raw:: latex

   \newline 

.. _`Parallel Maximum Likelihood`:

Parallel Maximum Likelihood
~~~~~~~~~~~~~~~~~~~~~~~~~~~

This example shows how to learn in parallel the parameters of a Bayesian
network from a stream of data using maximum likelihood.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/core/examples/learning/ParallelMaximumLikelihoodExample.java
   :language: java

.. raw:: latex

   \hyperref[sec:bns]{[Back to Top]}

.. raw:: latex

   \newline 

.. _`Streaming Variational Bayes`:

Streaming Variational Bayes
~~~~~~~~~~~~~~~~~~~~~~~~~~~

This example shows how to learn incrementally the parameters of a
Bayesian network from a stream of data with a Bayesian approach using
the following algorithm,

   Broderick, T., Boyd, N., Wibisono, A., Wilson, A. C., and Jordan, M.
   I. (2013). Streaming variational Bayes. In Advances in Neural
   Information Processing Systems (pp. 1727-1735).

In this second example we show a alternative implementation which
explicitly updates the model by batches by using the class SVB.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/core/examples/learning/SVBByBatchExample.java
   :language: java

.. raw:: latex

   \hyperref[sec:bns]{[Back to Top]}

.. raw:: latex

   \newline 

.. _`Parallel Streaming Variational Bayes`:

Parallel Streaming Variational Bayes
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This example shows how to learn in the parameters of a Bayesian network
from a stream of data with a Bayesian approach using the parallel
version of the SVB algorithm,

   Broderick, T., Boyd, N., Wibisono, A., Wilson, A. C., and Jordan, M.
   I. (2013). Streaming variational Bayes. In Advances in Neural
   Information Processing Systems (pp. 1727-1735).

.. literalinclude:: ../../examples/src/main/java/eu/amidst/core/examples/learning/ParallelSVBExample.java
   :language: java

.. raw:: latex

   \hyperref[sec:bns]{[Back to Top]}

.. raw:: latex

   \newline 

.. _`Concept Drift Methods`:

Concept Drift Methods
---------------------

.. _`Naive Bayes with Virtual Concept Drift Detection`:

Naive Bayes with Virtual Concept Drift Detection
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This example shows how to use the class
NaiveBayesVirtualConceptDriftDetector to run the virtual concept drift
detector detailed in

   Borchani et al. Modeling concept drift: A probabilistic graphical
   model based approach. IDA 2015.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/core/examples/conceptdrift/NaiveBayesVirtualConceptDriftDetectorExample.java
   :language: java

.. raw:: latex

   \hyperref[sec:bns]{[Back to Top]}

.. raw:: latex

   \newline 

.. _`HuginLink`:

HuginLink
---------

.. _`Models conversion between AMiDST and Hugin`:

Models conversion between AMiDST and Hugin
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This example shows how to use the class BNConverterToAMIDST and
BNConverterToHugin to convert a Bayesian network models between Hugin
and AMIDST formats

.. literalinclude:: ../../examples/src/main/java/eu/amidst/core/examples/huginlink/HuginConversionExample.java
   :language: java


.. raw:: latex

   \hyperref[sec:bns]{[Back to Top]}

.. raw:: latex

   \newline 

.. _`I/O of Bayesian Networks with Hugin net format`:


I/O of Bayesian Networks with Hugin net format
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This example shows how to use the class BNLoaderFromHugin and
BNWriterToHugin classes to load and write Bayesian networks in Hugin
format

.. literalinclude:: ../../examples/src/main/java/eu/amidst/core/examples/huginlink/HuginIOExample.java
   :language: java

.. raw:: latex

   \hyperref[sec:bns]{[Back to Top]}

.. raw:: latex

   \newline 

.. _`Invoking Hugin's inference engine`:


Invoking Hugin’s inference engine
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This example we show how to perform inference using
`Hugin <http://www.hugin.com>`__ inference engine within the AMiDST
toolbox

.. literalinclude:: ../../examples/src/main/java/eu/amidst/core/examples/huginlink/HuginInferenceExample.java
   :language: java

.. raw:: latex

   \hyperref[sec:bns]{[Back to Top]}

.. raw:: latex

   \newline 

.. _`Invoking Hugin's Parallel TAN`:


Invoking Hugin’s Parallel TAN
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This example we show how to perform inference using
`Hugin <http://www.hugin.com>`__ inference engine within the AMIDST
toolbox.

This example shows how to use `Hugin <http://www.hugin.com>`__\ ’s
functionality to learn in parallel a TAN model. An important remark is
that Hugin only allows to learn the TAN model for a data set completely
loaded into RAM memory. The case where our data set does not fit into
memory, it solved in AMIDST in the following way. We learn the structure
using a smaller data set produced by Reservoir sampling and, then, we
use AMIDST’s ParallelMaximumLikelihood to learn the parameters of the
TAN model over the whole data set.

For further details about the implementation of the parallel TAN
algorithm look at the following paper:

   Madsen, A.L. et al. A New Method for Vertical Parallelisation of TAN
   Learning Based on Balanced Incomplete Block Designs. Probabilistic
   Graphical Models. Lecture Notes in Computer Science Volume 8754,
   2014, pp 302-317.

.. literalinclude:: ../../examples/src/main/java/eu/amidst/core/examples/huginlink/HuginInferenceExample.java
   :language: java

.. raw:: latex

   \hyperref[sec:bns]{[Back to Top]}

.. raw:: latex

   \newline 

.. _`MoaLink`:

MoaLink
-------

.. _`AMIDST Classifiers from MOA`:

AMIDST Classifiers from MOA
~~~~~~~~~~~~~~~~~~~~~~~~~~~

The following command can be used to learn a Bayesian model with a
latent Gaussian variable (HG) and a multinomial with 2 states (HM), as
displayed in figure below. The VMP algorithm is used to learn the
parameters of these two non-observed variables and make predictions over
the class variable.

.. raw:: latex

   \centering

.. figure:: img/HODE.jpg
   :alt: HODE example
   :name: fig:bns:moalink:HODE
   :width: 10cm

   HODE example

.. code-block:: bash

   java -Xmx512m -cp "../lib/*" -javaagent:../lib/sizeofag-1.0.0.jar 
   moa.DoTask EvaluatePrequential -l \(bayes.AmidstClassifier -g 1 
   -m 2\) -s generators.RandomRBFGenerator -i 10000 -f 1000 -q 1000

.. raw:: latex

   \hyperref[sec:bns]{[Back to Top]}

.. raw:: latex

   \newline 

.. _sec:bns:moalink:moareg:

AMIDST Classifiers from MOA
~~~~~~~~~~~~~~~~~~~~~~~~~~~

It is possible to learn an enriched naive Bayes model for regression if
the class label is of a continuous nature. The following command uses
the model in Figure `2 <#fig:bns:moalink:HODEreg>`__ on a toy dataset
from WEKA’s collection of `regression
problems <http://prdownloads.sourceforge.net/weka/datasets-numeric.jar>`__.

.. raw:: latex

   \centering

.. figure:: img/regressionHODE.jpg
   :alt: HODE regression example
   :name: fig:bns:moalink:HODEreg
   :width: 10cm

   HODE regression example

.. code-block:: java

   java -Xmx512m -cp "../lib/*" -javaagent:../lib/sizeofag-1.0.0.jar 
   moa.DoTask EvaluatePrequentialRegression -l bayes.AmidstRegressor
   -s (ArffFileStream -f ./quake.arff)

Note that the simpler the dataset the less complex the model should be.
In this case, ``quake.arff`` is a very simple and small dataset that
should probably be learn with a more simple classifier, that is, a
high-bias-low-variance classifier, in order to avoid overfitting. This
aims at providing a simple running example.
