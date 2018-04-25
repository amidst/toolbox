/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.core.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * This class handles util methods related for parsing the options or parameters.
 */
public class OptionParser {

    /** Represents a sub option separator. */
    private static final String subOptionSeparator = "--";

    /** Represents a {@code HashMap} including all the options. */
    private static HashMap<String,HashMap<String,String>> allOptions;

    /** Represents the default configuration file name. */
    private static String confFileNameDefault = "configurationFiles/conf.txt";

    /** Represents the configuration file name. */
    private static String confFileName = "";

    /** Represents an {@code array} of {@code String} including the command line options. */
    private static String[] commandLineOptions = new String[0];

    /** Represents the set of all options (recursive one included) that are used for a particular run. */
    private static String optionString = "";

    /**
     * Sets the configuration file name.
     * @param fileName a {@code String} that represent the input file name.
     */
    public static void setConfFileName(String fileName){
        confFileName = fileName;
    }

    /**
     * Sets the options for a given {@code Class}.
     * @param className a given {@code Class}.
     * @param args an {@code array} of {@code String}.
     */
    public static void setArgsOptions(Class className, String[] args){
        setArgsOptions(className.getName(), args);
    }

    /**
     * Sets the options for a given class name ID.
     * @param classNameID a {@code String} that represents the class name ID.
     * @param args an {@code array} of {@code String}.
     */
    public static void setArgsOptions(String classNameID, String[] args) {

        commandLineOptions = new String[args.length+1];
        commandLineOptions[0] = subOptionSeparator+classNameID;
        int offset = 0;

        for (int i = 0; i < args.length; i++) {
            // Check -help and -confFileName (name of the class should be passed) and store args

            if(args[i].equalsIgnoreCase("-help") || args[i].equalsIgnoreCase("--help")){
                 if(args.length == 1){
                    String options = getListOptions(classNameID);
                    options = options.replace("\\","\n");
                    System.out.println(options);
                }else if(args[++i].equalsIgnoreCase("-r")){
                     //TODO: Parse this outputString to make it look/read good
                    String options = getListOptionsRecursively(classNameID);
                    options = options.replace("\\","\n");
                    System.out.println(options);
                }
                System.exit(0);
            }

            if(args[i].equalsIgnoreCase("-confFile")) {
                setConfFileName(args[i++]);
                offset = 2;
            }else{
                commandLineOptions[i+1-offset] = args[i];
            }
        }

        if(offset==2) {
            //Remove two last empty elements
            commandLineOptions = new String[commandLineOptions.length-2];
            System.arraycopy(commandLineOptions, 0 , commandLineOptions, 0, commandLineOptions.length-2);
        }
    }

    /**
     * Parses the set of all options.
     * @param classNameID the class name ID.
     * @param defaultOptions the default options.
     * @param optionName the option name.
     * @return a {@code String} containing the set of all options.
     */
    public static String parse(String classNameID, String defaultOptions, String optionName) {
        /**
         * The preference order is as follows:
         * 1) Configuration file options (if found or provided).
         * 2) Command line options (if provided).
         * 3) Options by default (listed in listOptions() in each class).
         * The first time it is called, it must put all options in a String (recursively) and print it at some point.
         */
        if(allOptions == null) {
            loadFileOptions();
            loadCommandLineOptions(classNameID);
            loadDefaultOptions(defaultOptions); //Only puts if absent
            optionString = allOptions.toString();
        }else if(allOptions.get(classNameID) == null){
            loadDefaultOptions(defaultOptions); //Only puts if absent
         }else if(allOptions.get(classNameID).get(optionName) == null){
            loadDefaultOptions(defaultOptions); //Only puts if absent
        }
        return allOptions.get(classNameID).get(optionName);
    }

