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
//TODO suff. stats for first form to be implemented (if required)

package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Map;

/**
 *
 * This class extends the abstract class {@link EF_ConditionalDistribution} and defines a conditional Multinomial Dirichlet distribution.
 * variable in exponential canonical form. It used for Bayesian learning tasks.
 *
 * <p> For further details about how exponential family models are considered in this toolbox, take a look at the following paper:
 * <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>) </p>
 */
public class EF_Multinomial_Dirichlet extends EF_ConditionalDistribution{

    /** Represents the conditioninig Dirichlet variable of this EF_Multinomial_Dirichlet distribution. */
    Variable dirichletVariable;

    /** Represents the number of states of the main multinomial variable. */
    int nOfStates;

    /**
     * Creates a new EF_Multinomial_Dirichlet distribution for given Multinomial and Dirichlet variables.
     * @param var a {@link Variable} object with a Multinomial distribution type.
     * @param dirichletVariable a {@link Variable} object with a Dirichlet distribution type.
     */
    public EF_Multinomial_Dirichlet(Variable var, Variable dirichletVariable) {

        if (!var.isMultinomial() && !var.isIndicator()) {
            throw new UnsupportedOperationException("Creating a Multinomial_Dirichlet EF distribution for a non-multinomial variable.");
        }
        if (!dirichletVariable.isDirichletParameter()) {
            throw new UnsupportedOperationException("Creating a Multinomial_Dirichlet EF distribution with a non-dirichlet variable.");
        }

        if (var.getNumberOfStates()!=dirichletVariable.getNumberOfStates()) {
            throw new UnsupportedOperationException("Creating a Multinomial_Dirichlet EF distribution with differetnt number of states.");
        }

        this.var=var;
        nOfStates = var.getNumberOfStates();
        this.dirichletVariable = dirichletVariable;
        this.parents = new ArrayList<>();
        this.parents.add(dirichletVariable);
    }

    /**
     * Returns the Dirichlet conditioning variable of this EF_Multinomial_Dirichlet distribution.
     * @return a {@link Variable} object.
     */
    public Variable getDirichletVariable() {
        return dirichletVariable;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public double getExpectedLogNormalizer(Map<Variable, MomentParameters> momentParents) {
        return 0.0;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {

        NaturalParameters naturalParameters = new ArrayVector(this.nOfStates);
        naturalParameters.copy(momentParents.get(this.dirichletVariable));

        return naturalParameters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {

        NaturalParameters naturalParameters = new ArrayVector(this.nOfStates);

        naturalParameters.copy(momentChildCoParents.get(this.var));

        return naturalParameters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends ConditionalDistribution> E toConditionalDistribution() {
        throw new UnsupportedOperationException("This method does not make sense. Parameter variables can not be converted. Use instead" +
                "public ConditionalDistribution toConditionalDistribution(Map<Variable, Vector> expectedValueParameterVariables);");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getExpectedLogNormalizer(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        throw new UnsupportedOperationException("No Implemented. This method is no really needed");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateNaturalFromMomentParameters() {
        throw new UnsupportedOperationException("No Implemented. EF_Multinomial_Dirichlet distribution should only be used for learning, use EF_Multinomial for inference.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMomentFromNaturalParameters() {
        throw new UnsupportedOperationException("No Implemented. EF_Multinomial_Dirichlet distribution should only be used for learning, use EF_Multinomial for inference.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        throw new UnsupportedOperationException("No Implemented. EF_Multinomial_Dirichlet distribution should only be used for learning, use EF_Multinomial for inference.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int sizeOfSufficientStatistics() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogNormalizer() {
        throw new UnsupportedOperationException("No Implemented. EF_Multinomial_Dirichlet distribution should only be used for learning, use EF_Multinomial for inference.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector createZeroVector() {
        throw new UnsupportedOperationException("No Implemented. EF_Multinomial_Dirichlet distribution should only be used for learning, use EF_Multinomial for inference.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics createInitSufficientStatistics() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConditionalDistribution toConditionalDistribution(Map<Variable, Vector> expectedValueParameterVariables) {
        Multinomial multinomial = new Multinomial(this.getVariable());

        Vector vector = expectedValueParameterVariables.get(this.dirichletVariable);

        for (int i = 0; i < this.nOfStates; i++) {
            multinomial.setProbabilityOfState(i,vector.get(i));

        }
        return multinomial;
    }
}
