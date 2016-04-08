## Bayesian Networks: Code Examples<a name="documentation"></a>

   * [Data Streams](#datastreamsexample)
   * [Random Variables](#variablesexample)
   * [Bayesian Networks](#bnexample)
       * [Creating Bayesian Networks](#bnnohiddenexample)
       * [Creating Bayesian Networks with latent variables](#bnhiddenexample)
       * [Modifying Bayesian Networks](#bnmodifyexample)
   * [I/O Functionality](#ioexample)
       * [I/O of Data Streams](#iodatastreamsexample)
       * [I/O of Bayesian Networks](#iobnsexample)
   * [Inference Algorithms](#inferenceexample)
       * [The Inference Engine](#inferenceengingeexample)
       * [Variational Message Passing](#vmpexample)
       * [Importance Sampling](#isexample)
   * [Learning Algorithms](#learningexample)
       * [Maximum Likelihood](#mlexample)
       * [Parallel Maximum Likelihood](#pmlexample)
       * [Streaming Variational Bayes](#svbexample)
       * [Parallel Streaming Variational Bayes](#psvbexample)
   * [Concept Drift Methods](#conceptdriftexample)
       * [Naive Bayes with Virtual Concept Drift Detection](#nbconceptdriftexample)
   * [HuginLink](#huginglinkexample)
       * [Models conversion between AMIDST and Hugin](#huginglinkconversionexample)
       * [I/O of Bayesian Networks with Hugin net format](#huginglinkioexample)
       * [Invoking Hugin's inference engine](#huginglinkinferenceexample)
       * [Invoking Hugin's Parallel TAN](#huginglinkTANexample)
   * [MoaLink](#moalinkexample)
       * [AMIDST Classifiers from MOA](#moalinkclassifiersexample)
       * [AMIDST Regression from MOA](#moalinkregressionsexample)


## Data Streams<a name="datastreamsexample"></a>
  
In this example we show how to use the main features of a *DataStream* object. More precisely,  we show six different ways of iterating over the data samples of a *DataStream* object.


```java
//We can open the data stream using the static class DataStreamLoader
DataStream<DataInstance> data = DataStreamLoader.openFromFile("datasets/SmallDataSet.arff");

//Access to the attributes defining the data set
System.out.println("Attributes defining the data set");
for (Attribute attribute : data.getAttributes()) {
    System.out.println(attribute.getName());
}
Attribute attA = data.getAttributes().getAttributeByName("A");

//1. Iterating over samples using a for loop
System.out.println("1. Iterating over samples using a for loop");
for (DataInstance dataInstance : data) {
    System.out.println("The value of attribute A for the current data instance is: " +
                                                          dataInstance.getValue(attA));
}


//2. Iterating using streams. We need to restart the data again 
//   as a DataStream can only be used once.
System.out.println("2. Iterating using streams.");
data.restart();
data.stream().forEach(dataInstance ->
                System.out.println("The value of attribute A for the current data "+
                                        instance is: " + dataInstance.getValue(attA))
);


//3. Iterating using parallel streams.
System.out.println("3. Iterating using parallel streams.");
data.restart();
data.parallelStream(10).forEach(dataInstance ->
                System.out.println("The value of attribute A for the current data "+
                                        instance is: " + dataInstance.getValue(attA))
);

//4. Iterating over a stream of data batches.
System.out.println("4. Iterating over a stream of data batches.");
data.restart();
data.streamOfBatches(10).forEach(batch -> {
    for (DataInstance dataInstance : batch)
                System.out.println("The value of attribute A for the current data "+
                                        instance is: " + dataInstance.getValue(attA))
});

//5. Iterating over a parallel stream of data batches.
System.out.println("5. Iterating over a parallel stream of data batches.");
data.restart();
data.parallelStreamOfBatches(10).forEach(batch -> {
    for (DataInstance dataInstance : batch)
                System.out.println("The value of attribute A for the current data "+
                                        instance is: " + dataInstance.getValue(attA))
});


//6. Iterating over data batches using a for loop
System.out.println("6. Iterating over data batches using a for loop.");
for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(10)) {
    for (DataInstance dataInstance : batch)
                System.out.println("The value of attribute A for the current data "+
                                        instance is: " + dataInstance.getValue(attA))
}
```

[[Back to Top]](#documentation)

## Random Variables<a name="variablesexample"></a>

This example show the basic functionality of the classes Variables and Variable.

```java
//We first create an empty Variables object
Variables variables = new Variables();

//We invoke the "new" methods of the object Variables to create new variables.
//Now we create a Gaussian variables
Variable gaussianVar = variables.newGaussianVariable("Gaussian");

//Now we create a Multinomial variable with two states
Variable multinomialVar = variables.newMultionomialVariable("Multinomial", 2);

//Now we create a Multinomial variable with two states: TRUE and FALSE
Variable multinomialVar2 = variables.newMultionomialVariable("Multinomial2", 
                                                Arrays.asList("TRUE, FALSE"));

//For Multinomial variables we can iterate over their different states
FiniteStateSpace states = multinomialVar2.getStateSpaceType();
states.getStatesNames().forEach(System.out::println);

//Variable objects can also be used, for example, to know if one variable 
//can be set as parent of some other variable
System.out.println("Can a Gaussian variable be parent of Multinomial variable? " +
        (multinomialVar.getDistributionType().isParentCompatible(gaussianVar)));

System.out.println("Can a Multinomial variable be parent of Gaussian variable? " +
        (gaussianVar.getDistributionType().isParentCompatible(multinomialVar)));
```


[[Back to Top]](#documentation)


## Bayesian Networks<a name="bnexample"></a>

### Creating Bayesian Networks<a name="bnnohiddenexample"></a>

In this example, we take a data set, create a BN and we compute the log-likelihood of all the samples
of this data set. The numbers defining the probability distributions of the BN are randomly fixed.

```java
//We can open the data stream using the static class DataStreamLoader
DataStream<DataInstance> data = DataStreamLoader.openFromFile("datasets/syntheticData.arff");


/**
 * 1. Once the data is loaded, we create a random variable for each of the attributes (i.e. data columns)
 * in our data.
 *
 * 2. {@link Variables} is the class for doing that. It takes a list of Attributes and internally creates
 * all the variables. We create the variables using Variables class to guarantee that each variable
 * has a different ID number and make it transparent for the user.
 *
 * 3. We can extract the Variable objects by using the method getVariableByName();
 */
Variables variables = new Variables(data.getAttributes());

Variable a = variables.getVariableByName("A");
Variable b = variables.getVariableByName("B");
Variable c = variables.getVariableByName("C");
Variable d = variables.getVariableByName("D");
Variable e = variables.getVariableByName("E");
Variable g = variables.getVariableByName("G");
Variable h = variables.getVariableByName("H");
Variable i = variables.getVariableByName("I");

/**
 * 1. Once you have defined your {@link Variables} object, the next step is to create
 * a DAG structure over this set of variables.
 *
 * 2. To add parents to each variable, we first recover the ParentSet object by the method
 * getParentSet(Variable var) and then call the method addParent().
 */
DAG dag = new DAG(variables);

dag.getParentSet(e).addParent(a);
dag.getParentSet(e).addParent(b);

dag.getParentSet(h).addParent(a);
dag.getParentSet(h).addParent(b);

dag.getParentSet(i).addParent(a);
dag.getParentSet(i).addParent(b);
dag.getParentSet(i).addParent(c);
dag.getParentSet(i).addParent(d);

dag.getParentSet(g).addParent(c);
dag.getParentSet(g).addParent(d);

/**
 * 1. We first check if the graph contains cycles.
 *
 * 2. We print out the created DAG. We can check that everything is as expected.
 */
if (dag.containCycles()) {
    try {
    } catch (Exception ex) {
        throw new IllegalArgumentException(ex);
    }
}

System.out.println(dag.toString());


/**
 * 1. We now create the Bayesian network from the previous DAG.
 *
 * 2. The BN object is created from the DAG. It automatically looks at the distribution tye
 * of each variable and their parents to initialize the Distributions objects that are stored
 * inside (i.e. Multinomial, Normal, CLG, etc). The parameters defining these distributions are
 * properly initialized.
 *
 * 3. The network is printed and we can have look at the kind of distributions stored in the BN object.
 */
BayesianNetwork bn = new BayesianNetwork(dag);
System.out.println(bn.toString());


/**
 * 1. We iterate over the data set sample by sample.
 *
 * 2. For each sample or DataInstance object, we compute the log of the probability that the BN object
 * assigns to this observation.
 *
 * 3. We accumulate these log-probs and finally we print the log-prob of the data set.
 */
double logProb = 0;
for (DataInstance instance : data) {
    logProb += bn.getLogProbabiltyOf(instance);
}
System.out.println(logProb);

BayesianNetworkWriter.saveToFile(bn, "networks/BNExample.bn");
```

[[Back to Top]](#documentation)


### Creating Bayesian Networks with latent variables <a name="bnhiddenexample"></a>

In this example, we simply show how to create a BN model with hidden variables. We simply create a BN for clustering, i.e.,  a naive-Bayes like structure with a single common hidden variable acting as parant of all the observable variables.
 
```java
//We can open the data stream using the static class DataStreamLoader
DataStream<DataInstance> data = DataStreamLoader.openFromFile("datasets/syntheticData.arff");

/**
 * 1. Once the data is loaded, we create a random variable for each of the attributes (i.e. data columns)
 * in our data.
 *
 * 2. {@link Variables} is the class for doing that. It takes a list of Attributes and internally creates
 * all the variables. We create the variables using Variables class to guarantee that each variable
 * has a different ID number and make it transparent for the user.
 *
 * 3. We can extract the Variable objects by using the method getVariableByName();
 */
Variables variables = new Variables(data.getAttributes());

Variable a = variables.getVariableByName("A");
Variable b = variables.getVariableByName("B");
Variable c = variables.getVariableByName("C");
Variable d = variables.getVariableByName("D");
Variable e = variables.getVariableByName("E");
Variable g = variables.getVariableByName("G");
Variable h = variables.getVariableByName("H");
Variable i = variables.getVariableByName("I");

/**
 * 1. We create the hidden variable. For doing that we make use of the method "newMultionomialVariable". When
 * a variable is created from an Attribute object, it contains all the information we need (e.g.
 * the name, the type, etc). But hidden variables does not have an associated attribute
 * and, for this reason, we use now this to provide this information.
 *
 * 2. Using the "newMultionomialVariable" method, we define a variable called HiddenVar, which is
 * not associated to any attribute and, then, it is a latent variable, its state space is a finite set with two elements, and its
 * distribution type is multinomial.
 *
 * 3. We finally create the hidden variable using the method "newVariable".
 */

Variable hidden = variables.newMultionomialVariable("HiddenVar", Arrays.asList("TRUE", "FALSE"));

/**
 * 1. Once we have defined your {@link Variables} object, including the latent variable,
 * the next step is to create a DAG structure over this set of variables.
 *
 * 2. To add parents to each variable, we first recover the ParentSet object by the method
 * getParentSet(Variable var) and then call the method addParent(Variable var).
 *
 * 3. We just put the hidden variable as parent of all the other variables. Following a naive-Bayes
 * like structure.
 */
DAG dag = new DAG(variables);

dag.getParentSet(a).addParent(hidden);
dag.getParentSet(b).addParent(hidden);
dag.getParentSet(c).addParent(hidden);
dag.getParentSet(d).addParent(hidden);
dag.getParentSet(e).addParent(hidden);
dag.getParentSet(g).addParent(hidden);
dag.getParentSet(h).addParent(hidden);
dag.getParentSet(i).addParent(hidden);

/**
 * We print the graph to see if is properly created.
 */
System.out.println(dag.toString());

/**
 * 1. We now create the Bayesian network from the previous DAG.
 *
 * 2. The BN object is created from the DAG. It automatically looks at the distribution type
 * of each variable and their parents to initialize the Distributions objects that are stored
 * inside (i.e. Multinomial, Normal, CLG, etc). The parameters defining these distributions are
 * properly initialized.
 *
 * 3. The network is printed and we can have look at the kind of distributions stored in the BN object.
 */
BayesianNetwork bn = new BayesianNetwork(dag);
System.out.println(bn.toString());

/**
 * Finally teh Bayesian network is saved to a file.
 */
BayesianNetworkWriter.saveToFile(bn, "networks/BNHiddenExample.bn");
```

[[Back to Top]](#documentation)


### Modifying Bayesian Networks <a name="bnmodifyexample"></a>

In this example we show how to access and modify the conditional probabilities of a Bayesian network model.

```java
//We first generate a Bayesian network with one multinomial, one Gaussian variable and one link.
BayesianNetworkGenerator.setNumberOfGaussianVars(1);
BayesianNetworkGenerator.setNumberOfMultinomialVars(1,2);
BayesianNetworkGenerator.setNumberOfLinks(1);

BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();

//We print the randomly generated Bayesian networks
System.out.println(bn.toString());

//We first access the variable we are interested in
Variable multiVar = bn.getStaticVariables().getVariableByName("DiscreteVar0");

//Using the above variable we can get the associated distribution and modify it
Multinomial multinomial = bn.getConditionalDistribution(multiVar);
multinomial.setProbabilities(new double[]{0.2, 0.8});

//Same than before but accessing the another variable
Variable normalVar = bn.getStaticVariables().getVariableByName("GaussianVar0");

//In this case, the conditional distribtuion is of the type "Normal given Multinomial Parents"
Normal_MultinomialParents normalMultiDist = bn.getConditionalDistribution(normalVar);
normalMultiDist.getNormal(0).setMean(1.0);
normalMultiDist.getNormal(0).setVariance(1.0);

normalMultiDist.getNormal(1).setMean(0.0);
normalMultiDist.getNormal(1).setVariance(1.0);

//We print modified Bayesian network
System.out.println(bn.toString());
```

[[Back to Top]](#documentation)

## I/O Functionality <a name="ioexample"></a>

### I/O of Data Streams <a name="iodatastreamsexample"></a>

In this example we show how to load and save data sets from [.arff](http://www.cs.waikato.ac.nz/ml/weka/arff.html) files. 

```java
//We can open the data stream using the static class DataStreamLoader
DataStream<DataInstance> data = DataStreamLoader.openFromFile("datasets/syntheticData.arff");

//We can save this data set to a new file using the static class DataStreamWriter
DataStreamWriter.writeDataToFile(data, "datasets/tmp.arff");
```

[[Back to Top]](#documentation)

### I/O of Bayesian Networks <a name="iobnsexample"></a>


In this example we show how to load and save Bayesian networks models for a binary file with ".bn" extension. In this toolbox Bayesian networks models are saved as serialized objects.

```java
//We can load a Bayesian network using the static class BayesianNetworkLoader
BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/WasteIncinerator.bn");

//Now we print the loaded model
System.out.println(bn.toString());

//Now we change the parameters of the model
bn.randomInitialization(new Random(0));

//We can save this Bayesian network to using the static class BayesianNetworkWriter
BayesianNetworkWriter.saveToFile(bn, "networks/tmp.bn");
```

[[Back to Top]](#documentation)

## Inference Algorithms <a name="inferenceexample"></a>

### The Inference Engine <a name="inferenceengingeexample"></a>

This example show how to perform inference in a Bayesian network model using the InferenceEngine static class. This class aims to be a straigthfoward way to perform queries over a Bayesian network model. By the default the \textit{VMP} inference method is invoked.

```java
//We first load the WasteIncinerator bayesian network which has multinomial 
//and Gaussian variables.
BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/WasteIncinerator.bn");

//We recover the relevant variables for this example: Mout which is normally 
//distributed, and W which is multinomial.
Variable varMout = bn.getStaticVariables().getVariableByName("Mout");
Variable varW = bn.getStaticVariables().getVariableByName("W");

//Set the evidence.
Assignment assignment = new HashMapAssignment(1);
assignment.setValue(varW,0);

//Then we query the posterior of
System.out.println("P(Mout|W=0) = " + InferenceEngine.getPosterior(varMout, bn, assignment));

//Or some more refined queries
System.out.println("P(0.7<Mout<6.59 | W=0) = " + 
 InferenceEngine.getExpectedValue(varMout, bn, v -> (0.7 < v && v < 6.59) ? 1.0 : 0.0 ));
```

[[Back to Top]](#documentation)

### Variational Message Passing <a name="vmpexample"></a>

This example we show how to perform inference on a general Bayesian network using the Variational Message Passing (VMP)
algorithm detailed in

> Winn, J. M., Bishop, C. M. (2005). Variational message passing. In Journal of Machine Learning Research (pp. 661-694).



```java
//We first load the WasteIncinerator bayesian network which has multinomial 
//and Gaussian variables.
BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/WasteIncinerator.bn");

//We recover the relevant variables for this example: Mout which is normally 
//distributed, and W which is multinomial.
Variable varMout = bn.getStaticVariables().getVariableByName("Mout");
Variable varW = bn.getStaticVariables().getVariableByName("W");

//First we create an instance of a inference algorithm. In this case, we use 
//the VMP class.
InferenceAlgorithm inferenceAlgorithm = new VMP();

//Then, we set the BN model
inferenceAlgorithm.setModel(bn);

//If exists, we also set the evidence.
Assignment assignment = new HashMapAssignment(1);
assignment.setValue(varW,0);
inferenceAlgorithm.setEvidence(assignment);

//Then we run inference
inferenceAlgorithm.runInference();

//Then we query the posterior of
System.out.println("P(Mout|W=0) = " + inferenceAlgorithm.getPosterior(varMout));

//Or some more refined queries
System.out.println("P(0.7<Mout<6.59 | W=0) = " + 
 inferenceAlgorithm.getExpectedValue(varMout, v -> (0.7 < v && v < 6.59) ? 1.0 : 0.0 ));

//We can also compute the probability of the evidence
System.out.println("P(W=0) = " + Math.exp(inferenceAlgorithm.getLogProbabilityOfEvidence()));
```

[[Back to Top]](#documentation)

### Importance Sampling <a name="isexample"></a>

This example we show how to perform inference on a general Bayesian network using an importance sampling
algorithm detailed in

>Fung, R., Chang, K. C. (2013). Weighing and integrating evidence for stochastic simulation in Bayesian networks. arXiv preprint arXiv:1304.1504.

```java
//We first load the WasteIncinerator bayesian network which has multinomial 
//and Gaussian variables.
BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/WasteIncinerator.bn");

//We recover the relevant variables for this example: Mout which is normally 
//distributed, and W which is multinomial.
Variable varMout = bn.getStaticVariables().getVariableByName("Mout");
Variable varW = bn.getStaticVariables().getVariableByName("W");

//First we create an instance of a inference algorithm. In this case, we use 
//the ImportanceSampling class.
InferenceAlgorithm inferenceAlgorithm = new ImportanceSampling();

//Then, we set the BN model
inferenceAlgorithm.setModel(bn);

//If exists, we also set the evidence.
Assignment assignment = new HashMapAssignment(1);
assignment.setValue(varW,0);
inferenceAlgorithm.setEvidence(assignment);

//We can also set to be run in parallel on multicore CPUs
inferenceAlgorithm.setParallelMode(true);

//Then we run inference
inferenceAlgorithm.runInference();

//Then we query the posterior of
System.out.println("P(Mout|W=0) = " + inferenceAlgorithm.getPosterior(varMout));

//Or some more refined queries
System.out.println("P(0.7<Mout<6.59 | W=0) = " + 
 inferenceAlgorithm.getExpectedValue(varMout, v -> (0.7 < v && v < 6.59) ? 1.0 : 0.0 ));

//We can also compute the probability of the evidence
System.out.println("P(W=0) = " + Math.exp(inferenceAlgorithm.getLogProbabilityOfEvidence()));
```

[[Back to Top]](#documentation)

## Learning Algorithms <a name="learningexample"></a>
### Maximum Likelihood <a name="mlexample"></a>


This other example shows how to learn incrementally the parameters of a Bayesian network using data batches,

```java
//We can open the data stream using the static class DataStreamLoader
DataStream<DataInstance> data = 
                  DataStreamLoader.openFromFile("datasets/WasteIncineratorSample.arff");

//We create a ParameterLearningAlgorithm object with the MaximumLikehood builder
ParameterLearningAlgorithm parameterLearningAlgorithm = new ParallelMaximumLikelihood();

//We fix the DAG structure
parameterLearningAlgorithm.setDAG(getNaiveBayesStructure(data,0));

//We should invoke this method before processing any data
parameterLearningAlgorithm.initLearning();


//Then we show how we can perform parameter learnig by a sequential updating of data batches.
for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)){
    parameterLearningAlgorithm.updateModel(batch);
}

//And we get the model
BayesianNetwork bnModel = parameterLearningAlgorithm.getLearntBayesianNetwork();

//We print the model
System.out.println(bnModel.toString());
```

[[Back to Top]](#documentation)

### Parallel Maximum Likelihood <a name="pmlexample"></a>

This example shows how to learn in parallel the parameters of a Bayesian network from a stream of data using maximum likelihood.

```java
//We can open the data stream using the static class DataStreamLoader
DataStream<DataInstance> data = 
           DataStreamLoader.openFromFile("datasets/syntheticData.arff");

//We create a MaximumLikelihood object with the MaximumLikehood builder
MaximumLikelihood parameterLearningAlgorithm = new MaximumLikelihood();

//We activate the parallel mode.
parameterLearningAlgorithm.setParallelMode(true);

//We fix the DAG structure
parameterLearningAlgorithm.setDAG(getNaiveBayesStructure(data,0));

//We set the batch size which will be employed to learn the model in parallel
parameterLearningAlgorithm.setBatchSize(100);

//We set the data which is going to be used for leaning the parameters
parameterLearningAlgorithm.setDataStream(data);

//We perform the learning
parameterLearningAlgorithm.runLearning();

//And we get the model
BayesianNetwork bnModel = parameterLearningAlgorithm.getLearntBayesianNetwork();

//We print the model
System.out.println(bnModel.toString());
```


[[Back to Top]](#documentation)

### Streaming Variational Bayes <a name="svbexample"></a>

This example shows how to learn incrementally the parameters of a Bayesian network from a stream of data with a Bayesian approach using the following algorithm,

>Broderick, T., Boyd, N., Wibisono, A., Wilson, A. C., \& Jordan, M. I. (2013). Streaming variational Bayes. 
In Advances in Neural Information Processing Systems (pp. 1727-1735).

In this second example we show a alternative implementation which explicitly updates the model by batches by using the class *SVB*.


```java
//We can open the data stream using the static class DataStreamLoader
DataStream<DataInstance> data = 
                      DataStreamLoader.openFromFile("datasets/WasteIncineratorSample.arff");

//We create a StreamingVariationalBayesVMP object
StreamingVariationalBayesVMP parameterLearningAlgorithm = new StreamingVariationalBayesVMP();

//We fix the DAG structure, which is a Naive Bayes with a 
//global latent binary variable
parameterLearningAlgorithm.setDAG(getHiddenNaiveBayesStructure(data));

//We fix the size of the window, which must be equal to the size of the data batches 
//we use for learning
parameterLearningAlgorithm.setWindowsSize(5);

//We can activate the output
parameterLearningAlgorithm.setOutput(true);

//We should invoke this method before processing any data
parameterLearningAlgorithm.initLearning();

//Then we show how we can perform parameter learnig by a sequential updating of 
//data batches.
for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(5)){
    parameterLearningAlgorithm.updateModel(batch);
}

//And we get the model
BayesianNetwork bnModel = parameterLearningAlgorithm.getLearntBayesianNetwork();

//We print the model
System.out.println(bnModel.toString());
```

[[Back to Top]](#documentation)

### Parallel Streaming Variational Bayes <a name="psvbexample"></a>

This example shows how to learn in the parameters of a Bayesian network from a stream of data with a Bayesian
approach using the parallel version  of the SVB algorithm, 

>Broderick, T., Boyd, N., Wibisono, A., Wilson, A. C., \& Jordan, M. I. (2013). Streaming variational Bayes. 
In Advances in Neural Information Processing Systems (pp. 1727-1735).

```java
//We can open the data stream using the static class DataStreamLoader
DataStream<DataInstance> data = 
                   DataStreamLoader.openFromFile("datasets/WasteIncineratorSample.arff");

//We create a ParallelSVB object
ParallelSVB parameterLearningAlgorithm = new ParallelSVB();

//We fix the number of cores we want to exploit
parameterLearningAlgorithm.setNCores(4);

//We fix the DAG structure, which is a Naive Bayes with a 
//global latent binary variable
parameterLearningAlgorithm.setDAG(StreamingVMPExample.getHiddenNaiveBayesStructure(data));


//We fix the size of the window
parameterLearningAlgorithm.getSVBEngine().setWindowsSize(100);

//We can activate the output
parameterLearningAlgorithm.setOutput(true);

//We set the data which is going to be used for leaning the parameters
parameterLearningAlgorithm.setDataStream(data);

//We perform the learning
parameterLearningAlgorithm.runLearning();

//And we get the model
BayesianNetwork bnModel = parameterLearningAlgorithm.getLearntBayesianNetwork();

//We print the model
System.out.println(bnModel.toString());
```

[[Back to Top]](#documentation)

## Concept Drift Methods <a name="conceptdriftexample"></a>

<!--- ### Maximum Likelihood with Fading <a name="mlfadingexample"></a>

This example shows how to adaptively learn the parameters of a Bayesian network from a stream of data using exponential forgetting with a given fading factor, directly inspired by the approach presented in

>Olesen, K. G., Lauritzen, S. L., \& Jensen, F. V. (1992, July). aHUGIN: A system creating adaptive causal probabilistic networks. In Proceedings of the Eighth international conference on Uncertainty in Artificial Intelligence (pp. 223-229). Morgan Kaufmann Publishers Inc.

```java
//We can open the data stream using the static class DataStreamLoader
DataStream<DataInstance> data = 
             DataStreamLoader.openFromFile("datasets/WasteIncineratorSample.arff");

//We create a ParameterLearningAlgorithm object with 
//the MaximumLikelihoodFading builder
MaximumLikelihoodFading parameterLearningAlgorithm = new MaximumLikelihoodFading();

//We fix the DAG structure
parameterLearningAlgorithm.setDAG(
                MaximimumLikelihoodByBatchExample.getNaiveBayesStructure(data, 0));

//We fix the fading or forgeting factor
parameterLearningAlgorithm.setFadingFactor(0.9);

//We set the batch size which will be employed to learn the model
parameterLearningAlgorithm.setBatchSize(100);

//We set the data which is going to be used for leaning the parameters
parameterLearningAlgorithm.setDataStream(data);

//We perform the learning
parameterLearningAlgorithm.runLearning();

//And we get the model
BayesianNetwork bnModel = parameterLearningAlgorithm.getLearntBayesianNetwork();

//We print the model
System.out.println(bnModel.toString());
```

[[Back to Top]](#documentation)

### Streaming Variational Bayes with Fading <a name="svbfadingexample"></a>

This example shows how to adaptively learn in the parameters of a Bayesian network from a stream of data with a Bayesian approach using a combination of the the following two methods,

>Broderick, T., Boyd, N., Wibisono, A., Wilson, A. C., \& Jordan, M. I. (2013). Streaming variational Bayes. 
In Advances in Neural Information Processing Systems (pp. 1727-1735).

>Olesen, K. G., Lauritzen, S. L., \& Jensen, F. V. (1992, July). aHUGIN: A system creating adaptive causal probabilistic networks. In Proceedings of the Eighth international conference on Uncertainty in Artificial Intelligence (pp. 223-229). Morgan Kaufmann Publishers Inc.

```java
//We can open the data stream using the static class DataStreamLoader
DataStream<DataInstance> data = 
           DataStreamLoader.openFromFile("datasets/WasteIncineratorSample.arff");

//We create a SVB object
SVBFading parameterLearningAlgorithm = new SVBFading();

//We fix the DAG structure
parameterLearningAlgorithm.setDAG(SVBExample.getHiddenNaiveBayesStructure(data));

//We fix the fading or forgeting factor
parameterLearningAlgorithm.setFadingFactor(0.9);

//We fix the size of the window
parameterLearningAlgorithm.setWindowsSize(100);

//We can activate the output
parameterLearningAlgorithm.setOutput(true);

//We set the data which is going to be used for leaning the parameters
parameterLearningAlgorithm.setDataStream(data);

//We perform the learning
parameterLearningAlgorithm.runLearning();

//And we get the model
BayesianNetwork bnModel = parameterLearningAlgorithm.getLearntBayesianNetwork();

//We print the model
System.out.println(bnModel.toString());
```

[[Back to Top]](#documentation)
-->
### Naive Bayes with Virtual Concept Drift Detection <a name="nbconceptdriftexample"></a>

This example shows how to use the class NaiveBayesVirtualConceptDriftDetector to run the virtual concept drift detector detailed in

> Borchani et al. Modeling concept drift: A probabilistic graphical model based approach. IDA 2015.

```java
//We can open the data stream using the static class DataStreamLoader
DataStream<DataInstance> data = DataStreamLoader.openFromFile("./datasets/DriftSets/sea.arff");

//We create a NaiveBayesVirtualConceptDriftDetector object
NaiveBayesVirtualConceptDriftDetector virtualDriftDetector = 
                                        new NaiveBayesVirtualConceptDriftDetector();

//We set class variable as the last attribute
virtualDriftDetector.setClassIndex(-1);

//We set the data which is going to be used
virtualDriftDetector.setData(data);

//We fix the size of the window
int windowSize = 1000;
virtualDriftDetector.setWindowsSize(windowSize);

//We fix the so-called transition variance
virtualDriftDetector.setTransitionVariance(0.1);

//We fix the number of global latent variables
virtualDriftDetector.setNumberOfGlobalVars(1);

//We should invoke this method before processing any data
virtualDriftDetector.initLearning();

//Some prints
System.out.print("Batch");
for (Variable hiddenVar : virtualDriftDetector.getHiddenVars()) {
    System.out.print("\t" + hiddenVar.getName());
}
System.out.println();


//Then we show how we can perform the sequential processing of
// data batches. They must be of the same value than the window
// size parameter set above.
int countBatch = 0;
for (DataOnMemory<DataInstance> batch : 
				data.iterableOverBatches(windowSize)){

    //We update the model by invoking this method. The output
    // is an array with a value associated
    // to each fo the global hidden variables
    double[] out = virtualDriftDetector.updateModel(batch);

    //We print the output
    System.out.print(countBatch + "\t");
    for (int i = 0; i < out.length; i++) {
        System.out.print(out[i]+"\t");
    }
    System.out.println();
    countBatch++;
}
```


[[Back to Top]](#documentation)

## HuginLink <a name="huginglinkexample"></a>
### Models conversion between AMIDST and Hugin <a name="huginglinkconversionexample"></a>

This example shows how to use the class BNConverterToAMIDST and BNConverterToHugin to convert a 
Bayesian network models between Hugin and AMIDST formats


```java
//We load from Hugin format
Domain huginBN = BNLoaderFromHugin.loadFromFile("networks/asia.net");

//Then, it is converted to AMIDST BayesianNetwork object
BayesianNetwork amidstBN = BNConverterToAMIDST.convertToAmidst(huginBN);

//Then, it is converted to Hugin Bayesian Network object
huginBN = BNConverterToHugin.convertToHugin(amidstBN);

System.out.println(amidstBN.toString());
System.out.println(huginBN.toString());
```


[[Back to Top]](#documentation)

### I/O of Bayesian Networks with Hugin net format <a name="huginglinkioexample"></a>

This example shows how to use the class BNLoaderFromHugin and BNWriterToHugin classes to load and
write Bayesian networks in Hugin format.

```java
//We load from Hugin format
Domain huginBN = BNLoaderFromHugin.loadFromFile("networks/asia.net");

//We save a AMIDST BN to Hugin format
BayesianNetwork amidstBN = BNConverterToAMIDST.convertToAmidst(huginBN);
BNWriterToHugin.saveToHuginFile(amidstBN,"networks/tmp.net");
```

[[Back to Top]](#documentation)

### Invoking Hugin's inference engine <a name="huginglinkinferenceexample"></a>

This example we show how to perform inference using [Hugin](http://www.hugin.com) inference engine within the AMIDST toolbox

```java
//We first load the WasteIncinerator bayesian network 
//which has multinomial and Gaussian variables.
BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/WasteIncinerator.bn");

//We recover the relevant variables for this example: 
//Mout which is normally distributed, and W which is multinomial.
Variable varMout = bn.getStaticVariables().getVariableByName("Mout");
Variable varW = bn.getStaticVariables().getVariableByName("W");

//First we create an instance of a inference algorithm. 
//In this case, we use the ImportanceSampling class.
InferenceAlgorithm inferenceAlgorithm = new HuginInference();

//Then, we set the BN model
inferenceAlgorithm.setModel(bn);

//If exists, we also set the evidence.
Assignment assignment = new HashMapAssignment(1);
assignment.setValue(varW,0);
inferenceAlgorithm.setEvidence(assignment);

//Then we run inference
inferenceAlgorithm.runInference();

//Then we query the posterior of
System.out.println("P(Mout|W=0) = " + inferenceAlgorithm.getPosterior(varMout));

//Or some more refined queries
System.out.println("P(0.7<Mout<3.5 | W=0) = " 
   + inferenceAlgorithm.getExpectedValue(varMout, v -> (0.7 < v && v < 3.5) ? 1.0 : 0.0 ));
```

[[Back to Top]](#documentation)

### Invoking Hugin's Parallel TAN <a name="huginglinkTANexample"></a>

This example we show how to perform inference using [Hugin](http://www.hugin.com) inference engine within the AMIDST toolbox


This example shows how to use [Hugin](http://www.hugin.com)'s functionality to learn in parallel a TAN model. An important remark is that [Hugin](http://www.hugin.com) only allows to learn the TAN model for a data set completely loaded into RAM memory. The case where our data set does not fit into memory, it solved in AMIDST in the following way. We learn the structure using a smaller data set produced by [Reservoir sampling](https://en.wikipedia.org/wiki/Reservoir_sampling) and, then, we use AMIDST's [ParallelMaximumLikelihood](http://amidst.github.io/toolbox/#pmlexample) to learn the parameters of the TAN model over the whole data set.

For further details about the implementation of the parallel TAN algorithm look at the following paper:

>Madsen, A.L. et al. A New Method for Vertical Parallelisation of TAN Learning Based on Balanced Incomplete Block Designs. Probabilistic Graphical Models. Lecture Notes in Computer Science Volume 8754, 2014, pp 302-317.

```java
//We load a Bayesian network to generate a data stream
//using BayesianNewtorkSampler class.
int sampleSize = 100000;
BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("networks/Pigs.bn");
BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);

//We fix the number of samples in memory used for performing the structural learning.
//They are randomly sub-sampled using Reservoir sampling.
int samplesOnMemory = 5000;

//We make different trials with different number of cores
ArrayList<Integer> vNumCores = new ArrayList(Arrays.asList(1, 2, 3, 4));

for (Integer numCores : vNumCores) {
    System.out.println("Learning TAN: " + samplesOnMemory + " samples on memory, " + numCores + " core/s ...");
    DataStream<DataInstance> data = sampler.sampleToDataStream(sampleSize);

    //The class ParallelTAN is created
    ParallelTAN tan = new ParallelTAN();

    //We activate the parallel mode.
    tan.setParallelMode(true);

    //We set the number of cores to be used for the structural learning
    tan.setNumCores(numCores);

    //We set the number of samples to be used for the learning the structure
    tan.setNumSamplesOnMemory(samplesOnMemory);

    //We set the root variable to be first variable
    tan.setNameRoot(bn.getVariables().getListOfVariables().get(0).getName());

    //We set the class variable to be the last variable
    tan.setNameTarget(bn.getVariables().getListOfVariables().get(bn.getVariables().getListOfVariables().size()-1).getName());

    Stopwatch watch = Stopwatch.createStarted();

    //We just invoke this mode to learn the TAN model for the data stream
    BayesianNetwork model = tan.learn(data);

    System.out.println(watch.stop());
}
```


## MoaLink <a name="moalinkexample"></a>
### AMIDST Classifiers from MOA <a name="moalinkclassifiersexample"></a>

The following command can be used to learn a Bayesian model with a latent Gaussian variable (HG) and a multinomial with 2 states (HM), as displayed in figure below. The VMP algorithm is used to learn the parameters of these two non-observed variables and make predictions over the class variable.


<p align="center">
<img title="Taxonomy" src="http://amidst.github.io/toolbox/images/HODE.jpg" width="400">
</p>

```
java -Xmx512m -cp "../lib/*" -javaagent:../lib/sizeofag-1.0.0.jar 
moa.DoTask EvaluatePrequential -l \(bayes.AmidstClassifier -g 1 
-m 2\) -s generators.RandomRBFGenerator -i 10000 -f 1000 -q 1000
```
[[Back to Top]](#documentation)

### AMIDST Regression from MOA <a name="moalinkregressionsexample"></a>

It is possible to learn an enriched naive Bayes model for regression if the class label is of a continuous nature. The following command uses the model in Figure \ref{fig:HODE}(b) on a toy dataset from WEKA's collection of [regression problems](http://prdownloads.sourceforge.net/weka/datasets-numeric.jar).


<p align="center">
<img title="Taxonomy" src="http://amidst.github.io/toolbox/images/regressionHODE.jpg" width="400">
</p>


```
java -Xmx512m -cp "../lib/*" -javaagent:../lib/sizeofag-1.0.0.jar 
moa.DoTask EvaluatePrequentialRegression -l bayes.AmidstRegressor
 -s (ArffFileStream -f ./quake.arff)
```

Note that the simpler the dataset the less complex the model should be. In this case, \texttt{quake.arff} is a very simple and small dataset that should probably be learn with a more simple classifier, that is, a high-bias-low-variance classifier, in order to avoid overfitting. This aims at providing a simple running example.

[[Back to Top]](#documentation)