package eu.amidst.core.exponentialfamily;

import eu.amidst.core.variables.DistType;
import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 13/11/14.
 */
public class EF_Multinomial extends EF_UnivariateDistribution {

    /**
     * The class constructor.
     * @param var_ The variable of the distribution.
     */
    public EF_Multinomial(Variable var) {

        if (var.getDistributionType()!= DistType.MULTINOMIAL)
            throw new UnsupportedOperationException("Creating a Multinomial EF distribution for a non-multinomial variable.");

        this.var=var;
        int nstates= var.getNumberOfStates();
        this.naturalParameters = new NaturalParameters(nstates);
        this.momentParameters = new MomentParameters(nstates);

        for (int i=0; i<nstates; i++){
            this.naturalParameters.set(i,-Math.log(nstates));
            this.momentParameters.set(i,1.0/nstates);
        }

    }


    @Override
    public double computeLogBaseMeasure(double val) {
        return 0;
    }

    @Override
    public double computeLogNormalizer() {
        return 0;
    }

    @Override
    public SufficientStatistics getSufficientStatistics(double val) {
        SufficientStatistics vec = new SufficientStatistics(this.getVariable().getNumberOfStates());
        vec.set((int) val, 1);
        return vec;
    }

    @Override
    public void updateNaturalFromMomentParameters() {
        int nstates= var.getNumberOfStates();
        for (int i=0; i<nstates; i++){
            this.naturalParameters.set(i, Math.log(this.momentParameters.get(i)));
        }
    }

    @Override
    public void updateMomentFromNaturalParameters() {
        int nstates= var.getNumberOfStates();
        for (int i=0; i<nstates; i++){
            this.momentParameters.set(i, Math.exp(this.naturalParameters.get(i)));
        }
    }

    @Override
    public int sizeOfSufficientStatistics() {
        return this.var.getNumberOfStates();
    }

    public static SufficientStatistics sufficientStatistics(int nstates, double val){
        SufficientStatistics vec = new SufficientStatistics(nstates);
        vec.set((int) val, 1);
        return vec;
    }

}
