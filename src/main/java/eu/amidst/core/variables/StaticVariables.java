/**
 ******************* ISSUE LIST **************************
 *
 * 1. Rename to Variables.
 * 2. We can/should remove all setters from VariableImplementation right?
 * 3. Is there any need for the field atts? It is only used in the constructor.
 * 4. The implementation of  "public StaticModelHeader(Attributes atts, HashMap<Attribute, DistType> typeDists)"
 * you need to specify for each attribute the disttype. We might think some default rule only violiated by those explict
 * assingments (Andres).

 *
 * ********************************************************
 */


package eu.amidst.core.variables;

import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.Attributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by afa on 02/07/14.
 */
public class StaticVariables {

    private List<Variable> allVariables;

    /**
     * Constructor where the distribution type of random variables is initialized by default.
     *
     */
    public StaticVariables(Attributes atts) {

        this.allVariables = new ArrayList<>();

        for (Attribute att : atts.getListExceptTimeAndSeq()) {
            VariableBuilder builder = new VariableBuilder(att);
            VariableImplementation var = new VariableImplementation(builder, allVariables.size());
            allVariables.add(var.getVarID(), var);
        }
    }

    /**
     * Constructor where the distribution type of random variables is provided as an argument.
     *
     */
    public StaticVariables(Attributes atts, HashMap<Attribute, DistType> typeDists) {

        this.allVariables = new ArrayList<>();

        for (Attribute att : atts.getListExceptTimeAndSeq()) {
            VariableBuilder builder;
            if (typeDists.containsKey(att)) {
                builder = new VariableBuilder(att, typeDists.get(att));
            }else{
                builder = new VariableBuilder(att);
            }

            VariableImplementation var = new VariableImplementation(builder, allVariables.size());
            allVariables.add(var.getVarID(), var);

        }
    }


    public Variable addHiddenVariable(VariableBuilder builder) {

        VariableImplementation var = new VariableImplementation(builder, allVariables.size());
        allVariables.add(var);
        return var;

    }

    public List<Variable> getVariables() {
        return this.allVariables;
    }

    public Variable getVariable(int varID) {
        return this.allVariables.get(varID);
    }

    public Variable getVariable(String name) {
        for(Variable var: getVariables()){
            if(var.getName().equals(name))
                return var;
        }
        throw new UnsupportedOperationException("Variable "+name+" is not part of the list of Variables (try uppercase)");
    }

    public int getNumberOfVars() {
        return this.allVariables.size();
    }

    private class VariableImplementation implements Variable {

        private String name;
        private int varID;
        private boolean observable;
        private int numberOfStates;
        private StateSpaceType stateSpaceType;
        private DistType distributionType;
        private Attribute attribute;

        public VariableImplementation(VariableBuilder builder, int varID) {
            this.name = builder.getName();
            this.varID = varID;
            this.observable = builder.isObservable();
            this.numberOfStates = builder.getNumberOfStates();
            this.stateSpaceType = builder.getStateSpaceType();
            this.distributionType = builder.getDistributionType();
            this.attribute = builder.getAttribute();
        }

        public String getName() {
            return this.name;
        }

        public int getVarID() {
            return varID;
        }

        public boolean isObservable() {
            return false;
        }

        public int getNumberOfStates() {
            return numberOfStates;
        }

        public StateSpaceType getStateSpaceType() {
            return stateSpaceType;
        }

        public DistType getDistributionType() {
            return distributionType;
        }

        public boolean isTemporalClone() {
            throw new UnsupportedOperationException("In a static context a variable cannot be temporal.");
        }

        public Attribute getAttribute(){return attribute;}

    }
}
