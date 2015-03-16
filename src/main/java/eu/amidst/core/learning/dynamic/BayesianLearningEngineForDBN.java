package eu.amidst.core.learning.dynamic;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.learning.BayesianLearningAlgorithmForBN;
import eu.amidst.core.learning.StreamingVariationalBayesVMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;

/**
 * Created by ana@cs.aau.dk on 04/03/15.
 */
public final class BayesianLearningEngineForDBN {

    private static BayesianLearningAlgorithmForDBN bayesianLearningAlgorithmForDBN = new StreamingVariationalBayesVMPForDBN();

    public static void setBayesianLearningAlgorithmForDBN(BayesianLearningAlgorithmForDBN bayesianLearningAlgorithmForDBN) {
        BayesianLearningEngineForDBN.bayesianLearningAlgorithmForDBN = bayesianLearningAlgorithmForDBN;
    }

    public static double updateModel(DataOnMemory<DynamicDataInstance> batch){
        return bayesianLearningAlgorithmForDBN.updateModel(batch);
    }

    public static void runLearning() {
        bayesianLearningAlgorithmForDBN.runLearning();
    }

    public static double getLogMarginalProbability(){
        return bayesianLearningAlgorithmForDBN.getLogMarginalProbability();
    }

    public static void setDataStream(DataStream<DynamicDataInstance> data){
        bayesianLearningAlgorithmForDBN.setDataStream(data);
    }

    public void setParallelMode(boolean parallelMode) {
        bayesianLearningAlgorithmForDBN.setParallelMode(parallelMode);
    }

    public static void setDynamicDAG(DynamicDAG dag){
        bayesianLearningAlgorithmForDBN.setDynamicDAG(dag);
    }

    public static DynamicBayesianNetwork getLearntDBN(){
        return bayesianLearningAlgorithmForDBN.getLearntDBN();
    }

    public static void main(String[] args) throws Exception{

    }
}
