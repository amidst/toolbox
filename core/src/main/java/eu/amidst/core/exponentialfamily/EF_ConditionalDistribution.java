/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.Map;


public abstract class EF_ConditionalDistribution extends EF_Distribution {


    protected List<Variable> parents;

    public List<Variable> getConditioningVariables() {
        return this.parents;
    }

    public abstract double getExpectedLogNormalizer(Variable parent, Map<Variable,MomentParameters> momentChildCoParents);

    public abstract double getExpectedLogNormalizer(Map<Variable,MomentParameters> momentParents);

    public abstract NaturalParameters getExpectedNaturalFromParents(Map<Variable,MomentParameters> momentParents);

    public abstract NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable,MomentParameters> momentChildCoParents);

    public abstract <E extends ConditionalDistribution> E toConditionalDistribution();

    public ConditionalDistribution toConditionalDistribution(Map<Variable, Vector> expectedValueParameterVariables){
        throw new UnsupportedOperationException("This transformation does not make sense for variables with any parameter variable as parent.");
    }

}
