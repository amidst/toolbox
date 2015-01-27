package eu.amidst.core.learning;


import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.StaticDataInstance;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.variables.DynamicVariables;
import eu.amidst.core.variables.StaticVariables;

/**
 * Created by andresmasegosa on 06/01/15.
 */
@FunctionalInterface
public interface DynamicStructuralLearningAlgorithm {
    public DynamicDAG learn(DataBase dataBase);
}
