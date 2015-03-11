package eu.amidst.core.variables.distributionTypes;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.EF_InverseGamma;
import eu.amidst.core.exponentialfamily.EF_Normal;
import eu.amidst.core.variables.DistributionType;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * Created by andresmasegosa on 04/03/15.
 */
public class NormalParameterType  extends DistributionType {

    public NormalParameterType(Variable var_) {
        super(var_);
    }

    @Override
    public boolean isParentCompatible(Variable parent) {
        return false;
    }

    @Override
    public Normal newUnivariateDistribution() {
        Normal normal = new Normal(variable);
        normal.setMean(0);
        normal.setVariance(1e10);
        return normal;
    }

    @Override
    public EF_Normal newEFUnivariateDistribution() {
        Normal normal = this.newUnivariateDistribution();
        EF_Normal ef_normal = normal.toEFUnivariateDistribution();
        return ef_normal;
    }

    @Override
    public <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents) {
        throw new UnsupportedOperationException("Normal Parameter Type does not allow conditional distributions");
    }
}
