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

package eu.amidst.dynamic;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.variables.DistributionTypeEnum;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.models.impl.DynamicBayesianNetworkImpl;
import eu.amidst.dynamic.models.impl.DynamicDAGImpl;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.dynamic.variables.impl.DynamicVariablesImpl;

import java.util.List;
import java.util.Map;

/**
 * Created by andresmasegosa on 10/12/15.
 */
public class DynamicModelFactory {

    public static DynamicVariables newDynamicVariables() {
        return new DynamicVariablesImpl();
    }

    public static DynamicVariables newDynamicVariables(Attributes atts) {
        return new DynamicVariablesImpl(atts);
    }

    public static DynamicVariables newDynamicVariables(Attributes atts, Map<Attribute, DistributionTypeEnum> typeDists) {
        return new DynamicVariablesImpl(atts, typeDists);
    }

    public static DynamicDAG newDynamicDAG(DynamicVariables dynamicVariables1) {
        return new DynamicDAGImpl(dynamicVariables1);
    }

    public static DynamicBayesianNetwork newDynamicBayesianNetwork(DynamicDAG dynamicDAG1) {
        return new DynamicBayesianNetworkImpl(dynamicDAG1);
    }

    public static DynamicBayesianNetwork newDynamicBayesianNetwork(DynamicDAG dynamicDAG1, List<ConditionalDistribution> distsTime0, List<ConditionalDistribution> distsTimeT) {
        return new DynamicBayesianNetworkImpl(dynamicDAG1, distsTime0, distsTimeT);
    }
}