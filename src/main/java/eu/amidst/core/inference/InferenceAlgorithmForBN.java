package eu.amidst.core.inference;

import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 30/01/15.
 */
public interface InferenceAlgorithmForBN {

    public void compileModel();

    public void setModel(BayesianNetwork model);

    public void setEvidence(Assignment assignment);

    public <E extends UnivariateDistribution> E getPosterior(Variable var);

}
