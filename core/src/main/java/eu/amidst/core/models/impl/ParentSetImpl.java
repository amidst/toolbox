/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package eu.amidst.core.models.impl;

import eu.amidst.core.models.ParentSet;
import eu.amidst.core.variables.Variable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the interface {@link ParentSet}.
 * It is used to handle the parent set operations.
 */
public class ParentSetImpl implements ParentSet, Serializable {

    /**
     * Represents a serialVersionUID value.
     */
    private static final long serialVersionUID = 3580889238865345208L;

    /**
     * Represents the main variable.
     */
    private Variable mainVar;

    /**
     * Represents the parent set of mainVar.
     */
    private List<Variable> vars;

    /**
     * Creates a new empty parent set for a given variable.
     * @param mainVar1 the main variable.
     */
    public ParentSetImpl(Variable mainVar1) {
        mainVar = mainVar1;
        this.vars = new ArrayList<Variable>();
    }

    /**
     * Returns the mainVar.
     * @return the mainVar.
     */
    @Override
    public Variable getMainVar() {
        return mainVar;
    }

    /**
     * Adds a given Variable as a new parent of the main variable.
     * That is, it adds a directed link from the new parent to the mainVar in the DAG.
     * @param var the new parent that will be added.
     */
    public void addParent(Variable var) {
        if (!mainVar.getDistributionType().isParentCompatible(var)){
            throw new IllegalArgumentException("Adding a parent var " +var.getName()+ " of type "+var.getDistributionTypeEnum().toString()+" which is not compatible " +
                    "with children variable "+this.getMainVar().getName()+" of type "+this.mainVar.getDistributionTypeEnum().toString());
        }

        if (this.contains(var)) {
            throw new IllegalArgumentException("Trying to add a duplicated parent");
        }

        vars.add(var);
    }

    /**
     * Removes a given Variable from the parent set of the main variable.
     * That is, it removes the directed link between a given Variable and the mainVar in the DAG.
     * @param var the parent variable that will be removed.
     */
    public void removeParent(Variable var) {
        vars.remove(var);
    }

    /**
     * Returns a list of parents.
     * @return the list of parents.
     */
    public List<Variable> getParents() {
        return vars;
    }

    /**
     * Returns the number of parents.
     * @return the number of parents.
     */
    public int getNumberOfParents() {
        return vars.size();
    }

    /**
     * Returns a textual representation of the parent set of this mainVar.
     * @return a String description of the parent set of this mainVar.
     */
    @Override
    public String toString() {

        int numParents = getNumberOfParents();
        StringBuilder str = new StringBuilder();
        str.append("{");

        for (int i = 0; i < numParents; i++) {
            Variable parent = getParents().get(i);
            str.append(parent.getName());
            if (i < numParents - 1) {
                str.append(", ");
            }
        }
        str.append("}");
        return str.toString();
    }

    /**
     * Defines the set of parent as unmodifiable, i.e., no add or remove operations are allowed.
     */
    public void blockParents() {
        //vars = Collections.unmodifiableList(vars);
    }

    /**
     * Tests if a given variable pertains to this parent set.
     * @param var a variable to be tested.
     * @return a boolean indicating if the variable pertains to this parent set (true) or not (false).
     */
    public boolean contains(Variable var) {
        return this.vars.contains(var);
    }

    @Override
    public boolean equals(ParentSet parentSet) {
        return this.equals((Object)parentSet);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ParentSet parentset = (ParentSet) o;

        if (this.getNumberOfParents() != parentset.getNumberOfParents()) {
            return false;
        } else {
            int i = 0;
            boolean eqs = true;
            while (i < this.getNumberOfParents() && eqs) {
                if (this.getParents().get(i).equals(parentset.getParents().get(i))) {
                    i++;
                } else {
                    eqs = false;
                }
            }
            return eqs;
        }
    }
}