/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

/**
 ******************* ISSUE LIST **************************
 *
 * 1. (Andres) Implement DynamicDAG with two DAGs: one for time 0 and another for time T.
 *
 * ********************************************************
 */

package eu.amidst.dynamic.models;

import eu.amidst.core.models.DAG;
import eu.amidst.core.models.ParentSet;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.util.List;

/**
 * Created by Hanen on 13/11/14.
 */
public interface DynamicDAG {

    DynamicVariables getDynamicVariables();

    /* Methods accessing structure at time T*/
    ParentSet getParentSetTimeT(Variable var);

    ParentSet getParentSetTime0(Variable var);

    boolean containCycles();

    List<ParentSet> getParentSetsTimeT();

    List<ParentSet> getParentSetsTime0();

    void setName(String name);

    String getName();

    DAG toDAGTimeT();

    DAG toDAGTime0();
}
