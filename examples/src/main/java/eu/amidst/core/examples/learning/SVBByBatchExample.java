
package eu.amidst.core.examples.learning;




import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.DAGGenerator;

/**
 *
 * This example shows how to learn incrementally the parameters of a Bayesian network from a stream of data with a Bayesian
 * approach using the following algorithm
 *
 * <i> Broderick, T., Boyd, N., Wibisono, A., Wilson, A. C., and Jordan, M. I. (2013). Streaming variational bayes.
 * In Advances in Neural Information Processing Systems (pp. 1727-1735). </i>
 *
 *
 * Created by andresmasegosa on 18/6/15.
 */
public class SVBByBatchExample {


    public static void main(String[] args) throws Exception {

        //We can open the data stream using the static class DataStreamLoader
        DataStream<DataInstance> data = DataStreamLoader.open("datasets/simulated/WasteIncineratorSample.arff");

        //We create a SVB object
        SVB parameterLearningAlgorithm = new SVB();

        //We fix the DAG structure
        parameterLearningAlgorithm.setDAG(DAGGenerator.getHiddenNaiveBayesStructure(data.getAttributes(),"H",2));

        //We fix the size of the window, which must be equal to the size of the data batches we use for learning
        parameterLearningAlgorithm.setWindowsSize(5);

        //We can activate the output
        parameterLearningAlgorithm.setOutput(true);

        //We should invoke this method before processing any data
        parameterLearningAlgorithm.initLearning();


        //Then we show how we can perform parameter learning by a sequential updating of data batches.
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(5)){
            double log_likelhood_of_batch = parameterLearningAlgorithm.updateModel(batch);
            System.out.println("Log-Likelihood of Batch: "+ log_likelhood_of_batch);
        }

        //And we get the model
        BayesianNetwork bnModel = parameterLearningAlgorithm.getLearntBayesianNetwork();

        //We print the model
        System.out.println(bnModel.toString());

    }

}
