.. _sec:lvmodels:

Tutorial: Easy Machine Learning with Latent Variable Models in AMIDST
=====================================================================

In AMIDST toolbox 0.4.2 the module *latent-variable-models*, that
contains a wide range of predefined latent variable models (see table
below), was included. In this tutorial we will show how the use of this
module simplifies the learning and inference processes. In fact, you
will be able to learn your model and to perform inference with a few
lines of code.

.. raw:: latex

   \centering

.. figure:: ../_static/img/amidstModels.png
   :alt: Set of predefined latent variable models in AMIDST
   :name: fig:lvmodels:amidstModels
   :width: 13cm
   :align: center

   Set of predefined latent variable models in AMIDST

Besides of the simplicity, the required code for learning a latent
variable model is also flexible: you will be able to change the learnt
model or the inference algorithm just with some slight modfications in
the code. Another advantage of using AMIDST for learning one of the
predefined models is that the procedure is traparent to the format of
your training data: you will use the same methods regardless of learning
from a local or a distributed dataset (with Flink). Note that this last
feature was included in the version 0.5.0 of the toolbox.

Setting up
----------

In order to follow this tutorial, you will need to have the java 8 (i.e.
SDK 1.8 or higher) installed in your system. For more details about the
system requirements, see this
`link <../GettingStarted/requirements.html>`__. Additionally, you can
download a ready-to-use IntelliJ maven project with all the code
examples in this tutorial. For that, use the following command:

.. code-block:: bash

   $ git clone https://github.com/amidst/tutorial.git      

Alternativelly, you would rather create a new maven project or use an
existing one. For that, you might check the `Getting
Started <../GettingStarted/index.html>`__ page.

Note that this project does not contain any AMIDST source or binary, it
only has some .java files using the AMIDST functionallity. Instead, each
of the AMIDST modules are provided through maven. Doing that, the
transitive dependences of the AMIDST toolbox are also downloaded in a
transparent way for the user. An scheme of this idea is shown below:

.. raw:: latex

   \centering

.. figure:: ../_static/img/overview.png
   :alt: Set of predefined latent variable models in AMIDST
   :name: fig:lvmodels:overview
   :width: 13cm
   :align: center

   Set of predefined latent variable models in AMIDST

.. raw:: latex

   \newpage 

If we open the downloaded project, we will see that it contains the
following relevant folders and files:

-  **datasets**: folder with local and distributed datasets used in this
   tutorial in ARFF format.

-  **doc**: folder containing documentation about this tutorial.

-  **lib**: folder for storing those libraries not available through
   maven.

-  **src/main/java**: folder with all the code example.

-  **pom.xml**: this is the maven configuration file where the AMIDST
   dependencies are defined.

In the pom.xml file of the downloaded project, only the module called
*latent-variable-models* is linked. However some other AMIDST are also
loaded as *latent-variable-models* module depends on them. This is the
case of the modules called *core*, *core-dynamic*, *flinklink*, etc. You
can see the full list of dependencies in the **maven project panel**,
usually located on the right side of the window (see image below). If
dependencies are not automatically downloaded, click on **Upload**
button. It is recommended to download the sources and java

.. raw:: latex

   \centering

.. figure:: ../_static/img/mavenpanel.png
   :alt: Maven project panel showing the list of dependencies
   :name: fig:lvmodels:mavenpanel
   :width: 10cm
   :align: center

   Maven project panel showing the list of dependencies

.. raw:: latex

   \newpage 

.. _sec:lvmodels:static:

Static Models
-------------

.. _sec:lvmodels:static:learning:

Learning and saving to disk
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Here we will see how can we learnt a static model from a local dataset
(non-distributed). In particular, we will use the financial-like dataset
**datasets/simulated/cajamar.arff** containing 4 continuous (normal
distributed) variables. From this data, a *Factor Analysis* model will
be learnt. In short, this model aims to reduce the dimensionality of a
set of observed continuous variables by expressing them as combination
of gaussians. A synthesis of this process is shown in the image below
where: :math:`X1, X2, X3` and :math:`X4` are the observed variables and
:math:`H1, H2` and :math:`H3` are the latent variables representing the
combination of gaussians.

.. raw:: latex

   \centering

.. figure:: ../_static/img/staticlearning.png
   :alt: Static learning process
   :name: fig:lvmodels:static:learning:scheme
   :width: 10cm
   :align: center

   Static learning process

