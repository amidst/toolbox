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

//TODO Check! Is this class should be removed?

/**
 * This class is just meant to serve as a temporary example to fill in options in STATIC CLASSES, that should
 * "overwrites" the methods below, it will be removed in the future.
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
        return "BayesianNetworkGenerator";
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