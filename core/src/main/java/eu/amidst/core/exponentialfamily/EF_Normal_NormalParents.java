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

import eu.amidst.core.distribution.ConditionalLinearGaussian;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import org.apache.commons.math.linear.*;

import java.io.Serializable;
import java.util.*;
import java.util.stream.IntStream;

/**
 *
 * This class extends the abstract class {@link EF_ConditionalDistribution} and defines a Conditional Linear Gaussian (CLG) distribution in exponential family canonical form.
 *
 * <p> For further details about how exponential family models are considered in this toolbox, take a look at the following paper:
 * <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>) </p>
 */
public class EF_Normal_NormalParents extends EF_ConditionalDistribution  {

    /** Represents the size of the sufficient statistics in this EF_Normal_NormalParents distribution. */
    int sizeSS;

    /** Represents the number of parents. */
    int nOfParents;


    /** Represents the vector of beta values (without beta0)*/
    double[] betas;

    /** Represents the beta0 value*/
    double beta0;

    /** Represents the variance */
    double variance;

    /**
     * Creates a new EF_Normal_NormalParents distribution.
     * @param var_ the main variable.
     * @param parents_ the list of Normal parent variables.
     */
    public EF_Normal_NormalParents(Variable var_, List<Variable> parents_) {

        this.var = var_;
        this.parents = parents_;

        if (!var_.isNormal())
            throw new UnsupportedOperationException("Creating a Normal|Normal EF distribution for a non-gaussian child variable.");

        for (Variable v : parents) {
            if (!v.isNormal())
                throw new UnsupportedOperationException("Creating a Normal|Normal EF distribution for a non-gaussian parent variable.");
        }

        nOfParents = parents.size();
        sizeSS = nOfParents*nOfParents + 3 * nOfParents + 2 ; // (n+1)^2 (covariance matrix of YX) + n (E(Y)) + 1 (E(X))

        this.var=var_;
        this.momentParameters = null;//this.createEmtpyCompoundVector();
        this.naturalParameters = null;//this.createEmtpyCompoundVector();

        this.beta0 = 0;
        this.variance = 0;
        this.betas = new double[parents.size()];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateNaturalFromMomentParameters() {

        /*
         * First step: means and convariances
         */
        CompoundVector globalMomentParam = (CompoundVector)this.momentParameters;
        double mean_X = globalMomentParam.getXYbaseMatrix().getEntry(0);
        RealVector mean_Y = globalMomentParam.getTheta_beta0BetaRV();

        double cov_XX = globalMomentParam.getcovbaseMatrix().getEntry(0, 0) - mean_X*mean_X;
        RealMatrix cov_YY = globalMomentParam.getcovbaseMatrix().getSubMatrix(1, nOfParents, 1, nOfParents).
                subtract(mean_Y.outerProduct(mean_Y));
        RealVector cov_XY = globalMomentParam.getcovbaseMatrix().getSubMatrix(0, 0, 1, nOfParents).getRowVector(0).
                subtract(mean_Y.mapMultiply(mean_X));
        //RealVector cov_YX = cov_XY; //outerProduct transposes the vector automatically

        /*
         * Second step: betas and variance
         */
        RealMatrix cov_YYInverse = new LUDecompositionImpl(cov_YY).getSolver().getInverse();
        RealVector beta = cov_YYInverse.preMultiply(cov_XY);

        this.betas = new double[beta.getDimension()];
        for (int i = 0; i<beta.getDimension(); i++){
            this.betas[i] = beta.getEntry(i);
        }
        this.beta0 = mean_X - beta.dotProduct(mean_Y);
        this.variance = cov_XX - beta.dotProduct(cov_XY);

    }

    public void setBetas(double[] betas) {
        this.betas = betas;
    }

    public void setBeta0(double beta0) {
        this.beta0 = beta0;
    }

    public void setVariance(double variance) {
        this.variance = variance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMomentFromNaturalParameters() {
        //throw new UnsupportedOperationException("Method not implemented yet!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalParameters getNaturalParameters(){

        CompoundVector naturalParametersCompound = this.createEmtpyCompoundVector();


        /* 1) theta_0  */
        double theta_0 = beta0 / variance;
        naturalParametersCompound.setThetaBeta0_NatParam(theta_0);

        /* 2) theta_0Theta */
        double variance2Inv =  1.0/(2*variance);
        //IntStream.range(0,coeffParents.length).forEach(i-> coeffParents[i]*=(beta_0*variance2Inv));
        double[] theta0_beta = Arrays.stream(betas).map(w->-w*beta0/variance).toArray();
        naturalParametersCompound.setThetaBeta0Beta_NatParam(theta0_beta);

        /* 3) theta_Minus1 */
        double theta_Minus1 = -variance2Inv;

        /* 4) theta_beta & 5) theta_betaBeta */
        naturalParametersCompound.setThetaCov_NatParam(theta_Minus1,betas, variance2Inv);

        this.naturalParameters = naturalParametersCompound;

        return this.naturalParameters;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        CompoundVector vectorSS = this.createEmtpyCompoundVector();

        double[] Xarray = {data.getValue(this.var)};

        double[] Yarray = this.parents.stream()
                .mapToDouble(w->data.getValue(w))
                .toArray();
        RealVector XYRealVector = new ArrayRealVector(Xarray,Yarray);
        vectorSS.setXYbaseVector(XYRealVector);

        RealMatrix covRealmatrix = XYRealVector.outerProduct(XYRealVector);

        vectorSS.setcovbaseVector(covRealmatrix);

        return vectorSS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int sizeOfSufficientStatistics() {
        return sizeSS;
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
        return (beta0*beta0)/(2*variance) + 0.5*Math.log(variance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompoundVector createZeroVector() {
        return new CompoundVector(nOfParents);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics createInitSufficientStatistics() {
        CompoundVector vectorSS = this.createEmtpyCompoundVector();

        double[] Xarray = {0.0};

        double[] Yarray = this.parents.stream()
                .mapToDouble(w-> 0.0)
                .toArray();
        RealVector XYRealVector = new ArrayRealVector(Xarray,Yarray);
        vectorSS.setXYbaseVector(XYRealVector);

        RealMatrix covRealmatrix = new Array2DRowRealMatrix(Yarray.length + 1,Yarray.length + 1);



        //We perform the "laplace" correction in that way to break symmetric covariance matrixes.
        Random rand = new Random(0);
        for (int i = 0; i < Yarray.length + 1; i++) {
            for (int j = 0; j < Yarray.length + 1; j++) {
                covRealmatrix.addToEntry(i,j,rand.nextDouble()+0.01);
            }
        }
        //covRealmatrix = covRealmatrix.scalarAdd(1.0);

        vectorSS.setcovbaseVector(covRealmatrix);

        return vectorSS;
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
    public double getExpectedLogNormalizer(Map<Variable, MomentParameters> momentParents) {
        int nOfBetas = this.betas.length;

        //From Beta_0, Beta, gamma and Y parents to variable X.
        double beta0Squared;
        double invVariance;
        double dotProductBetaY = 0;
        double sumSquaredMoments=0;
        double sumSquaredMeanMoments=0;

        for (int i = 0; i < nOfBetas; i++) {
            dotProductBetaY += momentParents.get(this.parents.get(i)).get(0) *
                    betas[i];
            sumSquaredMoments += betas[i]*betas[i]*
                    momentParents.get(this.parents.get(i)).get(1);

            sumSquaredMeanMoments += Math.pow(betas[i] *
                    momentParents.get(this.parents.get(i)).get(0), 2);
        }

        beta0Squared = beta0*beta0;
        invVariance = 1/variance;
        double logVar = Math.log(variance);

        return -0.5*logVar +  0.5*invVariance*(beta0Squared + dotProductBetaY*dotProductBetaY - sumSquaredMeanMoments + sumSquaredMoments + 2*beta0*dotProductBetaY);
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {
        int nOfBetas = this.betas.length;

        double dotProductBetaY = 0;

        for (int i = 0; i < nOfBetas; i++) {
            dotProductBetaY += momentParents.get(this.parents.get(i)).get(0) *
                    betas[i];
        }

        NaturalParameters naturalParameters = new EF_Normal.ArrayVectorParameter(2);

        naturalParameters.set(0, this.beta0 + dotProductBetaY);
        naturalParameters.set(1, 1/variance);

        return naturalParameters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {

        int parentID=this.parents.indexOf(parent);

        if (betas[parentID]==0){
            NaturalParameters naturalParameters = new EF_Normal.ArrayVectorParameter(2);
            naturalParameters.set(0,0);
            naturalParameters.set(1,0);

            return naturalParameters;
        }



        int nOfBetas = this.betas.length;

        double dotProductBetaY = 0;


        for (int i = 0; i < nOfBetas; i++) {
            dotProductBetaY += momentChildCoParents.get(this.parents.get(i)).get(0) *
                    betas[i];
        }


        double X = momentChildCoParents.get(var).get(0);

        double beta_iSquared = betas[parentID]*betas[parentID];
        double beta_i = betas[parentID];
        double Y_i = momentChildCoParents.get(this.parents.get(parentID)).get(0);
        double invVariance = 1/variance;


        double factor = beta_i/beta_iSquared;

        double mean = factor*(-beta0 + X - (dotProductBetaY-beta_i*Y_i));
        double precision = beta_iSquared*invVariance;

        NaturalParameters naturalParameters = new EF_Normal.ArrayVectorParameter(2);
        naturalParameters.set(0,mean);
        naturalParameters.set(1,precision);

        return naturalParameters;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ConditionalLinearGaussian toConditionalDistribution() {
        ConditionalLinearGaussian normal_normal = new ConditionalLinearGaussian(this.getVariable(), this.getConditioningVariables());

        normal_normal.setIntercept(beta0);
        normal_normal.setCoeffParents(Arrays.copyOfRange(betas, 0, betas.length));
        normal_normal.setVariance(this.variance);

        return normal_normal;
    }

    /**
     * Creates an empty compound parameter vector.
     * @return a {@link CompoundVector} object.
     */
    public CompoundVector createEmtpyCompoundVector() {
        return new CompoundVector(nOfParents);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<EF_ConditionalDistribution> toExtendedLearningDistribution(ParameterVariables variables, String nameSuffix) {
        List<EF_ConditionalDistribution> conditionalDistributions = new ArrayList<>();

        Variable varGamma = variables.newGammaParameter(this.var.getName() + "_Gamma_Parameter_"+nameSuffix+"_" + variables.getNumberOfVars());

        conditionalDistributions.add(varGamma.getDistributionType().newEFUnivariateDistribution());

        Variable normalBeta0 = variables.newGaussianParameter(this.var.getName() + "_alpha_Parameter_"+nameSuffix+"_"+variables.getNumberOfVars());


        conditionalDistributions.add(normalBeta0.getDistributionType().newEFUnivariateDistribution());

        List<Variable> betas = new ArrayList<>();
        for (int i = 0; i < this.parents.size(); i++) {
            Variable normalBetai = variables.newGaussianParameter(this.var.getName() + "_beta"+ (i+1) + "_Parameter_"+nameSuffix+"_"+variables.getNumberOfVars());
            betas.add(normalBetai);
            conditionalDistributions.add(normalBetai.getDistributionType().newEFUnivariateDistribution());
        }


        EF_Normal_Normal_Gamma condDist = new EF_Normal_Normal_Gamma(this.var,this.parents,normalBeta0, betas, varGamma);

        conditionalDistributions.add(condDist);

        return conditionalDistributions;
    }

    public static class CompoundVector implements SufficientStatistics, MomentParameters, NaturalParameters, Serializable {

        /** Represents the serial version ID for serializing the object. */
        private static final long serialVersionUID = -3436599636425587512L;

        int size;
        int nOfParents;
        RealVector XYbaseVector;
        RealMatrix covbaseVector;

        public CompoundVector(int nOfParents_) {

            nOfParents = nOfParents_;

            // E(XY) - Index {0, ..., noParents+1}
            XYbaseVector = new ArrayRealVector(nOfParents+1);

            // E(XY) - Index (i * (noParents+1) ) + j (+ noParents + 1)
            covbaseVector = new Array2DRowRealMatrix(nOfParents+1,nOfParents+1);

            size = nOfParents*nOfParents + 3 * nOfParents + 2;

        }

        public void setXYbaseVector(RealVector XYbaseVector_){
            XYbaseVector = XYbaseVector_;
        }

        public void setcovbaseVector(RealMatrix covbaseVector_){
            covbaseVector = covbaseVector_;
        }

        public void setMatrixByPosition(int position, RealMatrix vec) {
            switch (position){
                case 0: XYbaseVector = vec.getRowVector(0);
                case 1: covbaseVector = vec;
                default:
                    throw new IndexOutOfBoundsException("There are only two components (indexes 0 for XY and 1 for the" +
                            " cov. matrix) in a normal|normal EF distribution.");

            }
        }

        public RealVector getXYbaseMatrix(){
            return XYbaseVector;
        }

        public RealMatrix getcovbaseMatrix(){
            return covbaseVector;
        }

        public RealMatrix getMatrixByPosition(int position) {
            switch (position){
                case 0: return new Array2DRowRealMatrix(XYbaseVector.getData());//column RealMatrix
                case 1: return covbaseVector;
                default:
                    throw new IndexOutOfBoundsException("There are only two components (indexes 0 for XY and 1 for the" +
                            " cov. matrix) in a normal|normal EF distribution.");

            }
        }

        private static boolean isBetween(int x, int lower, int upper) {
            return lower <= x && x <= upper;
        }

        @Override
        public double get(int i) {
            if(i==0) {
                return XYbaseVector.getEntry(0);
            }else if(isBetween(i,1,nOfParents)) {
                return XYbaseVector.getEntry(i);
            }else{
                i = i-(nOfParents+1);
                int row = i/(nOfParents+1);
                int column = i - row*(nOfParents+1);
                return covbaseVector.getEntry(row, column);
            }
        }

        @Override
        public void set(int i, double val) {
            if(i==0) {
                XYbaseVector.setEntry(0, val);
            }else if(isBetween(i,1,nOfParents)) {
                XYbaseVector.setEntry(i, val);
            }else{
                i = i-(nOfParents+1);
                int row = i/(nOfParents+1);
                int column = i - row*(nOfParents+1);
                covbaseVector.setEntry(row, column, val);
            }
        }

        public void setThetaBeta0_NatParam(double val){
            XYbaseVector.setEntry(0,val);
        }

        public void setThetaBeta0Beta_NatParam(double[] val) {
            XYbaseVector.setSubVector(1,val);
        }

        public void setThetaCov_NatParam(double theta_Minus1, double[] beta, double variance2Inv){
            double[] theta_Minus1array = {theta_Minus1};
            double[] theta_beta = Arrays.stream(beta).map(w -> w * variance2Inv).toArray();
            RealVector covXY = new ArrayRealVector(theta_Minus1array, theta_beta);
            covbaseVector.setColumnVector(0, covXY);
            covbaseVector.setRowVector(0, covXY);

            RealVector betaRV = new ArrayRealVector(beta);
            RealMatrix theta_betaBeta = betaRV.outerProduct(betaRV).scalarMultiply(-variance2Inv);
            covbaseVector.setSubMatrix(theta_betaBeta.getData(),1,1);
        }

        public double getTheta_beta0(){
            return XYbaseVector.getEntry(0);
        }

        public double[] getTheta_beta0Beta(){
            return getXYbaseMatrix().getSubVector(1,nOfParents).toArray();

        }

        public double getTheta_Minus1(){
            return getcovbaseMatrix().getEntry(0,0);
        }

        public double[] getTheta_Beta(){
            return getcovbaseMatrix().getSubMatrix(0,0,1,nOfParents).getRow(0);
        }

        public double[][] getTheta_BetaBeta(){
            return getcovbaseMatrix().getSubMatrix(1,nOfParents,1,nOfParents).getData();
        }


        public RealVector getTheta_beta0BetaRV(){
            return getXYbaseMatrix().getSubVector(1,nOfParents);
        }

        public RealVector getTheta_BetaRV(){
            return getcovbaseMatrix().getSubMatrix(0,0,1,nOfParents).getRowVector(0);
        }

        public RealMatrix getTheta_BetaBetaRM(){
            return getcovbaseMatrix().getSubMatrix(1, nOfParents, 1, nOfParents);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void sum(Vector vector) {
            this.sum((CompoundVector)vector);
        }

        @Override
        public void copy(Vector vector) {
            this.copy((CompoundVector)vector);
        }

        @Override
        public void divideBy(double val) {
            //TODO Try to find a self based operation for efficiency
            XYbaseVector.mapDivideToSelf(val);
            covbaseVector = covbaseVector.scalarMultiply(1.0/val);
        }

        @Override
        public double dotProduct(Vector vec) {
            return this.dotProduct((CompoundVector)vec);
        }


        public double dotProduct(CompoundVector vec) {
            double result = this.getXYbaseMatrix().dotProduct(vec.getXYbaseMatrix()); //theta1
            result += IntStream.range(0,nOfParents+1).mapToDouble(p ->
                    this.getcovbaseMatrix().getRowVector(p).dotProduct(vec.getcovbaseMatrix().getRowVector(p))).sum();
            return result;
        }

        public void copy(CompoundVector vector) {
            XYbaseVector = vector.getXYbaseMatrix().copy();
            covbaseVector = vector.getcovbaseMatrix().copy();

        }

        public void sum(CompoundVector vector) {
            //TODO Try to find a self based operation for efficiency
            XYbaseVector = XYbaseVector.add(vector.getXYbaseMatrix());
            covbaseVector = covbaseVector.add(vector.getcovbaseMatrix());
        }

    }

}


