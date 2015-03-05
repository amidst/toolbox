package eu.amidst.core.learning;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;

/**
 * Created by ana@cs.aau.dk on 04/03/15.
 */
public final class BayesianLearningEngineForBN {

    private static BayesianLearningAlgorithmForBN bayesianLearningAlgorithmForBN = new BayesianVMPLearning();

    public static void setBayesianLearningAlgorithmForBN(BayesianLearningAlgorithmForBN bayesianLearningAlgorithmForBN) {
        BayesianLearningEngineForBN.bayesianLearningAlgorithmForBN = bayesianLearningAlgorithmForBN;
    }

    public static void runLearning(){
        bayesianLearningAlgorithmForBN.runLearning();
    }

    public static void setDAG(DAG dag){
        bayesianLearningAlgorithmForBN.setDAG(dag);
    }

    public static void setDataStream(DataStream<DataInstance> data){
        bayesianLearningAlgorithmForBN.setDataStream(data);
    }

    public static BayesianNetwork getLearntBayesianNetwork(){
        return bayesianLearningAlgorithmForBN.getLearntBayesianNetwork();
    }



    public static void main(String[] args) throws Exception{

    }
}
