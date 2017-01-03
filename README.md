# AMIDST Toolbox ([http://www.amidsttoolbox.com](http://www.amidsttoolbox.com))
v.0.6.1

[![GitHub version](https://badge.fury.io/gh/amidst%2Ftoolbox.svg)](https://badge.fury.io/gh/amidst%2Ftoolbox)
[![Build Status](https://travis-ci.org/amidst/toolbox.svg?branch=release-0.6.1)](https://travis-ci.org/amidst/toolbox)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/71e9ce7f576e473fa7f4c6846293e9d6)](https://www.codacy.com/app/rafacabanas/toolbox?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=amidst/toolbox&amp;utm_campaign=Badge_Grade)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# Description<a name="Description"></a>

## Probabilistic Machine Learning


<p align="center">
<img title="PGM" src="https://amidst.github.io/toolbox/docs/web/figs/pgm-crop.png" width="250">
</p>


The AMIDST Toolbox allows you to model your problem using a flexible probabilistic language based on graphical models. 
Then you fit your model with data using a Bayesian approach to handle modeling uncertainty.

## Multi-core and distributed processing

<p align="center">
<img title="Taxonomy" src="https://amidst.github.io/toolbox/docs/web/figs/cluster-crop.png" width="250">
</p>

AMIDST provides tailored parallel (powered by Java 8 Streams) and distributed (powered by [Flink](https://flink.apache.org) or [Spark](http://spark.apache.org)) implementations of Bayesian parameter learning for batch and streaming data. This processing is based on flexible and [scalable message passing algorithms](http://amidst.github.io/toolbox/docs/dVMP.pdf).

#Features<a name="features"></a>

* **Probabilistic Graphical Models**: Specify your model using probabilistic graphical models with [latent variables](http://www.amidsttoolbox.com/documentation/0-6-1/examples-061/bnetworks-061/)
and [temporal dependencies](http://www.amidsttoolbox.com/documentation/0-6-1/examples-061/dbnetworks-061/). AMIDST contains a  large list of predefined latent variable models: 

![](http://amidst.github.io/toolbox/docs/web/figs/amidstModels-crop.png)

* **Scalable inference**: Perform inference on your probabilistic models with powerful approximate and
scalable algorithms.

* **Data Streams**: Update your models when new data is available. This makes our toolbox 
appropriate for learning from (massive) data streams.

* **Large-scale Data**: Use your defined models to process massive data sets in a distributed 
computer cluster using **Apache Flink** or (soon) **Apache Spark**. 

* **Extensible**: Code your models or algorithms within AMiDST and expand the toolbox functionalities. 
Flexible toolbox for researchers performing their experimentation in machine learning.

* **Interoperability**: Leverage existing functionalities and algorithms by interfacing 
to other software tools such as [Hugin](http://www.amidsttoolbox.com/documentation/0-6-1/examples-061/bnetworks-061/#sec:bns:huginlink), [MOA](http://www.amidsttoolbox.com/documentation/0-6-1/examples-061/bnetworks-061/#sec:bns:moalink), Weka, R, etc.


#Simple Code Example<a name="example"></a>

## Fitting a model with local data

```java
        //Load the data
        String filename = "./data.arff";
        DataStream<DataInstance> data = DataStreamLoader.open(filename);

        //Learn the model
        Model model = new CustomGaussianMixture(data.getAttributes());
        model.updateModel(data);

        System.out.println(model.getModel());

        // Save with .bn format
        BayesianNetworkWriter.save(model.getModel(), "./example.bn");
```

## Fitting a model with distributed data


```java
        //Load the data
        String filename = "hdfs://dataDistributed.arff";
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        DataFlink<DataInstance> data = DataFlinkLoader.loadDataFromFolder(env, filename, false);

        //Learn the model
        Model model = new CustomGaussianMixture(data.getAttributes());
        model.updateModel(data);

        System.out.println(model.getModel());

        // Save with .bn format
        BayesianNetworkWriter.save(model.getModel(), "./example.bn");
```

#Real-World Uses Cases<a name="uses"></a>

## Risk prediction in credit operations


<p align="center">
<img title="PGM" src="https://amidst.github.io/toolbox/docs/web/figs/creditcard-crop.png" width="250">
</p>

AMIDST Toolbox has been used to track concept drift and do risk prediction in credit operations, 
and as data is collected continuously and reported on a daily basis, this gives rise to a streaming data 
classification problem. This work has been performed in collaboration with one of our partners, 
the Spanish bank BCC. It is expected to be into production at the beginning of 2017.


## Recognition of traffic maneuvers

<p align="center">
<img title="PGM" src="https://amidst.github.io/toolbox/docs/web/figs/cars-crop.png" width="350">
</p>

AMIDST Toolbox has been used to prototype models for early recognition of traffic maneuver 
intentions. Similarly to the previous case, data is continuously collected by car on-board 
sensors giving rise to a large and quickly evolving data stream. This work has been performed 
in collaboration with one of our partners, DAIMLER. 

# Documentation<a name="documentation"></a>

* [Getting Started!](http://www.amidsttoolbox.com/documentation/0-6-1/first-steps-061/getting-started-061/) explains how to
install the AMIDST toolbox, how this toolbox makes use of Java 8 new functional style programming
features, and why it is based on a module based architecture.

* [Toolbox Functionalities](http://amidst.github.io/toolbox/ToolboxFunctionalities.html) describes
the main functionalities (i.e., data streams, BNs, DBNs, static and dynamic learning and inference
engines, etc.) of the AMIDST toolbox.

* [Bayesian networks: Code Examples](http://www.amidsttoolbox.com/documentation/0-6-1/examples-061/bnetworks-061/) includes
a list of source code examples explaining how to use some functionalities of the AMIDST toolbox.

* [Dynamic Bayesian networks: Code Examples](http://www.amidsttoolbox.com/documentation/0-6-1/examples-061/dbnetworks-061/)
includes some source code examples of functionalities related to Dynamic Bayesian networks.

* [FlinkLink](http://www.amidsttoolbox.com/documentation/0-6-1/examples-061/flinklink-061/): Code Examples includes some 
source code examples of functionalities related to the module that integrates Apache Flink with AMIDST.

* [SparkLink](http://www.amidsttoolbox.com/documentation/0-6-1/examples-061/sparklink-061/): some source code examples of 
functionalities related to the module that integrates Apache Spark with AMIDST.

* [API JavaDoc](http://javadoc.amidsttoolbox.com/) of the AMIDST toolbox. 


# Scalability

## Multi-Core Scalablity using Java 8 Streams

Scalability is a main concern for the AMIDST toolbox. Java 8 streams are used to
provide parallel implementations of our learning algorithms. If more computation capacity is needed to process
data, AMIDST users can also use more CPU cores. As an example, the following figure shows how
the data processing capacity of our toolbox increases given the number of CPU cores when learning an
a probabilistic model (including a class variable C, two latent variables (dashed nodes), multinomial
(blue nodes) and Gaussian (green nodes) observable variables) using the AMIDST's learning engine.
As can be seen, using our variational learning engine, AMIDST toolbox is able to process data in the order
of gigabytes (GB) per hour depending on the number of available CPU cores with large and complex PGMs with
latent variables. Note that, these experiments were carried out on a Ubuntu Linux server with a x86_64
architecture and 32 cores. The size of the processed data set was measured according to the
[Weka](www.cs.waikato.ac.nz/ml/weka/)'s ARFF format.

<p align="center">
<img src="https://amidst.github.io/toolbox/docs/scalability.png" width="800">
</p>


## Distributed Scalablity using Apache Flink

If your data is really big and can not be stored in a single laptop, you can also learn 
your probabilistic model on it by using the AMIDST distributed learning engine based on 
a novel and state-of-the-art [distributed message passing scheme](http://amidst.github.io/toolbox/docs/dVMP.pdf) implemented on top 
of [Apache Flink](http://flink.com). As detailed in this [paper](http://amidst.github.io/toolbox/docs/dVMP.pdf), we were able to perform inference in a billion node (i.e. 10^9) probabilistic model in an Amazon's cluster with 2, 4, 8 and 16 nodes, each node containing 8 processing units. The following figure shows the scalability of our approach under these settings. 

<p align="center">
<img src="https://amidst.github.io/toolbox/docs/web/figs/flink-scalability.png" width="800">
</p>

# Spark Link Module on AMIDST

This module integrates the functionality of the AMIDST toolbox with the [Apache Spark](http://spark.apache.org) platform.

The following functionality is already implemented on the **sparklink** module:

* Data Sources integration: Reading and writing data from SparkSQL on AMIDST
* Distributed Sampling of Bayesian Networks
* Parametric learning from distributed data (Maximum Likelihood)

More information [here](https://github.com/amidst/toolbox/tree/develop/sparklink)

# Publications & Use-Cases

The following repository [https://github.com/amidst/toolbox-usecases](https://github.com/amidst/toolbox-usecases)
contains the source code and details about the publications and use-cases using the AMIDST toolbox.

# Upcoming Developments

The AMIDST toolbox is an expanding project and upcoming developments include for instance the ongoing
integration of the toolbox in [Spark](http://spark.apache.org) to enlarge its scalability capacities.
In addition, a new link to [R](http://static.amidst.eu/upload/dokumenter/Posters/PosterUseR.pdf)
is still in progress which will expand the AMIDST user-base.

# Contributing to AMIDST

AMIDST is an open source toolbox and the end-users are encouraged to upload their
contributions (which may include basic contributions, major extensions, and/or use-cases)
following the indications given in this [link](http://amidst.github.io/toolbox/ContributingToAMIDST.html).



# Acknowledgements and License
This software was performed as part of the AMIDST project. AMIDST has received funding from the European Union’s Seventh Framework Programme for research, technological development and demonstration under grant agreement no 619209.

This software is distributed under Apache License Version 2.0
