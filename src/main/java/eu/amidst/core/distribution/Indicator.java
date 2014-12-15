package eu.amidst.core.distribution;

import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.DistType;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;

/**
 * Created by andresmasegosa on 23/11/14.
 */
public class Indicator extends ConditionalDistribution{

    private ConditionalDistribution conditionalDistribution;
    private Uniform uniform;
    private Variable indicatorVar;

    public Indicator(Variable indicatorVar1,  ConditionalDistribution conditionalDistribution1) {
        if (indicatorVar1.getDistributionType() != DistType.INDICATOR) {
            throw new IllegalArgumentException("IndicatorVar_ should be of indicator type");
        }
        this.var = conditionalDistribution1.getVariable();
        this.parents = new ArrayList<>();
        for (Variable var: conditionalDistribution1.getConditioningVariables()){
            this.parents.add(var);
        }

        this.parents.add(indicatorVar1);
        this.conditionalDistribution=conditionalDistribution1;
        this.indicatorVar = indicatorVar1;
        this.uniform = new Uniform(this.getVariable());
    }

    public ConditionalDistribution getConditionalDistribution() {
        return conditionalDistribution;
    }

    public Variable getIndicatorVar() {
        return indicatorVar;
    }

    //TODO: I'm not sure about how to compute this
    @Override
    public int getNumberOfFreeParameters() {
        return 0;
    }

    @Override
    public double getLogConditionalProbability(Assignment assignment) {
        if (assignment.getValue(this.indicatorVar)==0.0) {
            return this.uniform.getLogProbability(assignment.getValue(this.getVariable()));
        }
        else {
            return this.conditionalDistribution.getLogConditionalProbability(assignment);
        }
    }

    @Override
    public UnivariateDistribution getUnivariateDistribution(Assignment assignment) {
        if (assignment.getValue(this.indicatorVar)==0.0) {
            return this.uniform;
        }else{
            return this.conditionalDistribution.getUnivariateDistribution(assignment);
        }
    }

    public String label(){
        return "Indicator of "+this.getConditionalDistribution().label();
    }
}
