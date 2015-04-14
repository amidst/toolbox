package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.BaseDistribution_MultinomialParents;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by ana@cs.aau.dk on 27/02/15.
 */
public class EF_NormalGamma extends EF_ConditionalLearningDistribution{

    Variable meanParameterVariable;
    Variable gammaParameterVariable;

    public EF_NormalGamma(Variable var_, Variable mean, Variable gamma){
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

        this.parametersParentVariables = new ArrayList();
        this.parametersParentVariables.add(meanParameterVariable);
        this.parametersParentVariables.add(gammaParameterVariable);
    }

    public Variable getMeanParameterVariable() {
        return meanParameterVariable;
    }

    public Variable getGammaParameterVariable() {
        return gammaParameterVariable;
    }

    /**
     * Of the second form (message from all parents to X variable). Needed to calculate the lower bound.
     *
     * @param momentParents
     * @return
     */
    @Override
    public double getExpectedLogNormalizer(Map<Variable, MomentParameters> momentParents) {
        double mean = momentParents.get(meanParameterVariable).get(0);
        double meansquare = momentParents.get(meanParameterVariable).get(1);

        double invVariance = momentParents.get(gammaParameterVariable).get(1);
        double logVar = momentParents.get(gammaParameterVariable).get(0);

        return 0.5*meansquare*invVariance - 0.5*logVar;
    }

    /**
     * Of the second form (message from all parents to X variable).
     * @param momentParents
     * @return
     */
    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {

        NaturalParameters naturalParameters = new ArrayVector(2);

        double mean = momentParents.get(meanParameterVariable).get(0);
        double invVariance = momentParents.get(gammaParameterVariable).get(1);

        naturalParameters.set(0,mean*invVariance);
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

        double X = momentChildCoParents.get(var).get(0);

        // Message to the mean (gaussian) variable
        if(meanParameterVariable == parent){
            double invVariance = momentChildCoParents.get(gammaParameterVariable).get(1);

            naturalParameters.set(0, X*invVariance);
            naturalParameters.set(1, -0.5*invVariance);
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

    @Override
    public double getExpectedLogNormalizer(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        throw new UnsupportedOperationException("No Implemented. This method is no really needed");
    }

    @Override
    public void updateNaturalFromMomentParameters() {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal for inference.");
    }

    @Override
    public void updateMomentFromNaturalParameters() {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal for inference.");
    }

    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal for inference.");
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
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal for inference.");
    }

    @Override
    public Vector createZeroedVector() {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal for inference.");
    }

    @Override
    public ConditionalDistribution toConditionalDistribution(Map<Variable, Vector> expectedParameters) {

        Normal normal = new Normal(this.var);

        normal.setMean(expectedParameters.get(this.meanParameterVariable).get(0));
        normal.setVariance(1.0/expectedParameters.get(this.gammaParameterVariable).get(0));

        return new BaseDistribution_MultinomialParents<Normal>(new ArrayList(), Arrays.asList(normal));
    }
}
