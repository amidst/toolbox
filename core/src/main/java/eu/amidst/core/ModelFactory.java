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

package eu.amidst.core;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.impl.BayesianNetworkImpl;
import eu.amidst.core.models.impl.DAGImpl;
import eu.amidst.core.variables.DistributionTypeEnum;
import eu.amidst.core.variables.Variables;
import eu.amidst.core.variables.impl.VariablesImpl;

import java.util.HashMap;
import java.util.List;

/**
 * Created by andresmasegosa on 10/12/15.
 */
public class ModelFactory {

    /**
     * Creates a new list of Variables.
     */
    public static Variables newVariables() {
        return new VariablesImpl();
    }

    /**
     * Creates a new list of Variables given a list of Attributes.
     *
     * @param atts a list of Attributes.
     */
    public static Variables newVariables(Attributes atts) {
        return new VariablesImpl(atts);
    }


    /**
     * Creates a new list of Variables given a list of Attributes and their corresponding distribution types.
     *
     * @param atts      a list of Attributes.
     * @param typeDists a {@link java.util.HashMap} object that maps the Attributes to their distribution types.
     */
    public static Variables newVariables(Attributes atts, HashMap<Attribute, DistributionTypeEnum> typeDists) {
        return new VariablesImpl(atts, typeDists);
    }

    /**
     * Creates a new DAG from a set of variables.
     *
     * @param variables the set of variables of type {@link Variables}.
     */
    public static DAG newDAG(Variables variables) {
        return new DAGImpl(variables);
    }

    /**
     * Creates a new BayesianNetwork from a dag.
     *
     * @param dag a directed acyclic graph.
     */
    public static BayesianNetwork newBayesianNetwork(DAG dag) {
        return new BayesianNetworkImpl(dag);
    }

    /**
     * Creates a new BayesianNetwork from a dag and a list of distributions.
     *
     * @param dag   a directed acyclic graph.
     * @param dists a list of conditional probability distributions.
     */
    public static BayesianNetwork newBayesianNetwork(DAG dag, List<ConditionalDistribution> dists) {
        return new BayesianNetworkImpl(dag,dists);
    }

}