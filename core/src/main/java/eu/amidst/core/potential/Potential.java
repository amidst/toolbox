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

package eu.amidst.core.potential;

import java.util.List;

/**
 * This interface defines and handles the basic operations for a potential.
 */
public interface Potential {

    /**
     * Sets the variables in this Potential.
     * @param variables a list of variables to set the potential with
     */
    void setVariables(List variables);

    /**
     * Returns the list of variables in this Potential.
     * @return the {@code List} of variables in the potential.
     */
    List getVariables();

    /**
     * Combines this Potential with an input given potential.
     * @param pot an input potential.
     */
    void combine(Potential pot);

    /**
     * Computes the marginalization of this Potential.
     * @param variables a list of variables.
     */
    void marginalize(List variables);
}
