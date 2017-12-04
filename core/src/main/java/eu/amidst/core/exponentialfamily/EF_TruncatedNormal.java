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

import eu.amidst.core.distribution.Normal;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 *
 * This class extends the abstract class {@link EF_UnivariateDistribution} and defines a Normal distribution in exponential family canonical form.
 *
 * <p> For further details about how exponential family models are considered in this toolbox, take a look at the following paper:
 * <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>) </p>
 */
public class EF_TruncatedNormal extends EF_UnivariateDistribution {


    public static final int EXPECTED_MEAN = 0;
    public static final int EXPECTED_SQUARE = 1;

    public static final int INDEX_MEAN = 0;
    public static final int INDEX_PRECISION = 1;

    private static final double LIMIT = 100000;

    //It defines the interval upper endpoint of the distribution
    private double upperInterval = 1;

    //It defines the interval lower endpoint of the distribution
    private double lowerInterval = 0;

    /**
     * Creates a new EF_Normal distribution for a given variable.
     *
     * @param var1 a {@link Variable} object with a Normal distribution type.
     */
    public EF_TruncatedNormal(Variable var1) {
        if (!var1.isNormal() && !var1.isParameterVariable()) {
            throw new UnsupportedOperationException("Creating a Gaussian EF distribution for a non-gaussian variable.");
        }

        this.parents = new ArrayList();

        this.var = var1;
        this.naturalParameters = new ArrayVectorParameter(2);
        this.momentParameters = new ArrayVector(2);

        this.momentParameters.set(EXPECTED_MEAN, 0);
        this.momentParameters.set(EXPECTED_SQUARE, 1);
        this.setMomentParameters(momentParameters);
    }


    public double getMean() {
        return this.naturalParameters.get(INDEX_MEAN);
    }

    public double getPrecision() {
        return this.naturalParameters.get(INDEX_PRECISION);
    }

    public double getUpperInterval() {
        return upperInterval;
    }

    public double getLowerInterval() {
        return lowerInterval;
    }

    /**
     * Set the interval upper endpoint of the truncated distribution.
     * @param upperInterval
     */
    public void setUpperInterval(double upperInterval) {
        this.upperInterval = upperInterval;
    }

    /**
     * Set the interval lower endpoint of the truncated distribution.
     * @param lowerInterval
     */
    public void setLowerInterval(double lowerInterval) {
        this.lowerInterval = lowerInterval;
    }

