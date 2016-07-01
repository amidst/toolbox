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

package eu.amidst.dynamic.variables;

import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class implements the interface {@link DynamicAssignment} and handles the dynamic assignments using a HashMap.
 */
public class HashMapDynamicAssignment implements DynamicAssignment {

    /** Represents an assignment as a {@link java.util.Map} object that maps variables to values. */
    private Map<Variable,Double> assignment;

    /** Represents the sequence ID. */
    long sequenceID;

    /** Represents the time ID. */
    long timeID;

    /**
     * Creates a new HashMapDynamicAssignment given the number of variables.
     * @param nOfVars the number of dynamic variables.
     */
    public HashMapDynamicAssignment(int nOfVars){
        assignment = new ConcurrentHashMap(nOfVars);
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
     * {@inheritDoc}
     */
    @Override
    public long getSequenceID() {
        return sequenceID;
    }

    public void setSequenceID(int sequenceID) {
        this.sequenceID = sequenceID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTimeID() {
        return timeID;
    }

    /**
     * Sets the TimeID.
     * @param timeID an {@code int} that represents the time ID.
     */
    public void setTimeID(int timeID) {
        this.timeID = timeID;
    }

}
