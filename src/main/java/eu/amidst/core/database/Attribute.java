/**
 ******************* ISSUE LIST **************************
 *
 * 1. The number of states should be parsed and stored.
 *
 *
 * ********************************************************
 */


package eu.amidst.core.database;

import eu.amidst.core.variables.MultinomialStateSpace;
import eu.amidst.core.variables.RealStateSpace;
import eu.amidst.core.variables.StateSpace;
import eu.amidst.core.variables.StateSpaceType;

/**
 * Created by sigveh on 10/20/14.
 */

public final class Attribute {

    private final int index;
    private final String name;
    private final StateSpace stateSpace;

    public Attribute(int index, String name, String unit, StateSpaceType stateSpaceType_, int numberOfStates) {

        this.index = index;
        this.name = name.toUpperCase();
        if (stateSpaceType_==StateSpaceType.FINITE_SET) {
            this.stateSpace = new MultinomialStateSpace(numberOfStates);
            this.stateSpace.setUnit(unit);
        }else if (stateSpaceType_== StateSpaceType.REAL) {
            this.stateSpace = new RealStateSpace();
            this.stateSpace.setUnit(unit);
        }else {
            throw new IllegalArgumentException("State Space not defined");
        }
    }

    public Attribute(int index, String name, StateSpaceType stateSpaceType_, int numberOfStates) {

        this.index = index;
        this.name = name.toUpperCase();
        if (stateSpaceType_==StateSpaceType.FINITE_SET) {
            this.stateSpace = new MultinomialStateSpace(numberOfStates);
        }else if (stateSpaceType_== StateSpaceType.REAL) {
            this.stateSpace = new RealStateSpace();
        }else {
            throw new IllegalArgumentException("State Space not defined");
        }
    }

    public Attribute(int index, String name, StateSpace stateSpace_) {
        this.index = index;
        this.name = name.toUpperCase();
        this.stateSpace = stateSpace_;
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        if (stateSpace.getStateSpaceType() != attribute.stateSpace.getStateSpaceType()) return false;
        if (!name.equals(attribute.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + stateSpace.hashCode();
        return result;
    }
}
