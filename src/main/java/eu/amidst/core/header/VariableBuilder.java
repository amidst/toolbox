package eu.amidst.core.header;

import eu.amidst.core.database.statics.readers.Attribute;


/**
 * Created by andresmasegosa on 04/11/14.
 */
public final class VariableBuilder {
    private static String name;
    private static boolean observable;
    private static int numberOfStates;
    private static StateSpaceType stateSpaceType;
    private static DistType distributionType;
    private static Attribute attribute;

    public VariableBuilder(Attribute att){
        this.name = att.getName();
        this.observable = true;
        this.numberOfStates = att.getNumberOfStates();
        this.stateSpaceType = att.getStateSpaceType();
        switch (att.getStateSpaceType()) {
            case REAL:
                this.distributionType = DistType.GAUSSIAN;
                break;
            case MULTINOMIAL:
                this.distributionType = DistType.MULTINOMIAL;
                break;
            default:
                throw new IllegalArgumentException(" The string \"" + att.getStateSpaceType() + "\" does not map to any Type.");
        }
        this.attribute = att;
    }

    public VariableBuilder(Attribute att, DistType typeDist){
        this.name = att.getName();
        this.observable = true;
        this.numberOfStates = att.getNumberOfStates();
        this.stateSpaceType = att.getStateSpaceType();
        this.distributionType = typeDist;
        this.attribute = att;
    }

    public static String getName() {
        return name;
    }

    public static boolean isObservable() {
        return observable;
    }

    public static int getNumberOfStates() {
        return numberOfStates;
    }

    public static StateSpaceType getStateSpaceType() {
        return stateSpaceType;
    }

    public static DistType getDistributionType() {
        return distributionType;
    }

    public static Attribute getAttribute() { return attribute; }

}
