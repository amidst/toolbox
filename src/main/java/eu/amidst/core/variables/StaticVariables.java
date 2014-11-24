/**
 ******************* ISSUE LIST **************************
 *
 * 1. Remove method getVariableByVarID()!!
 *
 * ********************************************************
 */

package eu.amidst.core.variables;

import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.Attributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by afa on 02/07/14.
 */
public class StaticVariables implements Iterable<Variable>{

    private List<Variable> allVariables;

    private HashMap<String, Integer> mapping;

    public StaticVariables() {
        this.allVariables = new ArrayList<>();
        this.mapping = new HashMap<>();
    }

    /**
     * Constructor where the distribution type of random variables is initialized by default.
     *
     */
    public StaticVariables(Attributes atts) {

        this.allVariables = new ArrayList<>();
        this.mapping = new HashMap<>();

        for (Attribute att : atts.getListExceptTimeAndSeq()) {
            VariableBuilder builder = new VariableBuilder(att);
            VariableImplementation var = new VariableImplementation(builder, allVariables.size());
            if (mapping.containsKey(var.getName()))
                throw new IllegalArgumentException("Attribute list contains duplicated names");
            this.mapping.put(var.getName(), var.getVarID());
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
            if (mapping.containsKey(var.getName()))
                throw new IllegalArgumentException("Attribute list contains duplicated names");
            this.mapping.put(var.getName(), var.getVarID());
            allVariables.add(var.getVarID(), var);

        }
    }
    public Variable addIndicatorVariable(Variable var) {
        if (!var.isObservable())
            throw new IllegalArgumentException("An indicator variable should be created from an observed variable");

        VariableBuilder builder = new VariableBuilder(var.getAttribute());
        builder.setName(var.getName()+"_Indicator");
        builder.setDistributionType(DistType.INDICATOR);

        VariableImplementation varNew = new VariableImplementation(builder, allVariables.size());
        if (mapping.containsKey(varNew.getName()))
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        this.mapping.put(varNew.getName(), varNew.getVarID());
        allVariables.add(varNew);
        return varNew;
    }

    public Variable addObservedVariable(Attribute att) {

        VariableImplementation var = new VariableImplementation(new VariableBuilder(att), allVariables.size());
        if (mapping.containsKey(var.getName()))
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        this.mapping.put(var.getName(), var.getVarID());
        allVariables.add(var);
        return var;

    }

    public Variable addObservedVariable(Attribute att, DistType distType) {
        VariableBuilder builder = new VariableBuilder(att);
        builder.setDistributionType(distType);
        VariableImplementation var = new VariableImplementation(builder, allVariables.size());
        if (mapping.containsKey(var.getName()))
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        this.mapping.put(var.getName(), var.getVarID());git
        allVariables.add(var);
        return var;

    }

    public Variable addHiddenVariable(VariableBuilder builder) {

        VariableImplementation var = new VariableImplementation(builder, allVariables.size());
        if (mapping.containsKey(var.getName()))
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        this.mapping.put(var.getName(), var.getVarID());
        allVariables.add(var);
        return var;

    }

    public List<Variable> getListOfVariables() {
        return this.allVariables;
    }

    public Variable getVariableById(int varID) {
        return this.allVariables.get(varID);
    }

    public Variable getVariableByName(String name) {
        Integer index = this.mapping.get(name);
        if (index==null)
            throw new UnsupportedOperationException("Variable "+name+" is not part of the list of Variables (try uppercase)");
        else
            return this.getVariableById(index.intValue());
    }

    public int getNumberOfVars() {
        return this.allVariables.size();
    }

    @Override
    public Iterator<Variable> iterator() {
        return this.allVariables.iterator();
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

        public boolean isDynamicVariable(){
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Variable var = (Variable) o;

            return this.getName().equals(var.getName());
        }

    }
}
