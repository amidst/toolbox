# Toolbox Functionalities<a name="functionalities"></a>

The AMIDST system is an open source Java 8 toolbox that makes use of a functional programming 
style to support parallel processing on mutli-core CPUs (Masegosa et al., 2015). 
AMIDST provides a collection of functionalities and algorithms for learning both static and dynamic 
hybrid Bayesian networks from streaming data. 

In what follows, we describe the main functionalities that the AMIDST toolbox supplies.

##AMIDST Core Structures<a name="pgms"></a>

An overview of the core structures of the AMIDST toolbox is illustrated in the figure below. 
Blue boxes represent software components that have been fully implemented, while green boxes represent components that are part of AMIDST design specification to be implemented in the future.

<p align="center">
<img title="Illustration of the design of the AMIDST components related to core structures. Nomenclature: The boxes in the figure represent software components (sets, possibly singletons, of classes), a rounded-arc going from X to Y indicate that Y ‘uses/references’ X, and an arc with an arrow from X to Y implies inheritance.
" src="https://amidst.github.io/toolbox/docs/PGMs.pdf" width="700">
</p>

The core structures mainly relates to the basic components that play a 
key role in ensuring the implementation of the different AMIDST learning and inference algorithms.

For starters, instantiation of a particular **probabilistic graphical model** (**PGM** component) will be required. 
Currently, it is possible to create either a static Bayesian network (**BN** component) or a two time-slice dynamic 
Bayesian network (**2T-DBN** component). Then, the **DAG** component is defined over a list of **Static Variables**, whereas 
the **2T-DBN** component is defined over a list of **Dynamic Variables**.

Both **BN** and **2T-DBN** models rely on the **Distributions** component to define the conditional probability distributions. 
This component supports both **Conditional Linear Gaussians** and **Exponential Family** representations of the distributions. 
The implementation of the latter ensures an alternative representation of the standard distributions based on vectors of natural and 
moment parameters.

##AMIDST Modules<a name="modules"></a> 

The following figure shows a high-level overview of the key modules of the AMIDST toolbox:

<p align="center">
<img title="Illustration of the design of the AMIDST components related to core structures." src="https://amidst.github.io/toolbox/docs/Functionalities.pdf" width="700">
</p>

###Data Sources<a name="datasources"></a> 

In the AMIDST framework, we consider two types of data sources for learning, namely, **DataStream**, 
where data arrives at high frequency with no storage of historical data, and **DataOnMemory** for static 
databases that simply correspond to traditional databases. 
The data format supported by AMIDST is [Weka](www.cs.waikato.ac.nz/ml/weka/)’s attribute-relation file format (ARFF).

