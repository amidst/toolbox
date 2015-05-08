/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.utils;


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

    public default void setOptions(String[] args){
        OptionParser.setArgsOptions(classNameID(),args);
        this.loadOptions();
    }

    public default void loadOptionsFromFile(String fileName){
        OptionParser.setConfFileName(fileName);
        OptionParser.loadFileOptions();
        OptionParser.loadDefaultOptions(classNameID());
        this.loadOptions();
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

    public static String listOptions(Class obj){
        try {
            return ((AmidstOptionsHandler) obj.newInstance()).listOptionsRecursively();
        }catch (Exception e){
            throw new IllegalArgumentException("The class " +obj+ " does not exist");
        }
    }

    public static String listOptionsRecursively(Class obj){
        try {
            return ((AmidstOptionsHandler) obj.newInstance()).listOptionsRecursively();
        }catch (Exception e){
            throw new IllegalArgumentException("The class "+ obj +" does not exist");
        }
    }


}