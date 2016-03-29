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
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Map;

/**
 *
 * This class extends the abstract class {@link EF_ConditionalDistribution} and defines a conditional Normal distribution given a Normal and Gamma parameter
 * variables in exponential family canonical form. It used for Bayesian learning tasks.
 *
 * <p> For further details about how exponential family models are considered in this toolbox, take a look at the following paper:
 * <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>) </p>
 */
public class EF_NormalGivenIndependentNormalGamma extends EF_ConditionalDistribution{

    /** Represents the Normal parameter variable for the mean value. */
    Variable meanParameterVariable;

    /** Represents the Gamma parameter variable for the variance value. */
    Variable gammaParameterVariable;

    /**
     * Creates a new EF_NormalGamma distribution.
     * @param var_ the main variable.
     * @param mean the Normal parameter variable for the mean value.
     * @param gamma the Gamma parameter variable for the variance value.
     */
    public EF_NormalGivenIndependentNormalGamma(Variable var_, Variable mean, Variable gamma){
        this.var = var_;
        this.meanParameterVariable = mean;
        this.gammaParameterVariable = gamma;
        this.parents = new ArrayList<>();
        this.parents.add(mean);
        this.parents.add(gamma);

        if (!var.isNormal())
            throw new UnsupportedOperationException("Creating a Normal-Inverse-Gamma EF distribution for a non-gaussian child variable.");


        if(!meanParameterVariable.isNormalParameter()){
            throw new UnsupportedOperationException("Creating a Normal-Inverse-Gamma EF distribution for a non-gaussian parent variable.");
        }

        if(!gammaParameterVariable.isGammaParameter()){
            throw new UnsupportedOperationException("Creating a Normal-Inverse-Gamma EF distribution for a non-inverse-gamma parent variable.");
        }

    }

    /**
     * Returns the Normal parameter variable of the mean value.
     * @return a {@link Variable} object.
     */
    public Variable getMeanParameterVariable() {
        return meanParameterVariable;
    }

    /**
     * Returns the Gamma parameter variable of the variance value.
     * @return A <code>Variable</code> object.
     */
    public Variable getGammaParameterVariable() {
        return gammaParameterVariable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getExpectedLogNormalizer(Map<Variable, MomentParameters> momentParents) {
        double meansquare = momentParents.get(meanParameterVariable).get(1);

        double invVariance = momentParents.get(gammaParameterVariable).get(1);
        double logVar = momentParents.get(gammaParameterVariable).get(0);

        return 0.5*meansquare*invVariance - 0.5*logVar;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {


        double mean = momentParents.get(meanParameterVariable).get(0);
        double invVariance = momentParents.get(gammaParameterVariable).get(1);

        NaturalParameters naturalParameters = new EF_Normal.ArrayVectorParameter(2);

        naturalParameters.set(0,mean);
        naturalParameters.set(1,invVariance);

        return naturalParameters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {

        NaturalParameters naturalParameters = new ArrayVector(2);

        double X = momentChildCoParents.get(var).get(0);

        // Message to the mean (gaussian) variable
        if(meanParameterVariable == parent){
            double invVariance = momentChildCoParents.get(gammaParameterVariable).get(1);

/*
            naturalParameters.set(0,invVariance*X);
            naturalParameters.set(1,-0.5*invVariance);
*/

            naturalParameters = new EF_NormalParameter.ArrayVectorParameter(2);
            naturalParameters.set(0, X);
            naturalParameters.set(1, invVariance);

            // Message to the gamma variable
        }else{
            double XSquare = momentChildCoParents.get(var).get(1);

            double mean = momentChildCoParents.get(meanParameterVariable).get(0);
            double meanSquare = momentChildCoParents.get(meanParameterVariable).get(1);

            naturalParameters.set(0,0.5);
            naturalParameters.set(1,-0.5*(XSquare - 2*X*mean + meanSquare));
        }

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
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal for inference.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMomentFromNaturalParameters() {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal for inference.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal for inference.");
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
        return -0.5*Math.log(2*Math.PI);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogNormalizer() {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal for inference.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector createZeroVector() {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal for inference.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConditionalDistribution toConditionalDistribution(Map<Variable, Vector> expectedValueParameterVariables) {

        Normal normal = new Normal(this.var);

        normal.setMean(expectedValueParameterVariables.get(this.meanParameterVariable).get(0));
        normal.setVariance(1.0/ expectedValueParameterVariables.get(this.gammaParameterVariable).get(0));

        return normal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics createInitSufficientStatistics() {
        throw new UnsupportedOperationException();
    }

}
