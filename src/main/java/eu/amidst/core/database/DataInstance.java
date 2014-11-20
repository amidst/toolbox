package eu.amidst.core.database;

import eu.amidst.core.variables.Variable;

/**
 * Created by ana@cs.aau.dk on 10/11/14.
 */
public interface DataInstance {

    public double getValue(Variable var);

    public int getSequenceID();

    public int getTimeID();

}

