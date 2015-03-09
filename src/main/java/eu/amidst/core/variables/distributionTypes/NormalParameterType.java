package eu.amidst.core.variables.distributionTypes;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.distribution.UnivariateDistribution;
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
        normal.setMean(2);
        normal.setVariance(1);
        return normal;
    }

    @Override
    public <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents) {
        throw new UnsupportedOperationException("Normal Parameter Type does not allow conditional distributions");
    }
}
