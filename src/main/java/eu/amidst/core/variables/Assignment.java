package eu.amidst.core.variables;

import eu.amidst.core.database.Attribute;

import java.util.List;

/**
 * Created by ana@cs.aau.dk on 03/11/14.
 */
public interface Assignment {

    double getValue(Variable var);

    // TODO Check THIS!!
    default String toString(List<Variable> vars){
        StringBuilder builder = new StringBuilder(vars.size()*2);
        vars.stream().limit(vars.size()-1).forEach(var -> builder.append(this.getValue(var)+","));
        builder.append(this.getValue(vars.get(vars.size()-1)));
        return builder.toString();
    }

    //TODO: Try to move the state space type how to print the values. So if more states spaces are included,
    //we do not need to change this method.
    default String toARFFString(List<Variable> vars){
        StringBuilder builder = new StringBuilder(vars.size()*2);

        //MEJORAR PONER CUANDO REAL
        for(int i=0; i<vars.size()-1;i++) {
            if (vars.get(i).getStateSpace().getStateSpaceType() == StateSpaceType.FINITE_SET) {
                FiniteStateSpace stateSpace = vars.get(i).getStateSpace();
                String nameState = stateSpace.getStatesName((int) this.getValue(vars.get(i)));
                builder.append(nameState + ",");
            }
            else if (vars.get(i).getStateSpace().getStateSpaceType() == StateSpaceType.REAL) {

                builder.append(this.getValue(vars.get(i))+ ",");
            }else{
                throw new IllegalArgumentException("Illegal State Space Type: " + vars.get(i).getStateSpace().getStateSpaceType());
            }
        }

        if(vars.get(vars.size()-1).getStateSpace().getStateSpaceType()  == StateSpaceType.FINITE_SET) {
            FiniteStateSpace stateSpace = vars.get(vars.size() - 1).getStateSpace();
            String nameState = stateSpace.getStatesName((int) this.getValue(vars.get(vars.size() - 1)));
            builder.append(nameState);
        }
        else if(vars.get(vars.size()-1).getStateSpace().getStateSpaceType()  == StateSpaceType.REAL) {
            builder.append(this.getValue(vars.get(vars.size() - 1)));
        }else{
            throw new IllegalArgumentException("Illegal State Space Type: " + vars.get(vars.size()-1).getStateSpace().getStateSpaceType());
        }
        return builder.toString();
    }

}
