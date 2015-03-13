package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Normal_NormalParents;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.RealVector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 25/02/15.
 */
public class EF_Normal_Normal_Gamma extends EF_ConditionalLearningDistribution{

    int nOfParents;

    List<Variable> realYVariables;
    List<Variable> betasVariables;
    Variable beta0Variable;
    Variable gammaVariable;


    /**
     *
     * @param var_ X variable
     * @param parents_ Y real parent variables
     * @param beta0 Beta0 parameter variable
     * @param betas_ Beta parameter variables
     * @param gamma Inverse-gamma parameter variable
     */
    public EF_Normal_Normal_Gamma(Variable var_, List<Variable> parents_, Variable beta0, List<Variable> betas_, Variable gamma){
        this.var = var_;
        this.realYVariables = parents_;
        this.betasVariables = betas_;
        this.beta0Variable = beta0;
        this.gammaVariable = gamma;
        this.parents = new ArrayList<>();
        this.parents.addAll(parents_);
        this.parents.addAll(betas_);
        this.parents.add(beta0);
        this.parents.add(gammaVariable);

        if (!var_.isNormal())
            throw new UnsupportedOperationException("Creating a Normal_Normal-Gamma EF distribution for a non-gaussian child variable.");

        for (Variable v : realYVariables) {
            if (!v.isNormal())
                throw new UnsupportedOperationException("Creating a Normal_Normal-Gamma EF distribution for a non-gaussian parent variable.");
        }

        for (Variable v : betasVariables) {
            if (!v.isNormalParameter())
                throw new UnsupportedOperationException("Creating a Normal_Normal-Gamma EF distribution for a non-gaussian parent variable.");
        }

        if(!beta0Variable.isNormalParameter()){
            throw new UnsupportedOperationException("Creating a Normal_Normal-Gamma EF distribution for a non-gaussian parent variable.");
        }

        if(!gammaVariable.isGammaParameter()){
            throw new UnsupportedOperationException("Creating a Normal_Normal-Gamma EF distribution for a non-gamma parent variable.");
        }

        nOfParents = parents.size();

        this.parametersParentVariables = new ArrayList();
        this.parametersParentVariables.addAll(betasVariables);
        this.parametersParentVariables.add(beta0Variable);
        this.parametersParentVariables.add(gammaVariable);
    }

    /**
     * Of the second form (message from all parents to X variable). Needed to calculate the lower bound.
     *
     * @param momentParents
     * @return
     */
    @Override
    public double getExpectedLogNormalizer(Map<Variable, MomentParameters> momentParents) {

        int nOfBetas = this.betasVariables.size();

        //From Beta_0, Beta, gamma and Y parents to variable X.
        double[] Beta_array = new double[nOfBetas];
        double[] Yarray = new double[nOfBetas];
        double beta0, beta0Squared;
        double invVariance;

        for (int i = 0; i < nOfBetas; i++) {
            Yarray[i] = momentParents.get(this.realYVariables.get(i)).get(0);
        }
        RealVector Y = new ArrayRealVector(Yarray);
        for (int i = 0; i < nOfBetas; i++) {
            Beta_array[i] = momentParents.get(this.betasVariables.get(i)).get(0);
        }
        RealVector Beta = new ArrayRealVector(Beta_array);

        beta0 = momentParents.get(beta0Variable).get(0);
        beta0Squared = momentParents.get(beta0Variable).get(1);
        invVariance = momentParents.get(gammaVariable).get(1);
        double logVar = momentParents.get(gammaVariable).get(0);


        double sumSquaredMoments=0;
        double sumSquaredMeanMoments=0;
        for (int i = 0; i < nOfBetas; i++) {
            sumSquaredMoments += momentParents.get(this.betasVariables.get(i)).get(1) *
                    momentParents.get(this.realYVariables.get(i)).get(1);

            sumSquaredMeanMoments += Math.pow(momentParents.get(this.betasVariables.get(i)).get(0) *
                    momentParents.get(this.realYVariables.get(i)).get(0), 2);
        }
        double dotProductBetaY = Beta.dotProduct(Y);



        /*double logNorm = -0.5*logVar + Beta.dotProduct(Y)*beta0*invVariance;

        RealMatrix YY = Y.outerProduct(Y);
        RealMatrix BetaBeta = Beta.outerProduct(Beta);

        for (int i = 0; i < nOfParents; i++) {
            YY.setEntry(i,i,YYarray[i]);
            BetaBeta.setEntry(i,i,BetaBeta_array[i]);
        }

        double betabeta = IntStream.range(0, nOfBetas).mapToDouble(p ->
                BetaBeta.getRowVector(p).dotProduct(YY.getRowVector(p))).sum();

        logNorm += betabeta*0.5*invVariance + beta0Square*0.5*invVariance;

        return logNorm;
        */


        return -0.5*logVar +  0.5*invVariance*(beta0Squared + dotProductBetaY*dotProductBetaY - sumSquaredMeanMoments + sumSquaredMoments + 2*beta0*dotProductBetaY);
    }


