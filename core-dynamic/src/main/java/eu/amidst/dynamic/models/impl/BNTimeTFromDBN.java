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

package eu.amidst.dynamic.models.impl;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by andresmasegosa on 10/12/15.
 */
public class BNTimeTFromDBN implements BayesianNetwork {

    DynamicBayesianNetwork dynamicBayesianNetwork;
    DAG dag;
    List<ConditionalDistribution> interfaceDist;
    List<ConditionalDistribution> allDist;

    public BNTimeTFromDBN(DynamicBayesianNetwork dynamicBayesianNetwork) {
        this.dynamicBayesianNetwork = dynamicBayesianNetwork;
        this.dag = new DAGTimeTFromDynamicDAG(this.dynamicBayesianNetwork.getDynamicDAG());

        interfaceDist = new ArrayList<>();

        List<Variable> vars = this.dynamicBayesianNetwork.getDynamicVariables().getListOfDynamicVariables();

        for (int i = 0; i < vars.size(); i++) {
            interfaceDist.add(this.dynamicBayesianNetwork.getDynamicVariables().getVariableById(i).getInterfaceVariable().newUnivariateDistribution());
        }

        allDist = new ArrayList<>();
        allDist.addAll(this.dynamicBayesianNetwork.getConditionalDistributionsTimeT());
        allDist.addAll(this.interfaceDist);

    }

    @Override
    public String getName() {
        return dynamicBayesianNetwork.getName();
    }

    @Override
    public void setName(String name) {
        this.dynamicBayesianNetwork.setName(name);
    }

    @Override
    public <E extends ConditionalDistribution> E getConditionalDistribution(Variable var) {
        if (var.isInterfaceVariable())
            return (E)this.interfaceDist.get(var.getVarID());
        else
            return dynamicBayesianNetwork.getConditionalDistributionTimeT(var);
    }

    @Override
    public void setConditionalDistribution(Variable var, ConditionalDistribution dist) {
        if (var.isInterfaceVariable())
            this.interfaceDist.set(var.getVarID(),dist);
        else
            this.dynamicBayesianNetwork.setConditionalDistributionTimeT(var,dist);
    }

    @Override
    public int getNumberOfVars() {
        return this.dynamicBayesianNetwork.getNumberOfVars()*2;
    }

    @Override
    public Variables getVariables() {
        return this.dag.getVariables();
    }

    @Override
    public DAG getDAG() {
        return this.dag;
    }

    @Override
    public double[] getParameters() {
        return null;
    }

    @Override
    public double getLogProbabiltyOf(Assignment assignment) {
        return this.dynamicBayesianNetwork.getLogProbabiltyOfFullAssignmentTimeT(assignment);
    }

    @Override
    public List<ConditionalDistribution> getConditionalDistributions() {
        return this.allDist;
    }

    @Override
    public void randomInitialization(Random random) {
        for (ConditionalDistribution conditionalDistribution : allDist) {
            conditionalDistribution.randomInitialization(random);
        }
    }

    @Override
    public boolean equalBNs(BayesianNetwork bnet, double threshold) {
        boolean equals = false;
        if (this.getDAG().equals(bnet.getDAG())){
            equals = true;
            for (Variable var : this.getVariables()) {
                equals = equals && this.getConditionalDistribution(var).equalDist(bnet.getConditionalDistribution(var), threshold);
            }
        }
        return equals;
    }
}
