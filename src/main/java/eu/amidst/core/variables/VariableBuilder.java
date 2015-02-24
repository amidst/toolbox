package eu.amidst.core.variables;

import eu.amidst.core.datastream.Attribute;


/**
 * Created by andresmasegosa on 04/11/14.
 */
public final class VariableBuilder {
    private static String name;
    private static boolean observable;
    private static StateSpace stateSpace;
    private static DistType distributionType;
    private static Attribute attribute;

    public VariableBuilder() {
    }

    public VariableBuilder(Attribute att){
        this.name = att.getName();
        this.observable = true;
        this.stateSpace = att.getStateSpace();
        switch (att.getStateSpace().getStateSpaceType()) {
            case REAL:
                this.distributionType = DistType.GAUSSIAN;
                break;
            case FINITE_SET:
                this.distributionType = DistType.MULTINOMIAL;
                break;
            default:
                throw new IllegalArgumentException(" The string \"" + att.getStateSpace() + "\" does not map to any Type.");
        }
        this.attribute = att;
    }

    public VariableBuilder(Attribute att, DistType typeDist){
        this.name = att.getName();
        this.observable = true;
        this.stateSpace = att.getStateSpace();
        this.distributionType = typeDist;
        this.attribute = att;
    }

    public static String getName() {
        return name;
    }

    public static boolean isObservable() {
        return observable;
    }

    public static StateSpace getStateSpace() {
        return stateSpace;
    }

    public static DistType getDistributionType() {
        return distributionType;
    }

    public static Attribute getAttribute() { return attribute; }

    public static void setName(String name) {
        VariableBuilder.name = name;
    }

    public static void setObservable(boolean observable) {
        VariableBuilder.observable = observable;
    }

    public static void setStateSpace(StateSpace stateSpace) {
        VariableBuilder.stateSpace = stateSpace;
    }

    public static void setDistributionType(DistType distributionType) {
        VariableBuilder.distributionType = distributionType;
    }

    public static void setAttribute(Attribute attribute) {
        VariableBuilder.attribute = attribute;
    }
}
