package eu.amidst.sparklink.examples.learning;

import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.DAGGenerator;
import eu.amidst.sparklink.core.data.DataSpark;
import eu.amidst.sparklink.core.io.DataSparkLoader;
import eu.amidst.sparklink.core.learning.ParallelMaximumLikelihood;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.SQLContext;

/**
 * Created by rcabanas on 10/06/16.
 */
public class MaximumLikelihoodLearningExample {
    public static void main(String[] args) throws Exception {
        SparkConf conf = new SparkConf().setAppName("SparkLink!").setMaster("local");;
        SparkContext sc = new SparkContext(conf);
        SQLContext sqlContext = new SQLContext(sc);

        //Path to dataset
        String path ="datasets/simulated/WI_samples.json";

        //Create an AMIDST object for managing the data
        DataSpark dataSpark = DataSparkLoader.open(sqlContext, path);

        //Learning algorithm
        ParallelMaximumLikelihood parameterLearningAlgorithm = new ParallelMaximumLikelihood();


        //We fix the BN structure
        DAG dag = DAGGenerator.getNaiveBayesStructure(dataSpark.getAttributes(), "W");

        parameterLearningAlgorithm.setDAG(dag);

        //We set the batch size which will be employed to learn the model in parallel
        parameterLearningAlgorithm.setBatchSize(100);
        //We set the data which is going to be used for leaning the parameters
        parameterLearningAlgorithm.setDataSpark(dataSpark);
        //We perform the learning
        parameterLearningAlgorithm.runLearning();
        //And we get the model
        BayesianNetwork bn = parameterLearningAlgorithm.getLearntBayesianNetwork();

        System.out.println(bn);


    }
}
