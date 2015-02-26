/**
 ******************* ISSUE LIST **************************
 *
 * 1. The number of states should be parsed and stored.
 *
 *
 * ********************************************************
 */


package eu.amidst.core.datastream;

import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;
import eu.amidst.core.variables.StateSpaceType;
import eu.amidst.core.variables.StateSpaceTypeEnum;

import java.io.Serializable;

/**
 * Created by sigveh on 10/20/14.
 */

public final class Attribute implements Serializable {

    private static final long serialVersionUID = -2932037991574118651L;

    private final int index;
    private final String name;
    private final StateSpaceType stateSpaceType;

    public Attribute(int index, String name, String unit, StateSpaceTypeEnum stateSpaceTypeEnum1, int numberOfStates) {

        this.index = index;
        this.name = name;
        if (stateSpaceTypeEnum1 == StateSpaceTypeEnum.FINITE_SET) {
            this.stateSpaceType = new FiniteStateSpace(numberOfStates);
            this.stateSpaceType.setUnit(unit);
        }else if (stateSpaceTypeEnum1 == StateSpaceTypeEnum.REAL) {
            this.stateSpaceType = new RealStateSpace();
            this.stateSpaceType.setUnit(unit);
        }else {
            throw new IllegalArgumentException("State Space not defined");
        }
    }

    public Attribute(int index, String name, StateSpaceTypeEnum stateSpaceTypeEnum1, int numberOfStates) {

        this.index = index;
        this.name = name;
        if (stateSpaceTypeEnum1 == StateSpaceTypeEnum.FINITE_SET) {
            this.stateSpaceType = new FiniteStateSpace(numberOfStates);
        }else if (stateSpaceTypeEnum1 == StateSpaceTypeEnum.REAL) {
            this.stateSpaceType = new RealStateSpace();
        }else {
            throw new IllegalArgumentException("State Space not defined");
        }
    }

    public Attribute(int index, String name, StateSpaceType stateSpaceType1) {
        this.index = index;
        this.name = name;
        this.stateSpaceType = stateSpaceType1;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public <E extends StateSpaceType> E getStateSpaceType() {
        return (E) stateSpaceType;
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

        if (stateSpaceType.getStateSpaceTypeEnum() != attribute.stateSpaceType.getStateSpaceTypeEnum()){return false;}
        if (!name.equals(attribute.name)) {return false;}

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + stateSpaceType.hashCode();
        return result;
    }
}
