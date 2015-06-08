/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.variables;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;

import java.io.Serializable;
import java.util.List;

/**
 * Created by andresmasegosa on 26/02/15.
 */
public abstract class DistributionType  implements Serializable {

    private static final long serialVersionUID = 4158293895929418259L;

    protected Variable variable;

    public DistributionType(Variable var_){
        this.variable=var_;
    }

    public abstract boolean isParentCompatible(Variable parent);

    public abstract <E extends UnivariateDistribution> E newUnivariateDistribution();

    public <E extends EF_UnivariateDistribution> E newEFUnivariateDistribution(){
        return this.newUnivariateDistribution().toEFUnivariateDistribution();
    }

    public abstract <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents);

    public <E extends EF_ConditionalDistribution> E newEFConditionalDistribution(List<Variable> parents){
        return this.newConditionalDistribution(parents).toEFConditionalDistribution();
    }

    public boolean areParentsCompatible(List<Variable> parents){
        for(Variable parent: parents){
            if (!isParentCompatible(parent))
                return false;
        }
        return true;
    }

    public static boolean containsParentsThisDistributionType(List<Variable> parents, DistributionTypeEnum distributionTypeEnum){
        for (Variable var : parents){
            if (var.getDistributionTypeEnum()==distributionTypeEnum)
                return true;
        }

        return false;
    }

}

