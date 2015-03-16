package eu.amidst.core.learning.dynamic;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;

/**
 * Created by ana@cs.aau.dk on 04/03/15.
 */
public interface BayesianLearningAlgorithmForDBN {

    double updateModel(DataOnMemory<DynamicDataInstance> batch);

    double getLogMarginalProbability();

    void runLearning();

    void setDynamicDAG(DynamicDAG dag);

    void setDataStream(DataStream<DynamicDataInstance> data);

    DynamicBayesianNetwork getLearntDBN();

    public void setParallelMode(boolean parallelMode);


}
