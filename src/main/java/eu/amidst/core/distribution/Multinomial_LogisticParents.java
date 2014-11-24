package eu.amidst.core.distribution;

import com.sun.org.apache.xpath.internal.operations.Mult;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.Collections;
import java.util.List;

public class Multinomial_LogisticParents extends ConditionalDistribution {

    private double[] intercept;

    private double[][] coeffParents;

    /**
     * The class constructor.
     *
     * @param var_     The variable of the distribution.
     * @param parents_ The set of parents of the variable.
     */
    public Multinomial_LogisticParents(Variable var_, List<Variable> parents_) {

        if (parents_.size() == 0)
            throw new UnsupportedOperationException("A multinomial logistic distribution can not be creadted from a empty set of parents.");

        this.var = var_;
        this.parents = parents_;
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
        this.parents = Collections.unmodifiableList(this.parents);
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
    public String label() {
        return "Multinomial Logistic";
    }

    public String toString() {

        String str = "";

        for (int i = 0; i < this.var.getNumberOfStates() - 1; i++) {
            str = str + "[ alpha = " + this.getIntercept(i);
            for (int j = 0; j < this.getCoeffParents(i).length; j++) {
                str = str + ", beta = " + this.getCoeffParents(i)[j];
            }
            str = str + "]\n";
        }
        return str;
    }
}