    /**
     * Of the second form (message from all parents to X variable).
     * @param momentParents
     * @return
     */
    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {

        int nOfBetas = this.betasVariables.size();
        NaturalParameters naturalParameters = new ArrayVector(2);

        //From Beta_0, Beta, gamma and Y parents to variable X.
        double[] Beta_array = new double[nOfBetas];
        double[] Yarray = new double[nOfBetas];
        double beta0;
        double invVariance;

        for (int i = 0; i < nOfBetas; i++) {
            Yarray[i] = momentParents.get(this.realYVariables.get(i)).get(0);
        }
        RealVector Y = new ArrayRealVector(Yarray);
        for (int i = 0; i < nOfBetas; i++) {
            Beta_array[i] = momentParents.get(this.betasVariables.get(i)).get(0);
        }
        RealVector Beta = new ArrayRealVector(Beta_array);

        beta0 = momentParents.get(beta0Variable).get(0);
        invVariance = momentParents.get(gammaVariable).get(1);

        naturalParameters.set(0, beta0 * invVariance + Beta.dotProduct(Y) * invVariance);

        naturalParameters.set(1,-0.5*invVariance);

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

        int nOfBetas = this.betasVariables.size();

        double[] Beta_array = new double[nOfBetas];
        double[] Yarray = new double[nOfBetas];

        double beta0;
        double invVariance;

        for (int i = 0; i < nOfBetas; i++) {
            Yarray[i] = momentChildCoParents.get(this.realYVariables.get(i)).get(0);
        }
        RealVector Y = new ArrayRealVector(Yarray);
        for (int i = 0; i < nOfBetas; i++) {
            Beta_array[i] = momentChildCoParents.get(this.betasVariables.get(i)).get(0);
        }
        RealVector Beta = new ArrayRealVector(Beta_array);

        beta0 = momentChildCoParents.get(beta0Variable).get(0);
        invVariance = momentChildCoParents.get(gammaVariable).get(1);

        double X = momentChildCoParents.get(var).get(0);


        // Message to a Y variable
        if(realYVariables.contains(parent)){

            int parentID=this.realYVariables.indexOf(parent);
            double beta_iSquared = momentChildCoParents.get(this.betasVariables.get(parentID)).get(1);

            RealVector BetaPrima = Beta.copy();
            BetaPrima.setEntry(parentID, 0);

            double beta_i = Beta.getEntry(parentID);

            naturalParameters.set(0, -beta0 * beta_i * invVariance +
                    beta_i * X * invVariance - BetaPrima.dotProduct(Y) * (beta_i*invVariance));

            naturalParameters.set(1, -0.5*beta_iSquared*invVariance);

        // Message to a Beta variable
        }else if(betasVariables.contains(parent)){

            int parentID=this.betasVariables.indexOf(parent);
            double Y_iSquared = momentChildCoParents.get(this.realYVariables.get(parentID)).get(1);

            RealVector BetaPrima = Beta.copy();
            BetaPrima.setEntry(parentID, 0);

            double Y_i = Y.getEntry(parentID);

            naturalParameters.set(0, -beta0 * Y_i * invVariance +
                    Y_i * X * invVariance - BetaPrima.dotProduct(Y) * (Y_i*invVariance));
            naturalParameters.set(1, -0.5*Y_iSquared*invVariance);


        // Message to the Beta0 variable
        }else if(beta0Variable == parent){

            naturalParameters.set(0, X * invVariance - Beta.dotProduct(Y) * invVariance);
            naturalParameters.set(1, -0.5*invVariance);

        // Message to the inv-Gamma variable
        }else if (gammaVariable==parent){

            double Xsquared = momentChildCoParents.get(var).get(1);
            double beta0Squared = momentChildCoParents.get(beta0Variable).get(1);

            double sumSquaredMoments=0;
            double sumSquaredMeanMoments=0;
            for (int i = 0; i < nOfBetas; i++) {
                sumSquaredMoments += momentChildCoParents.get(this.betasVariables.get(i)).get(1) *
                             momentChildCoParents.get(this.realYVariables.get(i)).get(1);

                sumSquaredMeanMoments += Math.pow(momentChildCoParents.get(this.betasVariables.get(i)).get(0) *
                        momentChildCoParents.get(this.realYVariables.get(i)).get(0), 2);
            }
            double dotProductBetaY = Beta.dotProduct(Y);
            naturalParameters.set(0, 0.5);
            naturalParameters.set(1, -0.5*(Xsquared + beta0Squared + dotProductBetaY*dotProductBetaY - sumSquaredMeanMoments + sumSquaredMoments  - 2*X*dotProductBetaY - 2*X*beta0 + 2*beta0*dotProductBetaY));

        }else{
            throw new IllegalArgumentException("Error");
        }

        return naturalParameters;
    }

    @Override
    public double getExpectedLogNormalizer(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        throw new UnsupportedOperationException("No Implemented. This method is no really needed");
    }

    @Override
    public <E extends ConditionalDistribution> E toConditionalDistribution() {
        return null;
    }

    @Override
    public void updateNaturalFromMomentParameters() {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal_NormalParents for inference.");
    }

    @Override
    public void updateMomentFromNaturalParameters() {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal_NormalParents for inference.");
    }

    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal_NormalParents for inference.");
    }

    @Override
    public int sizeOfSufficientStatistics() {
        return 0;
    }

    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        return -0.5*Math.log(2*Math.PI);
    }

    @Override
    public double computeLogNormalizer() {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal_NormalParents for inference.");
    }

    @Override
    public Vector createZeroedVector() {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal_NormalParents for inference.");
    }

    @Override
    public ConditionalDistribution toConditionalDistribution(Map<Variable, Vector> expectedParameters) {

        Normal_NormalParents dist = new Normal_NormalParents(this.var, this.realYVariables);

        double[] coeffParameters = new double[this.realYVariables.size()];
        for (int i = 0; i < this.realYVariables.size(); i++) {
            coeffParameters[i]=expectedParameters.get(this.betasVariables.get(i)).get(0);
        }

        dist.setCoeffParents(coeffParameters);

        dist.setIntercept(expectedParameters.get(this.beta0Variable).get(0));

        dist.setSd(Math.sqrt(1.0/expectedParameters.get(this.gammaVariable).get(0)));

        return dist;
    }
}
