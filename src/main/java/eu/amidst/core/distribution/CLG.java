package eu.amidst.core.distribution;
import eu.amidst.core.header.statics.Variable;
import eu.amidst.core.header.statics.Assignment;
import java.util.List;

/**
 * Created by afa on 03/11/14.
 */
public class CLG implements ConditionalDistribution {

    private double intercept;
    private double[] coeffParents;
    private double sd;

    private Variable var;
    private List<Variable> parents;


    public CLG(Variable var, List<Variable> parents) {
        this.var = var;
        this.parents = parents;
    }

    public double getIntercept() {
        return intercept;
    }

    public void setIntercept(double intercept) {
        this.intercept = intercept;
    }

    public double[] getCoeffParents() {
        return coeffParents;
    }

    public void setCoeffParents(double[] coeffParents) {
        this.coeffParents = coeffParents;
    }

    public double getSd() {
        return sd;
    }

    public void setSd(double sd) {
        this.sd = sd;
    }

    public Normal getUnivariateNormal(Assignment parentsAssignment){

        double mean = intercept;
        Normal univariateNormal = new Normal(var);
        int i = 0;

        for (Variable v:parents) {
            mean = mean + coeffParents[i] * parentsAssignment.getValue(v);
            i++;
        }

        univariateNormal.setSd(sd);
        univariateNormal.setMean(mean);

        return(univariateNormal);
    }


    @Override
    public List<Variable> getConditioningVariables() {
        return parents;
    }

    @Override
    public double getProbability(double value, Assignment parentsAssignment) {
        return(getUnivariateNormal(parentsAssignment).getProbability(value));
    }

    @Override
    public double getLogProbability(double value, Assignment parentsAssignment) {
        return(getUnivariateNormal(parentsAssignment).getLogProbability(value));
    }

    @Override
    public Variable getVariable() {
        return var;
    }
}
