package eu.amidst.core.exponentialfamily;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;

import org.apache.commons.math.linear.*;

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

        if (!var_.isGaussian())
            throw new UnsupportedOperationException("Creating a Normal|Normal EF distribution for a non-gaussian child variable.");

        for (Variable v : parents) {
            if (!v.isGaussian())
                throw new UnsupportedOperationException("Creating a Normal|Normal EF distribution for a non-gaussian parent variable.");
        }

        nOfParents = parents.size();
        sizeSS = nOfParents^2 + 3 * nOfParents + 2 ; // (n+1)^2 (covariance matrix of YX) + n (E(Y)) + 1 (E(X))

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
        RealVector mean_Y = globalMomentParam.getXYbaseMatrix().getSubVector(1,nOfParents);

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
        RealVector theta_0Theta = beta.mapMultiply(-beta_0 * variance2Inv);
        ((CompoundVector) this.naturalParameters).setXYbaseVector(new ArrayRealVector(theta_0array, theta_0Theta.getData()));

        /*
         * 3) theta_Minus1
         */
        double theta_Minus1 = -variance2Inv;

        /*
         * 4) theta_beta
         */
        RealVector theta_beta = beta.mapMultiply(1.0 / variance);

        /*
         * 5) theta_betaBeta
         */
        RealMatrix theta_betaBeta = beta.outerProduct(beta).scalarMultiply(-variance2Inv);

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
    public SufficientStatistics getSufficientStatistics(DataInstance data) {
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
    public double computeLogBaseMeasure(DataInstance dataInstance) {
        return -0.5*Math.log(2*Math.PI);
    }

    @Override
    public double computeLogNormalizer() {
        CompoundVector globalNaturalParameters = (CompoundVector)this.naturalParameters;
        double theta_0 = globalNaturalParameters.get(0);
        double theta_Minus1 = globalNaturalParameters.getcovbaseMatrix().getEntry(0,0);
        double variance = -0.5/theta_Minus1;
        double beta_0 = theta_0 * variance;

        return (beta_0*beta_0)/(2*variance) + Math.log(variance);
    }

    @Override
    public CompoundVector createZeroedVector() {
        return new CompoundVector(nOfParents);
    }

    @Override
    public double getExpectedLogNormalizer(Map<Variable, MomentParameters> momentParents) {
        return 0;
    }

    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {
        return null;
    }

    @Override
    public NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        return null;
    }

    public double getVariance(){
        double theta_Minus1 = ((CompoundVector)this.naturalParameters).getcovbaseMatrix().getEntry(0,0);
        return -0.5/theta_Minus1;
    }

    public double[] getAllBetaValues(){
        CompoundVector globalNaturalParameters = (CompoundVector)this.naturalParameters;
        double[] theta_beta = globalNaturalParameters.getXYbaseMatrix().toArray();
        double variance = getVariance();
        double beta0 = theta_beta[0]*variance;
        double[] beta = Arrays.stream(theta_beta).map(w->-w*2*variance/beta0).toArray();
        beta[0] = beta0;
        return beta;
    }



    public CompoundVector createEmtpyCompoundVector() {
        return new CompoundVector(nOfParents);
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

            size = nOfParents^2 + 3 * nOfParents + 2;

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

        public void setThetaCov_NatParam(double theta_Minus1, double[] theta_beta, double variance2Inv){
            double[] theta_Minus1array = {theta_Minus1};
            RealVector covXY = new ArrayRealVector(theta_Minus1array, theta_beta);
            covbaseVector.setColumnVector(0, covXY);
            covbaseVector.setRowVector(0, covXY);

            RealVector beta = new ArrayRealVector(theta_beta);
            RealMatrix theta_betaBeta = beta.outerProduct(beta).scalarMultiply(variance2Inv);
            covbaseVector.setSubMatrix(theta_betaBeta.getData(),1,1);
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
            double result = 0.0;
            result = this.getXYbaseMatrix().dotProduct(vec.getXYbaseMatrix()); //theta1
            result += this.getcovbaseMatrix().getRowVector(0).dotProduct(vec.getcovbaseMatrix().getRowVector(0));//theta2^1
            result += this.getcovbaseMatrix().getRowVector(1).dotProduct(vec.getcovbaseMatrix().getRowVector(1));//theta2^2
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
