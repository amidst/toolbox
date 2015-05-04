package eu.amidst.core.inference;

import eu.amidst.core.models.BayesianNetwork;

/**
 * Created by andresmasegosa on 4/5/15.
 */
public interface Sampler {

    BayesianNetwork getSamplingModel();
    
}
