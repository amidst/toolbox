package eu.amidst.core.learning;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.DAG;

/**
 * Created by andresmasegosa on 06/01/15.
 */
@FunctionalInterface
public interface StaticStructuralLearningAlgorithm {
    public DAG learn(DataStream<DataInstance> dataStream);
}
