package eu.amidst.core.exponentialfamily;

import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.RealVector;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 25/02/15.
 */
public class EF_NormalInverseGamma extends EF_ConditionalDistribution{

    int nOfParents;

    List<Variable> realYVariables;
    List<Variable> betasVariables;
    Variable beta0Variable;
    Variable invGammaVariable;


    /**
     *
     * @param var_ X variable
     * @param parents_ Y real parent variables
     * @param beta0 Beta0 parameter variable
     * @param betas_ Beta parameter variables
     * @param invGamma Inverse-gamma parameter variable
     */
    public EF_NormalInverseGamma(Variable var_, List<Variable> parents_, Variable beta0, List<Variable> betas_, Variable invGamma){
        this.var = var_;
        this.realYVariables = parents_;
        this.betasVariables = betas_;
        this.parents.addAll(parents_);
        this.parents.addAll(betas_);
        this.beta0Variable = beta0;
        this.invGammaVariable = invGamma;

        if (!var_.isNormal())
            throw new UnsupportedOperationException("Creating a Normal-Inverse-Gamma EF distribution for a non-gaussian child variable.");

        for (Variable v : realYVariables) {
            if (!v.isNormal() || !v.isInverseGamma())
                throw new UnsupportedOperationException("Creating a Normal-Inverse-Gamma EF distribution for a non-gaussian parent variable.");
        }

        for (Variable v : betasVariables) {
            if (!v.isNormal())
                throw new UnsupportedOperationException("Creating a Normal-Inverse-Gamma EF distribution for a non-gaussian parent variable.");
        }

        if(!beta0Variable.isNormal()){
            throw new UnsupportedOperationException("Creating a Normal-Inverse-Gamma EF distribution for a non-gaussian parent variable.");
        }

        if(!invGammaVariable.isInverseGamma()){
            throw new UnsupportedOperationException("Creating a Normal-Inverse-Gamma EF distribution for a non-inverse-gamma parent variable.");
        }

        nOfParents = parents.size();
    }

    /**
     * Of the second form (message from all parents to X variable). Needed to calculate the lower bound.
     *
     * @param momentParents
     * @return
     */
    @Override
    public double getExpectedLogNormalizer(Map<Variable, MomentParameters> momentParents) {

        //From Beta_0, Beta, gamma and Y parents to variable X.
        double[] Beta_array = new double[Math.floorDiv(nOfParents,2)];
        double[] Yarray = new double[Math.floorDiv(nOfParents,2)];
        double beta0;
        double invVariance;

        for (int i = 0; i < realYVariables.size(); i++) {
            Yarray[i] = momentParents.get(this.realYVariables.get(i)).get(0);
        }
        RealVector Y = new ArrayRealVector(Yarray);
        for (int i = 0; i < betasVariables.size(); i++) {
            Beta_array[i] = momentParents.get(this.betasVariables.get(i)).get(0);
        }
        RealVector Beta = new ArrayRealVector(Beta_array);

        beta0 = momentParents.get(beta0Variable).get(0);
        invVariance = momentParents.get(invGammaVariable).get(1);

        double logNorm = -0.5*Math.log(invVariance) + Beta.dotProduct(Y)*beta0*invVariance;

        RealMatrix YY = Y.outerProduct(Y);
        RealMatrix BetaBeta = Beta.outerProduct(Beta);

        double betabeta = IntStream.range(0, betasVariables.size()).mapToDouble(p ->
        {
            return BetaBeta.getRowVector(p).dotProduct(YY.getRowVector(p));
        })
                .sum();

        logNorm += betabeta*0.5*invVariance + beta0*beta0*0.5*invVariance;

        return logNorm;
    }


    /**
     * Of the second form (message from all parents to X variable).
     * @param momentParents
     * @return
     */
    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {
        NaturalParameters naturalParameters = new ArrayVector(2);

        //From Beta_0, Beta, gamma and Y parents to variable X.
        double[] Beta_array = new double[Math.floorDiv(nOfParents,2)];
        double[] Yarray = new double[Math.floorDiv(nOfParents,2)];
        double beta0;
        double invVariance;

        for (int i = 0; i < realYVariables.size(); i++) {
            Yarray[i] = momentParents.get(this.realYVariables.get(i)).get(0);
        }
        RealVector Y = new ArrayRealVector(Yarray);
        for (int i = 0; i < betasVariables.size(); i++) {
            Beta_array[i] = momentParents.get(this.betasVariables.get(i)).get(0);
        }
        RealVector Beta = new ArrayRealVector(Beta_array);

        beta0 = momentParents.get(beta0Variable).get(0);
        invVariance = momentParents.get(invGammaVariable).get(1);

        naturalParameters.set(0,beta0*invVariance + Beta.mapMultiplyToSelf(invVariance).dotProduct(Y));

        naturalParameters.set(1,-invVariance/2);

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
        return null;
    }

    @Override
    public double getExpectedLogNormalizer(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        throw new UnsupportedOperationException("No Implemented. This method is no really needed");
    }

    @Override
    public EF_UnivariateDistribution getNewBaseEFUnivariateDistribution() {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal_NormalParents for inference.");
    }

    @Override
    public EF_UnivariateDistribution getEFUnivariateDistribution(Assignment assignment) {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal_NormalParents for inference.");
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
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal_NormalParents for inference.");
    }

    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal_NormalParents for inference.");
    }

    @Override
    public double computeLogNormalizer() {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal_NormalParents for inference.");
    }

    @Override
    public Vector createZeroedVector() {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal_NormalParents for inference.");
    }

}
