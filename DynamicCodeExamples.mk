## Dynamic Bayesian networks: Code Examples<a name="documentation"></a>

   * [Dynamic Data Streams](#dynamicdatastreamsexample)
   * [Dynamic Random Variables](#dynamicvariablesexample)
   * [Dynamic Bayesian Networks](#dynamicbnexample)
       * [Creating Dynamic Bayesian Networks](#dynamicbn)
       * [Creating Dynamic Bayesian Networks with Latent Variables](#dynamicbnhidden)
       * [Modifying Bayesian Networks](#dynamicbnmodify)
   * [Sampling from Dynamic Bayesian Networks](#sampledynamicbn)
   * [Inference Algorithms for Dynamic Bayesian Networks](#dynamicinferenceexample)
       * [The Dynamic MAP Inference](#dynamicmap)
       * [The Dynamic Variational Message Passing](#dynamicvmp)
       * [The Dynamic Importance Sampling](#dynamicis)
   * [Dynamic Learning Algorithms](#dynamiclearningexample)
       * [Maximum Likelihood for DBNs](#dynamicml)
       * [Streaming Variational Bayes for DBNs](#dynamicsvb)


## Dynamic Data Streams<a name="dynamicdatastreamsexample"></a>
  
In this example we show how to use the main features of a *DataStream* object. More precisely,  we show  how to load a dynamic data stream and how to iterate over the *DynamicDataInstance* objects.

```java
//Open the data stream using the class DynamicDataStreamLoader
DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile("datasets/dynamicNB-samples.arff");

//Access the attributes defining the data stream
System.out.println("Attributes defining the data set");
for (Attribute attribute : data.getAttributes()) {
    System.out.println(attribute.getName());
}
Attribute classVar = data.getAttributes().getAttributeByName("ClassVar");

//Iterate over dynamic data instances
System.out.println("1. Iterating over samples using a for loop");
for (DynamicDataInstance dataInstance : data) {
    System.out.println("SequenceID = "+dataInstance.getSequenceID()+", TimeID = "+dataInstance.getTimeID());
    System.out.println("The value of attribute A for the current data instance is: " +
    	dataInstance.getValue(classVar));
}
```

[[Back to Top]](#documentation)

## Dynamic Random Variables<a name="dynamicvariablesexample"></a>

This example show the basic functionalities related to dynamic variables.

```java
//Create an empty DynamicVariables object
DynamicVariables variables = new DynamicVariables();

//Invoke the "new" methods of the object DynamicVariables to create new dynamic variables.

//Create a Gaussian dynamic variables
Variable gaussianVar = variables.newGaussianDynamicVariable("GaussianVar");

//Create a Multinomial dynamic variable with two states
Variable multinomialVar = variables.newMultinomialDynamicVariable("MultinomialVar", 2);

//Create a Multinomial dynamic variable with two states: TRUE and FALSE
Variable multinomialVar2 = variables.newMultinomialDynamicVariable("MultinomialVar2", Arrays.asList("TRUE, FALSE"));

//All dynamic Variables have an interface variable
Variable gaussianVarInt = gaussianVar.getInterfaceVariable();
Variable multinomialVarInt = multinomialVar.getInterfaceVariable();

//Get the "main" Variable associated with each interface variable through the DynamicVariable object
Variable mainMultinomialVar = variables.getVariableFromInterface(multinomialVarInt);

//Check whether a variable is an interface variable
System.out.println("Is Variable "+gaussianVar.getName()+" an interface variable? "
                +gaussianVar.isInterfaceVariable());
System.out.println("Is Variable "+gaussianVarInt.getName()+" an interface variable? "
                +gaussianVarInt.isInterfaceVariable());

//Check whether a variable is a dynamic variable
System.out.println("Is Variable "+multinomialVar.getName()+" a dynamic variable? "
                +gaussianVar.isDynamicVariable());
```


[[Back to Top]](#documentation)


## Dynamic Bayesian Networks<a name="dynamicbnexample"></a>

### Creating Dynamic Bayesian networks<a name="dynamicbn"></a>

This example creates a dynamic BN, from a dynamic data stream, with randomly generated probability distributions, then saves it to a file.

```java
//Open the data stream using the static class DynamicDataStreamLoader
DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(
                "datasets/syntheticDataDiscrete.arff");

/**
* 1. Once the data is loaded, we create a random variable for each of the attributes (i.e. data columns)
* in our data.
*
* 2. {@link DynamicVariables} is the class for doing that. It takes a list of Attributes and internally creates
* all the variables. We create the variables using DynamicVariables class to guarantee that each variable
* has a different ID number and make it transparent for the user. Each random variable has an associated
* interface variable.
*
* 3. We can extract the Variable objects by using the method getVariableByName();
*/
DynamicVariables dynamicVariables = new DynamicVariables(data.getAttributes());
DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

Variable A = dynamicVariables.getVariableByName("A");
Variable B = dynamicVariables.getVariableByName("B");
Variable C = dynamicVariables.getVariableByName("C");
Variable D = dynamicVariables.getVariableByName("D");
Variable E = dynamicVariables.getVariableByName("E");
Variable G = dynamicVariables.getVariableByName("G");

Variable A_Interface = dynamicVariables.getInterfaceVariable(A);
Variable B_Interface = dynamicVariables.getInterfaceVariable(B);

//Note that C_Interface and D_Interface are also created although they will not be used
//(we will not add temporal dependencies)

Variable E_Interface = dynamicVariables.getInterfaceVariable(E);
Variable G_Interface = dynamicVariables.getInterfaceVariable(G);

// Example of the dynamic DAG structure
// Time 0: Parents at time 0 are automatically created when adding parents at time T
dynamicDAG.getParentSetTimeT(B).addParent(A);
dynamicDAG.getParentSetTimeT(C).addParent(A);
dynamicDAG.getParentSetTimeT(D).addParent(A);
dynamicDAG.getParentSetTimeT(E).addParent(A);
dynamicDAG.getParentSetTimeT(G).addParent(A);
dynamicDAG.getParentSetTimeT(A).addParent(A_Interface);
dynamicDAG.getParentSetTimeT(B).addParent(B_Interface);
dynamicDAG.getParentSetTimeT(E).addParent(E_Interface);
dynamicDAG.getParentSetTimeT(G).addParent(G_Interface);

System.out.println(dynamicDAG.toString());

/**
* 1. We now create the Dynamic Bayesian network from the previous Dynamic DAG.
*
* 2. The DBN object is created from the DynamicDAG. It automatically looks at the distribution type
* of each variable and their parents to initialize the Distributions objects that are stored
* inside (i.e. Multinomial, Normal, CLG, etc). The parameters defining these distributions are
* properly initialized.
*
* 3. The network is printed and we can have a look at the kind of distributions stored in the DBN object.
*/
DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(dynamicDAG);
System.out.printf(dbn.toString());

/**
* Finally teh Bayesian network is saved to a file.
*/
DynamicBayesianNetworkWriter.saveToFile(dbn, "networks/DBNExample.bn");
```

[[Back to Top]](#documentation)


### Creating Dynamic Bayesian Networks with Latent Variables <a name="dynamicbnhidden"></a>

This example shows how to create a BN model with hidden variables. We simply create a BN for clustering, i.e., a naive Bayes like structure with a single hidden variable acting as parant of all the remaining observable variables.
 
```java
//Open the data stream using DynamicDataStreamLoader
DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(
                "datasets/syntheticDataDiscrete.arff");

/**
* 1. Once the data is loaded, create a random variable for each of the attributes (i.e. data columns)
*
* 2. {@link DynamicVariables} is the class for doing that. It takes a list of Attributes and internally creates
* all the variables. We create the variables using DynamicVariables class to guarantee that each variable
* has a different ID number and make it transparent for the user. Each random variable has an associated
* interface variable.
*
* 3. Extract the Variable objects by using the method getVariableByName()
*/
DynamicVariables dynamicVariables = new DynamicVariables(data.getAttributes());

Variable A = dynamicVariables.getVariableByName("A");
Variable B = dynamicVariables.getVariableByName("B");
Variable C = dynamicVariables.getVariableByName("C");
Variable D = dynamicVariables.getVariableByName("D");
Variable E = dynamicVariables.getVariableByName("E");
Variable G = dynamicVariables.getVariableByName("G");

Variable A_Interface = dynamicVariables.getInterfaceVariable(A);
Variable B_Interface = dynamicVariables.getInterfaceVariable(B);

//Note that C_Interface and D_Interface are also created although they will not be used
//(we will not add temporal dependencies)

Variable E_Interface = dynamicVariables.getInterfaceVariable(E);
Variable G_Interface = dynamicVariables.getInterfaceVariable(G);

/*
* Add a hidden multinomial variable (with 2 states) as parent of all variables except A
*/

Variable H = dynamicVariables.newMultinomialDynamicVariable("H", 2);
Variable H_Interface = dynamicVariables.getInterfaceVariable(H);

/**
* Once we have defined your {@link DynamicVariables} object, including the latent variable,
* the next step is to create a DynamicDAG structure over this set of variables.
*/

DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);
        
/*
* 1. To add parents to each variable, we first recover the ParentSet object by the method
* getParentSet(Variable var) and then call the method addParent(Variable var).
*
* 2. We just assign the hidden variable as parent of all the other variables (except A), and
* link it temporally.
*/

// Time 0: Parents at time 0 are automatically created when adding parents at time T.
dynamicDAG.getParentSetsTimeT().stream()
        .filter(pset -> pset.getMainVar().getVarID() != A.getVarID())
        .filter(pset -> pset.getMainVar().getVarID() != H.getVarID())
        .forEach(pset -> {
            pset.addParent(A);
            pset.addParent(H);
        });
dynamicDAG.getParentSetTimeT(A).addParent(A_Interface);
dynamicDAG.getParentSetTimeT(B).addParent(B_Interface);
dynamicDAG.getParentSetTimeT(E).addParent(E_Interface);
dynamicDAG.getParentSetTimeT(G).addParent(G_Interface);
dynamicDAG.getParentSetTimeT(H).addParent(H_Interface);

System.out.println(dynamicDAG.toString());

/**
* 1. Create the Dynamic Bayesian network from the previous Dynamic DAG.
*
* 2. The DBN object is created from the DynamicDAG. It automatically looks at the distribution type
* of each variable and their parents to initialize the Distributions objects that are stored
* inside (i.e. Multinomial, Normal, CLG, etc). The parameters defining these distributions are
* properly initialized.
*
* 3. The network is printed and we can have a look at the kind of distributions stored in the DBN object.
*/
DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(dynamicDAG);
System.out.printf(dbn.toString());

/**
* Finally the created Dynamic Bayesian network is saved to a file.
*/
DynamicBayesianNetworkWriter.saveToFile(dbn, "networks/DBNExample.bn");
```

[[Back to Top]](#documentation)


### Modifying Dynamic Bayesian Networks <a name="dynamicbnmodify"></a>

This example shows how to access and modify the conditional probabilities of a Dynamic Bayesian network model.

```java
/**
* 1. Load the data stream and create the dynamic DAG.
*/
DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(
                "datasets/syntheticDataDiscrete.arff");
        
DynamicVariables dynamicVariables = new DynamicVariables(data.getAttributes());
DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

Variable A = dynamicVariables.getVariableByName("A");
Variable B = dynamicVariables.getVariableByName("B");
Variable C = dynamicVariables.getVariableByName("C");
Variable D = dynamicVariables.getVariableByName("D");
Variable E = dynamicVariables.getVariableByName("E");
Variable G = dynamicVariables.getVariableByName("G");
Variable A_Interface = dynamicVariables.getInterfaceVariable(A);
Variable B_Interface = dynamicVariables.getInterfaceVariable(B);
Variable E_Interface = dynamicVariables.getInterfaceVariable(E);
Variable G_Interface = dynamicVariables.getInterfaceVariable(G);
        
dynamicDAG.getParentSetTimeT(B).addParent(A);
dynamicDAG.getParentSetTimeT(C).addParent(A);
dynamicDAG.getParentSetTimeT(D).addParent(A);
dynamicDAG.getParentSetTimeT(E).addParent(A);
dynamicDAG.getParentSetTimeT(G).addParent(A);
dynamicDAG.getParentSetTimeT(A).addParent(A_Interface);
dynamicDAG.getParentSetTimeT(B).addParent(B_Interface);
dynamicDAG.getParentSetTimeT(E).addParent(E_Interface);
dynamicDAG.getParentSetTimeT(G).addParent(G_Interface);
        
DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(dynamicDAG);
System.out.printf(dbn.toString());

/*
* 2. Modify the conditional probability distributions (CPDs) to new assigned values.
* IMPORTANT: The parents are indexed according to Koller (Chapter 10. Pag. 358). Example:
*  Parents: A = {A0,A1} and B = {B0,B1,B2}.
*  NumberOfPossibleAssignments = 6
*  Index   A    B
*   0     A0   B0
*   1     A1   B1
*   2     A0   B2
*   3     A1   B0
*   4     A0   B1
*   5     A1   B2
*/

// *********************** Modifiy the CPDs at TIME 0 ***********************************

// Variable A
Multinomial distA_Time0 = dbn.getConditionalDistributionTime0(A);
distA_Time0.setProbabilities(new double[]{0.3, 0.7});

// Variable B
Multinomial_MultinomialParents distB_Time0 = dbn.getConditionalDistributionTime0(B);
distB_Time0.getMultinomial(0).setProbabilities(new double[]{0.4, 0.1, 0.5});
distB_Time0.getMultinomial(1).setProbabilities(new double[]{0.2, 0.5, 0.3});

// Variable C
Multinomial_MultinomialParents distC_Time0 = dbn.getConditionalDistributionTime0(C);
distC_Time0.getMultinomial(0).setProbabilities(new double[]{0.4, 0.6});
distC_Time0.getMultinomial(1).setProbabilities(new double[]{0.2, 0.8});

// Variable D
Multinomial_MultinomialParents distD_Time0 = dbn.getConditionalDistributionTime0(D);
distD_Time0.getMultinomial(0).setProbabilities(new double[]{0.7, 0.3});
distD_Time0.getMultinomial(1).setProbabilities(new double[]{0.1, 0.9});

// Variable E
Multinomial_MultinomialParents distE_Time0 = dbn.getConditionalDistributionTime0(E);
distE_Time0.getMultinomial(0).setProbabilities(new double[]{0.8, 0.2});
distE_Time0.getMultinomial(1).setProbabilities(new double[]{0.1, 0.9});

// Variable G
Multinomial_MultinomialParents distG_Time0 = dbn.getConditionalDistributionTime0(G);
distG_Time0.getMultinomial(0).setProbabilities(new double[]{0.6, 0.4});
distG_Time0.getMultinomial(1).setProbabilities(new double[]{0.7, 0.3});

// ************************ Modifiy the CPDs at TIME T ***********************************

// Variable A
Multinomial_MultinomialParents distA_TimeT = dbn.getConditionalDistributionTimeT(A);
distA_TimeT.getMultinomial(0).setProbabilities(new double[]{0.15, 0.85});
distA_TimeT.getMultinomial(1).setProbabilities(new double[]{0.1, 0.9});

// Variable B
Multinomial_MultinomialParents distB_TimeT = dbn.getConditionalDistributionTimeT(B);
distB_TimeT.getMultinomial(0).setProbabilities(new double[]{0.1, 0.2, 0.7});
distB_TimeT.getMultinomial(1).setProbabilities(new double[]{0.6, 0.1, 0.3});
distB_TimeT.getMultinomial(2).setProbabilities(new double[]{0.3, 0.4, 0.3});
distB_TimeT.getMultinomial(3).setProbabilities(new double[]{0.2, 0.1, 0.7});
distB_TimeT.getMultinomial(4).setProbabilities(new double[]{0.5, 0.1, 0.4});
distB_TimeT.getMultinomial(5).setProbabilities(new double[]{0.1, 0.1, 0.8});

// Variable C: equals to the distribution at time 0 (C does not have temporal clone)
Multinomial_MultinomialParents distC_TimeT = dbn.getConditionalDistributionTimeT(C);
distC_TimeT.getMultinomial(0).setProbabilities(new double[]{0.4, 0.6});
distC_TimeT.getMultinomial(1).setProbabilities(new double[]{0.2, 0.8});

// Variable D: equals to the distribution at time 0 (D does not have temporal clone)
Multinomial_MultinomialParents distD_TimeT = dbn.getConditionalDistributionTimeT(D);
distD_TimeT.getMultinomial(0).setProbabilities(new double[]{0.7, 0.3});
distD_TimeT.getMultinomial(1).setProbabilities(new double[]{0.1, 0.9});

// Variable E
Multinomial_MultinomialParents distE_TimeT = dbn.getConditionalDistributionTimeT(E);
distE_TimeT.getMultinomial(0).setProbabilities(new double[]{0.3, 0.7});
distE_TimeT.getMultinomial(1).setProbabilities(new double[]{0.6, 0.4});
distE_TimeT.getMultinomial(2).setProbabilities(new double[]{0.7, 0.3});
distE_TimeT.getMultinomial(3).setProbabilities(new double[]{0.9, 0.1});

// Variable G
Multinomial_MultinomialParents distG_TimeT = dbn.getConditionalDistributionTimeT(G);
distG_TimeT.getMultinomial(0).setProbabilities(new double[]{0.2, 0.8});
distG_TimeT.getMultinomial(1).setProbabilities(new double[]{0.5, 0.5});
distG_TimeT.getMultinomial(2).setProbabilities(new double[]{0.3, 0.7});
distG_TimeT.getMultinomial(3).setProbabilities(new double[]{0.8, 0.2});

/*
* Print the new modeified DBN
*/
System.out.println(dbn.toString());
```

[[Back to Top]](#documentation)

## Sampling from Dynamic Bayesian Networks <a name="sampledynamicbn"></a>

This example shows how to use the DynamicBayesianNetworkSampler class to randomly generate a dynamic data stream from a given Dynamic Bayesian network.

```java
//Randomly generate a DBN with 3 continuous and 3 discrete variables with 2 states
DynamicBayesianNetworkGenerator dbnGenerator = new DynamicBayesianNetworkGenerator();
dbnGenerator.setNumberOfContinuousVars(3);
dbnGenerator.setNumberOfDiscreteVars(3);
dbnGenerator.setNumberOfStates(2);

//Create a Naive Bayes like structure with temporal links in the children (leaves) and 2 states for
//the class variable
DynamicBayesianNetwork network = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(
                new Random(0), 2, true);

//Create the sampler from this network
DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(network);
sampler.setSeed(0);

//Sample a dataStream of 3 sequences of 1000 samples each
DataStream<DynamicDataInstance> dataStream = sampler.sampleToDataBase(3,1000);

//Save the created data stream in a file
DataStreamWriter.writeDataToFile(dataStream, "./datasets/dnb-samples.arff");
```

[[Back to Top]](#documentation)

## Inference Algorithms for Dynamic Bayesian Networks <a name="dynamicinferenceexample"></a>

### The Dynamic MAP Inference <a name="dynamicmap"></a>

This example shows how to use the Dynamic MAP Inference algorithm.

```java
//Generate a random DBN model
DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(3);
DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
DynamicBayesianNetworkGenerator.setNumberOfStates(2);
DynamicBayesianNetworkGenerator.setNumberOfLinks(5);
DynamicBayesianNetwork dynamicBayesianNetwork = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);

//Initialize the Dynamic MAP object
int nTimeSteps = 6;
eu.amidst.dynamic.inference.DynamicMAPInference dynMAP = new eu.amidst.dynamic.inference.DynamicMAPInference();
dynMAP.setModel(dynamicBayesianNetwork);
dynMAP.setNumberOfTimeSteps(nTimeSteps);

Variable mapVariable = dynamicBayesianNetwork.getDynamicVariables().getVariableByName("ClassVar");
dynMAP.setMAPvariable(mapVariable);
        
//Generate an evidence for T=0,...,nTimeSteps-1
List<Variable> varsDynamicModel = dynamicBayesianNetwork.getDynamicVariables().getListOfDynamicVariables();

System.out.println("DYNAMIC VARIABLES:");
varsDynamicModel.forEach(var -> System.out.println("Var ID " + var.getVarID() + ": " + var.getName()));
System.out.println();
int indexVarEvidence1 = 2;
int indexVarEvidence2 = 3;
int indexVarEvidence3 = 4;
Variable varEvidence1 = varsDynamicModel.get(indexVarEvidence1);
Variable varEvidence2 = varsDynamicModel.get(indexVarEvidence2);
Variable varEvidence3 = varsDynamicModel.get(indexVarEvidence3);

List<Variable> varsEvidence = new ArrayList<>(3);
varsEvidence.add(0,varEvidence1);
varsEvidence.add(1,varEvidence2);
varsEvidence.add(2,varEvidence3);

double varEvidenceValue;

Random random = new Random(4634);

List<DynamicAssignment> evidence = new ArrayList<>(nTimeSteps);

for (int t = 0; t < nTimeSteps; t++) {
   HashMapDynamicAssignment dynAssignment = new HashMapDynamicAssignment(varsEvidence.size());

   for (int i = 0; i < varsEvidence.size(); i++) {

        dynAssignment.setSequenceID(12302253);
        dynAssignment.setTimeID(t);
        Variable varEvidence = varsEvidence.get(i);

        if (varEvidence.isMultinomial()) {
             varEvidenceValue = random.nextInt(varEvidence1.getNumberOfStates());
        } else {
             varEvidenceValue = -5 + 10 * random.nextDouble();
        }
             dynAssignment.setValue(varEvidence, varEvidenceValue);
        }
        evidence.add(dynAssignment);
    }
    System.out.println("EVIDENCE:");
    evidence.forEach(evid -> {
    System.out.println("Evidence at time " + evid.getTimeID());
    evid.getVariables().forEach(variable -> System.out.println(variable.getName() + ": " + Integer.toString((int) evid.getValue(variable))));
    System.out.println();
});
        
//Set the evidence and run the inference process
dynMAP.setEvidence(evidence);
dynMAP.runInference(eu.amidst.dynamic.inference.DynamicMAPInference.SearchAlgorithm.IS);

//Display the results
Assignment MAPestimate = dynMAP.getMAPestimate();
double MAPestimateProbability = dynMAP.getMAPestimateProbability();

System.out.println("MAP sequence over " + mapVariable.getName() + ":");
List<Variable> MAPvarReplications = MAPestimate.getVariables().stream().sorted((var1,var2) -> (var1.getVarID()>var2.getVarID()? 1 : -1)).collect(Collectors.toList());

StringBuilder sequence = new StringBuilder();
MAPvarReplications.stream().forEachOrdered(var -> sequence.append(Integer.toString((int) MAPestimate.getValue(var)) + ", "));
System.out.println(sequence.toString());
System.out.println("with probability prop. to: " + MAPestimateProbability);
```

[[Back to Top]](#documentation)

### The Dynamic Variational Message Passing <a name="dynamicvmp"></a>

This example shows how to use the Factored Frontier algorithm with Variational Message Passing for running inference on dynamic Bayesian networks.

```java

//Generate a dynamic Bayesian network (NB structure with class and attributes temporally linked)
DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(2);
DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
DynamicBayesianNetworkGenerator.setNumberOfStates(3);
DynamicBayesianNetwork extendedDBN = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(random,2,true);

System.out.println(extendedDBN.toString());

//Select the target variable for inference, in this case it corresponds to the class variable
Variable classVar = extendedDBN.getDynamicVariables().getVariableByName("ClassVar");


//Create a dynamic data stream with 3 sequences for prediction. The class var is made hidden.
DynamicBayesianNetworkSampler dynamicSampler = new DynamicBayesianNetworkSampler(extendedDBN);
dynamicSampler.setHiddenVar(classVar);
DataStream<DynamicDataInstance> dataPredict = dynamicSampler.sampleToDataBase(3,100);


//Select VMP with the factored frontier algorithm as the Inference Algorithm
FactoredFrontierForDBN FFalgorithm = new FactoredFrontierForDBN(new VMP());
InferenceEngineForDBN.setInferenceAlgorithmForDBN(FFalgorithm);

//Set the DBN model
InferenceEngineForDBN.setModel(extendedDBN);

int time = 0 ;
UnivariateDistribution posterior = null;
for (DynamicDataInstance instance : dataPredict) {
            
   //The InferenceEngineForDBN must be reset at the beginning of each Sequence.
   if (instance.getTimeID()==0 && posterior != null) {
       InferenceEngineForDBN.reset();
       time=0;
   }
            
   //Set the evidence 
   InferenceEngineForDBN.addDynamicEvidence(instance);

   //Run the inference process
   InferenceEngineForDBN.runInference();

   //Query the posterior of the target variable
   posterior = InferenceEngineForDBN.getFilteredPosterior(classVar);

   //Display the output
   System.out.println("P(ClassVar|e[0:"+(time++)+"]) = "+posterior);
}
```

[[Back to Top]](#documentation)

### The Dynamic Importance Sampling <a name="dynamicis"></a>

This example shows how to use the Factored Frontier algorithm with Importance Sampling for running inference in dynamic Bayesian networks.

```java
//Generate a dynamic Bayesian network (NB structure with class and attributes temporally linked)
DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(2);
DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
DynamicBayesianNetworkGenerator.setNumberOfStates(3);
DynamicBayesianNetwork extendedDBN = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(random, 2, true);

System.out.println(extendedDBN.toString());

//Select the target variable for inference, in this case the class variable
Variable classVar = extendedDBN.getDynamicVariables().getVariableByName("ClassVar");

//Create a dynamic dataset with 3 sequences for prediction. The class var is made hidden.
DynamicBayesianNetworkSampler dynamicSampler = new DynamicBayesianNetworkSampler(extendedDBN);
dynamicSampler.setHiddenVar(classVar);
DataStream<DynamicDataInstance> dataPredict = dynamicSampler.sampleToDataBase(3, 1000);

//Select IS with the factored frontier algorithm as the Inference Algorithm
ImportanceSampling importanceSampling = new ImportanceSampling();
importanceSampling.setKeepDataOnMemory(true);
FactoredFrontierForDBN FFalgorithm = new FactoredFrontierForDBN(importanceSampling);
InferenceEngineForDBN.setInferenceAlgorithmForDBN(FFalgorithm);

//Set the DBN model
InferenceEngineForDBN.setModel(extendedDBN);

int time = 0;
UnivariateDistribution posterior = null;
for (DynamicDataInstance instance : dataPredict) {
            
    //The InferenceEngineForDBN must be reset at the beginning of each Sequence.
    if (instance.getTimeID() == 0 && posterior != null) {
        InferenceEngineForDBN.reset();
        time = 0;
     }

    //Set the evidence.
    InferenceEngineForDBN.addDynamicEvidence(instance);

    //Run inference
    InferenceEngineForDBN.runInference();

    //Query the posterior of the target variable
    posterior = InferenceEngineForDBN.getFilteredPosterior(classVar);

    //Display the output
    System.out.println("P(ClassVar|e[0:" + (time++) + "]) = " + posterior);
}
```

[[Back to Top]](#documentation)

## Dynamic Learning Algorithms <a name="dynamiclearningexample"></a>

### Maximum Likelihood for DBNs <a name="dynamicml"></a>

This example shows how to learn the parameters of a dynamic Bayesian network using maximum likelihood from a randomly sampled data stream.

```java
//Generate a dynamic Bayesian network (NB structure with class and attributes temporally linked)
DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(2);
DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
DynamicBayesianNetworkGenerator.setNumberOfStates(3);
DynamicBayesianNetwork dbnRandom = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(random,2,true);

//Sample dynamic data stream from the created dbn with random parameters
DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dbnRandom);
sampler.setSeed(0);
DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(3,10000);

//Set batch size value and the parallel mode
DynamicMaximumLikelihood.setBatchSize(1000);
DynamicMaximumLikelihood.setParallelMode(true);
        
//Learn the DBN parameters with ML from data
DynamicBayesianNetwork dbnLearnt = DynamicMaximumLikelihood.learnDynamic(dbnRandom.getDynamicDAG(), data);

//We print the learnt DBN model
System.out.println(dbnLearnt.toString());        
```

[[Back to Top]](#documentation)

### Streaming Variational Bayes for DBNs <a name="dynamicsvb"></a>

This example shows how to learn the parameters of a dynamic Bayesian network using streaming variational Bayes from a randomly sampled data stream.


```java

//Generate a dynamic Bayesian network (NB structure with class and attributes temporally linked)
DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(2);
DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
DynamicBayesianNetworkGenerator.setNumberOfStates(3);
DynamicBayesianNetwork dbnRandom = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(random,2,true);

//Sample dynamic data stream from the created dbn with random parameters
DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dbnRandom);
sampler.setSeed(0);
//Sample 3 sequences of 100K instances
DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(3,10000);

//Parameter Learning with Streaming variational Bayes
DynamicSVB svb = new DynamicSVB();
        
//Set the desired options for the svb
svb.setWindowsSize(100);
svb.setSeed(0);
//If desired, we may also set some options for the VMP
VMP vmp = svb.getDynamicPlateauStructure().getVMPTimeT();
vmp.setOutput(true);
vmp.setTestELBO(true);
vmp.setMaxIter(1000);
vmp.setThreshold(0.0001);

//Set the dynamicDAG, the data and start learning
svb.setDynamicDAG(dbnRandom.getDynamicDAG());
svb.setDataStream(data);
svb.runLearning();

//Get the learnt DBN model
DynamicBayesianNetwork dbnLearnt = svb.getLearntDBN();

//Print the DBN model
System.out.println(dbnLearnt.toString());
```

[[Back to Top]](#documentation)