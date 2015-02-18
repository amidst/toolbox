package eu.amidst.core.utils;

import org.apache.log4j.spi.OptionHandler;
import org.eclipse.jetty.io.UncheckedIOException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * Created by ana@cs.aau.dk on 17/02/15.
 */
public class OptionParser {

    private static final String subOptionSeparator = "--";

    private static HashMap<String,HashMap<String,String>> allOptions;
    private static String confFileName = "./conf.txt";
    private static String[] commandLineOptions;
    /**
     * Contains all options (recursive one included) that are used for a particular execution
     */
    private static String optionString = "";


    public static void setConfFileName(String fileName){
        confFileName = fileName;
    }

    public static void setArgsOptions(Class className, String[] args){
        setArgsOptions(className.getName(), args);
    }

    public static void setArgsOptions(String classNameID, String[] args) {


        commandLineOptions = new String[args.length];
        int offset = 0;
        /*
         * Check -help and -confFileName (name of the class should be passed) and store args
         */
        for (int i = 0; i < args.length; i++) {
            if(args[i].equalsIgnoreCase("-help") || args[i].equalsIgnoreCase("--help")){
                if(args[i++].equalsIgnoreCase("-r")) {
                    System.out.println(getListOptionsRecursively(classNameID));
                }else{
                    System.out.println(getListOptions(classNameID));
                }
                System.exit(0);
            }
            if(args[i].equalsIgnoreCase("-confFile")) {
                setConfFileName(args[i++]);
                offset = 2;
            }else{
                commandLineOptions[i-offset] = args[i];
            }
        }
        if(offset==2) {//Remove two last empty elements
            commandLineOptions = new String[commandLineOptions.length-2];
            System.arraycopy(commandLineOptions, 0 , commandLineOptions, 0, commandLineOptions.length-2);
        }

    }


    public static String parse(String classNameID, String defaultOptions, String optionName) {
        /**
         * The preference order is as follows:
         * 1) Command line options (if provided).
         * 2) Configuration file options (if found or provided).
         * 3) Options by default (listed in listOptions() in each class).
         * The first time it is called, it must put all options in a String (recursively) and print it at some point.
         */
        if(allOptions == null) {
            loadFileOptions(classNameID);
            loadCommandLineOptions(classNameID);
            loadDefaultOptions(defaultOptions); //Only puts if absent
            optionString = allOptions.toString();
        }else if(allOptions.get(classNameID) == null){
            loadDefaultOptions(defaultOptions); //Only puts if absent
         }
        return allOptions.get(classNameID).get(optionName);
    }


    public static void loadFileOptions(String classNameID){

        allOptions = new HashMap<>();
        Path pathFile = Paths.get(confFileName);
        try {
            Files.lines(pathFile)
                    .filter(line -> !line.startsWith("%"))
                    .forEach(line -> parseLine(line));
        }catch (IOException ex){
            throw new UncheckedIOException(ex);
        }
    }

    private static void parseLine(String line){
        String[] splitLine = line.split(" ");
        HashMap<String,String> local = new HashMap<>();

        for (int i = 1; i < splitLine.length-1; i++) {
            local.put(splitLine[i], splitLine[i++]);
        }

        if(!local.isEmpty())
            allOptions.put(splitLine[0],local);
    }

    public static void loadCommandLineOptions(String classNameID){

        String className = classNameID;
        HashMap<String,String> local = new HashMap<>();

        for (int i = 0; i < commandLineOptions.length; i++) {
            if(commandLineOptions[i].equals(subOptionSeparator)) {
                allOptions.put(className,local);
                className = commandLineOptions[i++];
                local = new HashMap<>();
            }
            else{
                local.put(commandLineOptions[i], commandLineOptions[i++]);
            }
        }
        //In case the separator (for subOptions) has not been added at the end
        if(!local.isEmpty()){
            allOptions.put(className,local);
        }
    }

    public static void loadDefaultOptions(String defaultOptions){

        String[] options = defaultOptions.split(" |\\\\");
        String className = options[0];

        HashMap<String,String> local = new HashMap<>();

        for (int i = 1; i < options.length; i++) {
            if(options[i].equals("\t")){
                allOptions.putIfAbsent(className, local);
                className = options[i++];
                local = new HashMap<>();
            }else if(allOptions.get(className) == null){
                local.put(options[i], options[i++]);
            }
            else{
                allOptions.get(className).putIfAbsent(options[i], options[i++]);
            }
            i++; //Skip the description
        }
    }

    public static String getListOptions(String classNameID){
        try {
            return ((AmidstOptionsHandler) Class.forName(classNameID).getConstructor(String.class).newInstance()).listOptions();
        }catch (Exception e){
            throw new IllegalArgumentException("The class" +classNameID+ "does not exist");
        }
    }

    public static String getListOptionsRecursively(String classNameID){
        try {
            return ((AmidstOptionsHandler) Class.forName(classNameID).getConstructor(String.class).newInstance()).listOptionsRecursively();
        }catch (Exception e){
            throw new IllegalArgumentException("The class" + classNameID+ "does not exist");
        }
    }

}