    public void setNaturalWithMeanPrecision(double mean, double precision) {
        this.naturalParameters.set(INDEX_MEAN, mean);
        this.naturalParameters.set(INDEX_PRECISION, precision);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fixNumericalInstability() {

        /*
        if (this.getPrecision()>LIMIT) {
            this.naturalParameters.set(INDEX_PRECISION,LIMIT);
        }

        if (this.naturalParameters.get(1)<(-0.5*LIMIT)){ //To avoid numerical problems!
            double x = -0.5*this.naturalParameters.get(0)/this.getNaturalParameters().get(1);
            this.naturalParameters.set(0,x*LIMIT);
            this.naturalParameters.set(1,-0.5*LIMIT);
        }*/
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogBaseMeasure(double val) {
        return -0.5 * Math.log(2 * Math.PI);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogNormalizer() {
        double beta = (this.upperInterval - this.getMean()) * this.getPrecision();
        double alpha = (this.lowerInterval - this.getMean()) * this.getPrecision();
        double normalization_constant = 1/2 * org.apache.commons.math3.special.Erf.erf(beta/Math.sqrt(2), alpha/Math.sqrt(2));

        return -0.5 * Math.log(this.getPrecision()) + 0.5 * this.getPrecision() * Math.pow(this.getMean(), 2) - Math.log(normalization_constant);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector createZeroVector() {
        return new ArrayVector(2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalParameters createZeroNaturalParameters() {
        return new ArrayVectorParameter(2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics createInitSufficientStatistics() {

        SufficientStatistics vector = this.createZeroSufficientStatistics();

        double mean = 0;
        double meansquare = 0.1;

        vector.set(0, mean);
        vector.set(1, meansquare);

        return vector;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics getSufficientStatistics(double val) {
        SufficientStatistics vec = this.createZeroSufficientStatistics();
        vec.set(EXPECTED_MEAN, val);
        vec.set(EXPECTED_SQUARE, val * val);
        return vec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector getExpectedParameters() {
        Vector vec = new ArrayVectorParameter(1);
        vec.set(0, this.momentParameters.get(0));
        return vec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogProbabilityOf(double val) {
        throw new UnsupportedOperationException("No implemented yet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_UnivariateDistribution deepCopy(Variable var) {

        EF_TruncatedNormal copy = new EF_TruncatedNormal(var);
        copy.getNaturalParameters().copy(this.getNaturalParameters());
        copy.getMomentParameters().copy(this.getMomentParameters());

        return copy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_UnivariateDistribution deepCopy() {
        return this.deepCopy(this.var);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_UnivariateDistribution randomInitialization(Random random) {

        double mean = random.nextGaussian() * 10;
        double var = random.nextDouble() * 10 + 1;

        this.setNaturalWithMeanPrecision(mean, 1 / var);

        this.fixNumericalInstability();

        this.updateMomentFromNaturalParameters();

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Normal toUnivariateDistribution() {

        Normal normal = new Normal(this.getVariable());

        normal.setMean(this.getMean());
        normal.setVariance(1/this.getPrecision());

        return normal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateNaturalFromMomentParameters() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMomentFromNaturalParameters() {

        double beta = (this.upperInterval - this.getMean()) * this.getPrecision();
        double alpha = (this.lowerInterval - this.getMean()) * this.getPrecision();
        double normalization_constant = 1/2 * org.apache.commons.math3.special.Erf.erf(beta/Math.sqrt(2), alpha/Math.sqrt(2));

        NormalDistributionImpl aux = new NormalDistributionImpl(0,1);

        double newExpectedX = this.getMean() + (aux.density(alpha) - aux.density(beta))/normalization_constant * 1/this.getPrecision();

        this.momentParameters.set(EXPECTED_MEAN, newExpectedX);

//        double expt_square = 1 / this.getPrecision() + Math.pow(this.getMean(), 2);
//

        double newVariance = Math.pow(1/getPrecision(),2) * (1 + (alpha * (aux.density(alpha) - beta * aux.density(beta)))/normalization_constant - Math.pow( (aux.density(alpha) - aux.density(beta))/normalization_constant, 2));
        double newExpectedXSquared = newVariance + Math.pow(newExpectedX,2);

        if (newExpectedXSquared <= 0)
            throw new IllegalStateException("Zero or Negative expected square value");

        this.momentParameters.set(EXPECTED_SQUARE, newExpectedXSquared);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        return this.getSufficientStatistics(data.getValue(this.var));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int sizeOfSufficientStatistics() {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        return this.computeLogBaseMeasure(dataInstance.getValue(this.var));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<EF_ConditionalDistribution> toExtendedLearningDistribution(ParameterVariables variables, String nameSuffix) {
        List<EF_ConditionalDistribution> conditionalDistributions = new ArrayList<>();

        Variable varNormalGamma = variables.newNormalGamma(this.var.getName()+"_NormalGamma_Parameter_"+nameSuffix+"_"+variables.getNumberOfVars());

        conditionalDistributions.add(varNormalGamma.getDistributionType().newEFUnivariateDistribution());


        EF_NormalGivenJointNormalGamma dist = new EF_NormalGivenJointNormalGamma(this.var,varNormalGamma);
        conditionalDistributions.add(dist);

        return conditionalDistributions;
    }

/*
    public List<EF_ConditionalDistribution> toExtendedLearningDistribution(ParameterVariables variables, String nameSuffix) {
        List<EF_ConditionalDistribution> conditionalDistributions = new ArrayList<>();

        Variable varGamma = variables.newGammaParameter(this.var.getName()+"_Gamma_Parameter_"+nameSuffix+"_"+variables.getNumberOfVars());
        Variable normalMean = variables.newGaussianParameter(this.var.getName() + "_Mean_Parameter_"+nameSuffix+"_"+variables.getNumberOfVars());

        conditionalDistributions.add(varGamma.getDistributionType().newEFUnivariateDistribution());

        conditionalDistributions.add(normalMean.getDistributionType().newEFUnivariateDistribution());

        EF_NormalGivenIndependentNormalGamma dist = new EF_NormalGivenIndependentNormalGamma(this.var, normalMean, varGamma);
        conditionalDistributions.add(dist);

        return conditionalDistributions;
    }
*/

    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {
        NaturalParameters out = new ArrayVectorParameter(2);
        out.copy(this.getNaturalParameters());
        return out;
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public double kl(NaturalParameters naturalParameters, double logNormalizer) {
        double meanQ = naturalParameters.get(INDEX_MEAN);
        double precisionQ = naturalParameters.get(INDEX_PRECISION);

        double kl =  0.5*Math.log(this.getPrecision()) - 0.5*Math.log(precisionQ) + 0.5*precisionQ/this.getPrecision()
                + 0.5*precisionQ*Math.pow(this.getMean()-meanQ,2) -0.5;

        //double factor = precisionQ/this.getPrecision();
        //double kl = 0.5*(-Math.log(factor) + factor + precisionQ*Math.pow(this.getMean()-meanQ,2) -1);

        if (Double.isNaN(kl)){
            throw new IllegalStateException("NaN KL");
        }

        if (kl<0) {
            kl=0;
        }


        return kl;
    }

    /**
         * This class implements the interfaces {@link MomentParameters}, {@link NaturalParameters}, and {@link SufficientStatistics}.
         * It handles some array vector utility methods.
         */
    public static class ArrayVectorParameter implements MomentParameters, NaturalParameters, SufficientStatistics, Serializable {

        /** Represents the serial version ID for serializing the object. */
        private static final long serialVersionUID = -3436599636425587512L;

        /** Represents an array of {@code double}. */
        private double[] array;

        /**
         * Creates a new array vector given an {@code int} size.
         * @param size the size of the array vector.
         */
        public ArrayVectorParameter(int size){
            this.array = new double[size];
        }

        /**
         * Creates a new array vector given an array of {@code double}.
         * @param vec an array of {@code double}.
         */
        public ArrayVectorParameter(double[] vec){
            this.array=vec;
        }


        /**
         * Converts this ArrayVector to an array of {@code double}.
         * @return an array of {@code double}.
         */
        public double[] toArray(){
            return this.array;
        }

        /**
         * Copies the input source vector to this ArrayVector.
         * @param vector an input source ArrayVector object.
         */
        public void copy(ArrayVectorParameter vector){
            if (vector.size()!=vector.size())
                throw new IllegalArgumentException("Vectors with different sizes");
            System.arraycopy(vector.toArray(),0,this.array,0,vector.toArray().length);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double get(int i){
            return this.array[i];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void set(int i, double val){
            this.array[i]=val;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int size(){
            return this.array.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void sum(Vector vector) {
            if (this.size()!=vector.size())
                throw new IllegalArgumentException("Vectors has different sizes");

            double mean1 = this.array[INDEX_MEAN];
            double precision1 = this.array[INDEX_PRECISION];

            double mean2 = vector.get(INDEX_MEAN);
            double precision2 = vector.get(INDEX_PRECISION);

            if (precision1+precision2!=0) {

                double newmean = (precision1 / (precision1 + precision2)) * mean1 + (precision2 / (precision1 + precision2)) * mean2;

                double newprecision = precision1 + precision2;

                this.array[INDEX_MEAN] = newmean;
                this.array[INDEX_PRECISION] = newprecision;
            }

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void substract(Vector vector) {
            if (this.size()!=vector.size())
                throw new IllegalArgumentException("Vectors has different sizes");

            double mean1 = this.array[INDEX_MEAN];
            double precision1 = this.array[INDEX_PRECISION];

            double mean2 = vector.get(INDEX_MEAN);
            double precision2 = vector.get(INDEX_PRECISION);

            if (precision1-precision2!=0) {
                double newmean = (precision1 / (precision1 - precision2)) * mean1 - (precision2 / (precision1 - precision2)) * mean2;

                double newprecision = precision1 - precision2;

                this.array[INDEX_MEAN] = newmean;
                this.array[INDEX_PRECISION] = newprecision;
            }else{
                this.array[INDEX_MEAN] = 0;
                this.array[INDEX_PRECISION] = 0;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void copy(Vector vector){
            if (this.size()!=vector.size())
                throw new IllegalArgumentException("Vectors has different sizes");

            if (!vector.getClass().isAssignableFrom(ArrayVectorParameter.class))
                throw new IllegalArgumentException("Not compatible class");

            this.copy((ArrayVectorParameter)vector);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void divideBy(double val){
            throw new UnsupportedOperationException("");
        }

        /**
         /**
         * {@inheritDoc}
         */
        @Override
        public void multiplyBy(double val){
            this.array[INDEX_PRECISION] *= val;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double dotProduct(Vector vector) {
            double mean1 = this.array[INDEX_MEAN];
            double precision1 = this.array[INDEX_PRECISION];

            return precision1*mean1*vector.get(0) - 0.5*precision1*vector.get(1);
        }

    }

}