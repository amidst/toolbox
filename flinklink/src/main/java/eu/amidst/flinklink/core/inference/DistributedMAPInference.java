package eu.amidst.flinklink.core.inference;

import eu.amidst.core.inference.MAPInferenceRobust;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.varia.NullAppender;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Created by dario on 27/4/16.
 */
public class DistributedMAPInference {

    private BayesianNetwork model;
    private List<Variable> MAPVariables;
    private Assignment evidence;

    private int seed = 0;
    private int numberOfIterations = 100;
    private int numberOfStartingPoints = 50;

    private Assignment MAPEstimate;
    private double MAPEstimateLogProbability;

    private int numberOfCoresToUse = 2;

    public void setNumberOfCores(int numberOfCoresToUse) {
        this.numberOfCoresToUse = numberOfCoresToUse;
    }

    public void setModel(BayesianNetwork model) {
        this.model = model;
    }

    public void setEvidence(Assignment evidence_) {
        this.evidence = evidence_;
    }

    public void setMAPVariables(List<Variable> MAPVariables) {
        this.MAPVariables = MAPVariables;
    }

    public void setNumberOfIterations(int numberOfIterations) {
        this.numberOfIterations = numberOfIterations;
    }

    public void setNumberOfStartingPoints(int numberOfStartingPoints) {
        this.numberOfStartingPoints = numberOfStartingPoints;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public Assignment getEstimate() {
        return MAPEstimate;
    }

    public double getLogProbabilityOfEstimate() {
        return MAPEstimateLogProbability;
    }

    public void runInference() throws Exception {
        this.runInference(MAPInferenceRobust.SearchAlgorithm.HC_LOCAL);
    }

    public void runInference(MAPInferenceRobust.SearchAlgorithm searchAlgorithm) throws Exception {

        //MAPVariables = new ArrayList<>(1);
        //MAPVariables.add(model.getVariables().getVariableByName("ClassVar"));

        BasicConfigurator.configure(new NullAppender());

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();


        env.setParallelism(this.numberOfCoresToUse);
        env.getConfig().disableSysoutLogging();



        int endSequence = numberOfStartingPoints;
        if (searchAlgorithm.equals(MAPInferenceRobust.SearchAlgorithm.SAMPLING)) {
            endSequence = 2 * numberOfCoresToUse;
        }

        Tuple2<Assignment, Double> MAPResult = env.generateSequence(1, endSequence)
                .map(new LocalMAPInference(model, MAPVariables, searchAlgorithm, evidence, numberOfIterations, seed, searchAlgorithm.equals(MAPInferenceRobust.SearchAlgorithm.SAMPLING) ? numberOfStartingPoints / (2 * numberOfCoresToUse) : 1))
//                        aa -> {
//                    if(searchAlgorithm.equals(MAPInferenceRobust.SearchAlgorithm.SAMPLING))
//                        return new LocalMAPInference(model, MAPVariables, searchAlgorithm, evidence, numberOfIterations, seed, numberOfStartingPoints/(2*numberOfCoresToUse));
//                    else
//                        return new LocalMAPInference(model, MAPVariables, searchAlgorithm, evidence, numberOfIterations, seed, 1);
//                      })
                .reduce(new ReduceFunction<Tuple2<Assignment, Double>>() {
                    public Tuple2<Assignment, Double> reduce(Tuple2<Assignment, Double> tuple1, Tuple2<Assignment, Double> tuple2) {
                        if (tuple1.f1 > tuple2.f1) {
                            return tuple1;
                        } else {
                            return tuple2;
                        }
                    }
                }).collect().get(0);

        this.seed = new Random(seed).nextInt();
        MAPEstimate = MAPResult.f0;
        MAPEstimateLogProbability = MAPResult.f1;

    }


    static class LocalMAPInference implements MapFunction<Long, Tuple2<Assignment, Double>> {

        private BayesianNetwork model;
        private List<Variable> MAPVariables;
        private Assignment evidence;
        private int seed;
        private int numberOfIterations;
        private String searchAlgorithm;
        private int numberOfStartingPoints;

        public LocalMAPInference(BayesianNetwork model, List<Variable> MAPVariables, MAPInferenceRobust.SearchAlgorithm searchAlgorithm, Assignment evidence, int numberOfIterations, int seed, int numberOfStartingPoints) {
            this.model = model;
            this.MAPVariables = MAPVariables;
            this.evidence = evidence;
            this.numberOfIterations = numberOfIterations;
            this.seed = seed;
            this.searchAlgorithm = searchAlgorithm.name();
            this.numberOfStartingPoints = numberOfStartingPoints;
        }

        @Override
        public Tuple2<Assignment, Double> map(Long value) throws Exception {

            MAPInferenceRobust localMAPInference = new MAPInferenceRobust();
            localMAPInference.setModel(model);
            localMAPInference.setMAPVariables(MAPVariables);
            localMAPInference.setSeed(seed + value.intValue());

            localMAPInference.setNumberOfStartingPoints(numberOfStartingPoints);

            localMAPInference.setNumberOfIterations(numberOfIterations);
            localMAPInference.setParallelMode(false);
            localMAPInference.setEvidence(evidence);

            localMAPInference.runInference(MAPInferenceRobust.SearchAlgorithm.valueOf(searchAlgorithm));

            Assignment MAPEstimate = localMAPInference.getEstimate();
            double logProbMAPEstimate = localMAPInference.getLogProbabilityOfEstimate();

            return new Tuple2<>(MAPEstimate, logProbMAPEstimate);
        }
    }

    public static void main(String[] args) throws Exception {

        // Bayesian network model
        BayesianNetworkGenerator.setNumberOfMultinomialVars(8, 2);
        BayesianNetworkGenerator.setNumberOfGaussianVars(30);
        BayesianNetwork model = BayesianNetworkGenerator.generateNaiveBayes(2);
        System.out.println(model);

        DistributedMAPInference distributedMAPInference =  new DistributedMAPInference();
        distributedMAPInference.setModel(model);

        // MAP Variables
        List<Variable> MAPVariables = new ArrayList<>(1);
        MAPVariables.add(model.getVariables().getVariableByName("ClassVar"));

        distributedMAPInference.setMAPVariables(MAPVariables);

        // Evidence
        Assignment evidence = new HashMapAssignment(2);
        evidence.setValue(model.getVariables().getVariableByName("DiscreteVar0"),0);
        evidence.setValue(model.getVariables().getVariableByName("GaussianVar0"),3);

        distributedMAPInference.setEvidence(evidence);


        // Set parameters and run inference
        distributedMAPInference.setSeed(28235);
        distributedMAPInference.setNumberOfIterations(300);
        distributedMAPInference.setNumberOfStartingPoints(40);

        distributedMAPInference.runInference();

        // Show results
        System.out.println(distributedMAPInference.getEstimate().outputString(MAPVariables));
        System.out.println(distributedMAPInference.getLogProbabilityOfEstimate());

    }

}
