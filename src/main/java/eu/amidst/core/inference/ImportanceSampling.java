package eu.amidst.core.inference;

import eu.amidst.core.distribution.*;
import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.utils.LocalRandomGenerator;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by afa on 10/2/15.
 */

public class ImportanceSampling implements InferenceAlgorithmForBN {

    private BayesianNetwork model;
    private BayesianNetwork samplingModel;
    private int sampleSize;
    private List<Variable> causalOrder;
    public Stream<WeightedAssignment> weightedSampleStream;
    private int seed = 0;
    //TODO The sampling distributions must be restricted to the evidence
    private Assignment evidence;
    private boolean parallelMode = true;


    private class WeightedAssignment {
        private HashMapAssignment assignment;
        private double weight;

        public WeightedAssignment(HashMapAssignment assignment_, double weight_){
            this.assignment = assignment_;
            this.weight = weight_;
        }

        public String toString() {
            StringBuilder str = new StringBuilder();
            str.append("[ ");

            for (Map.Entry<Variable, Double> entry : this.assignment.entrySet()) {
                str.append(entry.getKey().getName() + " = " + entry.getValue());
                str.append(", ");
            }
            str.append("Weight = " + weight + " ]");
            return str.toString();
        }
    }

    public void setParallelMode(boolean parallelMode_) {
        this.parallelMode = parallelMode_;
    }

    public ImportanceSampling() {

    }
    public void setSamplingModel(BayesianNetwork samplingModel_) {
        this.samplingModel = samplingModel_;
        this.causalOrder = Utils.getCausalOrder(samplingModel.getDAG());
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    private WeightedAssignment getWeightedAssignment(Random random) {

        HashMapAssignment samplingAssignment = new HashMapAssignment(1);
        HashMapAssignment modelAssignment = new HashMapAssignment(1);
        double numerator = 1.0;
        double denominator = 1.0;

        for (Variable samplingVar : causalOrder) {
            Variable modelVar = this.model.getStaticVariables().getVariableById(samplingVar.getVarID());
            ConditionalDistribution samplingDistribution = this.samplingModel.getConditionalDistribution(samplingVar);
            UnivariateDistribution univariateSamplingDistribution = samplingDistribution.getUnivariateDistribution(samplingAssignment);

            double simulatedValue;
            simulatedValue = univariateSamplingDistribution.sample(random);
            denominator = denominator/univariateSamplingDistribution.getProbability(simulatedValue);
            UnivariateDistribution univariateModelDistribution = this.model.getConditionalDistribution(modelVar).getUnivariateDistribution(modelAssignment);
            numerator = numerator * univariateModelDistribution.getProbability(simulatedValue);
            modelAssignment.setValue(modelVar,simulatedValue);
            samplingAssignment.setValue(samplingVar, simulatedValue);
        }
        double weight = numerator*denominator;
        WeightedAssignment weightedAssignment = new WeightedAssignment(samplingAssignment,weight);
        return weightedAssignment;
    }

    private void computeWeightedSampleStream() {

        LocalRandomGenerator randomGenerator = new LocalRandomGenerator(seed);
        weightedSampleStream =  IntStream.range(0, sampleSize).mapToObj(i -> getWeightedAssignment(randomGenerator.current()));
        if (parallelMode) weightedSampleStream = weightedSampleStream.parallel();
    }

    @Override
    public void runInference() {
       this.computeWeightedSampleStream();
    }

    @Override
    public void setModel(BayesianNetwork model_) {
        this.model = model_;
    }

    @Override
    public BayesianNetwork getOriginalModel() {
        return this.model;
    }

    @Override
    public void setEvidence(Assignment evidence_) {
        this.evidence = evidence_;
    }

    @Override
    //TODO For continuous variables, instead of returning a Gaussian distributions, we should return a Mixture of Gaussians instead!!!
    public <E extends UnivariateDistribution> E getPosterior(Variable var) {

        Variable samplingVar = this.samplingModel.getStaticVariables().getVariableById(var.getVarID());
        // TODO Could we build this object in a general way for Multinomial and Normal?
        EF_UnivariateDistribution ef_univariateDistribution;
        if (var.isMultinomial()){
            ef_univariateDistribution = new EF_Multinomial(samplingVar);
        }
        else if (var.isNormal()) {
            ef_univariateDistribution = new EF_Normal(samplingVar);
        }
        else {
            throw new IllegalArgumentException("Variable type not allowed.");
        }

        AtomicInteger dataInstanceCount = new AtomicInteger(0);
        SufficientStatistics sumSS = weightedSampleStream
                .peek(w -> {
                    dataInstanceCount.getAndIncrement();
                })
                .map(e -> {
                    SufficientStatistics SS = ef_univariateDistribution.getSufficientStatistics(e.assignment);
                    SS.multiplyBy(e.weight);
                    return SS;
                })
                .reduce(SufficientStatistics::sumVector).get();

        sumSS.divideBy(dataInstanceCount.get());

        ef_univariateDistribution.setMomentParameters(sumSS);

        Distribution posteriorDistribution = ef_univariateDistribution.toUnivariateDistribution();

        //Normalize Multinomial distributions
        if(var.isMultinomial()) {
            ((Multinomial) posteriorDistribution).
                    setProbabilities(Utils.normalize(((Multinomial) posteriorDistribution).getProbabilities()));
        }

        return (E)posteriorDistribution;
    }
}
