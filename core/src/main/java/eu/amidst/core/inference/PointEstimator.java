package eu.amidst.core.inference;

import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;

/**
 * This interface handles a Bayesian posterior point-estimator in a {@link BayesianNetwork}.
 *
 * @see "eu.amidst.core.examples.inference"
 */
public interface PointEstimator {

    /**
     * Runs inference.
     */
    void runInference();

    /**
     * Sets the model for this PointEstimator.
     * @param model a {@link BayesianNetwork} model to which this PointEstimator will be applied.
     */
    void setModel(BayesianNetwork model);

    /**
     * Returns the original model of this PointEstimator.
     * @return a {@link BayesianNetwork} object that represents the original model of this InferenceAlgorithm.
     */
    BayesianNetwork getOriginalModel();

    /**
     * Sets the evidence for this PointEstimator.
     * @param assignment an {@link Assignment} object to which the evidence will be set.
     */
    void setEvidence(Assignment assignment);

    /**
     * Sets the parallel mode for this PointEstimator.
     * Note that this method is only implemented for the point-estimaros that can be run in parallel.
     * @param parallelMode_ a {@code boolean} that indicates whether this PointEstimator can be run in parallel (i.e., true) or not (i.e., false).
     */
    default void setParallelMode(boolean parallelMode_){

    }

    /**
     * Sets the seed.
     * @param seed an {@code int} that represents the seed value to be set.
     */
    void setSeed(int seed);

    /**
     * Returns the log probability of the computed estimate.
     * @return an {@link Assignment} with the computed estimate.
     */
    Assignment getEstimate();

    /**
     * Returns the log probability of the computed estimate.
     * @return the log probability of the computed estimate.
     */
    double getLogProbabilityOfEstimate();


}
