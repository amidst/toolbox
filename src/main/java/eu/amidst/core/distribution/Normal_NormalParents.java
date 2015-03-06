/**
 ******************* ISSUE LIST **************************
 *
 * 1. In the constructor, should we initialize the CLG attributes in this way?
 *
 * 2. The name of the method computeProbabilityOf(..) is a bit confusing for continuous domains. It does not compute probabilities but the
 * value for the density function which is not a probability. However as this class implements this method of ConditionalDistribution,
 * we could leave like this.
 *
 * 3. QAPlug gives a warning when using the same name for a attribute and a given argument, e.g. this.var = var
 * ********************************************************
 */

package eu.amidst.core.distribution;

import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.exponentialfamily.EF_Normal_NormalParents;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * <h2>This class implements a Conditional Linear Gaussian distribution, i.e. a distribution of a normal variable with
 * continuous normal parents.</h2>
 *
 * TODO Reimplement the management of coffParents using a Map<Variale,Double>
 * @author Antonio Fernández
 * @version 1.0
 * @since 2014-11-4
 */
public class Normal_NormalParents extends ConditionalDistribution {


    /**
     * The "intercept" parameter of the distribution
     */
    private double intercept;

    /**
     * The set of coefficients, one for each parent
     */
    private double[] coeffParents;

    /**
     * The standard deviation of the variable (it does not depends on the parents)
     */
    private double sd;

    /**
     * The class constructor.
     * @param var1 The variable of the distribution.
     * @param parents1 The set of parents of the variable.
     */
    public Normal_NormalParents(Variable var1, List<Variable> parents1) {

        this.var = var1;
        this.parents = parents1;
        this.intercept = 0;
        coeffParents = new double[parents.size()];
        for (int i = 0; i < parents.size(); i++) {
            coeffParents[i] = 1;
        }
        this.sd = 1;

        //Make them unmodifiable
        this.parents = Collections.unmodifiableList(this.parents);
    }

    /**
     * Gets the intercept of the distribution.
     * @return A <code>double</code> value with the intercept.
     */
    public double getIntercept() {
        return intercept;
    }

    /**
     * Sets the intercept of the distribution.
     * @param intercept1 A <code>double</code> value with the intercept.
     */
    public void setIntercept(double intercept1) {
        this.intercept = intercept1;
    }

    /**
     * Gets the coefficients for the parent variables.
     * @return An array of <code>double</code> with the coefficients.
     */
    public double[] getCoeffParents() {
        return coeffParents;
    }

    /**
     * Sets the coefficients of the distribution
     * @param coeffParents1 An array of <code>double</code> with the coefficients, one for each parent.
     */
    public void setCoeffParents(double[] coeffParents1) {
        if(coeffParents1.length != this.coeffParents.length)
            throw new UnsupportedOperationException("The number of beta parametersParentVariables for the Normal_Normal distribution" +
                    " does not match with the number of parents");
        this.coeffParents = coeffParents1;
    }

    /**
     * Gets the standard deviation of the variable.
     * @return A <code>double</code> value with the standard deviation.
     */
    public double getSd() {
        return sd;
    }

    /**
     * Sets the standard deviation of the variable.
     * @param sd1 A <code>double</code> value with the standard deviation.
     */
    public void setSd(double sd1) {
        this.sd = sd1;
    }

    /**
     * Gets the corresponding univariate normal distribution after conditioning the distribution to a parent assignment.
     * @param parentsAssignment An <code>Assignment</code> for the parents.
     * @return A <code>Normal</code> object with the univariate distribution.
     */
    public Normal getNormal(Assignment parentsAssignment) {

        double mean = intercept;
        Normal univariateNormal = new Normal(var);
        int i = 0;

        for (Variable v : parents) {
            mean = mean + coeffParents[i] * parentsAssignment.getValue(v);
            i++;
        }

        univariateNormal.setSd(sd);
        univariateNormal.setMean(mean);

        return (univariateNormal);
    }


    @Override
    //Note that the intercept has not been included as a free parameter.
    public int getNumberOfFreeParameters() {
        return(coeffParents.length + 1);
    }


    /**
     * Computes the logarithm of the evaluated density function in a point after conditioning the distribution to a
     * given parent <code>Assignment</code>.
     * @param assignment An <code>Assignment</code>
     * @return A <code>double</code> with the logarithm of the corresponding density value.
     */
    @Override
    public double getLogConditionalProbability(Assignment assignment) {
        double value = assignment.getValue(this.var);
        return (getNormal(assignment).getLogProbability(value));
    }

    @Override
    public UnivariateDistribution getUnivariateDistribution(Assignment assignment) {
        return this.getNormal(assignment);
    }

    public String label(){
        return "Normal|Normal";
    }

    @Override
    public void randomInitialization(Random random) {
        this.intercept = random.nextGaussian();
        for (int j = 0; j < this.coeffParents.length; j++) {
            this.coeffParents[j]=random.nextGaussian();
        }
        this.sd = random.nextDouble()+0.1;
    }

    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append("[ alpha = " +this.getIntercept() + ", ");

        for (int i=0;i<this.getCoeffParents().length;i++){
            str.append("beta"+(i+1)+" = "+ this.getCoeffParents()[i] + ", ");
        }
        str.append("sd = " + this.getSd() + " ]");

        return str.toString();
    }

    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist.getClass().getName().equals("eu.amidst.core.distribution.Normal_NormalParents"))
            return this.equalDist((Normal_NormalParents)dist,threshold);
        return false;
    }

    public boolean equalDist(Normal_NormalParents dist, double threshold) {
        boolean equals = false;
        if (Math.abs(this.getIntercept() - dist.getIntercept()) <= threshold && Math.abs(this.getSd() - dist.getSd()) <= threshold) {
            equals = true;
            for (int i = 0; i < this.getCoeffParents().length; i++) {
                equals = equals && Math.abs(this.coeffParents[i] - dist.coeffParents[i]) <= threshold;
            }
        }
        return equals;
    }

    @Override
    public EF_Normal_NormalParents toEFConditionalDistribution() {

        EF_Normal_NormalParents ef_normal_normalParents = new EF_Normal_NormalParents(this.getVariable(), this.getConditioningVariables());

        EF_Normal_NormalParents.CompoundVector naturalParameters = ef_normal_normalParents.createEmtpyCompoundVector();

        double beta_0 = this.getIntercept();
        double[] coeffParents = this.getCoeffParents();
        double sd = this.getSd();

        double variance = sd*sd;
        /*
         * 1) theta_0
         */
        double theta_0 = beta_0 / variance;
        naturalParameters.setThetaBeta0_NatParam(theta_0);

        /*
         * 2) theta_0Theta
         */
        double variance2Inv =  1.0/(2*variance);
        //IntStream.range(0,coeffParents.length).forEach(i-> coeffParents[i]*=(beta_0*variance2Inv));
        double[] theta0_beta = Arrays.stream(coeffParents).map(w->-w*beta_0/variance).toArray();
        naturalParameters.setThetaBeta0Beta_NatParam(theta0_beta);

        /*
         * 3) theta_Minus1
         */
        double theta_Minus1 = -variance2Inv;

        /*
         * 4) theta_beta & 5) theta_betaBeta
         */
        naturalParameters.setThetaCov_NatParam(theta_Minus1,coeffParents, variance2Inv);

        ef_normal_normalParents.setNaturalParameters(naturalParameters);
        return ef_normal_normalParents;

    }

}
