package eu.amidst.core.headers;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by afa on 02/07/14.
 */
public class StaticModelHeader {
    private StaticDataHeader dataHeader;
    private ArrayList<Variable> allVariables;
    private int classID;

    public StaticModelHeader(StaticDataHeader dataHeader){
        this.dataHeader=dataHeader;
        this.allVariables=new ArrayList<>();
        for (Variable var: dataHeader.getObservedVariables()){
            allVariables.add(var.getVarID(),var);
        }
    }

    public StaticDataHeader getStaticDataHeader(){
        return dataHeader;
    }

    public Variable addHiddenVariable(String name, int numberOfStates){
        VariableImplementation var = new VariableImplementation(name);
        var.setNumberOfStates(numberOfStates);
        var.setObservable(false);
        var.setVarID(allVariables.size());
        allVariables.add(var);
        return var;
    }

    public List<Variable> getVariables() {
        return null;
    }

    public Variable getVariable(int varID) {
        return null;
    }

    public int getNumberOfVars(){ return 0;}

    private class VariableImplementation implements Variable {
        private String name;
        private int varID;
        private boolean observable;
        private int numberOfStates;

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
    }
}
