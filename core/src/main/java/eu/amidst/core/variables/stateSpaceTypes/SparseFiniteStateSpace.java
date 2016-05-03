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

package eu.amidst.core.variables.stateSpaceTypes;

import eu.amidst.core.variables.StateSpaceType;
import eu.amidst.core.variables.StateSpaceTypeEnum;

/**
 * This class extends the abstract class {@link StateSpaceType} and implements the interface {@code Iterable<String>}.
 * It defines and handles the state space for discrete variables, i.e., the finite state space.
 */
public class SparseFiniteStateSpace extends StateSpaceType {

    /** Represents the number of states. */
    private int numberOfStates;

    /**
     * Creates a new FiniteStateSpace given the number of states.
     * @param numberOfStates1 the number of states.
     */
    public SparseFiniteStateSpace(int numberOfStates1) {
        super(StateSpaceTypeEnum.SPARSE_FINITE_SET);
        this.numberOfStates=numberOfStates1;
    }


    /**
     * Returns the number of states.
     * @return the number of states.
     */
    public int getNumberOfStates() {
        return numberOfStates;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String stringValue(double value) {
        return value+"";
    }
}