Note that the required functionality for learning the predefined model
is provided by the module *latent-variable-models*. A code-example for
learning a factor analysis model is shown below.

.. literalinclude:: ../../extensions/tutorials/src/main/java/eu/amidst/tutorial/usingAmidst/examples/StaticModelLearning.java
   :language: java

`See on
GitHub <https://github.com/amidst/tutorial/blob/master/src/main/java/eu/amidst/tutorial/usingAmidst/examples/StaticModelLearning.java>`__

For learning any of the available static models, we create an object of
any of the classes inheriting from the class *Model*. These classes
encapsulates all the fuctionality for learning/updating a
latent-variable model. For example, in the code above we create an
object of the class *FactorAnalysis* which is actually stored as *Model*
object. The flexibility of the toolbox is due to this hierarchical
desing: if we aim to change the model learnt from our data, we simply
have to change the constructor used (assuming that our data also fits
the constraints of the new model). For example, if we aim to learn a
mixture of factor analysers instead, we simply have to replace the line

.. code-block:: java

   Model model = new FactorAnalysis(data.getAtributes());

by

.. code-block:: java

   Model model = new MixtureOfFactorAnalysers(data.getAtributes());

Note that the method for learning the model, namely
*Model::updateMode(DataStream<DataInstance>)* will always be the same
regardless of the particular type of static model.

The actual learnt model is an object of the class *BayesianNetwork*
which is stored as a member variable of *Model*. Thus, for using the
network, we simply have to extract with the method *Model::getModel()*.
One of the actions we can perform with it is saving it into the local
file system. For saving it in *.bn* format:

.. code-block:: java

   BayesianNetworkWriter::save(BayesianNetwork bn, String path)

Alternatively, and assuming that we have the hugin library available, we
can also save it in *.net* format:

.. code-block:: java

   BayesianNetworkWriteToHuginr::save(BayesianNetwork bn, String path)

.. _sec:lvmodels:static:flinklearning:

Learning from Flink
~~~~~~~~~~~~~~~~~~~

In previous section we showed how the AMIDST toolbox can be used for
learning a static model from a non-distributed dataset. In addition, you
can use the pre-defined models to process massive data sets in a
distributed computer cluster using **Apache Flink**. In particular, the
model can be learnt from a *distributed ARFF folder* or from a file
accesible via a HDFS url. A scheme of the learning process is shown
below.

.. raw:: latex

   \centering

.. figure:: ../_static/img/distributedlearning.png
   :alt: Distributed learning process scheme
   :name: fig:lvmodels:static:flinklearning:scheme
   :width: 10cm
   :align: center

   Distributed learning process scheme

A code example for learning from Flink is shown below. Note that it only
differs from the one in previous section in lines 25 to 33. In these
lines, the Flink session is configurated and the stream is loaded, which
is managed with an object of the class *DataFlink* (instead of
*DataStream*).

.. literalinclude:: ../../extensions/tutorials/src/main/java/eu/amidst/tutorial/usingAmidst/examples/StaticModelFlink.java
   :language: java

`See on
GitHub <https://github.com/amidst/tutorial/blob/master/src/main/java/eu/amidst/tutorial/usingAmidst/examples/StaticModelFlink.java>`__

In previous example, the distributed dataset is stored in our local file
system. Instead, we might need to load from a distributed file system.
For that, simply replace the string indicating the path. That is,
replace

.. code-block:: java

   String filename = "datasets/simulated/cajamarDistributed.arff"

by

.. code-block:: java

   String filename = "hdfs://distributed-server/path-to-file"

.. _sec:lvmodels:static:inference:

Inference
~~~~~~~~~

Making probabilistic inference in BNs (a.k.a *belief updating*) consists
of the computation of the posterior probability distribution for a set
of variables of interest given some evidence over some other variables
(see image below).

.. raw:: latex

   \centering

.. figure:: ../_static/img/staticinference.png
   :alt: Learning process scheme
   :name: fig:lvmodels:static:inference:scheme
   :width: 10cm
   :align: center

   Learning process scheme

The inference process is the same regardless of the way we have learnt
our model: we simply have to obtain the BN learnt (stored as an object
of the class *BayesianNetwork*), set the target variables and the
evidence. As an example, let us consider the following code:

.. literalinclude:: ../../extensions/tutorials/src/main/java/eu/amidst/tutorial/usingAmidst/examples/StaticModelInference.java
   :language: java

