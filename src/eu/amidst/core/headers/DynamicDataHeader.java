package eu.amidst.core.headers;

import java.util.*;

/**
 * Created by afa on 02/07/14.
 */
public class DynamicDataHeader {
    private ArrayList<DynamicVariable> vars;

    public ArrayList<DynamicVariable> getObservedVariables() {
        return vars;
    }

    public void setObservedVariables(ArrayList<DynamicVariable> vars) {
        this.vars = vars;
    }

    public int getNumberOfObservableVariables() {
        return vars.size();
    }

    public Variable addObservedVariable(int dataPositionAtTimeT, String name, int numberOfStates) {
        DynamicVariableImplementation var = new DynamicVariableImplementation(name);
        var.setNumberOfStates(numberOfStates);
        var.setObservable(true);
        var.setVarID(dataPositionAtTimeT);
        vars.add(dataPositionAtTimeT, var);
        return var;
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
            return -1;
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