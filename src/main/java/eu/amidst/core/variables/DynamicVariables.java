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

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by afa on 02/07/14.
 */
public class DynamicVariables  implements Iterable<Variable>, Serializable {

    private static final long serialVersionUID = -4959625141445606681L;

    private List<Variable> allVariables;
    private List<Variable> temporalClones;

    private Map<String, Integer> mapping;

    public DynamicVariables() {
        this.allVariables = new ArrayList();
        this.temporalClones = new ArrayList();
        this.mapping = new ConcurrentHashMap<>();
    }

    public DynamicVariables(Attributes atts) {

        this.allVariables = new ArrayList<>();
        this.temporalClones = new ArrayList<>();
        this.mapping = new ConcurrentHashMap<>();

        for (Attribute att : atts.getListExceptTimeAndSeq()) {
            VariableBuilder builder = new VariableBuilder(att);
            VariableImplementation var = new VariableImplementation(builder, allVariables.size());
            if (mapping.containsKey(var.getName())) {
                throw new IllegalArgumentException("Attribute list contains duplicated names");
            }
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
    public DynamicVariables(Attributes atts, Map<Attribute, DistributionTypeEnum> typeDists) {

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
            if (mapping.containsKey(var.getName())) {
                throw new IllegalArgumentException("Attribute list contains duplicated names");
            }
            this.mapping.put(var.getName(), var.getVarID());
            allVariables.add(var.getVarID(), var);

            VariableImplementation temporalClone = new VariableImplementation(var);
            temporalClones.add(var.getVarID(), temporalClone);

        }
    }

    public Variable createTemporalClone(Variable var){
        VariableImplementation temporalClone = new VariableImplementation(var);
        temporalClones.add(var.getVarID(), temporalClone);
        return temporalClone;
    }

    public Variable getTemporalClone(Variable var){
        return temporalClones.get(var.getVarID());
    }

    public Variable getVariableFromTemporalClone(Variable var){
        return allVariables.get(var.getVarID());
    }

    /*
    public Variable addIndicatorDynamicVariable(Variable var) {
        if (!var.isObservable()) {
            throw new IllegalArgumentException("An indicator variable should be created from an observed variable");
        }

        if (var.getStateSpace().getStateSpaceType()!=StateSpaceType.REAL) {
            throw new IllegalArgumentException("An indicator variable should be created from an real variable");
        }

        VariableBuilder builder = new VariableBuilder(var.getAttribute());
        builder.setName(var.getName()+"_Indicator");
        builder.setDistributionType(DistType.INDICATOR);

        VariableImplementation varNew = new VariableImplementation(builder, allVariables.size());
        if (mapping.containsKey(varNew.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(varNew.getName(), varNew.getVarID());
        allVariables.add(varNew);

        VariableImplementation temporalClone = new VariableImplementation(varNew);
        temporalClones.add(varNew.getVarID(),temporalClone);

        return varNew;

    }
*/

    public Variable newMultinomialLogisticDynamicVariable(Attribute att) {
        return this.newDynamicVariable(att, DistributionTypeEnum.MULTINOMIAL_LOGISTIC);
    }

    public Variable newMultinomialLogisticDynamicVariable(String name, int nOfStates) {
        return this.newDynamicVariable(name, DistributionTypeEnum.MULTINOMIAL_LOGISTIC, new FiniteStateSpace(nOfStates));
    }

    public Variable newMultinomialLogisticDynamicVariable(String name, List<String> states) {
        return this.newDynamicVariable(name, DistributionTypeEnum.MULTINOMIAL_LOGISTIC, new FiniteStateSpace(states));
    }

    public Variable newMultionomialDynamicVariable(Attribute att) {
        return this.newDynamicVariable(att, DistributionTypeEnum.MULTINOMIAL);
    }

    public Variable newMultinomialDynamicVariable(String name, int nOfStates) {
        return this.newDynamicVariable(name, DistributionTypeEnum.MULTINOMIAL, new FiniteStateSpace(nOfStates));
    }

    public Variable newMultinomialDynamicVariable(String name, List<String> states) {
        return this.newDynamicVariable(name, DistributionTypeEnum.MULTINOMIAL, new FiniteStateSpace(states));
    }

    public Variable newGaussianDynamicVariable(Attribute att) {
        return this.newDynamicVariable(att, DistributionTypeEnum.NORMAL);
    }

    public Variable newGaussianDynamicVariable(String name) {
        return this.newDynamicVariable(name, DistributionTypeEnum.NORMAL, new RealStateSpace());
    }


    public Variable newDynamicVariable(Attribute att) {

        VariableImplementation var = new VariableImplementation(new VariableBuilder(att), allVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(var.getName(), var.getVarID());
        allVariables.add(var);

        VariableImplementation temporalClone = new VariableImplementation(var);
        temporalClones.add(var.getVarID(),temporalClone);

        return var;
    }

    public Variable newDynamicVariable(String name, DistributionTypeEnum distributionTypeEnum, StateSpaceType stateSpaceType) {
        VariableBuilder variableBuilder = new VariableBuilder();
        variableBuilder.setName(name);
        variableBuilder.setDistributionType(distributionTypeEnum);
        variableBuilder.setStateSpaceType(stateSpaceType);
        variableBuilder.setObservable(false);
        VariableImplementation var = new VariableImplementation(variableBuilder, allVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(var.getName(), var.getVarID());
        allVariables.add(var);

        VariableImplementation temporalClone = new VariableImplementation(var);
        temporalClones.add(var.getVarID(),temporalClone);

        return var;
    }

    public Variable newDynamicVariable(Attribute att, DistributionTypeEnum distributionTypeEnum) {
        VariableBuilder variableBuilder = new VariableBuilder(att);
        variableBuilder.setDistributionType(distributionTypeEnum);
        VariableImplementation var = new VariableImplementation(variableBuilder, allVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(var.getName(), var.getVarID());
        allVariables.add(var);

        VariableImplementation temporalClone = new VariableImplementation(var);
        temporalClones.add(var.getVarID(),temporalClone);

        return var;
    }

    public Variable newDynamicVariable(VariableBuilder builder) {

        VariableImplementation var = new VariableImplementation(builder, allVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(var.getName(), var.getVarID());
        allVariables.add(var);

        VariableImplementation temporalClone = new VariableImplementation(var);
        temporalClones.add(var.getVarID(),temporalClone);

        return var;
    }

    public Variable newRealDynamicVariable(Variable var){
        if (!var.isObservable()) {
            throw new IllegalArgumentException("A Real variable should be created from an observed variable");
        }

        if (var.getStateSpaceType().getStateSpaceTypeEnum()!= StateSpaceTypeEnum.REAL) {
            throw new IllegalArgumentException("An Real variable should be created from a real variable");
        }

        VariableBuilder builder = new VariableBuilder(var.getAttribute());
        builder.setName(var.getName()+"_Real");

        VariableImplementation varNew = new VariableImplementation(builder, allVariables.size());
        if (mapping.containsKey(varNew.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
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

    public Variable getVariableById(int varID) {
       return this.allVariables.get(varID);
    }


    //public Variable getTemporalCloneById(int varID) {
    //    return this.temporalClones.get(varID);
    //}

    public Variable getVariableByName(String name) {
        Integer index = this.mapping.get(name);
        if (index==null) {
            throw new UnsupportedOperationException("Variable " + name + " is not part of the list of Variables");
        }
        else {
            return this.getVariableById(index.intValue());
        }
    }

    public Variable getTemporalCloneByName(String name) {
        return this.getTemporalClone(this.getVariableByName(name));
    }

    public int getNumberOfVars() {
        return this.allVariables.size();
    }


    public Variable getVariable(String name) {
        for(Variable var: this){
            if(var.getName().equals(name)) {
                return var;
            }
        }
        throw new UnsupportedOperationException("Variable "+name+" is not part of the list of Variables");
    }

    public void block(){
        this.allVariables = Collections.unmodifiableList(this.allVariables);
    }

    @Override
    public Iterator<Variable> iterator() {
        return this.allVariables.iterator();
    }


    //TODO Implements hashCode method!!
    private static class VariableImplementation implements Variable, Serializable {

        private static final long serialVersionUID = 7934186475276412196L;

        private String name;
        private int varID;
        private boolean observable;
        private StateSpaceType stateSpaceType;
        private DistributionTypeEnum distributionTypeEnum;
        private DistributionType distributionType;
        private Attribute attribute;
        private final boolean isTemporalClone;
        private int numberOfStates = -1;

        /*
         * Constructor for a Variable (not a temporal clone)
         */
        public VariableImplementation(VariableBuilder builder, int varID) {
            this.name = builder.getName();
            this.varID = varID;
            this.observable = builder.isObservable();
            this.stateSpaceType = builder.getStateSpaceType();
            this.distributionTypeEnum = builder.getDistributionType();
            this.attribute = builder.getAttribute();
            this.isTemporalClone = false;

            if (this.getStateSpaceType().getStateSpaceTypeEnum()== StateSpaceTypeEnum.FINITE_SET) {
                this.numberOfStates = ((FiniteStateSpace) this.stateSpaceType).getNumberOfStates();
            }

            this.distributionType=distributionTypeEnum.newDistributionType(this);
        }

        /*
         * Constructor for a Temporal clone (based on a variable)
         */
        public VariableImplementation(Variable variable) {
            this.name = variable.getName()+"_TClone";
            this.varID = variable.getVarID();
            this.observable = variable.isObservable();
            this.stateSpaceType = variable.getStateSpaceType();
            this.distributionTypeEnum = variable.getDistributionTypeEnum();
            this.attribute = variable.getAttribute();
            this.isTemporalClone = true;

            if (this.getStateSpaceType().getStateSpaceTypeEnum()== StateSpaceTypeEnum.FINITE_SET) {
                this.numberOfStates = ((FiniteStateSpace) this.stateSpaceType).getNumberOfStates();
            }

            this.distributionType=distributionTypeEnum.newDistributionType(this);

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

        @Override
        public <E extends StateSpaceType> E getStateSpaceType() {
            return (E) stateSpaceType;
        }

        @Override
        public int getNumberOfStates() {
            return this.numberOfStates;
        }

        public DistributionTypeEnum getDistributionTypeEnum() {
            return distributionTypeEnum;
        }

        @Override
        public <E extends DistributionType> E getDistributionType() {
            return (E)this.distributionType;
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
            if (this == o){
                return true;
            }
            if (o == null || getClass() != o.getClass()){
                return false;
            }

            Variable var = (Variable) o;

            return this.isTemporalClone()==var.isTemporalClone() && this.getVarID()==var.getVarID();
        }


        @Override
        public int hashCode(){
            return this.name.hashCode();
        }


        @Override
        public boolean isParameterVariable() {
            return false;
        }

    }
}
