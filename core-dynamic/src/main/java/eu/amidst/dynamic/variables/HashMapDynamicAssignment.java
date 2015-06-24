/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.dynamic.variables;

import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ana@cs.aau.dk on 03/11/14.
 */
public class HashMapDynamicAssignment implements DynamicAssignment {
    private Map<Variable,Double> assignment;
    int sequenceID;
    int timeID;

    public HashMapDynamicAssignment(int nOfVars){
        assignment = new ConcurrentHashMap(nOfVars);
    }

    @Override
    public double getValue(Variable key){
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
    public void setValue(Variable var, double val) {
        this.assignment.put(var,val);
    }

    @Override
    public Set<Variable> getVariables() {
        return null;
    }

    @Override
    public int getSequenceID() {
        return sequenceID;
    }

    public void setSequenceID(int sequenceID) {
        this.sequenceID = sequenceID;
    }

    @Override
    public int getTimeID() {
        return timeID;
    }

    public void setTimeID(int timeID) {
        this.timeID = timeID;
    }


    // Now you can use the following loop to iterate over all assignments:
    // for (Map.Entry<Variable, Double> entry : assignment.entrySet()) return entry;
    public Set<Map.Entry<Variable,Double>> entrySet(){
        return assignment.entrySet();
    }

}
