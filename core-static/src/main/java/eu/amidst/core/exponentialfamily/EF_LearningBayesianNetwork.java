/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 06/01/15.
 */
public class EF_LearningBayesianNetwork extends EF_Distribution {

    List<EF_ConditionalDistribution> distributionList;
    ParameterVariables parametersVariables;

    public EF_LearningBayesianNetwork(DAG dag){

        parametersVariables = new ParameterVariables(dag.getStaticVariables().getNumberOfVars());

        distributionList =
                dag.getStaticVariables()
                        .getListOfVariables()
                        .stream()
                        .map(var -> var.getDistributionType().newEFConditionalDistribution(dag.getParentSet(var).getParents()).toExtendedLearningDistribution(parametersVariables))
                        .flatMap(listOfDist -> listOfDist.stream())
                        .sorted((a,b) -> a.getVariable().getVarID() - b.getVariable().getVarID())
                        .collect(Collectors.toList());

        this.naturalParameters = null;
        this.momentParameters = null;
    }


    public EF_LearningBayesianNetwork(List<EF_ConditionalDistribution> distributions){

        parametersVariables = new ParameterVariables(distributions.size());

        distributionList =
                distributions
                        .stream()
                        .map(dist -> dist.toExtendedLearningDistribution(parametersVariables))
                        .flatMap(listOfDist -> listOfDist.stream())
                        .sorted((a,b) -> a.getVariable().getVarID() - b.getVariable().getVarID())
                        .collect(Collectors.toList());

        this.naturalParameters = null;
        this.momentParameters = null;
    }

    public ParameterVariables getParametersVariables() {
        return parametersVariables;
    }

    public List<ConditionalDistribution> toConditionalDistribution(){
        List<ConditionalDistribution> condDistList = new ArrayList<>();

        for (EF_ConditionalDistribution dist: distributionList) {
            if (dist.getVariable().isParameterVariable())
                continue;

            EF_ConditionalDistribution distLearning = dist;
            Map<Variable, Vector> expectedParameters = new HashMap<>();
            for(Variable var: distLearning.getConditioningVariables()){
                if (!var.isParameterVariable())
                    continue;;
                EF_UnivariateDistribution uni =  ((EF_UnivariateDistribution)distributionList.get(var.getVarID()));
                expectedParameters.put(var, uni.getExpectedParameters());
            }
            condDistList.add(distLearning.toConditionalDistribution(expectedParameters));
        }


        condDistList = condDistList.stream().sorted((a, b) -> a.getVariable().getVarID() - b.getVariable().getVarID()).collect(Collectors.toList());

        return condDistList;
    }

    public EF_BayesianNetwork toEFBayesianNetwork(){
        EF_BayesianNetwork ef_bn = new EF_BayesianNetwork();
        List<EF_ConditionalDistribution> distributions = new ArrayList<>();
        distributions.addAll(this.distributionList);
        ef_bn.setDistributionList(distributions);

        return ef_bn;
    }

    public List<EF_ConditionalDistribution> getDistributionList() {
        return distributionList;
    }


    public EF_ConditionalDistribution getDistribution(Variable var) {
        return distributionList.get(var.getVarID());
    }

    public void setDistribution(Variable var, EF_ConditionalDistribution dist) {
        distributionList.set(var.getVarID(), dist);
    }

    @Override
    public void updateNaturalFromMomentParameters() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    @Override
    public void updateMomentFromNaturalParameters() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    @Override
    public int sizeOfSufficientStatistics() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    @Override
    public double computeLogNormalizer() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    @Override
    public Vector createZeroedVector() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

}
