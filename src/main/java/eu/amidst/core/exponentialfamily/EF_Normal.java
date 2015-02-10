package eu.amidst.core.exponentialfamily;

import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;

import java.util.Random;

/**
 * Created by andresmasegosa on 13/11/14.
 */
public class EF_Normal extends EF_UnivariateDistribution {

    public static final int EXPECTED_MEAN = 0;
    public static final int EXPECTED_SQUARE = 1;

    public EF_Normal(Variable var1) {
        if (!var1.isGaussian()) {
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
    public EF_UnivariateDistribution deepCopy() {

        EF_Normal copy = new EF_Normal(this.getVariable());
        copy.getNaturalParameters().copy(this.getNaturalParameters());

        return copy;
    }

    @Override
    public EF_UnivariateDistribution randomInitialization(Random random) {

        this.getMomentParameters().set(0,random.nextGaussian()*10);
        this.getMomentParameters().set(1,random.nextDouble()*10);

        this.updateNaturalFromMomentParameters();

        return this;
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

}
