package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.Normal;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by andresmasegosa on 13/11/14.
 */
public class EF_Normal extends EF_UnivariateDistribution {

    public static final int EXPECTED_MEAN = 0;
    public static final int EXPECTED_SQUARE = 1;

    public EF_Normal(Variable var1) {
        if (!var1.isNormal()) {
            throw new UnsupportedOperationException("Creating a Gaussian EF distribution for a non-gaussian variable.");
        }

        this.var=var1;
        this.naturalParameters = this.createZeroedNaturalParameters();
        this.momentParameters = this.createZeroedMomentParameters();

        this.momentParameters.set(EXPECTED_MEAN,0);
        this.momentParameters.set(EXPECTED_SQUARE,1);
        this.setMomentParameters(momentParameters);
    }

    @Override
    public double computeLogBaseMeasure(double val) {
        return -0.5*Math.log(2*Math.PI);
    }

    @Override
    public double computeLogNormalizer() {
        double m0=this.momentParameters.get(EXPECTED_MEAN);
        double m1=this.momentParameters.get(EXPECTED_SQUARE);
        return m0*m0/(2*(m1-m0*m0)) + 0.5*Math.log(m1-m0*m0);
    }

    @Override
    public Vector createZeroedVector() {
        return new ArrayVector(2);
    }

    @Override
    public SufficientStatistics getSufficientStatistics(double val) {
        SufficientStatistics vec = this.createZeroedSufficientStatistics();
        vec.set(EXPECTED_MEAN,val);
        vec.set(EXPECTED_SQUARE,val*val);
        return vec;
    }

    @Override
    public Vector getExpectedParameters() {
        Vector vec = new ArrayVector(1);
        vec.set(0,this.momentParameters.get(0));
        return vec;
    }

    @Override
    public EF_UnivariateDistribution deepCopy() {

        EF_Normal copy = new EF_Normal(this.getVariable());
        copy.getNaturalParameters().copy(this.getNaturalParameters());

        return copy;
    }

    @Override
    public EF_UnivariateDistribution randomInitialization(Random random) {

        double mean = random.nextGaussian()*10;
        double sd = random.nextDouble()+ 0.1;

        this.getNaturalParameters().set(0,mean/(sd*sd));
        this.getNaturalParameters().set(1,-1/(2*sd*sd));

        this.updateMomentFromNaturalParameters();

        return this;
    }

    @Override
    public Normal toUnivariateDistribution() {

        Normal normal = new Normal(this.getVariable());
        double mean = this.getMomentParameters().get(EF_Normal.EXPECTED_MEAN);
        double sigma = this.getMomentParameters().get(EF_Normal.EXPECTED_SQUARE) - mean * mean;

        normal.setMean(mean);
        normal.setSd(Math.sqrt(sigma));

        return normal;
    }

    @Override
    public void updateNaturalFromMomentParameters() {
        double m0=this.momentParameters.get(EXPECTED_MEAN);
        double m1=this.momentParameters.get(EXPECTED_SQUARE);
        // var = E(X^2) - E(X)^2 = m1 - m0*m0
        this.naturalParameters.set(0,m0/(m1-m0*m0));
        this.naturalParameters.set(1,-0.5/(m1-m0*m0));
    }

    @Override
    public void updateMomentFromNaturalParameters() {
        double n0 = this.naturalParameters.get(0);
        double n1 = this.naturalParameters.get(1);
        this.momentParameters.set(EXPECTED_MEAN,-0.5*n0/n1);
        this.momentParameters.set(EXPECTED_SQUARE,-0.5/n1 + 0.25*Math.pow(n0/n1,2));
    }

    @Override
    public int sizeOfSufficientStatistics() {
        return 2;
    }

    @Override
    public List<EF_ConditionalLearningDistribution> toExtendedLearningDistribution(ParameterVariables variables) {
        List<EF_ConditionalLearningDistribution> conditionalDistributions = new ArrayList<>();

        Variable varInvGamma = variables.newInverseGammaParameter(this.var.getName()+"_InverseGamma_Parameter");

        conditionalDistributions.add(
                new EF_BaseDistribution_MultinomialParents<EF_InverseGamma>(new ArrayList<>(), Arrays.asList(new EF_InverseGamma(varInvGamma))));

        Variable normalMean = variables.newGaussianParameter(this.var.getName() + "_Mean_Parameter");

        conditionalDistributions.add(
                new EF_BaseDistribution_MultinomialParents<EF_Normal>(new ArrayList<>(), Arrays.asList(new EF_Normal(normalMean))));

        EF_NormalInverseGamma dist = new EF_NormalInverseGamma(this.var, normalMean, varInvGamma);
        conditionalDistributions.add(dist);

        return conditionalDistributions;
    }
}
