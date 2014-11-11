package eu.amidst.core.header;

/**
 * Created by andresmasegosa on 04/11/14.
 */
public final class VariableBuilder {
    private static String name;
    private static boolean observable;
    private static int numberOfStates;
    private static StateSpaceType stateSpaceType;
    private static DistType distributionType;

    public static String getName() {
        return name;
    }

    public static void setName(String name) {
        VariableBuilder.name = name;
    }

    public static boolean isObservable() {
        return observable;
    }

    public static void setIsObservable() {
        VariableBuilder.observable = observable;
    }

    public static int getNumberOfStates() {
        return numberOfStates;
    }

    public static void setNumberOfStates(int numberOfStates) {
        VariableBuilder.numberOfStates = numberOfStates;
    }

    public static StateSpaceType getStateSpaceType() {
        return stateSpaceType;
    }

    public static void setStateSpaceType(StateSpaceType stateSpaceStateSpaceType) {
        VariableBuilder.stateSpaceType = stateSpaceStateSpaceType;
    }

    public static DistType getDistributionType() {
        return distributionType;
    }

    public static void setDistributionType(DistType distributionType) {
        VariableBuilder.distributionType = distributionType;
    }

}
