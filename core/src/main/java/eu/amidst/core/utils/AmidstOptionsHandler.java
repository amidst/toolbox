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

/**
 * This interface handles the different possible options or parameters.
 */
public interface AmidstOptionsHandler {

    /**
     * Returns the list of options.
     * @return a {@code String} containing le list of options.
     */
    String listOptions();

    /**
     * Returns the list of options recursively.
     * @return a {@code String} containing le list of options.
     */
    String listOptionsRecursively();

    /**
     * Loads the options.
     */
    void loadOptions();

    /**
     * Returns the name of this class.
     * @return a {@code String} containing the name of this class.
     */
    default String classNameID(){
        return this.getClass().getName();
    }

    /**
     * Sets the option list.
     * @param args the option list.
     */
    default void setOptions(String[] args){
        OptionParser.setArgsOptions(classNameID(),args);
        this.loadOptions();
    }

    /**
     * Loads the list of options from a given file name.
     * @param fileName a {@code String} containing the file name.
     */
    default void loadOptionsFromFile(String fileName){
        OptionParser.setConfFileName(fileName);
        OptionParser.loadFileOptions();
        OptionParser.loadDefaultOptions(classNameID());
        this.loadOptions();
    }

    /**
     * Returns the {@code String} value of an option given its name.
     * @param optionName the option name.
     * @return an {@code String} containing the value of the option.
     */
    default String getOption(String optionName) {
        return OptionParser.parse(classNameID(), listOptions(), optionName);
    }

    /**
     * Returns the {@code int} value of an option given its name.
     * @param optionName the option name.
     * @return an {@code int} containing the value of the option.
     */
    default int getIntOption(String optionName){
        return Integer.parseInt(this.getOption(optionName));
    }

    /**
     * Returns the {@code double} value of an option given its name.
     * @param optionName the option name.
     * @return a {@code double} containing the value of the option.
     */
    default double getDoubleOption(String optionName){
        return Double.parseDouble(this.getOption(optionName));
    }

    /**
     * Returns the {@code boolean} value of an option given its name.
     * @param optionName the option name.
     * @return a {@code boolean} containing the value of the option.
     */
    default boolean getBooleanOption(String optionName){
        return this.getOption(optionName).equalsIgnoreCase("true") || this.getOption(optionName).equalsIgnoreCase("T");
    }

    /**
     * Returns recursively the list of options of a given a {@code Class} instance.
     * @param obj a {@code Class} instance.
     * @return a {@code String} containing le list of options.
     */
    static String listOptionsRecursively(Class obj){
        try {
            return ((AmidstOptionsHandler) obj.newInstance()).listOptionsRecursively();
        }catch (Exception e){
            throw new IllegalArgumentException("The class "+ obj +" does not exist");
        }
    }

}