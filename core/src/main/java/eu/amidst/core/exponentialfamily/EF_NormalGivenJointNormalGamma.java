


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
public class EF_NormalGivenJointNormalGamma extends EF_ConditionalDistribution{

    /** Represents the Gamma parameter variable for the variance value. */
    Variable normalGammaParameterVariable;

    /**
     * Creates a new EF_NormalGamma distribution.
     * @param var_ the main variable.
     * @param normalGammaParameterVariable the Normal-Gamma parameter variable .
     */
    public EF_NormalGivenJointNormalGamma(Variable var_, Variable normalGammaParameterVariable){
        this.var = var_;
        this.normalGammaParameterVariable = normalGammaParameterVariable;
        this.parents = new ArrayList<>();
        this.parents.add(normalGammaParameterVariable);

        if (!var.isNormal())
            throw new UnsupportedOperationException("Creating a Normal-Inverse-Gamma EF distribution for a non-gaussian child variable.");


        if(!this.normalGammaParameterVariable.isNormalGammaParameter()){
            throw new UnsupportedOperationException("Creating a Normal-Inverse-Gamma EF distribution for a non normal-gamma parent variable.");
        }


    }

    /**
     * Returns the NormalGamma parameter variable of the mean value.
     * @return a {@link Variable} object.
     */
    public Variable getNormalGammaParameterVariable() {
        return normalGammaParameterVariable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getExpectedLogNormalizer(Map<Variable, MomentParameters> momentParents) {
        double loggamma = momentParents.get(normalGammaParameterVariable).get(EF_JointNormalGamma.LOGGAMMA);
        double meansquareGamma = momentParents.get(normalGammaParameterVariable).get(EF_JointNormalGamma.MUSQUARE_GAMMA);

        return 0.5*meansquareGamma - 0.5*loggamma;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {


        double muGamma = momentParents.get(normalGammaParameterVariable).get(EF_JointNormalGamma.MU_GAMMA);
        double gamma = momentParents.get(normalGammaParameterVariable).get(EF_JointNormalGamma.GAMMA);

        NaturalParameters naturalParameters = new EF_Normal.ArrayVectorParameter(2);

        naturalParameters.set(0,muGamma/gamma);
        naturalParameters.set(1,gamma);

        return naturalParameters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {

        NaturalParameters naturalParameters = new ArrayVector(4);

        double X = momentChildCoParents.get(var).get(0);
        double XSquare = momentChildCoParents.get(var).get(1);

        naturalParameters.set(EF_JointNormalGamma.MU_GAMMA,X);
        naturalParameters.set(EF_JointNormalGamma.MUSQUARE_GAMMA,-0.5);
        naturalParameters.set(EF_JointNormalGamma.GAMMA,-0.5*XSquare);
        naturalParameters.set(EF_JointNormalGamma.LOGGAMMA,0.5);

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

        normal.setMean(expectedValueParameterVariables.get(this.normalGammaParameterVariable).get(0));
        normal.setVariance(1.0/expectedValueParameterVariables.get(this.normalGammaParameterVariable).get(1));

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
