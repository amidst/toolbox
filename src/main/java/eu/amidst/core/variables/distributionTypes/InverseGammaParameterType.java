package eu.amidst.core.variables.distributionTypes;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.exponentialfamily.EF_Dirichlet;
import eu.amidst.core.exponentialfamily.EF_InverseGamma;
import eu.amidst.core.variables.DistributionType;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * Created by andresmasegosa on 04/03/15.
 */
public class InverseGammaParameterType extends DistributionType {

    public InverseGammaParameterType(Variable var_) {
        super(var_);
    }

    @Override
    public boolean isParentCompatible(Variable parent) {
        return false;
    }

    @Override
    public Normal newUnivariateDistribution() {
        throw new UnsupportedOperationException("Inverse Gamma Parameter Type does not allow standard distributions");
    }

    @Override
    public EF_InverseGamma newEFUnivariateDistribution() {
        EF_InverseGamma inverseGamma = new EF_InverseGamma(this.variable);
        inverseGamma.getNaturalParameters().set(0, -2.1); //alpha = 1.1
        inverseGamma.getNaturalParameters().set(1, -2.1);   //beta = 1
        return inverseGamma;
    }


    @Override
    public <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents) {
        throw new UnsupportedOperationException("Inverse Gamma Parameter Type does not allow conditional distributions");
    }
}
