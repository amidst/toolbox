package eu.amidst.core.variables;

import java.util.List;

/**
 * Created by ana@cs.aau.dk on 03/11/14.
 */
public interface Assignment {

    double getValue(Variable var);

    void setValue(Variable var, double value);

    // TODO Check THIS!!
    default String toString(List<Variable> vars){
        StringBuilder builder = new StringBuilder(vars.size()*2);
        builder.append("{");
        vars.stream().limit(vars.size()-1).forEach(var -> builder.append(var.getName()+ " = "+(int)this.getValue(var)+", "));
        builder.append(vars.get(vars.size()-1).getName()+ " = "+ (int)this.getValue(vars.get(vars.size()-1)));
        builder.append("}");
        return builder.toString();
    }

}
