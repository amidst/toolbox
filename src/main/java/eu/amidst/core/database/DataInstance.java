package eu.amidst.core.database;

import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

/**
 * Created by ana@cs.aau.dk on 10/11/14.
 */
public interface DataInstance extends Assignment{

    @Override
    default double getValue(Variable var){
        return this.getValue(var.getAttribute());
    }

    @Override
    default void setValue(Variable var, double value){
        this.setValue(var.getAttribute(), value);
    }

    double getValue(Attribute att);

    void setValue(Attribute att, double val);

}

