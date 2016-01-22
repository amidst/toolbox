package eu.amidst.reviewMeeting2016;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.learning.parametric.bayesian.PlateuStructure;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.learning.dynamic.DynamicParallelVB;
import eu.amidst.huginlink.io.DBNWriterToHugin;
import org.apache.flink.api.java.ExecutionEnvironment;

/**
 * Created by ana@cs.aau.dk on 21/01/16.
 */
public class CajaMarDemoNB {

    //static Logger logger = LoggerFactory.getLogger(CajaMarDemo.class);

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
        String fileName = "hdfs:///tmp_conceptdrift_data";
        //String fileName = "./datasets/dataFlink/conceptdrift/data";

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
/*
        for (Variable var: variables){
            if (var==classVar)
                continue;

            dynamicDAG.getParentSetTimeT(var).addParent(classVar);
        }
  */
        // Link the class through time
        dynamicDAG.getParentSetTimeT(classVar).addParent(classVar.getInterfaceVariable());

        // Show the dynamic DAG structure
        System.out.println(dynamicDAG.toString());

        /*************************************************************************************
         * 3.- LEARN DYNAMIC NAIVE BAYES AND SHOW CPD
         *************************************************************************************/

        // Set the number of available months for learning
        int nMonths = Integer.parseInt(args[0]);

        long start = System.nanoTime();

        //Parallel Bayesian learning engine - parameters
        DynamicParallelVB parallelVB = new DynamicParallelVB();

        //
        parallelVB.setPlateuStructure(new PlateuStructure());
        // Convergence parameters
        parallelVB.setGlobalThreshold(0.1);
        parallelVB.setMaximumGlobalIterations(100);
        parallelVB.setLocalThreshold(0.1);
        parallelVB.setMaximumLocalIterations(100);
        // Set the seed
        parallelVB.setSeed(5);
        // Set the batch/window size or level of parallelization (result is independent of this parameter)
        parallelVB.setBatchSize(1000);
        // Set the dynamic DAG to learn from (resulting DAG is nVariables*nSamples*nMonths)
        parallelVB.setDAG(dynamicDAG);
        // Show debugging output for VB
        parallelVB.setOutput(true);

        // Initiate parallel VB learning (set all necessary parameters prior to learning)
        parallelVB.initLearning();


        for (int i = 0; i < nMonths; i++) {
            System.out.println("--------------- MONTH " + i + " --------------------------");
            //Load the data for that month
            DataFlink<DynamicDataInstance> dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env,
                    fileName+i+".arff", false);

            //Update the model with the provided data
            parallelVB.updateModelWithNewTimeSlice(i, dataNew);
        }

        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;
        System.out.println("Running time (DNB): "+seconds+" seconds.");

        //Show the learnt Dynamic Bayesian network
        System.out.println(parallelVB.getLearntDynamicBayesianNetwork());


        DBNWriterToHugin.saveToHuginFile(parallelVB.getLearntDynamicBayesianNetwork(),"networks/DNB.net");

    }
}