Moreover, we ensure a scalable **DistributedDataProcessing** using [Apache Flink](http://flink.apache.org) that runs the AMIDST developed algorithms 
on top of the Hadoop and Yarn architectures on [Amazon Elastic MapReduce (EMR)](https://aws.amazon.com/elasticmapreduce/). This part will be soon released!

Each of the data sources are furthermore connected to the **DataInstance** and **DynamicDataInstance** components 
that represent a particular evidence configuration (static or dynamic, respectively) such as the observed values 
of a collection of variables at time t or a particular row in a database.

###Inference Engines<a name="inference"></a>

* **The Static Inference Engine** includes all the inference algorithms for a static Bayesian networks (BNs). 
It consists of five implemented methods, namely, **HUGIN Exact Inference** (Madsen et al., 2005), 
**Importance Sampling** (Hammersley and Handscomb, 1964; Salmeron et al., 2015), 
**Variational Message Passing (VMP)** (Winn and Bishop, 2005), **Static MAP Inference**, and **MPE Inference**. 
This module has been designed and implemented to be extendable and support future implementations of other 
inference algorithms such as the **Expectation Propagation (EP)** algorithm. 

* **The Dynamic Inference Engine** includes all the inference algorithms for dynamic Bayesian networks (DBNs). 
It consists of the implementation of the dynamic versions of HUGIN Exact Inference, Importance Sampling, 
Variational Message Passing by means of the **Factored Frontier** algorithm, as well as **Dynamic MAP**. 
The latter is implemented by computing (variational) posterior distributions over subsets of MAP variables. 


###Learning Engine<a name="learning"></a>  

Learning engine includes the **Structural Learning**, **Parameter Learning**, and **Feature Selection** (still under development) 
components that are connected to both **PGM** and **DataStream**. 

* **The Structural Learning** component currently includes parallel implementations of the tree augmented naive Bayes 
classifier (**Parallel TAN**) (Madsen et al., 2014) as well as a constraint-based method for learning general 
Bayesian networks (**Parallel PC**) (Madsen et al., 2015). Both of the these implementations are based on the use of threads, 
and rely on interfacing to the Hugin AMIDST API.

* **The Parameter Learning** in AMIDST models can be performed using either a **fully Bayesian approach** 
or a **maximum likelihood-based approach** (Scholz, 2004). The Bayesian approach to parameter learning is closely linked to the 
task of probabilistic inference supported by the Streaming Variational Bayes algorithm (Broderick et al., 2013), using Variational Message Passing as the underlying inference 
algorithm (VMP) provided by the Static Inference Engine in the toolbox.

##Links to MOA and Hugin<a name="librarylinks"></a> 
AMIDST leverages existing functionalities and algorithms by interfacing to software tools such as 
[Hugin](http://www.hugin.com), and [MOA](http://moa.cms.waikato.ac.nz) (Massive Online Analysis). 
This allows the toolbox to efficiently exploit well-established systems and also broaden the AMIDST 
user-base. 

* **HuginLink** consists of a set of functionalities implemented to link the AMIDST toolbox 
with [Hugin](http://www.hugin.com) commercial software. 
This connection extends AMIDST by providing some of the main functionalities 
of [Hugin](http://www.hugin.com), such as exact inference algorithms and scalable structural learning 
algorithms (Madsen et al., 2014). [Hugin](http://www.hugin.com) is a third-party commercial software 
and to access to these functionalities it is needed a license of the software and to follow some specific 
installation steps (further information is given [here](http://amidst.github.io/toolbox/#installhugin)).

* **MoaLink** ensures an easy use of AMDIST functionalities within [MOA](http://moa.cms.waikato.ac.nz). 
The main idea is that any model deployed in AMIDST can be integrated and evaluated using MOA's graphical user interface. 
As a proof of concept, MoaLink already provides a classification, a regression and a clustering method based on 
BN models with latent variables. These models are learnt in a streaming fashion using AMIDST Learning Engine. 

##Concept drift<a name="conceptdrift"></a> 
AMIDST also offers some support for dealing with concept drift while learning BNs from data streams. 
More precisely, the toolbox supports a novel probabilistic approach based on latent variables 
(Borchani et al., 2015) for detecting and adapting to concept drifts.

###Bibliography

Hanen Borchani, Ana M. Martinez, Andres R. Masegosa, et al. 
Modeling concept drift: A probabilistic graphical model based approach. 
In The Fourteenth International Symposium on Intelligent Data Analysis, pages 72-83, 2015.

Tamara Broderick, Nicholas Boyd, Andre Wibisono, Ashia C. Wilson, and Michael I. Jordan. 
Streaming variational Bayes. 
In Advances in Neural Information Processing Systems, pages 1727–1735, 2013.

J.M. Hammersley and D.C. Handscomb. 
Monte Carlo Methods. 
Methuen & Co, London, UK, 1964.

Anders L. Madsen, Frank Jensen, Uffe B. Kjærulff, and Michael Lang. 
HUGIN-the tool for Bayesian networks and influence diagrams. 
International Journal of Artificial Intelligence Tools, 14(3):507–543, 2005.

Anders L. Madsen, Frank Jensen, Antonio Salmeron, et al. 
A new method for vertical parallelisation of TAN learning Based on balanced incomplete block designs. 
In The Seventh European Workshop on Probabilistic Graphical Models, pages 302–317, 2014.

Anders L. Madsen, Frank Jensen, Antonio Salmeron, et al. 
Parallelization of the PC Algorithm. 
In The XVI Conference of the Spanish Association for Artificial Intelligence, pages 14-24, 2015.

Andres R. Masegosa, Ana M. Martinez, and Hanen Borchani. 
Probabilistic graphical models on multi-core CPUs using Java 8. 
IEEE Computational Intelligence Magazine, Special Issue on Computational Intelligence Software, Under review, 2015.

Antonio Salmeron, Dario Ramos-Lopez, Hanen Borchani, et al. 
Parallel importance sampling in conditional linear Gaussian networks. 
In The XVI Conference of the Spanish Association for Artificial Intelligence, pages 36-46, 2015.

F. W. Scholz. Maximum Likelihood Estimation. John Wiley & Sons, Inc., 2004.

John M. Winn and Christopher M. Bishop. 
Variational message passing. Journal of Machine Learning Research, 6:661–694, 2005.