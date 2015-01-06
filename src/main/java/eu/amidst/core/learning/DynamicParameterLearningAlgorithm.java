package eu.amidst.core.learning;

import eu.amidst.core.database.DataBase;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;

/**
 * Created by andresmasegosa on 06/01/15.
 */
@FunctionalInterface
public interface DynamicParameterLearningAlgorithm {
       public DynamicBayesianNetwork learn(DynamicDAG dag, DataBase dataBase);
}
