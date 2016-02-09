package eu.amidst.modelExperiments;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.learning.parametric.ParallelVB;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ana@cs.aau.dk on 08/02/16.
 */
public class IDAmodelDistributedVMP {

    static Logger logger = LoggerFactory.getLogger(IDAmodelDistributedVMP.class);


    public static DAG getDAGstructure(Attributes attributes){
        // Create a Variables object from the attributes of the input data stream.
        Variables variables = new Variables(attributes);

        // Define the class variable.
        Variable classVar = variables.getVariableByName("DEFAULT");

        // Define the global hidden variable.
        Variable globalHiddenVar = variables.newGaussianVariable("GlobalHidden");

        // Define a local hidden variable.
        Variable localHiddenVar = variables.newGaussianVariable("LocalHidden");

        // Create an empty DAG object with the defined variables.
        DAG dag = new DAG(variables);

        // Link the class as parent of all attributes
        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .filter(w -> w.getMainVar() != globalHiddenVar)
                .filter(w -> w.getMainVar() != localHiddenVar)
                .forEach(w -> w.addParent(classVar));

        // Link the global hidden as parent of all predictive attributes
        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .filter(w -> w.getMainVar() != globalHiddenVar)
                .filter(w -> w.getMainVar() != localHiddenVar)
                .forEach(w -> w.addParent(globalHiddenVar));

        // Link the local hidden as parent of all predictive attributes
        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .filter(w -> w.getMainVar() != globalHiddenVar)
                .filter(w -> w.getMainVar() != localHiddenVar)
                .forEach(w -> w.addParent(localHiddenVar));


        // Show the new dynamic DAG structure
        System.out.println(dag.toString());

        return dag;
    }

    public static void main(String[] args) throws Exception {

        //String fileName = "hdfs:///tmp_uai100K.arff";
        //String fileName = "./datasets/dataFlink/uai1K.arff";
        String fileName = args[0];

        int windowSize = Integer.parseInt(args[1]);
        int globalIter = Integer.parseInt(args[2]);
        int localIter = Integer.parseInt(args[3]);
        int seed = Integer.parseInt(args[4]);

        //BasicConfigurator.configure();
        //PropertyConfigurator.configure(args[4]);

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        //env.setParallelism(1);


        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFolder(env,fileName, true);

        DAG hiddenNB = getDAGstructure(dataFlink.getAttributes());

        long start = System.nanoTime();

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setGlobalThreshold(0.1);
        parallelVB.setMaximumGlobalIterations(globalIter);
        parallelVB.setLocalThreshold(0.1);
        parallelVB.setMaximumLocalIterations(localIter);
        parallelVB.setSeed(seed);

        //Set the window size
        parallelVB.setBatchSize(windowSize);

        parallelVB.setIdenitifableModelling(new IdentifiableIDAUAIModel());


        parallelVB.setDAG(hiddenNB);
        parallelVB.setDataFlink(dataFlink);
        parallelVB.runLearning();
        BayesianNetwork LearnedBnet = parallelVB.getLearntBayesianNetwork();
        System.out.println(LearnedBnet.toString());

        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;
        logger.info("Total running time: {} seconds.", seconds);

    }

}
