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
 * 1. (Andres) Implement DynamicBN with two BNs: one for time 0 and another for time T.
 *
 * ********************************************************
 */

package eu.amidst.dynamic.models;


import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.util.List;
import java.util.Random;

/**
 * <h2>This class implements a dynamic Bayesian network.</h2>
 *
 * @author afalvarez@ual.es, andres@cs.aau.dk & ana@cs.aau.dk
 * @version 1.0
 * @since 2014-07-3
 *
 */
public interface DynamicBayesianNetwork{
    
    void setConditionalDistributionTime0(Variable var, ConditionalDistribution dist);

    void setConditionalDistributionTimeT(Variable var, ConditionalDistribution dist);

    void setName(String name);

    String getName();

    int getNumberOfDynamicVars();

    DynamicVariables getDynamicVariables();

    <E extends ConditionalDistribution> E getConditionalDistributionTimeT(Variable var);

    <E extends ConditionalDistribution> E getConditionalDistributionTime0(Variable var);

    DynamicDAG getDynamicDAG ();

    int getNumberOfVars();

    double getLogProbabiltyOfFullAssignmentTimeT(Assignment assignment);

    double getLogProbabiltyOfFullAssignmentTime0(Assignment assignment);

    List<ConditionalDistribution> getConditionalDistributionsTimeT();

    List<ConditionalDistribution> getConditionalDistributionsTime0();

     void randomInitialization(Random random);

    boolean equalDBNs(DynamicBayesianNetwork bnet, double threshold);

    BayesianNetwork toBayesianNetworkTime0();

    BayesianNetwork toBayesianNetworkTimeT();

}
