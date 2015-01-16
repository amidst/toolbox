package eu.amidst.core.exponentialfamily;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.DistType;
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

    public static final int EXPECTED_XY = 0;
    public static final int EXPECTED_cov = 1;

    double variance;

    public EF_Normal_NormalParents(Variable var_, List<Variable> parents_) {

        this.var = var_;
        this.parents = parents_;

        if (var_.getDistributionType()!= DistType.GAUSSIAN)
            throw new UnsupportedOperationException("Creating a Normal|Normal EF distribution for a non-gaussian child variable.");

        for (Variable v : parents) {
            if (v.getDistributionType() != DistType.GAUSSIAN)
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
        RealVector mean_Y = globalMomentParam.getXYbaseMatrix();

        double cov_XX = globalMomentParam.getcovbaseMatrix().getEntry(0, 0) - mean_X*mean_X;
        RealMatrix cov_YY = globalMomentParam.getMatrixByPosition(EXPECTED_cov).getSubMatrix(1, nOfParents, 1, nOfParents).
                            subtract(mean_Y.outerProduct(mean_Y));
        RealVector cov_XY = globalMomentParam.getMatrixByPosition(EXPECTED_cov).getSubMatrix(0, 0, 1, nOfParents).getRowVector(0).
                            subtract(mean_Y.mapAdd(mean_X));
        //RealVector cov_YX = cov_XY; //outerProduct transpose the vector automatically

        /*
         * Second step: betas and variance
         */
        RealMatrix cov_YYInverse = new LUDecompositionImpl(cov_YY).getSolver().getInverse();
        RealMatrix cov_XYbyCov_YYInv = (new Array2DRowRealMatrix(cov_XY.getData())).multiply(cov_YYInverse);

        double beta_0 = mean_X - cov_XYbyCov_YYInv.preMultiply(mean_Y).getEntry(0);
        RealVector beta = cov_XYbyCov_YYInv.getColumnVector(0);
        variance = cov_XX - cov_XYbyCov_YYInv.preMultiply(cov_XY).getEntry(0);

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
        RealMatrix theta_betaBeta = beta.outerProduct(beta).scalarMultiply(variance2Inv);

        /*
         * Store natural parameters
         */
        RealMatrix natural_XY = new Array2DRowRealMatrix(nOfParents+1,nOfParents+1);
        double[] theta_Minus1array = {theta_Minus1};
        RealVector covXY = new ArrayRealVector(theta_Minus1array, theta_beta.getData());
        natural_XY.setColumnVector(0, covXY);
        natural_XY.setRowVector(0, covXY);
        natural_XY.setSubMatrix(theta_betaBeta.getData(),1,1);
        ((CompoundVector) this.naturalParameters).setcovbaseVector(theta_betaBeta);

    }

    @Override
    public void updateMomentFromNaturalParameters() {
        throw new UnsupportedOperationException("Method not implemented yet!");
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

        RealMatrix covRealmatrix = new Array2DRowRealMatrix(nOfParents+1,nOfParents+1);


        covRealmatrix = XYRealVector.outerProduct(XYRealVector);

        vectorSS.setcovbaseVector(covRealmatrix);

        return vectorSS;
    }

    @Override
    public int sizeOfSufficientStatistics() {
        return sizeSS;
    }

    @Override
    public double computeLogBaseMeasure(DataInstance dataInstance) {
        CompoundVector globalNaturalParameters = (CompoundVector)this.naturalParameters;
        double[] theta_beta = globalNaturalParameters.getMatrixByPosition(EXPECTED_XY).getRow(0);
        double[] beta = Arrays.stream(theta_beta).map(w->w*variance).toArray();

        double[] YdataInstance = this.parents.stream()
                                     .mapToDouble(w -> dataInstance.getValue(w))
                                     .toArray();

        double[] result = new double[YdataInstance.length];
        return beta[0] + IntStream.range(0, YdataInstance.length)
                                  .mapToDouble(i -> result[i] = beta[i + 1] * YdataInstance[i]).sum();
    }

    @Override
    public double computeLogNormalizer() {
        CompoundVector globalNaturalParameters = (CompoundVector)this.naturalParameters;
        double theta_0 = globalNaturalParameters.get(0);
        double beta_0 = theta_0 * variance;

        return theta_0*beta_0 + Math.log(variance);
    }

    @Override
    public Vector createZeroedVector() {
        return new ArrayVector(sizeSS);
    }

    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {
        return null;
    }

    @Override
    public NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        return null;
    }



    private CompoundVector createEmtpyCompoundVector() {
        return new CompoundVector(nOfParents);
    }

    static class CompoundVector implements SufficientStatistics, MomentParameters, NaturalParameters {

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
            XYbaseVector.mapAddToSelf(1.0 / val);
            covbaseVector.scalarMultiply(1.0/val);
        }

        @Override
        public double dotProduct(Vector vec) {
          return this.dotProduct((CompoundVector)vec);
        }

        public double dotProduct(CompoundVector vec) {
            return 0.0;
        }

        public void copy(CompoundVector vector) {
            XYbaseVector = vector.getMatrixByPosition(EXPECTED_XY).copy().getColumnVector(0);
            covbaseVector = vector.getMatrixByPosition(EXPECTED_cov).copy();

        }

        public void sum(CompoundVector vector) {
            XYbaseVector.add(vector.getMatrixByPosition(EXPECTED_XY).getColumnVector(0));
            covbaseVector.add(vector.getMatrixByPosition(EXPECTED_cov));
        }

    }

}
