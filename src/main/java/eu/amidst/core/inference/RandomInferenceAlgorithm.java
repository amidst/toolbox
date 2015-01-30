package eu.amidst.core.inference;

import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 30/01/15.
 */
public class RandomInferenceAlgorithm implements InferenceAlgorithmForBN{

    BayesianNetwork model;
    Assignment assignment;

    @Override
    public void compileModel() {

    }

    @Override
    public void setModel(BayesianNetwork model_) {
        this.model=model_;
    }

    @Override
    public void setEvidence(Assignment assignment_) {
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(model);
        sampler.setSeed(0);
        sampler.setParallelMode(false);
        assignment = sampler.sampleToDataBase(1).stream().findFirst().get();
    }

    @Override
    public <E extends UnivariateDistribution> E getPosterior(Variable var) {
        return (E)this.model.getDistribution(var).getUnivariateDistribution(this.assignment);
    }

}
