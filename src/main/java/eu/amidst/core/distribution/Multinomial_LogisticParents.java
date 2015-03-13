package eu.amidst.core.distribution;

import eu.amidst.core.utils.CheckVariablesOrder;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * TODO: Change coeffParents to be an array of Hashmaps
 */

public class Multinomial_LogisticParents extends ConditionalDistribution {

    private double[] intercept;

    private double[][] coeffParents;

    /**
     * The class constructor.
     *
     * @param var1     The variable of the distribution.
     * @param parents1 The set of parents of the variable.
     */
    public Multinomial_LogisticParents(Variable var1, List<Variable> parents1) {

        if (parents1.size() == 0) {
            throw new UnsupportedOperationException("A multinomial logistic distribution can not be created from a empty set of parents.");
        }

        this.var = var1;
        this.parents = parents1;
        this.intercept = new double[var.getNumberOfStates() - 1];
        this.coeffParents = new double[var.getNumberOfStates() - 1][parents.size()];

        for (int k = 0; k < var.getNumberOfStates() - 1; k++) {
            intercept[k] = 0;
            coeffParents[k] = new double[parents.size() + 1];
            for (int i = 0; i < parents.size(); i++) {
                coeffParents[k][i] = 1;
            }
        }
        //Make them unmodifiable
        this.parents = Collections.unmodifiableList(CheckVariablesOrder.orderListOfVariables(this.parents));
    }
    //TODO: I'm not sure about the free parameters in this distribution.
    @Override
    public int getNumberOfFreeParameters() {

        int n=0;
        for(int i=0;i<this.coeffParents.length;i++){
            n+=this.getCoeffParents(i).length;
        }
        return n;
    }

    public double getIntercept(int state) {
        return intercept[state];
    }

    public void setIntercept(int state, double intercept) {
        this.intercept[state] = intercept;
    }

    public double[] getCoeffParents(int state) {
        return coeffParents[state];
    }

    public void setCoeffParents(int state, double[] coeffParents) {
        this.coeffParents[state] = coeffParents;
    }

    public Multinomial getMultinomial(Assignment parentsAssignment) {

        double[] probs = new double[this.var.getNumberOfStates()];

        for (int i = 0; i < var.getNumberOfStates() - 1; i++) {
            probs[i] = intercept[i];
            int cont = 0;
            for (Variable v : parents) {
                probs[i] += coeffParents[i][cont] * parentsAssignment.getValue(v);
                cont++;
            }
        }

        probs = Utils.logs2probs(probs);

        Multinomial multinomial = new Multinomial(this.var);
        multinomial.setProbabilities(probs);

        return multinomial;
    }

    @Override
    public double getLogConditionalProbability(Assignment assignment) {
        double value = assignment.getValue(this.var);
        return (getMultinomial(assignment).getLogProbability(value));
    }

    @Override
    public UnivariateDistribution getUnivariateDistribution(Assignment assignment) {
        return this.getMultinomial(assignment);
    }

    @Override
    public String label() {
        return "Multinomial Logistic";
    }

    @Override
    public void randomInitialization(Random random) {
        for (int i = 0; i < this.coeffParents.length; i++) {
            this.intercept[i] = random.nextGaussian();
            for (int j = 0; j < this.coeffParents[i].length; j++) {
                this.coeffParents[i][j]=random.nextGaussian();
            }
        }
    }

    @Override
    public String toString() {

        StringBuilder str = new StringBuilder();
        str.append("");

        for (int i = 0; i < this.var.getNumberOfStates() - 1; i++) {
            str.append("[ alpha = " + this.getIntercept(i));
            for (int j = 0; j < this.getCoeffParents(i).length; j++) {
                str.append(", beta = " + this.getCoeffParents(i)[j]);
            }
            str.append("]\n");
        }
        return str.toString();
    }

    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist.getClass().getName().equals("eu.amidst.core.distribution.Multinomial_LogisticParents"))
            return this.equalDist((Multinomial_LogisticParents)dist,threshold);
        return false;
    }

    public boolean equalDist(Multinomial_LogisticParents dist, double threshold) {
        boolean equals = true;
        for (int i = 0; i < this.intercept.length; i++) {
            equals = equals && Math.abs(this.getIntercept(i) - dist.getIntercept(i)) <= threshold;
        }
        if (equals) {
            for (int i = 0; i < this.coeffParents.length; i++) {
                for (int j = 0; j < this.coeffParents[i].length; j++) {
                    equals = equals && Math.abs(this.coeffParents[i][j] - dist.coeffParents[i][j]) <= threshold;
                }
            }
        }
        return equals;
    }
}
