package eu.amidst.core.learning;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.DAG;

/**
 * Created by ana@cs.aau.dk on 04/03/15.
 */
public interface BayesianLearningAlgorithmForBN {

    public void runLearning();

    public void setDAG(DAG dag);

    public void setDataStream(DataStream<DataInstance> data);


}
