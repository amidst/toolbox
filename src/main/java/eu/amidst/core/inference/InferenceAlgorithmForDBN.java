package eu.amidst.core.inference;

import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.DynamicAssignment;
import eu.amidst.core.variables.Variable;

import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 30/01/15.
 */
public interface InferenceAlgorithmForDBN {

    public void runInference();

    public void setModel(DynamicBayesianNetwork model);

    public DynamicBayesianNetwork getModel();

    public void addDynamicEvidence(DynamicAssignment assignment);

    public void reset();

    public <E extends UnivariateDistribution> E getFilteredPosterior(Variable var);

    public <E extends UnivariateDistribution> E getPredictivePosterior(Variable var, int nTimesAhead);

    public int getTimeIDOfLastEvidence();

    public int getTimeIDOfPosterior();

}
