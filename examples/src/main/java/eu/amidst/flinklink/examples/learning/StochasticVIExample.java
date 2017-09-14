package eu.amidst.flinklink.examples.learning;

import eu.amidst.Main;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.DAGGenerator;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.learning.parametric.ParameterLearningAlgorithm;
import eu.amidst.flinklink.core.learning.parametric.StochasticVI;
import eu.amidst.flinklink.core.utils.DataSetGenerator;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;

/**
 * Created by rcabanas on 14/06/16.
 */
public class StochasticVIExample {
    public static void main(String[] args) throws Exception {

        boolean hadoop_cluster = false;

        if (args.length>1){
            hadoop_cluster = Boolean.parseBoolean(args[0]);
        }

        final ExecutionEnvironment env;

        //Set-up Flink session.
        if(hadoop_cluster){
            env = ExecutionEnvironment.getExecutionEnvironment();
            env.getConfig().disableSysoutLogging();
        }else{
            Configuration conf = new Configuration();
            conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
            conf.setInteger("taskmanager.numberOfTaskSlots",Main.PARALLELISM);
            env = ExecutionEnvironment.createLocalEnvironment(conf);
            env.setParallelism(Main.PARALLELISM);
            env.getConfig().disableSysoutLogging();
        }
        //generate a random dataset
        DataFlink<DataInstance> dataFlink = new DataSetGenerator().generate(env,1234,1000,5,0);

        //Creates a DAG with the NaiveBayes structure for the random dataset
        DAG dag = DAGGenerator.getNaiveBayesStructure(dataFlink.getAttributes(), "DiscreteVar4");
        System.out.println(dag.toString());


        //Create the  Learner object
        ParameterLearningAlgorithm learningAlgorithmFlink =
                new StochasticVI();

        //Learning parameters
        learningAlgorithmFlink.setBatchSize(10);
        learningAlgorithmFlink.setDAG(dag);

        //Initialize the learning process
        learningAlgorithmFlink.initLearning();

        //Learn from the flink data
        learningAlgorithmFlink.updateModel(dataFlink);

        //Specific parameters for the algorithm
        ((StochasticVI)learningAlgorithmFlink).setLearningFactor(0.7);
        ((StochasticVI)learningAlgorithmFlink).setDataSetSize((int) dataFlink.getDataSet().count());


        //Print the learnt BN
        BayesianNetwork bn = learningAlgorithmFlink.getLearntBayesianNetwork();
        System.out.println(bn);



    }
}
