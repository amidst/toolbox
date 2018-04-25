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

package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.utils.SparseVectorDefaultValue;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.DistributionTypeEnum;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.stateSpaceTypes.SparseFiniteStateSpace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 *
 * This class extends the abstract class {@link EF_UnivariateDistribution} and defines a Multinomial distribution in exponential family canonical form.
 *
 * <p> For further details about how exponential family models are considered in this toolbox, take a look at the following paper:
 * <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>) </p>
 */
public class EF_SparseMultinomial extends EF_UnivariateDistribution {

    /**
     * Creates a new EF_Multinomial distribution for a given {@link Variable} object.
     * @param var a {@link Variable} object with a Multinomial distribution type.
     */
    public EF_SparseMultinomial(Variable var) {

        if (!var.isSparseMultinomial()) {
            throw new UnsupportedOperationException("Creating a SparseMultinomial EF distribution for a non-multinomial variable.");
        }

        this.parents = new ArrayList<>();

        this.var=var;
        int nstates= var.getNumberOfStates();
        this.naturalParameters = this.createZeroNaturalParameters();
        this.momentParameters = this.createZeroMomentParameters();

        this.getSparseNaturalParameters().setDefaultValue(-Math.log(nstates));
        this.getSparseMomentParameters().setDefaultValue(1.0/nstates);

    }

    private SparseVectorDefaultValue getSparseNaturalParameters(){
        return ((SparseVectorDefaultValue)this.naturalParameters);
    }

    private SparseVectorDefaultValue getSparseMomentParameters(){
        return ((SparseVectorDefaultValue)this.momentParameters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogBaseMeasure(double val) {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogNormalizer() {
        double sum = 0;

        SparseVectorDefaultValue naturalSparse = this.getSparseNaturalParameters();

        for (Integer index : naturalSparse.getNonZeroEntries()) {
            sum+=Math.exp(this.naturalParameters.get(index));
        }

        sum += naturalSparse.getNDefaultValues()*Math.exp(naturalSparse.getDefaultValue());

        return Math.log(sum);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector createZeroVector() {
        return new SparseVectorDefaultValue(this.var.getNumberOfStates(),0.0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics getSufficientStatistics(double val) {
        SufficientStatistics vec = this.createZeroSufficientStatistics();
        vec.set((int) val%this.var.getNumberOfStates(), 1);
        return vec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector getExpectedParameters() {
        return this.momentParameters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogProbabilityOf(double val) {
        return this.naturalParameters.dotProduct(this.getSufficientStatistics(val)) + this.computeLogBaseMeasure(val) - this.computeLogNormalizer();
    }

    /**
     * {@inheritDoc}
     */
    @Override

    public void updateNaturalFromMomentParameters() {
        this.getSparseNaturalParameters().reset();

        for (Integer index : this.getSparseMomentParameters().getNonZeroEntries()) {
            this.naturalParameters.set(index, Math.log(this.momentParameters.get(index)));
        }

        this.getSparseNaturalParameters().setDefaultValue(Math.log(this.getSparseMomentParameters().getDefaultValue()));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fixNumericalInstability() {
        this.naturalParameters = (NaturalParameters)SparseVectorDefaultValue.logNormalize(this.getSparseNaturalParameters()); //To avoid numerical problems!
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMomentFromNaturalParameters() {

        this.getSparseMomentParameters().reset();
        for (Integer index : this.getSparseNaturalParameters().getNonZeroEntries()) {
            this.momentParameters.set(index, Math.exp(this.naturalParameters.get(index)));
        }
        this.getSparseMomentParameters().setDefaultValue(Math.exp(this.getSparseNaturalParameters().getDefaultValue()));

        SparseVectorDefaultValue.normalize(this.getSparseMomentParameters());


    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int sizeOfSufficientStatistics() {
        return this.var.getNumberOfStates();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_UnivariateDistribution deepCopy(Variable var) {
        EF_SparseMultinomial copy = new EF_SparseMultinomial(var);
        copy.getNaturalParameters().copy(this.getNaturalParameters());
        copy.getMomentParameters().copy(this.getMomentParameters());
        return copy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_UnivariateDistribution randomInitialization(Random random) {
        throw new UnsupportedOperationException("");

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Multinomial toUnivariateDistribution() {
        throw new UnsupportedOperationException("");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<EF_ConditionalDistribution> toExtendedLearningDistribution(ParameterVariables variables, String nameSuffix){

        Variable varDirichlet = variables.newVariable(this.var.getName()+"_DirichletParameter_"+nameSuffix+"_"+variables.getNumberOfVars(), DistributionTypeEnum.DIRICHLET_PARAMETER, new SparseFiniteStateSpace(this.var.getNumberOfStates()));

        EF_Dirichlet uni = varDirichlet.getDistributionType().newEFUnivariateDistribution();

        return Arrays.asList(new EF_SparseMultinomial_Dirichlet(this.var, varDirichlet), uni);
    }

    /*
        @Override
    public List<EF_ConditionalDistribution> toExtendedLearningDistribution(ParameterVariables variables, String nameSuffix){

        Variable varDirichlet = variables.newSparseDirichletParameter(this.var.getName()+"_DirichletParameter_"+nameSuffix+"_"+variables.getNumberOfVars(), this.var.getNumberOfStates());

        EF_SparseDirichlet uni = varDirichlet.getDistributionType().newEFUnivariateDistribution();

        return Arrays.asList(new EF_SparseMultinomial_SparseDirichlet(this.var, varDirichlet), uni);
    }


     */

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics createInitSufficientStatistics() {


        SufficientStatistics vector = this.createZeroSufficientStatistics();

        ((SparseVectorDefaultValue)vector).setDefaultValue(1.0/this.var.getNumberOfStates());

        return vector;
    }
}
