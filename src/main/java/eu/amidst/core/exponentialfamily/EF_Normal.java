package eu.amidst.core.exponentialfamily;

import eu.amidst.core.variables.DistType;
import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 13/11/14.
 */
public class EF_Normal extends EF_UnivariateDistribution {

    public static final int EXPECTED_MEAN = 0;
    public static final int EXPECTED_SQUARE = 1;

    public EF_Normal(Variable var1) {
        if (var1.getDistributionType()!= DistType.GAUSSIAN)
            throw new UnsupportedOperationException("Creating a Gaussian EF distribution for a non-gaussian variable.");

        this.var=var1;
        this.naturalParameters = new NaturalParameters(2);
        this.momentParameters = new MomentParameters(2);

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
        double m_0=this.momentParameters.get(EXPECTED_MEAN);
        double m_1=this.momentParameters.get(EXPECTED_SQUARE);
        return m_0*m_0/(2*(m_1-m_0*m_0)) + 0.5*Math.log(m_1-m_0*m_0);
    }

    @Override
    public SufficientStatistics getSufficientStatistics(double val) {
        SufficientStatistics vec = new SufficientStatistics(2);
        vec.set(EXPECTED_MEAN,val);
        vec.set(EXPECTED_SQUARE,val*val);
        return vec;
    }

    @Override
    public void updateNaturalFromMomentParameters() {
        double m_0=this.momentParameters.get(EXPECTED_MEAN);
        double m_1=this.momentParameters.get(EXPECTED_SQUARE);
        // var = E(X^2) - E(X)^2 = m_1 - m_0*m_0
        this.naturalParameters.set(0,m_0/(m_1-m_0*m_0));
        this.naturalParameters.set(1,-0.5/(m_1-m_0*m_0));
    }

    @Override
    public void updateMomentFromNaturalParameters() {
        double n_0 = this.naturalParameters.get(0);
        double n_1 = this.naturalParameters.get(1);
        this.momentParameters.set(EXPECTED_MEAN,-0.5*n_0/n_1);
        this.momentParameters.set(EXPECTED_SQUARE,-0.5/n_1 + 0.25*Math.pow(n_0/n_1,2));
    }

    @Override
    public int sizeOfSufficientStatistics() {
        return 2;
    }

}
