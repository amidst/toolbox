package eu.amidst.core.headers;

import eu.amidst.core.headers.Variable;

import java.util.*;

/**
 * Created by afa on 02/07/14.
 */
public class StaticDataHeader {
    private ArrayList<Variable> vars;

    public ArrayList<Variable> getObservedVariables() {
        return vars;
    }

    public void setObservedVariables(ArrayList<Variable> vars) {
        this.vars = vars;
    }

    public int getNumberOfObservableVariables(){
        return vars.size();
    }

    public Variable addObservedVariable(int dataPosition, String name, int numberOfStates){
        VariableImplementation var = new VariableImplementation(name);
        var.setNumberOfStates(numberOfStates);
        var.setObservable(true);
        var.setVarID(dataPosition);
        vars.add(var);
        return var;
    }

    private class VariableImplementation implements Variable {
        private String name;
        private int varID;
        private boolean observable;
        private int numberOfStates;
        private boolean isLeave=false;

        public VariableImplementation(String name) {
            this.name = new String(name);
        }

        public String getName(){
            return this.name;
        }
        public int getVarID() {
            return varID;
        }

        public void setVarID(int id) {
            this.varID = id;
        }

        public void setObservable(boolean observable) {
        }

        public boolean isObservable() {
            return false;
        }

        public int getNumberOfStates() {
            return numberOfStates;
        }

        public void setNumberOfStates(int numberOfStates) {
            this.numberOfStates = numberOfStates;
        }

        public boolean isLeave(){
            return this.isLeave;
        }

        public void setLeave(boolean isLeave){
            this.isLeave=isLeave;
        }
    }

}