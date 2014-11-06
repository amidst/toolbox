/**
 ******************* ISSUE LIST **************************
 *
 * 1. Rename to DynamicVariables
 *
 *
 * ********************************************************
 */



package eu.amidst.core.header.dynamics;

import eu.amidst.core.database.statics.readers.Attribute;
import eu.amidst.core.database.statics.readers.Attributes;
import eu.amidst.core.database.statics.readers.DistType;
import eu.amidst.core.database.statics.readers.Kind;
import eu.amidst.core.header.statics.Variable;
import eu.amidst.core.header.statics.VariableBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by afa on 02/07/14.
 */
public class DynamicModelHeader {
    private Attributes atts;
    private List<Variable> allVariables;
    private List<Variable> temporalClones;

    public DynamicModelHeader(Attributes atts) {
        this.atts = atts;

        this.allVariables = new ArrayList<>();
        for (Attribute att : atts.getSet()) {
            VariableBuilder builder = new VariableBuilder();

            VariableBuilder.setName(att.getName());
            if (att.getKind() == Kind.INTEGER ) {
                VariableBuilder.setNumberOfStates(2);
            }
            VariableBuilder.setStateSpaceKind(Kind.INTEGER);

            VariableImplementation var = new VariableImplementation(builder);

            allVariables.add(var.getVarID(), var);

            VariableImplementation temporalClone = var;
            temporalClones.add(var.getVarID(),temporalClone);
        }
    }

    public Variable getTemporalClone(Variable var){
        return temporalClones.get(var.getVarID());
    }

    public Variable getTemporalVar(Variable var){
        return allVariables.get(var.getVarID());
    }

    public Attributes getAttributes() {
        return atts;
    }

    public Variable addHiddenVariable(VariableBuilder builder) {

        VariableImplementation var = new VariableImplementation(builder);
        var.setVarID(allVariables.size());
        allVariables.add(var);

        VariableImplementation temporalClone = var;
        temporalClones.add(var.getVarID(),temporalClone);

        return var;
    }

    public List<Variable> getVariables() {
        return this.allVariables;
    }

    public Variable getVariableById(int varID) {
        return this.allVariables.get(varID);
    }

    public Variable getVariableByTimeId(int varTimeID){
        return this.allVariables.get(varTimeID%this.allVariables.size());
    }

    public int getNumberOfVars() {
        return this.allVariables.size();
    }

    private class VariableImplementation implements Variable {
        private String name;
        private int varID;
        private boolean observable;
        private int numberOfStates;
        private Kind stateSpaceKind;
        private DistType distributionType;

        public VariableImplementation(VariableBuilder builder) {
            this.name = builder.getName();
            this.observable = builder.isObservable();

        }

        public String getName() {
            return this.name;
        }

        public int getVarID() {
            return varID;
        }

        private void setVarID(int id) {
            this.varID = id;
        }

        public void setObservable(boolean observable) {
            this.observable = observable;
        }

        public boolean isObservable() {
            return observable;
        }

        public int getNumberOfStates() {
            return numberOfStates;
        }

        public void setNumberOfStates(int numberOfStates) {
            this.numberOfStates = numberOfStates;
        }

        public Kind getStateSpaceKind() {
            return stateSpaceKind;
        }

        public void setStateSpaceKind(Kind stateSpaceKind) {
            this.stateSpaceKind = stateSpaceKind;
        }

        public DistType getDistributionType() {
            return distributionType;
        }

        public void setDistributionType(DistType distributionType) {
            this.distributionType = distributionType;
        }

    }
}
