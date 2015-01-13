package eu.amidst.core.variables;

import eu.amidst.core.database.Attribute;

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

    default String toARFFString(List<Variable> vars){
        StringBuilder builder = new StringBuilder(vars.size()*2);

        //MEJORAR PONER CUANDO REAL
        for(int i=0; i<vars.size()-1;i++) {
            if (vars.get(i).getStateSpace().getStateSpaceType() == StateSpaceType.FINITE_SET) {
                FiniteStateSpace stateSpace = vars.get(i).getStateSpace();
                String nameState = stateSpace.getStatesName((int) this.getValue(vars.get(i)));
                builder.append(nameState + ",");
            }
            else{
                builder.append(this.getValue(vars.get(i))+ ",");
            }
        }

        if(vars.get(vars.size()-1).getStateSpace().getStateSpaceType()  == StateSpaceType.FINITE_SET) {
            FiniteStateSpace stateSpace = vars.get(vars.size() - 1).getStateSpace();
            String nameState = stateSpace.getStatesName((int) this.getValue(vars.get(vars.size() - 1)));
            builder.append(nameState);
        }
        else{
            builder.append(this.getValue(vars.get(vars.size() - 1)));
        }
        return builder.toString();
    }

}
