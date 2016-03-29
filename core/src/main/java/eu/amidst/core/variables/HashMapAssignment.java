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

package eu.amidst.core.variables;

import eu.amidst.core.utils.Utils;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class implements the interface {@link Assignment} and handles the assignments using a HashMap.
 */
public class HashMapAssignment implements Assignment, Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = -3436599636425587512L;

    /** Represents an assignment as a {@link java.util.Map} object that maps variables to values. */
    private Map<Variable,Double> assignment;

    /**
     * Creates a new HashMapAssignment.
     */
    public HashMapAssignment(){
        assignment = new ConcurrentHashMap();
    }

    /**
     * Creates a new HashMapAssignment given the number of variables.
     * @param nOfVars the number of variables.
     */
    public HashMapAssignment(int nOfVars){
        assignment = new ConcurrentHashMap(nOfVars);
    }

    /**
     * Creates a new HashMapAssignment given an {@link Assignment} object.
     * @param assignment1 an assignment.
     */
    public HashMapAssignment(Assignment assignment1){
        Set<Variable> variableList = assignment1.getVariables();
        assignment = new ConcurrentHashMap(variableList.size());
        for(Variable var: variableList) {
            this.setValue(var,assignment1.getValue(var));
        }
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(Variable var, double val) {
        this.assignment.put(var,val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Variable> getVariables() {
        return assignment.keySet();
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this HashMapAssignment.
     * @return a set view of the mappings contained in this HashMapAssignment.
     */
    public Set<Map.Entry<Variable,Double>> entrySet(){
        return assignment.entrySet();
    }

}
