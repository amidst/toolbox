package eu.amidst.core.inference;

import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 30/01/15.
 */
public interface InferenceAlgorithmForBN {

    public void runInference();

    public void setModel(BayesianNetwork model);

    public BayesianNetwork getModel();

    public void setEvidence(Assignment assignment);

    public <E extends UnivariateDistribution> E getPosterior(Variable var);

}