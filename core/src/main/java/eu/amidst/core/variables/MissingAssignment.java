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


import com.google.common.collect.Sets;
import eu.amidst.core.utils.Utils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class implements the interface {@link Assignment} and handles the assignments that contain missing values.
 */
public class MissingAssignment implements Assignment, Serializable{

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    /** Represents an object of type {@link Assignment}. */
    Assignment assignment;

    /** Represents the variables with missing values as a {@link java.util.Map} object that maps variables
     * to boolean values (equal to true if a variable has missing values and false otherwise).
     */
    Map<Variable,Boolean> missingVars = new HashMap();

    /**
     * Creates a new MissingAssignment.
     * @param assignment_ an assignment.
     */
    public MissingAssignment(Assignment assignment_){
        this.assignment=assignment_;
    }

    /**
     * Adds a variable with missing values to this MissingAssignment.
     * @param var the variable to be added.
     */
    public void addMissingVariable(Variable var){
        this.missingVars.put(var,true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getValue(Variable var) {
        if (this.missingVars.containsKey(var))
            return Utils.missingValue();
        else
            return assignment.getValue(var);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(Variable var, double value) {
        if (this.missingVars.containsKey(var))
            throw new IllegalArgumentException("A missing variable can not be modified");
        else
            assignment.setValue(var,value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Variable> getVariables() {
        return Sets.union(assignment.getVariables(),missingVars.keySet());
    }
}
