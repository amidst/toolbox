
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

package eu.amidst.core.models;

import eu.amidst.core.variables.Variable;
import java.util.Iterator;
import java.util.List;

/**
 * The ParentSet interface is used for handling the parent sets in a DAG.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#bnexample"> http://amidst.github.io/toolbox/CodeExamples.html#bnexample </a>  </p>
 *
 * <p> For further details about the implementation of this class using Java 8 functional-style programming look at the following paper: </p>
 *
 * <i> Masegosa et al. Probabilistic Graphical Models on Multi-Core CPUs using Java 8. IEEE-CIM (2015). </i>
 *
 */
public interface ParentSet extends Iterable<Variable>{

    /**
     * Returns the main variable.
     * @return the main variable.
     */
    Variable getMainVar();

    /**
     * Adds a given Variable as a new parent of the main variable.
     * @param var the new parent that will be added.
     */
    void addParent(Variable var);

    /**
     * Removes a given Variable from the parent set of the main variable.
     * @param var the parent variable that will be removed.
     */
    void removeParent(Variable var);

    /**
     * Returns a list of parents.
     * @return the list of parents.
     */
    List<Variable> getParents();

    /**
     * Returns the number of parents.
     * @return the number of parents.
     */
    int getNumberOfParents();

    /**
     * Returns a textual representation of the parent set.
     * @return a String description of the parent set.
     */
    String toString();

    /**
     * Defines the set of parent as unmodifiable.
     */
    void blockParents();

    /**
     * Tests if a given variable pertains to this parent set.
     * @param var a variable to be tested.
     * @return a boolean indicating if the variable pertains to this parent set (true) or not (false).
     */
    boolean contains(Variable var);

    /**
     * {@inheritDoc}
     */
    @Override
    boolean equals(Object o);

    /**
     * {@inheritDoc}
     */
    @Override
    default Iterator<Variable> iterator(){
        return this.getParents().iterator();
    }
}
