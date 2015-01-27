package eu.amidst.core.database;

import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

/**
 * Created by ana@cs.aau.dk on 10/11/14.
 */
public interface DataInstance extends Assignment{

    double getValue(Variable var);

}

