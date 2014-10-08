package eu.amidst.core.header.dynamics;

import eu.amidst.core.header.statics.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by afa on 02/07/14.
 */
public class DynamicModelHeader {
    private DynamicDataHeader dataHeader;
    private List<DynamicVariable> allVariables;
    private int markovOrder = 1;

    public DynamicModelHeader(DynamicDataHeader dataHeader, int markovOrder) {
        this.dataHeader = dataHeader;
        this.allVariables = new ArrayList<>();
        for (DynamicVariable var : dataHeader.getObservedVariables()) {
            allVariables.add(var.getVarID(), var);
        }

        this.markovOrder=markovOrder;
    }

    public int getMarkovOrder(){ return this.markovOrder; }

    public DynamicDataHeader getDynamicDataHeader() {
        return dataHeader;
    }

    public Variable addHiddenVariable(String name, int numberOfStates) {
        DynamicVariableImplementation var = new DynamicVariableImplementation(name);
        var.setNumberOfStates(numberOfStates);
        var.setObservable(false);
        var.setVarID(allVariables.size());
        allVariables.add(var);
        return var;
    }

    public List<DynamicVariable> getVariables() {
        return this.allVariables;
    }

    public DynamicVariable getVariableById(int varID) {
        return this.allVariables.get(varID);
    }

    public DynamicVariable getVariableByTimeId(int varTimeID){
        return this.allVariables.get(varTimeID%this.allVariables.size());
    }

    public int getNumberOfVars() {
        return this.allVariables.size();
    }

    private class DynamicVariableImplementation implements DynamicVariable {
        private String name;
        private int varID;
        private boolean observable;
        private int numberOfStates;
        private boolean isLeave = false;
        private boolean isTemporalConnected = true;



        public DynamicVariableImplementation(String name) {
            this.name = new String(name);
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

        @Override
        public int getTimeVarID(int previousTime) {
            return DynamicModelHeader.this.getNumberOfVars()*(-previousTime) + this.getVarID();
        }

        @Override
        public boolean isTemporalConnected() {
            return isTemporalConnected;
        }

        @Override
        public void setTemporalConnected(boolean isTemporalConnected) {
            this.isTemporalConnected=isTemporalConnected;
        }

        public boolean isContinuous(){
            return this.numberOfStates==0;
        }
    }
}
