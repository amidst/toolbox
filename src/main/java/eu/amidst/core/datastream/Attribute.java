/**
 ******************* ISSUE LIST **************************
 *
 * 1. The number of states should be parsed and stored.
 *
 *
 * ********************************************************
 */


package eu.amidst.core.datastream;

import eu.amidst.core.variables.FiniteStateSpace;
import eu.amidst.core.variables.RealStateSpace;
import eu.amidst.core.variables.StateSpace;
import eu.amidst.core.variables.StateSpaceType;

import java.io.Serializable;

/**
 * Created by sigveh on 10/20/14.
 */

public final class Attribute implements Serializable {

    private static final long serialVersionUID = -2932037991574118651L;

    private final int index;
    private final String name;
    private final StateSpace stateSpace;

    public Attribute(int index, String name, String unit, StateSpaceType stateSpaceType1, int numberOfStates) {

        this.index = index;
        this.name = name;
        if (stateSpaceType1==StateSpaceType.FINITE_SET) {
            this.stateSpace = new FiniteStateSpace(numberOfStates);
            this.stateSpace.setUnit(unit);
        }else if (stateSpaceType1== StateSpaceType.REAL) {
            this.stateSpace = new RealStateSpace();
            this.stateSpace.setUnit(unit);
        }else {
            throw new IllegalArgumentException("State Space not defined");
        }
    }

    public Attribute(int index, String name, StateSpaceType stateSpaceType1, int numberOfStates) {

        this.index = index;
        this.name = name;
        if (stateSpaceType1==StateSpaceType.FINITE_SET) {
            this.stateSpace = new FiniteStateSpace(numberOfStates);
        }else if (stateSpaceType1== StateSpaceType.REAL) {
            this.stateSpace = new RealStateSpace();
        }else {
            throw new IllegalArgumentException("State Space not defined");
        }
    }

    public Attribute(int index, String name, StateSpace stateSpace1) {
        this.index = index;
        this.name = name;
        this.stateSpace = stateSpace1;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public <E extends StateSpace> E getStateSpace() {
        return (E)stateSpace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }

        if (o == null || getClass() != o.getClass()){
            return false;
        }

        Attribute attribute = (Attribute) o;

        if (stateSpace.getStateSpaceType() != attribute.stateSpace.getStateSpaceType()){return false;}
        if (!name.equals(attribute.name)) {return false;}

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + stateSpace.hashCode();
        return result;
    }
}
