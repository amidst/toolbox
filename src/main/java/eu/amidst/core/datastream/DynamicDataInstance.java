package eu.amidst.core.datastream;

import eu.amidst.core.variables.DynamicAssignment;
import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 27/01/15.
 */
public interface DynamicDataInstance extends DataInstance, DynamicAssignment{

    int getSequenceID();

    int getTimeID();

    double getValue(Attribute att, boolean present);

    void setValue(Attribute att, double val, boolean present);

    @Override
    default double getValue(Attribute att){
        return this.getValue(att,true);
    }

    @Override
    default void setValue(Attribute att, double val){
        this.setValue(att,val,true);
    }

    @Override
    default double getValue(Variable var) {
        if (var.isInterfaceVariable()) {
            return this.getValue(var.getAttribute(),false);
        } else {
            return this.getValue(var.getAttribute(),true);
        }
    }

    @Override
    default void setValue(Variable var, double val) {
        if (var.isInterfaceVariable()) {
            this.setValue(var.getAttribute(), val, false);
        } else {
            this.setValue(var.getAttribute(), val, true);
        }
    }
}
