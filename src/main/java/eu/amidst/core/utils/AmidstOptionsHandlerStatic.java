package eu.amidst.core.utils;

/**
 * Created by ana@cs.aau.dk on 17/02/15.
 *
 * This class is just meant to serve as temporary example to fill in options in STATIC CLASSES, that should
 * "overwrite" the methods below, it will be removed in the future.
 */
public final class AmidstOptionsHandlerStatic {


    /*
    public static String listOptions(){
        return  classNameID() +", "+
                "-numberOfVars, 10, Total number of variables\\" +
                "-numberOfLinks, 3, Number of links\\" +
                "-numberOfDiscreteVars, 10, Number of discrete variables\\"+
                "-numberOfContinuousVars, 0, Number of continuous variables.\\" +
                "-numberOfStates, 2, Number of states per discrete variable\\" +
                "-seed, 0, seed for random number generator\\";
    }

    public static void loadOptions(){
        numberOfVars = getIntOption("-numberOfVars");
        numberOfLinks = getIntOption("-numberOfLinks");
        numberOfDiscreteVars = getIntOption("-numberOfDiscreteVars");
        numberOfContinuousVars = getIntOption("-numberOfContinuousVars");
        numberOfStates = getIntOption("-numberOfStates");
        seed = getIntOption("-seed");
    }

    public static String classNameID(){
        return "eu.amidst.core.utils.BayesianNetworkGenerator";
    }

    public static void setOptions(String[] args) {
        OptionParser.setArgsOptions(classNameID(),args);
        loadOptions();
    }

    public static void loadOptionsFromFile(String fileName){
        OptionParser.setConfFileName(fileName);
        OptionParser.loadFileOptions();
        OptionParser.loadDefaultOptions(classNameID());
        loadOptions();
    }

    public static String getOption(String optionName) {
        return OptionParser.parse(classNameID(), listOptions(), optionName);
    }

    public static int getIntOption(String optionName){
        return Integer.parseInt(getOption(optionName));
    }

    public default double getDoubleOption(String optionName){
        return Double.parseDouble(this.getOption(optionName));
    }

    public default boolean getBooleanOption(String optionName){
        return this.getOption(optionName).equalsIgnoreCase("true") || this.getOption(optionName).equalsIgnoreCase("T");
    }

    */

}