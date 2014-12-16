package eu.amidst.core.variables;

import java.util.List;

/**
 * Created by ana@cs.aau.dk on 03/11/14.
 */
public interface Assignment {

    double getValue(Variable var);

    default String toString(List<Variable> vars){
        StringBuilder builder = new StringBuilder(vars.size()*2);
        vars.stream().limit(vars.size()-1).forEach(var -> builder.append(this.getValue(var)+","));
        builder.append(this.getValue(vars.get(vars.size()-1)));
        return builder.toString();
    }

}
