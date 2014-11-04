package eu.amidst.core.header.statics;

import eu.amidst.core.database.statics.readers.Attributes;
import eu.amidst.core.database.statics.readers.Kind;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by afa on 02/07/14.
 */
public class StaticModelHeader {
    private Attributes atts;
    private List<Variable> allVariables;

    public StaticModelHeader(Attributes atts) {
        this.atts = atts;
        this.allVariables = new ArrayList<>();

        //Create a Variable object for each att in atts. 


        for (Variable var : dataHeader.getObservedVariables()) {
            allVariables.add(var.getVarID(), var);
        }
    }

    public StaticDataHeader getStaticDataHeader() {
        return dataHeader;
    }

    public Variable addHiddenVariable(VariableBuilder builder) {
        VariableImplementation var = new VariableImplementation(builder);
        var.setVarID(allVariables.size());
        allVariables.add(var);
        return var;
    }

    public List<Variable> getVariables() {
        return this.allVariables;
    }

    public Variable getVariable(int varID) {
        return this.allVariables.get(varID);
    }

    public int getNumberOfVars() {
        return this.allVariables.size();
    }

    private class VariableImplementation implements Variable {
        private String name;
        private int varID;
        private boolean observable;
        private int numberOfStates;
        private boolean isLeave = false;
        private Kind stateSpaceKind;


        public VariableImplementation(VariableBuilder builder) {

            this.name = builder.getName();
            this.varID = builder.getVarID();
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

        public void setObservable(boolean observable) { this.observable=observable; }

        public boolean isObservable() {
            return false;
        }

        public int getNumberOfStates() {
            return numberOfStates;
        }

        public void setNumberOfStates(int numberOfStates) {
            this.numberOfStates = numberOfStates;
        }

        public boolean isLeave() {
            return this.isLeave;
        }

        public void setLeave(boolean isLeave) {
            this.isLeave = isLeave;
        }

        public boolean isContinuous(){
            return this.numberOfStates==0;
        }
    }
}
