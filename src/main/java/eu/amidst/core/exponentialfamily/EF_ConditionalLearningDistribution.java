package eu.amidst.core.exponentialfamily;

import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.Map;

/**
 * Created by andresmasegosa on 04/03/15.
 */
public abstract class EF_ConditionalLearningDistribution extends EF_ConditionalDistribution{

    protected List<Variable> parametersParentVariables;

    public List<Variable> getParameterParentVariables() {
        return parametersParentVariables;
    }

    //public abstract EF_ConditionalDistribution toEFConditionalDistribution(Map<Variable, Vector> expectedParameters);

}
