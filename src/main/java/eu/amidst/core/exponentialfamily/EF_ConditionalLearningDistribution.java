package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 04/03/15.
 */
public abstract class EF_ConditionalLearningDistribution extends EF_ConditionalDistribution{

    protected List<Variable> parametersParentVariables;

    public List<Variable> getParameterParentVariables() {
        return parametersParentVariables;
    }

    public List<Variable> getNonParameterParentVariables() {
        return this.parents.stream().filter(var -> !var.isParameterVariable()).collect(Collectors.toList());
    }


    public abstract ConditionalDistribution toConditionalDistribution(Map<Variable, Vector> expectedParameters);

}
