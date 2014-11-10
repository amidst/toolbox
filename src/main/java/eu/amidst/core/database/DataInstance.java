package eu.amidst.core.database;

import eu.amidst.core.database.statics.readers.DataStream;
import eu.amidst.core.header.Variable;

/**
 * Created by ana@cs.aau.dk on 10/11/14.
 */
public interface DataInstance {

    public double getValue(Variable var);

}
