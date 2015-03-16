package eu.amidst.core.learning.dynamic;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;

/**
 * Created by andresmasegosa on 06/01/15.
 */
@FunctionalInterface
public interface DynamicParameterLearningAlgorithm {
       public DynamicBayesianNetwork learn(DynamicDAG dag, DataStream<DynamicDataInstance> dataStream);
}
