/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.variables.distributionTypes;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.exponentialfamily.EF_Dirichlet;
import eu.amidst.core.variables.DistributionType;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * Created by andresmasegosa on 04/03/15.
 */
public class DirichletParameterType extends DistributionType {

    public DirichletParameterType(Variable var_) {
        super(var_);
    }

    @Override
    public boolean isParentCompatible(Variable parent) {
        return false;
    }

    @Override
    public Normal newUnivariateDistribution() {
        throw new UnsupportedOperationException("Dirichlet Parameter Type does not allow standard distributions");
    }

    @Override
    public EF_Dirichlet newEFUnivariateDistribution() {
         return new EF_Dirichlet(this.variable,2);
    }


    @Override
    public <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents) {
        throw new UnsupportedOperationException("Dirichlet Parameter Type does not allow conditional distributions");
    }
}
