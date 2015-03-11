package eu.amidst.core.variables.distributionTypes;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.exponentialfamily.EF_Gamma;
import eu.amidst.core.exponentialfamily.EF_InverseGamma;
import eu.amidst.core.variables.DistributionType;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * Created by andresmasegosa on 04/03/15.
 */
public class GammaParameterType extends DistributionType {

    public GammaParameterType(Variable var_) {
        super(var_);
    }

    @Override
    public boolean isParentCompatible(Variable parent) {
        return false;
    }

    @Override
    public Normal newUnivariateDistribution() {
        throw new UnsupportedOperationException("Gamma Parameter Type does not allow standard distributions");
    }

    @Override
    public EF_Gamma newEFUnivariateDistribution() {
        EF_Gamma gamma = new EF_Gamma(this.variable);
        double alpha = 1;
        double beta = 1;
        gamma.getNaturalParameters().set(0, alpha - 1);
        gamma.getNaturalParameters().set(1, -beta);
        gamma.updateMomentFromNaturalParameters();
        return gamma;
    }


    @Override
    public <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents) {
        throw new UnsupportedOperationException("Inverse Gamma Parameter Type does not allow conditional distributions");
    }
}
