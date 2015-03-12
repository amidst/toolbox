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

import eu.amidst.core.exponentialfamily.EF_Normal_NormalParents;
import eu.amidst.core.utils.CheckVariablesOrder;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <h2>This class implements a Conditional Linear Gaussian distribution, i.e. a distribution of a normal variable with
 * continuous normal parents.</h2>
 *
 * TODO Reimplement the management of coffParents using a Map<Variale,Double>
 * @author Antonio Fernández
 * @version 1.0
 * @since 2014-11-4
 */
public class ConditionalLinearGaussian extends ConditionalDistribution {


    /**
     * The "intercept" parameter of the distribution
     */
    private double beta0;

    /**
     * The set of coefficients, one for each parent
     */
    private Map<Variable, Double> betas;

    /**
     * The variance of the variable (it does not depends on the parents)
     */
    private double variance;

    /**
     * The class constructor.
     * @param var1 The variable of the distribution.
     * @param parents1 The set of parents of the variable.
     */
    public ConditionalLinearGaussian(Variable var1, List<Variable> parents1) {

        this.var = var1;
        this.parents = parents1;
        this.beta0 = 0;
        betas = new HashMap<>();
        for(Variable parent: parents1){
            betas.put(parent,1.0);
        }
        this.variance = 1.0;

        //Make them unmodifiable
        this.parents = Collections.unmodifiableList(CheckVariablesOrder.orderListOfVariables(this.parents));
    }

    /**
     * Gets the beta0 (intercept) of the distribution.
     * @return A <code>double</code> value of beta0.
     */
    public double getBeta0() {
        return beta0;
    }

    /**
     * Sets the beta0 of the distribution.
     * @param beta0_ A <code>double</code> value for beta0.
     */
    public void setBeta0(double beta0_) {
        this.beta0 = beta0_;
    }

    /**
     * Gets the beta coefficients for the parent variables.
     * @return A hashmap of <code>Variable, Double</code> with the beta coefficients for each parent.
     */
    public Map<Variable, Double> getBetas() {
        return betas;
    }

    /**
     * Sets the beta coefficients of the distribution
     * @param betas_ A hashmap of <code>Variable, Double</code> with the coefficients, one for each parent.
     */
    public void setBetas(Map<Variable, Double> betas_) {
        if(betas_.size() != this.betas.size())
            throw new UnsupportedOperationException("The number of beta parameters for the CLG distribution" +
                    " does not match with the number of parents");
        this.betas = betas_;
    }

    public void setBetaForParent(Variable parent, double beta){
        betas.put(parent, beta);
    }

    public double getBetaForParent(Variable parent){
        return betas.get(parent);
    }

    public double[] getArrayOfBetas(){
        return (double[])betas.keySet().stream().sorted((a, b) -> a.getVarID() - b.getVarID()).
                mapToDouble(var->betas.get(var)).toArray();
    }
    /**
     * Gets the standard deviation of the variable.
     * @return A <code>double</code> value with the standard deviation.
     */
    public double getSd() {
        return Math.sqrt(variance);
    }

    /**
     * Sets the standard deviation of the variable.
     * @param sd1 A <code>double</code> value with the standard deviation.
     */
    public void setSd(double sd1) {
        this.variance = sd1*sd1;
    }

    /**
     * Gets the standard deviation of the variable.
     * @return A <code>double</code> value with the standard deviation.
     */
    public double getVariance() {
        return variance;
    }

    /**
     * Sets the standard deviation of the variable.
     * @param variance1 A <code>double</code> value with the standard deviation.
     */
    public void setVariance(double variance1) {
        this.variance = variance1;
    }

    /**
     * Gets the corresponding univariate normal distribution after conditioning the distribution to a parent assignment.
     * @param parentsAssignment An <code>Assignment</code> for the parents.
     * @return A <code>Normal</code> object with the univariate distribution.
     */
    public Normal getNormal(Assignment parentsAssignment) {

        double mean = beta0;
        Normal univariateNormal = new Normal(var);

        for (Variable v : parents) {
            mean = mean + betas.get(v) * parentsAssignment.getValue(v);
        }

        univariateNormal.setVariance(variance);
        univariateNormal.setMean(mean);

        return (univariateNormal);
    }


    @Override
    //Note that the intercept has not been included as a free parameter.
    public int getNumberOfFreeParameters() {
        return(betas.size() + 1);
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
        return "CLG";
    }

    @Override
    public void randomInitialization(Random random) {
        this.beta0 = random.nextGaussian();

        for(Variable parent:this.parents){
            this.betas.put(parent, random.nextGaussian());
        }
        //this.variance = Math.pow(random.nextDouble()+0.1, 2);
        this.variance = random.nextDouble()+0.1;
    }

    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append("[ alpha = " +this.getBeta0() + ", ");

        for(Variable parent:this.parents){
            str.append("beta_" + parent.getName() + " = " + this.getBetas().get(parent) + ", ");
        }
        str.append("variance = " + this.getVariance() + " ]");

        return str.toString();
    }

    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist.getClass().getName().equals("eu.amidst.core.distribution.ConditionalLinearGaussian"))
            return this.equalDist((ConditionalLinearGaussian)dist,threshold);
        return false;
    }

    public boolean equalDist(ConditionalLinearGaussian dist, double threshold) {
        boolean equals = false;
        if (Math.abs(this.getBeta0() - dist.getBeta0()) <= threshold && Math.abs(this.getVariance() - dist.getVariance()) <= threshold) {
            equals = true;
            for(Variable parent:this.parents){
                equals = equals && Math.abs(this.betas.get(parent) - dist.getBetas().get(parent)) <= threshold;
            }
        }
        return equals;
    }

    @Override
    public EF_Normal_NormalParents toEFConditionalDistribution() {

        for(Variable parent:this.parents){
            if(!parent.isNormal()){
                throw new UnsupportedOperationException("Only CLGs with normal parents can be converted into EF Conditional Distributions");
            }
        }

        EF_Normal_NormalParents ef_normal_normalParents = new EF_Normal_NormalParents(this.getVariable(), this.getConditioningVariables());

        EF_Normal_NormalParents.CompoundVector naturalParameters = ef_normal_normalParents.createEmtpyCompoundVector();

        double beta_0 = this.getBeta0();
        double[] betas = this.getArrayOfBetas();
        double variance = this.getVariance();

        /*
         * 1) theta_0
         */
        double theta_0 = beta_0 / variance;
        naturalParameters.setThetaBeta0_NatParam(theta_0);

        /*
         * 2) theta_0Theta
         */
        double variance2Inv =  1.0/(2*variance);
        //IntStream.range(0,betas.length).forEach(i-> betas[i]*=(beta_0*variance2Inv));
        double[] theta0_beta = Arrays.stream(betas).map(w->-w*beta_0/variance).toArray();
        naturalParameters.setThetaBeta0Beta_NatParam(theta0_beta);

        /*
         * 3) theta_Minus1
         */
        double theta_Minus1 = -variance2Inv;

        /*
         * 4) theta_beta & 5) theta_betaBeta
         */
        naturalParameters.setThetaCov_NatParam(theta_Minus1,betas, variance2Inv);

        ef_normal_normalParents.setNaturalParameters(naturalParameters);
        return ef_normal_normalParents;

    }

}
