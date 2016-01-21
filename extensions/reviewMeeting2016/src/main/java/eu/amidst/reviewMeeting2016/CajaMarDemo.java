package eu.amidst.reviewMeeting2016;

import eu.amidst.core.conceptdrift.utils.GaussianHiddenTransitionMethod;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.learning.parametric.bayesian.PlateuStructure;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.flinklink.core.conceptdrift.IdentifiableIDAModel;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.learning.dynamic.DynamicParallelVB;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Created by ana@cs.aau.dk on 21/01/16.
 */
public class CajaMarDemo {

    static Logger logger = LoggerFactory.getLogger(CajaMarDemo.class);
    public static int seed = 5;
    public static int batchSize = 500;

    public static double transitionVariance = 0.1;

    public static void main(String[] args) throws Exception {

        /*
         * Create flink ExecutionEnvironment variable:
         * The ExecutionEnviroment is the context in which a program is executed. A local environment will cause
         * execution in the current JVM, a remote environment will cause execution on a remote cluster installation.
         */
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        /*************************************************************************************
         * 1.- READ DATA TO GET MODEL HEADER (ATTRIBUTES)
         *************************************************************************************/

        // The demo can be run on your local computer or a cluster with hadoop, (un)comment as appropriate
        //String fileName = "hdfs:///tmp_conceptdrift_data";
        String fileName = "./datasets/dataFlink/conceptdrift/data";

        // Load the first batch of data (first month) to get the model header (attributes) necessary to create
        // the dynamic DAG
        DataFlink<DynamicDataInstance> data0 = DataFlinkLoader.loadDynamicDataFromFolder(env,fileName+0+".arff", false);
        Attributes attributes = data0.getAttributes();

        System.out.println(attributes);
        /*************************************************************************************
         * 2. - CREATE A DYNAMIC NAIVE BAYES DAG
         *************************************************************************************/

        // Create a Variables object from the attributes of the input data stream.
        DynamicVariables variables = new DynamicVariables(attributes);

        // Define the class variable.
        Variable classVar = variables.getVariableById(0);

        // Create an empty DAG object with the defined variables.
        DynamicDAG dynamicDAG = new DynamicDAG(variables);

        // Link the class as parent of all attributes
        dynamicDAG.getParentSetsTimeT()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .forEach(w -> w.addParent(classVar));

        // Link the class through time
        dynamicDAG.getParentSetTimeT(classVar).addParent(classVar.getInterfaceVariable());

        // Show the dynamic DAG structure
        System.out.println(dynamicDAG.toString());

        /*************************************************************************************
         * 3.- LEARN DYNAMIC NAIVE BAYES AND SHOW CPD
         *************************************************************************************/

        // Set the number of available months for learning
        int nMonths = 5;

        long start = System.nanoTime();

        //Parallel Bayesian learning engine - parameters
        DynamicParallelVB parallelVB = new DynamicParallelVB();
        // Convergence parameters
        parallelVB.setGlobalThreshold(0.1);
        parallelVB.setMaximumGlobalIterations(100);
        parallelVB.setLocalThreshold(0.1);
        parallelVB.setMaximumLocalIterations(100);
        // Set the seed
        parallelVB.setSeed(0);
        // Set the batch/window size or level of parallelization (result is independent of this parameter)
        parallelVB.setBatchSize(1000);
        // Set the dynamic DAG to learn from (resulting DAG is nVariables*nSamples*nMonths)
        parallelVB.setDAG(dynamicDAG);
        // Show debugging output for VB
        parallelVB.setOutput(true);

        // Initiate parallel VB learning (set all necessary parameters prior to learning)
        parallelVB.initLearning();


        for (int i = 0; i < nMonths; i++) {
            logger.info("--------------- MONTH " + i + " --------------------------");
            //Load the data for that month
            DataFlink<DynamicDataInstance> dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env,
                    fileName+i+".arff", false);

            //Update the model with the provided data
            parallelVB.updateModelWithNewTimeSlice(i, dataNew);
        }

        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;
        logger.info("Running time: {} seconds.", seconds);

        //Show the learnt Dynamic Bayesian network
        System.out.println(parallelVB.getLearntDynamicBayesianNetwork());


        /*************************************************************************************
         * 4.- INCLUDE LATENT VARIABLE (H) ON HNB AND LEARN (IDA-LIKE TRANSITION)
         *************************************************************************************/

        // Define the global latent binary variable.
        Variable globalHiddenVar = variables.newGaussianDynamicVariable("GlobalHidden");

        dynamicDAG.updateDynamicVariables(variables);

        // Link the hidden as parent of all predictive attributes
        dynamicDAG.getParentSetsTimeT()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .filter(w -> w.getMainVar() != globalHiddenVar)
                .forEach(w -> w.addParent(globalHiddenVar));

        // Show the new dynamic DAG structure
        System.out.println(dynamicDAG.toString());

        /*************************************************************************************
         * 5.- LEARN DYNAMIC NAIVE BAYES WITH HIDDEN VARIABLE AND SHOW EXPECTED VALUE OF H
         *************************************************************************************/

        // Create the plateu structure to replicate
        parallelVB.setPlateuStructure(new PlateuStructure(Arrays.asList(globalHiddenVar)));

        // Define the transition for the global hidden variable, starting with a standard N(0,1)
        // Gaussian and transition variance (that is summed to that of the previous step) 1.
        GaussianHiddenTransitionMethod gaussianHiddenTransitionMethod =
                new GaussianHiddenTransitionMethod(Arrays.asList(globalHiddenVar), 0, 0.1);
        gaussianHiddenTransitionMethod.setFading(1.0);
        parallelVB.setTransitionMethod(gaussianHiddenTransitionMethod);

        // Update the dynamic DAG to learn from
        parallelVB.setDAG(dynamicDAG);

        //Set the procedure to make the model identifiable
        parallelVB.setIdenitifableModelling(new IdentifiableIDAModel());

        //Init learning
        parallelVB.initLearning();

        //Collect expected output for the global variable each month
        double[] output = new double[nMonths];

        System.out.println("--------------- MONTH " + 0 + " --------------------------");
        //Load the data for the first month
        parallelVB.updateModelWithNewTimeSlice(0, data0);
        //Compute expected value for H the first month
        Normal normal = parallelVB.getParameterPosteriorTime0(globalHiddenVar);
        output[0] = normal.getMean();

        for (int i = 1; i < nMonths; i++) {
            System.out.println("--------------- MONTH " + i + " --------------------------");
            //Load the data for that month
            DataFlink<DynamicDataInstance> dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env,
                    "./datasets/dataFlink/conceptdrift/data" + i + ".arff", false);
            parallelVB.updateModelWithNewTimeSlice(i, dataNew);
            //Compute expected value for H this month
            normal = parallelVB.getParameterPosteriorTimeT(globalHiddenVar);
            output[i] = normal.getMean();
        }

        System.out.println(parallelVB.getLearntDynamicBayesianNetwork());

        for (int i = 0; i < nMonths; i++) {
            System.out.println("E(H_"+i+") =\t" + output[i]);
        }

    }
}
