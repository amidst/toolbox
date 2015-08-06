/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.ConditionalLinearGaussian;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import org.apache.commons.math.linear.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 08/12/14.
 */
public class EF_Normal_NormalParents extends EF_ConditionalDistribution  {

    int sizeSS;

    int nOfParents;

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
        this.momentParameters = this.createEmtpyCompoundVector();
        this.naturalParameters = this.createEmtpyCompoundVector();
    }

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


        double beta_0 = mean_X - beta.dotProduct(mean_Y);
        double variance = cov_XX - beta.dotProduct(cov_XY);


        /*
         * Third step: natural parameters (5 in total)
         */

        /*
         * 1) theta_0
         */
        double theta_0 = beta_0 / variance;
        double[] theta_0array = {theta_0};

        /*
         * 2) theta_0Theta
         */
        double variance2Inv =  1.0/(2*variance);
        RealVector theta_0Theta = beta.mapMultiply(-beta_0 / variance);
        ((CompoundVector) this.naturalParameters).setXYbaseVector(new ArrayRealVector(theta_0array, theta_0Theta.getData()));

        /*
         * 3) theta_Minus1
         */
        double theta_Minus1 = -variance2Inv;

        /*
         * 4) theta_beta
         */
        RealVector theta_beta = beta.mapMultiply(variance2Inv);

        /*
         * 5) theta_betaBeta
         */
        RealMatrix theta_betaBeta = beta.outerProduct(beta).scalarMultiply(-variance2Inv*2);

        /*
         * Store natural parameters
         */
        RealMatrix natural_XY = new Array2DRowRealMatrix(nOfParents+1,nOfParents+1);
        double[] theta_Minus1array = {theta_Minus1};
        RealVector covXY = new ArrayRealVector(theta_Minus1array, theta_beta.getData());
        natural_XY.setColumnVector(0, covXY);
        natural_XY.setRowVector(0, covXY);
        natural_XY.setSubMatrix(theta_betaBeta.getData(),1,1);
        ((CompoundVector) this.naturalParameters).setcovbaseVector(natural_XY);

    }

    @Override
    public void updateMomentFromNaturalParameters() {
        //throw new UnsupportedOperationException("Method not implemented yet!");
    }

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

    @Override
    public int sizeOfSufficientStatistics() {
        return sizeSS;
    }



    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        return -0.5*Math.log(2*Math.PI);
    }

    @Override
    public double computeLogNormalizer() {
        CompoundVector globalNaturalParameters = (CompoundVector)this.naturalParameters;
        double theta_0 = globalNaturalParameters.get(0);
        double theta_Minus1 = globalNaturalParameters.getcovbaseMatrix().getEntry(0,0);
        double variance = -0.5/theta_Minus1;
        double beta_0 = theta_0 * variance;

        return (beta_0*beta_0)/(2*variance) + 0.5*Math.log(variance);
    }

    @Override
    public CompoundVector createZeroedVector() {
        return new CompoundVector(nOfParents);
    }

    @Override
    public double getExpectedLogNormalizer(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        throw new UnsupportedOperationException("No Implemented. This method is no really needed");
    }

    /**
     * Of the second form. Needed to calculate the lower bound.
     *
     * @param momentParents
     * @return
     */
    @Override
    public double getExpectedLogNormalizer(Map<Variable, MomentParameters> momentParents) {
        CompoundVector globalNaturalParameters = (CompoundVector)this.naturalParameters;
        double logNorm = -0.5*Math.log(-2*globalNaturalParameters.getTheta_Minus1());

        double[] Yarray = new double[nOfParents];
        double[] YYarray = new double[nOfParents];

        for (int i = 0; i < nOfParents; i++) {
            Yarray[i] = momentParents.get(this.getConditioningVariables().get(i)).get(0);
            YYarray[i] = momentParents.get(this.getConditioningVariables().get(i)).get(1);
        }
        RealVector Y = new ArrayRealVector(Yarray);

        logNorm -= globalNaturalParameters.getTheta_beta0BetaRV().
                dotProduct(new ArrayRealVector(Y));

        RealMatrix YY = Y.outerProduct(Y);
        for (int i = 0; i < nOfParents; i++) {
            YY.setEntry(i,i,YYarray[i]);
        }

        logNorm -= IntStream.range(0,nOfParents).mapToDouble(p ->
        {
                return globalNaturalParameters.getTheta_BetaBetaRM().getRowVector(p).dotProduct(YY.getRowVector(p));
        })
                .sum();

        logNorm -= Math.pow(globalNaturalParameters.getTheta_beta0(),2)/(4*globalNaturalParameters.getTheta_Minus1());

        return logNorm;
    }


    /**
     * Of the second form.
     * @param momentParents
     * @return
     */
    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {
        NaturalParameters naturalParameters = new ArrayVector(2);
        CompoundVector globalNaturalParameters = (CompoundVector)this.naturalParameters;

        double[] Yarray = new double[nOfParents];
        for (int i = 0; i < nOfParents; i++) {
            Yarray[i] = momentParents.get(this.getConditioningVariables().get(i)).get(0);
        }
        RealVector Y = new ArrayRealVector(Yarray);
        naturalParameters.set(0,globalNaturalParameters.getTheta_beta0() +
                2*globalNaturalParameters.getTheta_BetaRV().dotProduct(Y));

        naturalParameters.set(1,globalNaturalParameters.getTheta_Minus1());

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
        NaturalParameters naturalParameters = new ArrayVector(2);
        CompoundVector globalNaturalParameters = (CompoundVector)this.naturalParameters;

        int parentID=this.getConditioningVariables().indexOf(parent);

        RealVector theta_BetaBetaPrima = globalNaturalParameters.getTheta_BetaBetaRM().getRowVector(parentID);//.copy();
        theta_BetaBetaPrima.setEntry(parentID, 0);

        double[] Yarray = new double[nOfParents];
        for (int i = 0; i < nOfParents; i++) {
            Yarray[i] = momentChildCoParents.get(this.getConditioningVariables().get(i)).get(0);
        }

        RealVector YY_i = new ArrayRealVector(Yarray);

        naturalParameters.set(0,globalNaturalParameters.getTheta_beta0Beta()[parentID]+
                2*globalNaturalParameters.getTheta_Beta()[parentID]*momentChildCoParents.get(var).get(0)+
                2*theta_BetaBetaPrima.dotProduct(YY_i));

        naturalParameters.set(1, globalNaturalParameters.getTheta_BetaBeta()[parentID][parentID]);

        return naturalParameters;
    }


    @Override
    public ConditionalLinearGaussian toConditionalDistribution() {
        ConditionalLinearGaussian normal_normal = new ConditionalLinearGaussian(this.getVariable(), this.getConditioningVariables());

        double[] allBeta = this.getAllBetaValues();

        normal_normal.setIntercept(allBeta[0]);
        normal_normal.setCoeffParents(Arrays.copyOfRange(allBeta, 1, allBeta.length));
        normal_normal.setVariance(this.getVariance());

        return normal_normal;
    }

    public double getVariance(){
        double theta_Minus1 = ((CompoundVector)this.naturalParameters).getcovbaseMatrix().getEntry(0,0);
        return -0.5/theta_Minus1;
    }

    public double[] getAllBetaValues(){
        CompoundVector globalNaturalParameters = (CompoundVector)this.naturalParameters;
        double[] theta_beta0beta = globalNaturalParameters.getXYbaseMatrix().toArray();
        double variance = getVariance();
        double beta0 = theta_beta0beta[0]*variance;
        double[] beta = Arrays.stream(theta_beta0beta).map(w->-w*variance/beta0).toArray();
        beta[0] = beta0;
        return beta;
    }



    public CompoundVector createEmtpyCompoundVector() {
        return new CompoundVector(nOfParents);
    }

    @Override
    public List<EF_ConditionalDistribution> toExtendedLearningDistribution(ParameterVariables variables) {
        List<EF_ConditionalDistribution> conditionalDistributions = new ArrayList<>();

        Variable varGamma = variables.newGammaParameter(this.var.getName() + "_Gamma_Parameter_" + variables.getNumberOfVars());

        conditionalDistributions.add(varGamma.getDistributionType().newEFUnivariateDistribution());

        Variable normalBeta0 = variables.newGaussianParameter(this.var.getName() + "_Beta0_Parameter_"+variables.getNumberOfVars());


        conditionalDistributions.add(normalBeta0.getDistributionType().newEFUnivariateDistribution());

        List<Variable> betas = new ArrayList<>();
        for (Variable variableParent: this.parents){
            Variable normalBetai = variables.newGaussianParameter(this.var.getName() + "_Beta_" + variableParent.getName() + "_Parameter_"+variables.getNumberOfVars());
            betas.add(normalBetai);
            conditionalDistributions.add(normalBetai.getDistributionType().newEFUnivariateDistribution());
        }


        EF_Normal_Normal_Gamma condDist = new EF_Normal_Normal_Gamma(this.var,this.parents,normalBeta0, betas, varGamma);

        conditionalDistributions.add(condDist);

        return conditionalDistributions;
    }

    public static class CompoundVector implements SufficientStatistics, MomentParameters, NaturalParameters {

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