`See on
GitHub <https://github.com/amidst/tutorial/blob/master/src/main/java/eu/amidst/tutorial/usingAmidst/examples/StaticModelInference.java>`__

Note that the learning algorithm can be easily changed by simply
modifying line 35 where *VMP algorithm*. If we aim to use *Importance
Sampling algorithm*, replace such line with:

.. code-block:: java

       InferenceAlgorithm infer = new ImportanceSampling();

Alternatively, we can use *Hugin Inference algorithm* (assuming that we
have the corresponding libraries):

.. code-block:: java

   InferenceAlgorithm infer = new HuginInference();

.. _sec:lvmodels:static:custom:

Custom static model
~~~~~~~~~~~~~~~~~~~

It could happend that your model of interest is not predifined. In that
case you can implement it yourself. For that purpose, create a new class
inheriting the class *Model*. Then, add the code to the constructor with
an object *Attributes* as input parameter, and the code of the method
*void buildDAG()*. This last method is called before learning process
and creates the object representing the DAG. As an example, the code
below shows how to create a custom Gaussian Mixture.

.. literalinclude:: ../../extensions/tutorials/src/main/java/eu/amidst/tutorial/usingAmidst/practice/CustomGaussianMixture.java
   :language: java

`See on
GitHub <https://github.com/amidst/tutorial/blob/master/src/main/java/eu/amidst/tutorial/usingAmidst/practice/CustomGaussianMixture.java>`__

.. _sec:lvmodels:dynamic:

Dynamic Models
--------------

.. _sec:lvmodels:dynamic:learning:

 Learning and saving to disk 
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When dealing with temporal data, it might be advisable to learn a
dynamic model. The module *latent-variable-models* in AMDIST also
suports such kinds of models. For that the classes inheriting
*DynamicModel* will be used. A synthesis of the learning process is
shown below.

.. raw:: latex

   \centering

.. figure:: ../_static/img/dynamicLearning.png
   :alt: Learning process scheme of a dynamic model
   :name: fig:lvmodels:dynamic:learning:scheme
   :width: 10cm
   :align: center

   Learning process scheme of a dynamic model

A code-example is given below. Here, a *Hidden Markov Model (HMM)* is
learnt from a financial dataset with temporal information. Note the
difference within the static learning is the way the dataset is loaded:
now, it is handled with an object of the class
*DataStream<DynamicDataInstance>*. Finally, the DBN learnt is saved to
disk with the method *DynamicBayesianNetworkWriter::save(String
pathFile)*.

.. literalinclude:: ../../extensions/tutorials/src/main/java/eu/amidst/tutorial/usingAmidst/examples/DynamicModelLearning.java
   :language: java

`See on
GitHub <https://github.com/amidst/tutorial/blob/master/src/main/java/eu/amidst/tutorial/usingAmidst/examples/DynamicModelLearning.java>`__

Inference
~~~~~~~~~

In the following code-example, the inference process of dynamic models
is illustrated. First, a DBN is loaded from disk (line 24). Then, a
dynamic dataset is loaded for testing our model (lines 29 to 30). Then
the inference algorithm and target variables are set. In the final loop,
the inference is perform for each data instance.

.. literalinclude:: ../../extensions/tutorials/src/main/java/eu/amidst/tutorial/usingAmidst/examples/DynamicModelInference.java
   :language: java

`See on
GitHub <https://github.com/amidst/tutorial/blob/master/src/main/java/eu/amidst/tutorial/usingAmidst/examples/DynamicModelInference.java>`__

Note that the inference algorithm can be easily change if line 33 is
modified by replacing it with:

.. code-block:: java

   InferenceAlgorithmForDBN = new FactoredFrontierForDBN(new VMP());

.. _sec:lvmodels:dynamic:custom:

Custom dynamic model
~~~~~~~~~~~~~~~~~~~~

Like for static models, we might be interested in creating our own
dynamic models. In this case, you will have to create a class inheriting
*DynamicModel*. Here below an example of a custom *Kalman Filter* is
given.

.. literalinclude:: ../../extensions/tutorials/src/main/java/eu/amidst/tutorial/usingAmidst/practice/CustomKalmanFilter.java
   :language: java

`See on
GitHub <https://github.com/amidst/tutorial/blob/master/src/main/java/eu/amidst/tutorial/usingAmidst/practice/CustomKalmanFilter.java>`__
