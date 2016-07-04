import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.DAGGenerator;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import eu.amidst.flinklink.core.learning.parametric.*;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.hadoop.shaded.org.jboss.netty.util.internal.SystemPropertyUtil;

import java.io.FileNotFoundException;

/**
 * Created by rcabanas on 07/06/16.
 */
public class filnklink_usage {

    public static void main(String[] args) throws Exception {


/*
        //Load the data
        String pathFileData = "datasets/simulated/exampleDS_d5_c0.arff";
        //pathFileData = "./ignoredfolder/datasets/flinkfile.arff";


        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();


        //DataFlink<DataInstance> data = DataFlinkLoader..open(env, pathFileData, false);
        //DataFlink<DataInstance> data = DataFlinkLoader.loadDataFromFolder(env,pathFileData,false);




        // Print the data
        // Note that data is not ordered by SEQUENCE_ID: reads the file in parallel
        data.getDataSet()
                .collect()
                .stream()
                .forEach(d -> System.out.println(d));


        //Print the attributes
        data.getAttributes().forEach(a -> System.out.println(a.getName()));




        DataFlinkWriter.writeDataToARFFFolder(data, "./ignoredfolder/datasets/flinkfile.arff");


        DAG dag = DAGGenerator.getNaiveBayesStructure(data.getAttributes(), "DiscreteVar4");

        System.out.println(dag.toString());

        ParameterLearningAlgorithm learningAlgorithmFlink = new ParallelMaximumLikelihood();


                // new StochasticVI();
                // new ParallelMaximumLikelihood();
                 new DistributedVI();
                 new ParallelVB();
                // new dVMP();

        learningAlgorithmFlink.setBatchSize(10);
        learningAlgorithmFlink.setDAG(dag);
        learningAlgorithmFlink.setDataFlink(data);

    //    ((StochasticVI)learningAlgorithmFlink).setLearningFactor(0.7);
    //    ((StochasticVI)learningAlgorithmFlink).setDataSetSize(data.getDataSet().count());

        learningAlgorithmFlink.initLearning();
        learningAlgorithmFlink.updateModel(data);
        BayesianNetwork bn = learningAlgorithmFlink.getLearntBayesianNetwork();
        System.out.println(bn);

*/
    }






}
