/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class the abstract class {@link EF_Distribution} and defines a "Bayesian extended" model for a given Bayesian network.
 * This extended model is basically defined to deal with Bayesian learning approach.
 * In such settings, given a BN model, we need to consider new random variables acting as a prior distributions over the parameters
 * of this BN model. This results in a new extended BN model including the new parameter prior random variables.
 *
 * <p> For further details about how exponential family models are considered in this toolbox take a look at the following paper:
 * <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>) </p>
 *
 */
public class EF_LearningBayesianNetwork extends EF_Distribution {

    /** Represents the list of distributions representing this EF_LearningBayesianNetwork model. */
    List<EF_ConditionalDistribution> distributionList;

    /** Represents the parameter variables included in this EF_LearningBayesianNetwork model. */
    ParameterVariables parametersVariables;

    /** Represents the list of Variables which are not expended*/
    List<Variable> non_expand;
    /**
     * Creates a new EF_LearningBayesianNetwork object from a given {@link DAG} object.
     * @param dag a {@link DAG} object.
     */
    public EF_LearningBayesianNetwork(DAG dag){

        this.non_expand = new ArrayList<>();
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
     * Creates a new EF_LearningBayesianNetwork object from a given list {@link EF_ConditionalDistribution} objects.
     * @param distributions a list of {@link EF_ConditionalDistribution} objects.
     */
    public EF_LearningBayesianNetwork(List<EF_ConditionalDistribution> distributions){
        this(distributions,new ArrayList<>());
    }

    /**
     * Creates a new EF_LearningBayesianNetwork object from a given list {@link EF_ConditionalDistribution} objects.
     * @param distributions a list of {@link EF_ConditionalDistribution} objects.
     */

    /**
     * Creates a new EF_LearningBayesianNetwork object from a given list {@link EF_ConditionalDistribution} objects.
     * @param distributions a list of {@link EF_ConditionalDistribution} objects.
     * @param non_expand, a list of {@link Variable} objects which are not expanded.
     */
    public EF_LearningBayesianNetwork(List<EF_ConditionalDistribution> distributions, List<Variable> non_expand){

        this.non_expand = non_expand;
        parametersVariables = new ParameterVariables(distributions.size());

        distributionList =
                distributions
                        .stream()
                        .map(dist -> {
                            if (this.non_expand.contains(dist.getVariable()))
                                return Arrays.asList(dist);
                            else
                                return dist.toExtendedLearningDistribution(parametersVariables);
                        })
                        .flatMap(listOfDist -> listOfDist.stream())
                        .sorted((a,b) -> a.getVariable().getVarID() - b.getVariable().getVarID())
                        .collect(Collectors.toList());

        this.naturalParameters = null;
        this.momentParameters = null;
    }

    /**
     * Returns the set of parameter variables included in this EF_LearningBayesianNetwork model.
     * @return A {@link ParameterVariables} object.
     */
    public ParameterVariables getParametersVariables() {
        return parametersVariables;
    }


    /**
     * Returns the list of parameter variables included in this EF_LearningBayesianNetwork model.
     * @return A {@link java.util.List} of {@link Variable} objects.
     */
    public List<Variable> getListOfParametersVariables(){ return this.parametersVariables.getListOfParamaterVariables();}

    /**
     * Returns the list of non parameter variables included in this EF_LearningBayesianNetwork model.
     * @return A {@link java.util.List} of {@link Variable} objects.
     */
    public List<Variable> getListOfNonParameterVariables() {
        return this.distributionList.stream()
                .map(dist -> dist.getVariable())
                .filter(var -> !var.isParameterVariable())
                .collect(Collectors.toList());
    }

    /**
     * Converts the distributions of this EF_LearningBayesianNetwork model into a list of {@link ConditionalDistribution} objects.
     * This conversion also removes the parameter variables by replacing them with their expected value.
     * @return a {@code java.util.List} of {@link ConditionalDistribution} objects.
     */
    public List<ConditionalDistribution> toConditionalDistribution(){
        List<ConditionalDistribution> condDistList = new ArrayList<>();

        for (EF_ConditionalDistribution dist: distributionList) {
            if (dist.getVariable().isParameterVariable())
                continue;

            if (this.non_expand.contains(dist.getVariable())){
                condDistList.add(dist.toConditionalDistribution());
                continue;
            }

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
     * Returns the list of {@link EF_ConditionalDistribution} objects.
     * @return a {@code java.util.List} of {@link EF_ConditionalDistribution} objects.
     */
    public List<EF_ConditionalDistribution> getDistributionList() {
        return distributionList;
    }

    /**
     * Returns the {@link EF_ConditionalDistribution} object associated with a given variable.
     * @param var a {@link Variable} object.
     * @param <E> the subtype of {@link EF_ConditionalDistribution} we are retrieving.
     * @return a {@link EF_ConditionalDistribution} object.
     */
    public <E extends EF_ConditionalDistribution> E getDistribution(Variable var) {
        return (E)distributionList.get(var.getVarID());
    }

    /**
     * Sets the {@link EF_ConditionalDistribution} for a given variable.
     * @param var a {@link Variable} object.
     * @param dist the {@link EF_ConditionalDistribution} object to be set.
     */
    public void setDistribution(Variable var, EF_ConditionalDistribution dist) {
        distributionList.set(var.getVarID(), dist);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateNaturalFromMomentParameters() {
        throw new UnsupportedOperationException("This method does not apply in this case!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMomentFromNaturalParameters() {
        throw new UnsupportedOperationException("This method does not apply in this case!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        throw new UnsupportedOperationException("This method does not apply in this case!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int sizeOfSufficientStatistics() {
        throw new UnsupportedOperationException("This method does not apply in this case!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        throw new UnsupportedOperationException("This method does not apply in this case!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogNormalizer() {
        throw new UnsupportedOperationException("This method does not apply in this case!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector createZeroVector() {
        throw new UnsupportedOperationException("This method does not apply in this case!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics createInitSufficientStatistics(){
        return new CompoundVector(this.distributionList.stream().map(w-> w.createInitSufficientStatistics()).collect(Collectors.toList()));
    }
}
