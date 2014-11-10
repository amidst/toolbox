/**
 ******************* ISSUE LIST **************************
 *
 * 1. Rename to Variables.
 * 2. We can/should remove all setters from VariableImplementation right?
 * 3. Is there any need for the field atts? It is only used in the constructor.
 *
 * ********************************************************
 */


package eu.amidst.core.header.statics;

import eu.amidst.core.database.statics.readers.Attribute;
import eu.amidst.core.database.statics.readers.Attributes;
import eu.amidst.core.database.statics.readers.DistType;
import eu.amidst.core.database.statics.readers.StateSpaceType;
import eu.amidst.core.header.Variable;
import eu.amidst.core.header.VariableBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by afa on 02/07/14.
 */
public class StaticModelHeader {
    private Attributes atts;
    private List<Variable> allVariables;


    /**
     * Constructor where the distribution type of random variables is initialized by default.
     *
     */
    public StaticModelHeader(Attributes atts) {
        this.atts = atts;
        this.allVariables = new ArrayList<>();

        for (Attribute att : this.atts.getSet()) {
            VariableBuilder builder = new VariableBuilder();

            VariableBuilder.setName(att.getName());
            VariableBuilder.setIsObservable();

            VariableBuilder.setStateSpaceType(att.getStateSpaceType());
            switch (att.getStateSpaceType()) {
                case REAL:
                    VariableBuilder.setDistributionType(DistType.GAUSSIAN);
                    break;
                case INTEGER:
                    VariableBuilder.setDistributionType(DistType.GAUSSIAN);
                    break;
                default:
                    throw new IllegalArgumentException(" The string \"" + att.getStateSpaceType() + "\" does not map to any Type.");
            }

            VariableBuilder.setNumberOfStates(att.getNumberOfStates());

            VariableImplementation var = new VariableImplementation(builder, att.getIndex());
            allVariables.add(var.getVarID(), var);

        }
    }

    /**
    * Constructor where the distribution type of random variables is provided as an argument.
    *
    */
    public StaticModelHeader(Attributes atts, HashMap<Attribute, DistType> typeDists) {
        this.atts = atts;
        this.allVariables = new ArrayList<>();

        for (Attribute att : this.atts.getSet()) {
            VariableBuilder builder = new VariableBuilder();

            VariableBuilder.setName(att.getName());
            VariableBuilder.setIsObservable();

            VariableBuilder.setStateSpaceType(att.getStateSpaceType());
            switch (att.getStateSpaceType()) {
                case REAL:
                    VariableBuilder.setDistributionType(typeDists.get(att));
                    break;
                case INTEGER:
                    VariableBuilder.setDistributionType(typeDists.get(att));
                    break;
                default:
                    throw new IllegalArgumentException(" The string \"" + att.getStateSpaceType() + "\" does not map to any Type.");
            }
            VariableBuilder.setNumberOfStates(att.getNumberOfStates());

            VariableImplementation var = new VariableImplementation(builder, att.getIndex());
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

        public VariableImplementation(VariableBuilder builder, int varID) {
            this.name = builder.getName();
            this.varID = varID;
            this.observable = builder.isObservable();
            this.numberOfStates = builder.getNumberOfStates();
            this.stateSpaceType = builder.getStateSpaceType();
            this.distributionType = builder.getDistributionType();
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

    }
}
