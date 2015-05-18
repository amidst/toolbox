/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

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
 * //TODO suff. stats for first form to be implemented (if required)
 * Created by ana@cs.aau.dk on 27/02/15.
 */
public class EF_Multinomial_Dirichlet extends EF_ConditionalDistribution{


    Variable dirichletVariable;
    int nOfStates;

    public EF_Multinomial_Dirichlet(Variable var, Variable dirichletVariable) {

        if (!var.isMultinomial()) {
            throw new UnsupportedOperationException("Creating a Multinomial_Dirichlet EF distribution for a non-multinomial variable.");
        }
        if (!dirichletVariable.isDirichletParameter()) {
            throw new UnsupportedOperationException("Creating a Multinomial_Dirichlet EF distribution with a non-dirichlet variable.");
        }

        this.var=var;
        nOfStates = var.getNumberOfStates();
        this.dirichletVariable = dirichletVariable;
        this.parents = new ArrayList<>();
        this.parents.add(dirichletVariable);

    }

    public Variable getDirichletVariable() {
        return dirichletVariable;
    }

    /**
     * Of the second form (message from all parents to X variable). Needed to calculate the lower bound.
     *
     * @param momentParents
     * @return
     */
    @Override
    public double getExpectedLogNormalizer(Map<Variable, MomentParameters> momentParents) {

        return 0.0;
    }

    /**
     * Of the second form (message from all parents to X variable).
     * @param momentParents
     * @return
     */
    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {

        NaturalParameters naturalParameters = new ArrayVector(this.nOfStates);
        naturalParameters.copy(momentParents.get(this.dirichletVariable));

        return naturalParameters;
    }

    /**
     * It is the message to one node to its parent @param parent, taking into account the suff. stat. if it is observed
     * or the moment parameters if not, and incorporating the message (with moment param.) received from all co-parents.
     * (Third form EF equations).
     *
     * @param parent
     * @param momentChildCoParents
     * @return
     */
    @Override
    public NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {

        NaturalParameters naturalParameters = new ArrayVector(this.nOfStates);

        naturalParameters.copy(momentChildCoParents.get(this.var));

        return naturalParameters;
    }

    @Override
    public <E extends ConditionalDistribution> E toConditionalDistribution() {
        throw new UnsupportedOperationException("This method does not make sense. Parameter variables can not be converted. Use instead" +
                "public ConditionalDistribution toConditionalDistribution(Map<Variable, Vector> expectedValueParameterVariables);");
    }

    @Override
    public double getExpectedLogNormalizer(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        throw new UnsupportedOperationException("No Implemented. This method is no really needed");
    }

    @Override
    public void updateNaturalFromMomentParameters() {
        throw new UnsupportedOperationException("No Implemented. EF_Multinomial_Dirichlet distribution should only be used for learning, use EF_Multinomial for inference.");
    }

    @Override
    public void updateMomentFromNaturalParameters() {
        throw new UnsupportedOperationException("No Implemented. EF_Multinomial_Dirichlet distribution should only be used for learning, use EF_Multinomial for inference.");
    }

    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        throw new UnsupportedOperationException("No Implemented. EF_Multinomial_Dirichlet distribution should only be used for learning, use EF_Multinomial for inference.");
    }

    @Override
    public int sizeOfSufficientStatistics() {
        return 0;
    }

    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        return 0;
    }

    @Override
    public double computeLogNormalizer() {
        throw new UnsupportedOperationException("No Implemented. EF_Multinomial_Dirichlet distribution should only be used for learning, use EF_Multinomial for inference.");
    }

    @Override
    public Vector createZeroedVector() {
        throw new UnsupportedOperationException("No Implemented. EF_Multinomial_Dirichlet distribution should only be used for learning, use EF_Multinomial for inference.");
    }

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
