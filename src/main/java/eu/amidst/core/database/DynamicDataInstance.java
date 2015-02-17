package eu.amidst.core.database;

import eu.amidst.core.variables.DynamicAssignment;

/**
 * Created by andresmasegosa on 27/01/15.
 */
public interface DynamicDataInstance extends DataInstance, DynamicAssignment{

    int getSequenceID();

    int getTimeID();

}
