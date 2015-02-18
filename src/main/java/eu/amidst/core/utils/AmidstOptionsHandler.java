package eu.amidst.core.utils;

import eu.amidst.core.inference.VMP;

import java.util.HashMap;

/**
 * Created by ana@cs.aau.dk on 17/02/15.
 */
public interface AmidstOptionsHandler {

    public String listOptions();

    public String listOptionsRecursively();

    public void loadOptions();

    public default String classNameID(){
        return this.getClass().getName();
    }

    public default String getOption(String optionName) {
        return OptionParser.parse(classNameID(), listOptions(), optionName);
    }

    public default void setOptions(String[] args) throws ClassNotFoundException{
        OptionParser.setArgsOptions(classNameID(),args);
        this.loadOptions();
    }

    public default void setConfFileName(String fileName){
        OptionParser.setConfFileName(fileName);
    }

    public default int getIntOption(String optionName){
        return Integer.parseInt(this.getOption(optionName));
    }

    public default double getDoubleOption(String optionName){
        return Double.parseDouble(this.getOption(optionName));
    }

    public default boolean getBooleanOption(String optionName){
        return this.getOption(optionName).equalsIgnoreCase("true") || this.getOption(optionName).equalsIgnoreCase("T");
    }

    public static String getListOptions(Class obj){
        try {
            return ((AmidstOptionsHandler) obj.newInstance()).listOptionsRecursively();
        }catch (Exception e){
            throw new IllegalArgumentException("The class obj does not exist");
        }
    }

    public static String getListOptionsRecursively(Class obj){
        try {
            return ((AmidstOptionsHandler) obj.newInstance()).listOptionsRecursively();
        }catch (Exception e){
            throw new IllegalArgumentException("The class obj does not exist");
        }
    }


}