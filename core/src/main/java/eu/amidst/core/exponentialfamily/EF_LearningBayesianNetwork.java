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
 * This class represents a "Bayesian extended" model for a given Bayesian network. That is to say,
 * if we want to learn, using a Bayesian approach, a given BN model, we need to consider new random
 * variables acting as a prior distributions over the parameters of our BN model. This results in a new
 * extended BN model including the new parameter prior random variables. This class represents such as
 * extended model.
 *
 *
 * <p> For further details about how exponential family models are considered in this toolbox look at the following paper </p>
 * <p> <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>)
 * </p>
 *
 */
public class EF_LearningBayesianNetwork extends EF_Distribution {

    /** The list of distributions representing the model*/
    List<EF_ConditionalDistribution> distributionList;

    /** The parameter variables included in the model*/
    ParameterVariables parametersVariables;

    /**
     * Create a new EF_LearningBayesianNetwork object from a {@link DAG} object.
     * @param dag, a <code>DAG</code> object
     */
    public EF_LearningBayesianNetwork(DAG dag){

        parametersVariables = new ParameterVariables(dag.getVariables().getNumberOfVars());

        distributionList =
                dag.getVariables()
                        .getListOfVariables()
                        .stream()
                        .map(var -> var.getDistributionType().newEFConditionalDistribution(dag.getParentSet(var).getParents()).toExtendedLearningDistribution(parametersVariables))
                        .flatMap(listOfDist -> listOfDist.stream())
                        .sorted((a,b) -> a.getVariable().getVarID() - b.getVariable().getVarID())
                        .collect(Collectors.toList());

        this.naturalParameters = null;
        this.momentParameters = null;
    }


    /**
     * Create a new EF_LearningBayesianNetwork object from a list {@EF_ConditionalDistribution DAG} objects.
     * @param distributions, a list of <code>EF_ConditionalDistribution</code> objects.
     */
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

    /**
     * Returns the set of parameter variables included in the extended BN model.
     * @return A <code>ParameterVariables</code> object.
     */
    public ParameterVariables getParametersVariables() {
        return parametersVariables;
    }

    /**
     * Return a list of ConditionalDistribution distributions object by converting each of the EF_ConditionalDistribution
     * object of the model. This conversion also remove the parameter variables by replacing them with their expected value.
     * @return A list of <code>ConditionalDistribution</code> object.
     */
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

    /**
     * Returns the list of EF_ConditionalDistribution objects.
     * @return A list of <code>EF_ConditionalDistribution</code> objects.
     */
    public List<EF_ConditionalDistribution> getDistributionList() {
        return distributionList;
    }


    /**
     * Returns the EF_ConditionalDistribution object associated to a given variable.
     * @param var, a <code>Variable</code> object
     * @param <E>, the subtype of EF_ConditionalDistribution we are retrieving.
     * @return A <code>EF_ConditionalDistribution</code> object.
     */
    public <E extends EF_ConditionalDistribution> E getDistribution(Variable var) {
        return (E)distributionList.get(var.getVarID());
    }

    /**
     * Returns the EF_ConditionalDistribution object associated to a given variable.
     * @param var, a <code>Variable</code> object.
     * @param dist, a <code>EF_ConditionalDistribution</code> object.
     */
    public void setDistribution(Variable var, EF_ConditionalDistribution dist) {
        distributionList.set(var.getVarID(), dist);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void updateNaturalFromMomentParameters() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMomentFromNaturalParameters() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int sizeOfSufficientStatistics() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogNormalizer() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector createZeroVector() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }
}
