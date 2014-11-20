/**
 ******************* ISSUE LIST **************************
 *
 * 1. The number of states should be parsed and stored.
 *
 *
 * ********************************************************
 */


package eu.amidst.core.database;

import eu.amidst.core.variables.StateSpaceType;

/**
 * Created by sigveh on 10/20/14.
 */

public final class Attribute {

    private final int index;
    private final String unit;
    private final String name;
    private final StateSpaceType stateSpaceType;
    private final int numberOfStates;

    public Attribute(int index, String name, String unit, StateSpaceType stateSpaceType, int numberOfStates) {
        this.index = index;
        this.name = name.toUpperCase();
        this.unit = unit;
        this.stateSpaceType = stateSpaceType;
        this.numberOfStates = numberOfStates;
    }

    public Attribute(int index, String name, StateSpaceType stateSpaceType, int numberOfStates) {
        this.index = index;
        this.name = name.toUpperCase();
        this.unit = "NA";
        this.stateSpaceType = stateSpaceType;
        this.numberOfStates = numberOfStates;
    }


    public Attribute(String name, StateSpaceType stateSpaceType, int numberOfStates) {
        this.index = -1;
        this.name = name.toUpperCase();
        this.unit = "NA";
        this.stateSpaceType = stateSpaceType;
        this.numberOfStates = numberOfStates;
    }


    public int getIndex() {
        return index;
    }

    public String getUnit() {
        return unit;
    }

    public String getName() {
        return name;
    }

    public StateSpaceType getStateSpaceType() {
        return stateSpaceType;
    }

    public int getNumberOfStates(){ return numberOfStates;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        if (stateSpaceType != attribute.stateSpaceType) return false;
        if (!name.equals(attribute.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + stateSpaceType.hashCode();
        return result;
    }
}
