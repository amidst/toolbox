/**
 ******************* ISSUE LIST **************************
 *
 * 1. Would it be better to catch the exception in getValue?
 *    Make sure a message is shown! (I am not sure since it is an unchecked exception).
 *
 * ********************************************************
 */

package eu.amidst.core.database.dynamics.readers.impl;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.statics.readers.impl.StaticDataInstance;
import eu.amidst.core.header.Variable;

/**
 * Created by ana@cs.aau.dk on 10/11/14.
 */
public class DynamicDataInstance implements DataInstance{

    StaticDataInstance present;
    StaticDataInstance past;

    public DynamicDataInstance(StaticDataInstance past, StaticDataInstance present){
        this.past = new StaticDataInstance(past);
        this.present = new StaticDataInstance(present);
    }

    @Override
    public double getValue(Variable var) {
        if (var.isTemporalClone())
            return past.getValue(var);
        else
            return present.getValue(var);

    }

    public StaticDataInstance getFullPastInstance(){
        return this.past;
    }

    public StaticDataInstance getFullPresentInstance(){
        return this.past;
    }
}
