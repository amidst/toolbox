package eu.amidst.modelExperiments;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.learning.parametric.bayesian.PlateuStructure;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.learning.parametric.StochasticVI;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static eu.amidst.modelExperiments.DAGsGeneration.getIDALocalGlobalDAG;

/**
 * Created by ana@cs.aau.dk on 08/02/16.
 */
public class IDAmodelDistributedSVI {

    static Logger logger = LoggerFactory.getLogger(IDAmodelDistributedSVI.class);

    public static void main(String[] args) throws Exception {

        //String fileName = "hdfs:///tmp_uai100K.arff";
        //String fileName = "./datasets/dataFlink/uai1K.arff";
        //args= new String[]{" " +
        //        "./datasets/dataFlink/uai10K.arff", "10", "1000", "50", "0", "10000", "0.75"};

        String fileName = args[0];

        int windowSize = Integer.parseInt(args[1]);
        int localIter = Integer.parseInt(args[2]);
        long timeLimit = Long.parseLong(args[3]);
        int seed = Integer.parseInt(args[4]);
        int dataSetSize = Integer.parseInt(args[5]);
        double learningRate = Double.parseDouble(args[6]);

        //BasicConfigurator.configure();
        //PropertyConfigurator.configure(args[4]);

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        //env.setParallelism(1);


        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFolder(env,fileName, false);

        DAG hiddenNB = getIDALocalGlobalDAG(dataFlink.getAttributes());
        long start = System.nanoTime();

        //Parameter Learning
        StochasticVI stochasticVI = new StochasticVI();
        stochasticVI.setLocalThreshold(0.1);
        stochasticVI.setMaximumLocalIterations(localIter);
        stochasticVI.setSeed(seed);
        //Set the window size
        stochasticVI.setBatchSize(windowSize);

        stochasticVI.setLearningFactor(learningRate);
        stochasticVI.setDataSetSize(dataSetSize);
        stochasticVI.setTimiLimit(timeLimit);


        List<Variable> hiddenVars = new ArrayList<>();
        hiddenVars.add(hiddenNB.getVariables().getVariableByName("GlobalHidden"));
        stochasticVI.setPlateuStructure(new PlateuStructure(hiddenVars));
        //GaussianHiddenTransitionMethod gaussianHiddenTransitionMethod = new GaussianHiddenTransitionMethod(hiddenVars, 0, 1);
        //gaussianHiddenTransitionMethod.setFading(1.0);
        //stochasticVI.setTransitionMethod(gaussianHiddenTransitionMethod);

        stochasticVI.setDAG(hiddenNB);
        stochasticVI.setDataFlink(dataFlink);
        stochasticVI.runLearning();
        BayesianNetwork LearnedBnet = stochasticVI.getLearntBayesianNetwork();
        System.out.println(LearnedBnet.toString());

        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;
        logger.info("Total running time: {} seconds.", seconds);

    }

}
