package eu.amidst.flinklink.core.inference;

import eu.amidst.core.inference.MAPInference;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Created by dario on 27/4/16.
 */
public class DistributedMAPInference {

    private BayesianNetwork model;
    static private List<Variable> causalOrder;

    private int sampleSize;
    private int seed = 0;
    private Random MAPrandom;
    private int numberOfIterations=100;
    private int numberOfStartingPoints = 50;

    private Assignment evidence;

    private long numberOfDiscreteVariables = 0;
    private long numberOfDiscreteVariablesInEvidence = 0;

    private boolean parallelMode = true;


    private List<Variable> MAPVariables;
    private Assignment MAPEstimate;
    private double MAPEstimateLogProbability;

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

    public Assignment getMAPEstimate() {
        return MAPEstimate;
    }

    public double getMAPEstimateLogProbability() {
        return MAPEstimateLogProbability;
    }

    public void runInference() throws Exception {

        MAPVariables = new ArrayList<>(1);
        MAPVariables.add(model.getVariables().getVariableByName("ClassVar"));

        BasicConfigurator.configure();
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        System.out.println(env.getParallelism());

        Tuple2<Assignment, Double> MAPResult = env.generateSequence(0,numberOfStartingPoints).map(new LocalMAPInference(model, MAPVariables, evidence, seed)).reduce( new ReduceFunction<Tuple2 <Assignment, Double>>() {
            public Tuple2<Assignment, Double> reduce(Tuple2 <Assignment, Double> tuple1, Tuple2 <Assignment, Double> tuple2) {
                if (tuple1.f1 > tuple2.f1) {
                    return tuple1;
                }
                else {
                    return tuple2;
                }
            }
        }).collect().get(0);

        MAPEstimate = MAPResult.f0;
        MAPEstimateLogProbability = MAPResult.f1;


    }


    static class LocalMAPInference implements MapFunction<Long, Tuple2<Assignment, Double>> {

        private BayesianNetwork model;
        private List<Variable> MAPVariables;
        private Assignment evidence;
        private int seed;

        public LocalMAPInference(BayesianNetwork model, List<Variable> MAPVariables, Assignment evidence, int seed) {
            this.model = model;
            this.MAPVariables = MAPVariables;
            this.seed = seed;
            this.evidence = evidence;
        }

        @Override
        public Tuple2<Assignment, Double> map(Long value) throws Exception {

            MAPInference localMAPInference = new MAPInference();
            localMAPInference.setModel(model);
            localMAPInference.setMAPVariables(MAPVariables);
            localMAPInference.setSeed(seed + value.intValue());
            localMAPInference.setNumberOfStartingPoints(1);
            localMAPInference.setEvidence(evidence);

            localMAPInference.runInference();

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
        distributedMAPInference.setNumberOfIterations(300);
        distributedMAPInference.setNumberOfStartingPoints(40);

        distributedMAPInference.runInference();


        System.out.println(distributedMAPInference.getMAPEstimate().outputString(MAPVariables));
        System.out.println(distributedMAPInference.getMAPEstimateLogProbability());

    }

}
