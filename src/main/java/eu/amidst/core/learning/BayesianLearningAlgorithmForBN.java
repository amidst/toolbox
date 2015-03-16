package eu.amidst.core.learning;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;

/**
 * Created by ana@cs.aau.dk on 04/03/15.
 */
public interface BayesianLearningAlgorithmForBN {

    double updateModel(DataOnMemory<DataInstance> batch);

    double getLogMarginalProbability();

    void runLearning();

    void setDAG(DAG dag);

    void setDataStream(DataStream<DataInstance> data);

    BayesianNetwork getLearntBayesianNetwork();

    public void setParallelMode(boolean parallelMode);

}
