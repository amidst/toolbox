# New Release of AMIDST 2.0

We have made a new release of AMIDST 2.0.

The new features of this release are incorporated in the Core Dynamic module, specifically the Dynamic Inference Engine, 
including all the inference algorithms for dynamic Bayesian networks. 
It consists of the implementation of the dynamic versions of HUGIN Exact Inference, 
Importance Sampling, Variational Message Passing by means of the Factored Frontier algorithm, as well as Dynamic MAP.

# Scope

This toolbox offers a collection of scalable and parallel algorithms for inference and learning of both static and dynamic Bayesian 
networks from streaming data. For example, AMIDST provides parallel multi-core implementations of Bayesian parameter 
learning, using streaming variational Bayes and variational message passing. Additionally, AMIDST efficiently leverages 
existing functionalities and algorithms by interfacing to existing software tools such as [Hugin](http://www.hugin.com) 
and [MOA](http://moa.cms.waikato.ac.nz). AMIDST is an open source Java toolbox released under the 
Apache Software License version 2.0.

The figure below shows a non-exhaustive taxonomy of relevant data mining tools dealing with probabilistic graphical models (PGMs) and data streams. To the best of our knowledge, existing software systems for PGMs only focus on mining stationary data sets, and hence, the main goal of AMIDST is to fill this gap and provide a significant contribution within the areas of PGMs and mining data streams.

<p align="center">
<img title="Taxonomy" src="https://amidst.github.io/toolbox/docs/taxonomy.png" width="500">
</p>

# Documentation<a name="documentation"></a>

* [Getting Started!](http://amidst.github.io/toolbox/GettingStarted.html) explains how to install the AMIDST toolbox, how this toolbox makes use of Java 8 new functional style programming features, and why it is based on a module based architecture.

* [Toolbox Functionalities](http://amidst.github.io/toolbox/ToolboxFunctionalities.html) describes the main functionalities (i.e., data streams, BNs, DBNs, static and dynamic learning and inference engines, etc.) of the AMIDST toolbox.

* [Bayesian networks: Code Examples](http://amidst.github.io/toolbox/CodeExamples.html) includes a list of source code examples explaining how to use some functionalities of the AMIDST toolbox.

* [Dynamic Bayesian networks: Code Examples](http://amidst.github.io/toolbox/DynamicCodeExamples.html) includes some source code examples of functionalities related to Dynamic Bayesian networks.

* [API JavaDoc](http://amidst.github.io/toolbox/javadoc/index.html) of the AMIDST toolbox. 

# Scalability

Scalability is a main concern for the AMIDST toolbox. Java 8 functional programming style is used to provide parallel implementations of the algorithms. If more computation capacity is needed to process data streams, AMIDST users can also use more CPU cores. As an example, the following figure shows how the data processing capacity of our toolbox increases given the number of CPU cores when learning an hybrid BN model (including a class variable C, two latent variables (dashed nodes), multinomial (blue nodes) and Gaussian (green nodes) observable variables) using the AMIDST's learning engine. As can be seen, using our variational learning engine, AMIDST toolbox is able to process data in the order of gigabytes (GB) per hour depending on the number of available CPU cores with large and complex PGMs with latent variables. Note that, these experiments were carried out on a Ubuntu Linux server with a x86_64 architecture and 32 cores. The size of the processed data set was measured according to the [Weka](www.cs.waikato.ac.nz/ml/weka/)'s ARFF format.

<p align="center">
<img src="https://amidst.github.io/toolbox/docs/scalability.png" width="800">
</p>

# Publications & Use-Cases

The following repository [https://github.com/amidst/toolbox-usecases](https://github.com/amidst/toolbox-usecases) contains the source code and details about the publications and use-cases using the AMIDST toolbox.

# Upcoming Developments

The AMIDST toolbox is an expanding project and upcoming developments include for instance the ongoing integration of the toolbox in Big Data platforms such as [Flink](http://flink.apache.org) and [Spark](http://spark.apache.org) to enlarge its scalability capacities. In addition, a new link to [R](http://static.amidst.eu/upload/dokumenter/Posters/PosterUseR.pdf) is still in progress which will expand the AMIDST user-base.

# Contributing to AMIDST

AMIDST is an open source toolbox and the end-users are encouraged to upload their contributions (which may include basic contributions, major extensions, and/or use-cases) following the indications given in this [link](http://amidst.github.io/toolbox/ContributingToAMIDST.html).
