package eu.amidst.reviewMeeting2016;

import eu.amidst.core.conceptdrift.utils.GaussianHiddenTransitionMethod;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.learning.parametric.bayesian.PlateuStructure;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.learning.dynamic.DynamicParallelVB;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ana@cs.aau.dk on 21/01/16.
 */
public class CajaMarDemo {

    static Logger logger = LoggerFactory.getLogger(CajaMarDemo.class);
    public static int seed = 0;
    public static int batchSize = 500;

    public static double transitionVariance = 0.1;

    public static void main(String[] args) throws Exception {

        /*
         * Create flink ExecutionEnvironment variable
         */
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        /*************************************************************************************
         * 1.- READ DATA TO GET MODEL HEADER (ATTRIBUTES)
         *************************************************************************************/

        //String fileName = "hdfs:///tmp_conceptdrift_data";
        String fileName = "./datasets/dataFlink/conceptdrift/data";

        DataFlink<DynamicDataInstance> data0 = DataFlinkLoader.loadDynamicDataFromFolder(env,fileName+0+".arff", false);

        Attributes attributes = data0.getAttributes();

        /*************************************************************************************
         * 2. - CREATE A DYNAMIC NAIVE BAYES DAG
         *************************************************************************************/

        // Create a Variables object from the attributes of the input data stream.
        DynamicVariables variables = new DynamicVariables(attributes);

        // Define the class variable.
        Variable classVar = variables.getVariableById(0);

        // Create a DAG object with the defined variables.
        DynamicDAG dynamicDAG = new DynamicDAG(variables);

        // Link the class as parent of all attributes
        dynamicDAG.getParentSetsTimeT()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .forEach(w -> w.addParent(classVar));

        // Link the class and the attributes through time
        dynamicDAG.getParentSetsTimeT()
                .stream()
                .forEach(w -> w.addParent(w.getMainVar().getInterfaceVariable()));

        System.out.println(dynamicDAG.toString());

        /*************************************************************************************
         * 3.- LEARN DYNAMIC NAIVE BAYES AND SHOW CPD
         *************************************************************************************/

        // Set the number of available months for learning
        int nMonths = 10;

        long start = System.nanoTime();

        // Parameter Learning with DynamicParallelVB (parameter setting)
        DynamicParallelVB parallelVB = new DynamicParallelVB();
        parallelVB.setGlobalThreshold(0.1);
        parallelVB.setMaximumGlobalIterations(10);
        parallelVB.setLocalThreshold(0.1);
        parallelVB.setMaximumLocalIterations(100);
        parallelVB.setSeed(seed);
        parallelVB.setBatchSize(batchSize);
        parallelVB.setDAG(dynamicDAG);
        parallelVB.setOutput(true);

        // Initiate learning
        parallelVB.initLearning();

        System.out.println("--------------- MONTH " + 0 + " --------------------------");
        parallelVB.updateModelWithNewTimeSlice(0, data0);


        for (int i = 1; i < nMonths; i++) {
            logger.info("--------------- MONTH " + i + " --------------------------");
            DataFlink<DynamicDataInstance> dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env,
                    fileName+i+".arff", false);
            parallelVB.updateModelWithNewTimeSlice(i, dataNew);
        }

        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;
        logger.info("Running time: {} seconds.", seconds);

        System.out.println(parallelVB.getLearntDynamicBayesianNetwork().toString());


        /*************************************************************************************
         * 4.- INCLUDE LATENT VARIABLE (H) ON HNB AND LEARN (IDA-LIKE TRANSITION)
         *************************************************************************************/

        // Define the global latent binary variable.
        Variable globalHiddenVar = variables.newMultinomialDynamicVariable("GlobalHidden", 2);

        // Link the class as parent of the hidden variable.
        dynamicDAG.getParentSetTimeT(globalHiddenVar).addParent(classVar);

        // Link the hidden as parent of all attributes
        dynamicDAG.getParentSetsTimeT()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .forEach(w -> w.addParent(globalHiddenVar));

        // Link the hidden variable through time
        dynamicDAG.getParentSetTimeT(globalHiddenVar).addParent(globalHiddenVar.getInterfaceVariable());

        System.out.println(dynamicDAG.toString());

        /*************************************************************************************
         * 5.- LEARN DYNAMIC NAIVE BAYES WITH HIDDEN VARIABLE AND SHOW EXPECTED VALUE OF H
         *************************************************************************************/
        List<Variable> hiddenVars = new ArrayList<Variable>();
        hiddenVars.add(globalHiddenVar);

        DynamicParallelVB svb = new DynamicParallelVB();
        svb.setSeed(seed);
        svb.setPlateuStructure(new PlateuStructure(hiddenVars));
        GaussianHiddenTransitionMethod gaussianHiddenTransitionMethod =
                new GaussianHiddenTransitionMethod(hiddenVars, 0, transitionVariance);
        gaussianHiddenTransitionMethod.setFading(1.0);
        svb.setTransitionMethod(gaussianHiddenTransitionMethod);
        svb.setBatchSize(batchSize);
        svb.setDAG(dynamicDAG);

        svb.setOutput(false);
        svb.setGlobalThreshold(0.001);
        svb.setLocalThreshold(0.001);
        svb.setMaximumLocalIterations(100);
        svb.setMaximumGlobalIterations(100);

        svb.initLearning();

        double[] output = new double[nMonths];

        System.out.println("--------------- DATA " + 0 + " --------------------------");
        svb.updateModelWithNewTimeSlice(0, data0);
        Normal normal = svb.getParameterPosteriorTime0(globalHiddenVar);
        output[0] = normal.getMean();

        for (int i = 1; i < nMonths; i++) {
            System.out.println("--------------- DATA " + i + " --------------------------");
            DataFlink<DynamicDataInstance> dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env,
                    "./datasets/dataFlink/conceptdrift/data" + i + ".arff", false);
            svb.updateModelWithNewTimeSlice(i, dataNew);
            normal = svb.getParameterPosteriorTimeT(globalHiddenVar);
            output[i] = normal.getMean();

            System.out.println(svb.getLearntDynamicBayesianNetwork());

        }

        System.out.println(svb.getLearntDynamicBayesianNetwork());

        for (int i = 0; i < nMonths; i++) {
            System.out.println("E(H_"+i+") =\t" + output[i]);
        }

    }
}
