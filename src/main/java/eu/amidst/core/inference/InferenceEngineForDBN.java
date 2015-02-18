package eu.amidst.core.inference;

import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.variables.DynamicAssignment;
import eu.amidst.core.variables.Variable;

import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 12/02/15.
 */
public final class InferenceEngineForDBN {


    private static InferenceAlgorithmForDBN inferenceAlgorithmForDBN;// = new VMP();

    private static Stream<DynamicAssignment> dataSequence;


    public static InferenceAlgorithmForDBN getInferenceAlgorithmForDBN() {
        return inferenceAlgorithmForDBN;
    }

    public static void setInferenceAlgorithmForDBN(InferenceAlgorithmForDBN inferenceAlgorithmForDBN) {
        InferenceEngineForDBN.inferenceAlgorithmForDBN = inferenceAlgorithmForDBN;
    }

    public static void reset() {inferenceAlgorithmForDBN.reset();}

    public static void runInference(){
        inferenceAlgorithmForDBN.runInference();
    }

    public static void setModel(DynamicBayesianNetwork model){
        inferenceAlgorithmForDBN.setModel(model);
    }

    public static DynamicBayesianNetwork getModel(){
        return inferenceAlgorithmForDBN.getModel();
    }

    public static void addDynamicEvidence(DynamicAssignment assignment){
        inferenceAlgorithmForDBN.addDynamicEvidence(assignment);
    }

    public static <E extends UnivariateDistribution> E getFilteredPosterior(Variable var){
        return inferenceAlgorithmForDBN.getFilteredPosterior(var);
    }

    public static <E extends UnivariateDistribution> E getPredictivePosterior(Variable var, int nTimesAhead){
        return inferenceAlgorithmForDBN.getPredictivePosterior(var,nTimesAhead);
    }

    public static int getTimeIDOfPosterior(){
        return inferenceAlgorithmForDBN.getTimeIDOfPosterior();
    }

    public static int getTimeIDOfLastEvidence(){
        return inferenceAlgorithmForDBN.getTimeIDOfLastEvidence();
    }


    public static void setDataSequence(Stream<DynamicAssignment> dataSequence_){
        dataSequence=dataSequence_;
    }

    public static <E extends UnivariateDistribution> Stream<E> getStreamOfFilteredPosteriors(Variable var){
        return dataSequence.map( data -> {
            inferenceAlgorithmForDBN.addDynamicEvidence(data);
            inferenceAlgorithmForDBN.runInference();
            return inferenceAlgorithmForDBN.getFilteredPosterior(var);
        });
    }

    public static <E extends UnivariateDistribution> Stream<E> getStreamOfPredictivePosteriors(Variable var, int nTimesAhead){
        return dataSequence.map( data -> {
            inferenceAlgorithmForDBN.addDynamicEvidence(data);
            inferenceAlgorithmForDBN.runInference();
            return inferenceAlgorithmForDBN.getPredictivePosterior(var, nTimesAhead);
        });
    }

    public static <E extends UnivariateDistribution> E getLastFilteredPosteriorInTheSequence(Variable var){
        final UnivariateDistribution[] dist = new UnivariateDistribution[1];
        getStreamOfFilteredPosteriors(var).forEach(e -> dist[0]=e);
        return (E)dist[0];
    }

    public static <E extends UnivariateDistribution> E getLastPredictivePosteriorInTheSequence(Variable var, int nTimesAhead){
        final UnivariateDistribution[] dist = new UnivariateDistribution[1];
        getStreamOfPredictivePosteriors(var, nTimesAhead).forEach(e -> dist[0]=e);
        return (E)dist[0];
    }

}