    /**
     * Loads the configuration file options.
     */
    public static void loadFileOptions(){

        allOptions = new HashMap<>();
        if(confFileName.isEmpty()) {
            Path pathFile = Paths.get(confFileNameDefault);
            try {
                Files.lines(pathFile)
                        .filter(line -> !line.startsWith("%"))
                        .forEach(line -> parseLine(line));
            } catch (IOException ex) {
                //Continue with default options
            }
        }else{
            Path pathFile = Paths.get(confFileName);
            try {
                Files.lines(pathFile)
                        .filter(line -> !line.startsWith("%"))
                        .forEach(line -> parseLine(line));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }

    /**
     * Parses a {@code String} line.
     * @param line a {@code String} line
     */
    private static void parseLine(String line){
        String[] splitLine = line.split("\\s+|\\s*,\\s*|\\s*=\\s*");
        HashMap<String,String> local = new HashMap<>();

        if(allOptions.get(splitLine[0])==null) {
            for (int i = 1; i < splitLine.length - 1; i++) {
                local.put(splitLine[i].trim(), splitLine[++i].trim());
            }

            if (!local.isEmpty())
                allOptions.put(splitLine[0].trim(), local);
        }else{//When this method is called from AmidstOptionHandler.loadOptionsFromFile()
            for (int i = 1; i < splitLine.length - 1; i++) {
                allOptions.get(splitLine[0]).put(splitLine[i].trim(), splitLine[++i].trim());
            }
        }
    }

    /**
     * Loads the command line options.
     * @param classNameID the class name ID.
     */
    public static void loadCommandLineOptions(String classNameID){

        String className = classNameID;
        HashMap<String,String> local = new HashMap<>();

        for (int i = 0; i < commandLineOptions.length; i++) {
            if(commandLineOptions[i].startsWith(subOptionSeparator)) {
                if(!local.isEmpty()) {
                    allOptions.put(className, local);
                }
                className = commandLineOptions[i].replace(subOptionSeparator,"");
                local = new HashMap<>();
            }
            else if(allOptions.get(className) == null){
                if((i+1)<commandLineOptions.length)
                    if(!commandLineOptions[i+1].trim().startsWith("-")) {
                        local.put(commandLineOptions[i].trim(), commandLineOptions[++i].trim());
                        continue;
                    }
                local.put(commandLineOptions[i].trim(), "true");
            }else{
                if((i+1)<commandLineOptions.length)
                    if(!commandLineOptions[i+1].trim().startsWith("-")) {
                        allOptions.get(className).put(commandLineOptions[i].trim(), commandLineOptions[++i].trim());
                        continue;
                    }
                allOptions.get(className).put(commandLineOptions[i].trim(), "true");
            }
        }
        //In case the separator (for subOptions) has not been added at the end
        if(!local.isEmpty()){
            allOptions.put(className,local);
        }
    }

    /**
     * Loads the set of options by default.
     * @param defaultOptions s {@code String} containing the set of default options.
     */
    public static void loadDefaultOptions(String defaultOptions){

        String[] options = defaultOptions.split(",\\\\|\\s*,\\s*|\\\\");
        String className = options[0];

        HashMap<String,String> local = new HashMap<>();

        for (int i = 1; i < options.length; i++) {
            if(options[i].equals("\t")){
                allOptions.putIfAbsent(className, local);
                className = options[++i].trim();
                local = new HashMap<>();
            }else if(allOptions.get(className) == null){
                local.put(options[i].trim(), options[++i].trim());
            }
            else{
                allOptions.get(className).putIfAbsent(options[i].trim(), options[++i].trim());
            }
            i++; //Skip the description
        }
        if(!local.isEmpty()){
            allOptions.put(className,local);
        }
    }

    /**
     * Returns the list of options of a given a class name ID.
     * @param classNameID a given a class name ID
     * @return a {@code String} containing le list of options.
     */
    public static String getListOptions(String classNameID){
        try {
            return ((AmidstOptionsHandler) Class.forName(classNameID).newInstance()).listOptions();
        }catch (Exception e){
            throw new IllegalArgumentException("The class " + classNameID + " does not exist");
        }
    }

    /**
     * Returns recursively the list of options of a given a class name ID.
     * @param classNameID a given a class name ID
     * @return a {@code String} containing le list of options.
     */
    public static String getListOptionsRecursively(String classNameID){
        try {
            return ((AmidstOptionsHandler) Class.forName(classNameID).newInstance()).listOptionsRecursively();
        }catch (Exception e){
            throw new IllegalArgumentException(e);
        }
    }

}
