package eu.amidst.core.datastream;

import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * Created by ana@cs.aau.dk on 10/11/14.
 */
public interface DataInstance extends Assignment {

    @Override
    default double getValue(Variable var) {
        return this.getValue(var.getAttribute());
    }

    @Override
    default void setValue(Variable var, double value) {
        this.setValue(var.getAttribute(), value);
    }

    double getValue(Attribute att);

    void setValue(Attribute att, double val);

    default String toString(Attributes atts) {
        StringBuilder builder = new StringBuilder(atts.getList().size()*2);
        builder.append("{");
        atts.getList().stream().limit(atts.getList().size()-1).forEach(att -> builder.append(att.getName()+ " = "+this.getValue(att)+", "));
        builder.append(atts.getList().get(atts.getList().size()-1).getName()+ " = "+ this.getValue(atts.getList().get(atts.getList().size()-1)));
        builder.append("}");
        return builder.toString();
    }

}