/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.ida2015;


import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.filereaders.DataInstanceImpl;
import eu.amidst.core.datastream.filereaders.DataRow;
import eu.amidst.core.datastream.filereaders.DataStreamFromFile;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataWriter;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.StateSpaceTypeEnum;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class scriptAddIndicators{

    public static String[] indicatorVars = {"VAR01","VAR05","VAR06","VAR07","VAR08","VAR09","VAR10","VAR11","VAR12","VAR13","VAR14","VAR15"};

    public static void addIndicatorVarsToCajamar(String path)  throws IOException {
        ARFFDataReader reader= new ARFFDataReader();
        reader.loadFromFile(path);

        Attributes atts = reader.getAttributes();
        List<Attribute> newAtts= new ArrayList<>();
        for(Attribute att: reader.getAttributes().getList()){
            newAtts.add(att);
        }
        Attribute classAtt = newAtts.get(newAtts.size()-1);
        newAtts.remove(classAtt); //Remove the class to append at the end

        for(Attribute att: atts.getList()){
            String name = att.getName();
            for (int s = 0; s < indicatorVars.length; s++) {
                if(name.equalsIgnoreCase(indicatorVars[s])){
                    //Add an indicator variable
                    Attribute attIndicator = new Attribute(newAtts.size(), "INDICATOR_"+name, StateSpaceTypeEnum.FINITE_SET, 2);
                    newAtts.add(attIndicator);
                }
            }
        }
        newAtts.add(classAtt);

        String newPath = path.replace(".arff", "INDICATORS.arff");

        FileWriter fw = new FileWriter(newPath);
        fw.write("@relation dataset\n\n");

        for (Attribute att : newAtts){
            fw.write(ARFFDataWriter.attributeToARFFString(att)+"\n");
        }

        fw.write("\n\n@data\n\n");

        DataStreamFromFile data = new DataStreamFromFile(reader);

        data.stream().forEach(e -> {
            DataRow dataRow = new DataRowFromAtts(newAtts.size());
            for (Attribute att : atts) {
                dataRow.setValue(att, e.getValue(att));
                String name = att.getName();
                for (int s = 0; s < indicatorVars.length; s++) {
                    if(name.equalsIgnoreCase(indicatorVars[s])) {
                        //Add an indicator variable
                        Attribute attIndicator = getAttribute(newAtts, "INDICATOR_" + name);
                        dataRow.setValue(attIndicator, e.getValue(att) == 0 ? 1 : 0);
                    }
                }
            }
            DataInstance assignment = new DataInstanceImpl(dataRow);
            try {
                fw.write(ARFFDataWriter.dataInstanceToARFFString(new Attributes(newAtts), assignment) + "\n");
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });

        fw.close();

    }

    private static Attribute getAttribute(List<Attribute> atts, String name){
        for(Attribute att: atts){
            if(att.getName().equalsIgnoreCase(name))
                return att;
        }
        throw new UnsupportedOperationException("Attribute not found, something failed");
    }

    private static class DataRowFromAtts implements DataRow {
        private Map<Attribute,Double> assignment;

        public DataRowFromAtts(int nOfAtts){
            assignment = new ConcurrentHashMap(nOfAtts);
        }

        @Override
        public double getValue(Attribute key){
            Double val = assignment.get(key);
            if (val!=null){
                return val.doubleValue();
            }
            else {
                //throw new IllegalArgumentException("No value stored for the requested variable: "+key.getName());
                return Utils.missingValue();
            }
        }
        @Override
        public void setValue(Attribute att, double val) {
            this.assignment.put(att,val);
        }


        // Now you can use the following loop to iterate over all assignments:
        // for (Map.Entry<Variable, Double> entry : assignment.entrySet()) return entry;
        public Set<Map.Entry<Attribute,Double>> entrySet(){
            return assignment.entrySet();
        }

    }

    public static void main(String[] args) {
        try {
            //addIndicatorVarsToCajamar(args[0]);
            addIndicatorVarsToCajamar("/Users/ana/Documents/core/datasets/dynamicDataOnlyContinuous.arff");
        }catch (IOException ex){}
    }
}