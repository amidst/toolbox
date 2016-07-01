package eu.amidst.flinklink.examples.learning;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.DAGGenerator;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.learning.parametric.ParameterLearningAlgorithm;
import eu.amidst.flinklink.core.learning.parametric.StochasticVI;
import eu.amidst.flinklink.core.utils.DataSetGenerator;

/**
 * Created by rcabanas on 14/06/16.
 */
public class StochasticVIExample {
    public static void main(String[] args) throws Exception {

        //generate a random dataset
        DataFlink<DataInstance> dataFlink = new DataSetGenerator().generate(1234,1000,5,0);

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
        ((StochasticVI)learningAlgorithmFlink).setDataSetSize(dataFlink.getDataSet().count());


        //Print the learnt BN
        BayesianNetwork bn = learningAlgorithmFlink.getLearntBayesianNetwork();
        System.out.println(bn);



    }
}