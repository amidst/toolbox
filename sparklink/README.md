# Spark Link Module on AMIDST

This module integrates the functionality of the AMIDST toolbox with the [Apache Spark](http://spark.apache.org) platform.

The following functionality is already implemented on the **sparklink** module:

* Data Sources integration: Reading and writing data from SparkSQL on AMIDST
* Distributed Sampling of Bayesian Networks
* Parametric learning from distributed data (Maximum Likelihood)

###### ROADMAP

* Support for VMP


## API

This module is designed to interact with the [SparkSQL](http://spark.apache.org/docs/latest/sql-programming-guide.html) `DataFrame` API. It makes immediately available all the compatible data sources, namely, CSV, Parquet, JSON...

the `DataSpark`class is the entry point for the **sparklink** API, by transforming a `DataFrame` object into am AMIDST distributed collection over the Spark cluster.

Notice that this API is interoperable between **Java** and **Scala**.

### Examples (Scala)

###### Setting up Spark

```scala
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.sql.SQLContext

// Init Spark
val conf = new SparkConf().setAppName("SparkLink!")
val sc = new SparkContext(conf)
val sqlContext = new org.apache.spark.sql.SQLContext(sc)
```

###### Reading Data

```scala
import eu.amidst.sparklink.core.data.DataSpark
import eu.amidst.sparklink.core.io.DataSparkLoader

// Path for your file:
val path = "hdfs://..."
// {format} can be any supported one: parquet, json, csv...
val df = sqlContext.read.format("parquet").load(path);

// Load data into AMIDST, the schema is automatically transformed
val dataSpark : DataSpark = DataSparkLoader.loadSparkDataFrame(df)

```

###### Learning a BN
```scala
import eu.amidst.core.io.BayesianNetworkLoader

// Asume a structure bn, provided from file or previously built
import eu.amidst.sparklink.core.util.BayesianNetworkSampler
import eu.amidst.sparklink.core.learning.ParallelMaximumLikelihood

// Load a Bayesian Network
val bn = BayesianNetworkLoader.loadFromFile("<path>")

val parameterLearningAlgorithm = new ParallelMaximumLikelihood()
//We fix the DAG structure
parameterLearningAlgorithm.setDAG(bn.getDAG)
//We set the batch size which will be employed to learn the model in parallel
parameterLearningAlgorithm.setBatchSize(100)
//We set the data which is going to be used for leaning the parameters
parameterLearningAlgorithm.setDataSpark(dataSpark)
//We perform the learning
parameterLearningAlgorithm.runLearning()
//And we get the model
val result = parameterLearningAlgorithm.getLearntBayesianNetwork()

```

###### Sampling a BN
```scala
import eu.amidst.sparklink.core.util.BayesianNetworkSampler

// Load a Bayesian Network
val bn = BayesianNetworkLoader.loadFromFile("<path>")

// Create a local sampler
val sampler = new BayesianNetworkSampler(bn)

// Sample data in parallel:
val nSamples = 1000000 // Number of sampled examples (accross all nodes)
val parallelism = 4 // Number of parallel partitions (cores)
val sampledDataSpark : DataSpark = sampler.sampleToDataSpark(sc, nSamples, parallelism)

// Use the data or write it to a file (any format):
sampledDataSpark.getDataFrame().write.format("json").save("hdfs://...")
```


### Examples (Java)

**TODO** very soon...



## Building and compatibility

**To be Tested!** The sparklink module should work with any version of Spark 1.x that includes the DataFrame API, namely 1.3.x or up.

By default the pom file is configured to compile it against the latest stable version of Spark v1.x available.

## Development and usage questions

Ask @jacintoArias
