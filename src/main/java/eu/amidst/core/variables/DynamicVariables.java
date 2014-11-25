/**
 ******************* ISSUE LIST **************************
 *
 * 1. Rename to DynamicVariables
 * 2. We can/should remove all setters from VariableImplementation right?
 * 3. Is there any need for the field atts? It is only used in the constructor.
 * 4. If the fields in VariableImplementation are all objects then the TemporalClone only contains
 *    pointers, which would ensure consistency, although we are not planing to modify these values.
 *
 * 5. Remove method getVariableByVarID();
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
public class DynamicVariables  implements Iterable<Variable>{

    private List<Variable> allVariables;
    private List<Variable> temporalClones;

    private HashMap<String, Integer> mapping;

    public DynamicVariables() {
        this.allVariables = new ArrayList<>();
        this.temporalClones = new ArrayList<>();
        this.mapping = new HashMap<>();
    }

    public DynamicVariables(Attributes atts) {

        this.allVariables = new ArrayList<>();
        this.temporalClones = new ArrayList<>();
        this.mapping = new HashMap<>();

        for (Attribute att : atts.getListExceptTimeAndSeq()) {
            VariableBuilder builder = new VariableBuilder(att);
            VariableImplementation var = new VariableImplementation(builder, allVariables.size());
            if (mapping.containsKey(var.getName()))
                throw new IllegalArgumentException("Attribute list contains duplicated names");
            this.mapping.put(var.getName(), var.getVarID());
            allVariables.add(var.getVarID(), var);


            VariableImplementation temporalClone = new VariableImplementation(var);
            temporalClones.add(var.getVarID(), temporalClone);
        }
    }

    /**
     * Constructor where the distribution type of random variables is provided as an argument.
     *
     */
    public DynamicVariables(Attributes atts, HashMap<Attribute, DistType> typeDists) {

        this.allVariables = new ArrayList<>();
        this.temporalClones = new ArrayList<>();

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

            VariableImplementation temporalClone = new VariableImplementation(var);
            temporalClones.add(var.getVarID(), temporalClone);

        }
    }

    public Variable getTemporalClone(Variable var){
        return temporalClones.get(var.getVarID());
    }

    public Variable getVariableFromTemporalClone(Variable var){
        return allVariables.get(var.getVarID());
    }

    public Variable addIndicatorDynamicVariable(Variable var) {
        if (!var.isObservable())
            throw new IllegalArgumentException("An indicator variable should be created from an observed variable");

        if (var.getStateSpaceType()!=StateSpaceType.REAL)
            throw new IllegalArgumentException("An indicator variable should be created from an real variable");

        VariableBuilder builder = new VariableBuilder(var.getAttribute());
        builder.setName(var.getName()+"_Indicator");
        builder.setDistributionType(DistType.INDICATOR);

        VariableImplementation varNew = new VariableImplementation(builder, allVariables.size());
        if (mapping.containsKey(varNew.getName()))
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        this.mapping.put(varNew.getName(), varNew.getVarID());
        allVariables.add(varNew);

        VariableImplementation temporalClone = new VariableImplementation(varNew);
        temporalClones.add(varNew.getVarID(),temporalClone);

        return varNew;

    }

    public Variable addObservedDynamicVariable(Attribute att) {

        VariableImplementation var = new VariableImplementation(new VariableBuilder(att), allVariables.size());
        if (mapping.containsKey(var.getName()))
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        this.mapping.put(var.getName(), var.getVarID());
        allVariables.add(var);

        VariableImplementation temporalClone = new VariableImplementation(var);
        temporalClones.add(var.getVarID(),temporalClone);

        return var;
    }

    public Variable addObservedDynamicVariable(Attribute att, DistType distType) {
        VariableBuilder variableBuilder = new VariableBuilder(att);
        variableBuilder.setDistributionType(distType);
        VariableImplementation var = new VariableImplementation(variableBuilder, allVariables.size());
        if (mapping.containsKey(var.getName()))
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        this.mapping.put(var.getName(), var.getVarID());
        allVariables.add(var);

        VariableImplementation temporalClone = new VariableImplementation(var);
        temporalClones.add(var.getVarID(),temporalClone);

        return var;
    }
    public Variable addHiddenDynamicVariable(VariableBuilder builder) {

        VariableImplementation var = new VariableImplementation(builder, allVariables.size());
        if (mapping.containsKey(var.getName()))
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        this.mapping.put(var.getName(), var.getVarID());
        allVariables.add(var);

        VariableImplementation temporalClone = new VariableImplementation(var);
        temporalClones.add(var.getVarID(),temporalClone);

        return var;
    }

    public Variable addRealDynamicVariable(Variable var){
        if (!var.isObservable())
            throw new IllegalArgumentException("A Real variable should be created from an observed variable");

        if (var.getStateSpaceType()!=StateSpaceType.REAL)
            throw new IllegalArgumentException("An Real variable should be created from a real variable");

        VariableBuilder builder = new VariableBuilder(var.getAttribute());
        builder.setName(var.getName()+"_Real");

        VariableImplementation varNew = new VariableImplementation(builder, allVariables.size());
        if (mapping.containsKey(varNew.getName()))
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        this.mapping.put(varNew.getName(), varNew.getVarID());
        allVariables.add(varNew);

        VariableImplementation temporalClone = new VariableImplementation(varNew);
        temporalClones.add(varNew.getVarID(),temporalClone);

        return varNew;
    }

    public List<Variable> getListOfDynamicVariables() {
        return this.allVariables;
    }

    //public List<Variable> getListOfTemporalClones() {
    //    return this.temporalClones;
    //}

    private Variable getVariableById(int varID) {
       return this.allVariables.get(varID);
    }


    //public Variable getTemporalCloneById(int varID) {
    //    return this.temporalClones.get(varID);
    //}

    public Variable getVariableByName(String name) {
        Integer index = this.mapping.get(name);
        if (index==null)
            throw new UnsupportedOperationException("Variable "+name+" is not part of the list of Variables (try uppercase)");
        else
            return this.getVariableById(index.intValue());
    }

    public Variable getTemporalCloneByName(String name) {
        return this.getTemporalClone(this.getVariableByName(name));
    }

    public int getNumberOfVars() {
        return this.allVariables.size();
    }


    public Variable getVariable(String name) {
        for(Variable var: this){
            if(var.getName().equals(name))
                return var;
        }
        throw new UnsupportedOperationException("Variable "+name+" is not part of the list of Variables (try uppercase)");
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
        private final boolean isTemporalClone;

        /*
         * Constructor for a Variable (not a temporal clone)
         */
        public VariableImplementation(VariableBuilder builder, int varID) {
            this.name = builder.getName();
            this.varID = varID;
            this.observable = builder.isObservable();
            this.numberOfStates = builder.getNumberOfStates();
            this.stateSpaceType = builder.getStateSpaceType();
            this.distributionType = builder.getDistributionType();
            this.attribute = builder.getAttribute();
            this.isTemporalClone = false;
        }

        /*
         * Constructor for a Temporal clone (based on a variable)
         */
        public VariableImplementation(Variable variable) {
            this.name = variable.getName()+"_TClone";
            this.varID = variable.getVarID();
            this.observable = variable.isObservable();
            this.numberOfStates = variable.getNumberOfStates();
            this.stateSpaceType = variable.getStateSpaceType();
            this.distributionType = variable.getDistributionType();
            this.attribute = variable.getAttribute();
            this.isTemporalClone = true;
        }

        public String getName() {
            return this.name;
        }

        public int getVarID() {
            return varID;
        }

        public boolean isObservable() {
            return observable;
        }

        public int getNumberOfStates() {
            return numberOfStates;
        }

        @Override
        public StateSpaceType getStateSpaceType() {
            return stateSpaceType;
        }

        public DistType getDistributionType() {
            return distributionType;
        }

        public boolean isTemporalClone(){
            return isTemporalClone;
        }

        public Attribute getAttribute(){return attribute;}

        public boolean isDynamicVariable(){
            return true;
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
