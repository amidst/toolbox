package eu.amidst.flinklink.core.inference;

import eu.amidst.core.inference.MAPInference;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.variables.Assignment;
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

    static private BayesianNetwork model;
    static private List<Variable> causalOrder;

    private int sampleSize;
    static private int seed = 0;
    private Random MAPrandom;
    private int numberOfIterations=100;

    private Assignment evidence;

    private long numberOfDiscreteVariables = 0;
    private long numberOfDiscreteVariablesInEvidence = 0;

    private boolean parallelMode = true;


    static private List<Variable> MAPVariables;
    private Assignment MAPestimate;
    private double MAPestimateLogProbability;

    public static void main(String[] args) throws Exception {

        BayesianNetworkGenerator.setNumberOfMultinomialVars(8, 2);
        BayesianNetworkGenerator.setNumberOfGaussianVars(30);
        model = BayesianNetworkGenerator.generateNaiveBayes(2);
        System.out.println(model);

        MAPVariables = new ArrayList<>(1);
        MAPVariables.add(model.getVariables().getVariableByName("ClassVar"));

        BasicConfigurator.configure();
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        System.out.println(env.getParallelism());

        Tuple2<Assignment, Double> MAPResult = env.generateSequence(0,50).map(new LocalMAPInference(model, MAPVariables, seed)).reduce( new ReduceFunction<Tuple2 <Assignment, Double>>() {
            public Tuple2<Assignment, Double> reduce(Tuple2 <Assignment, Double> tuple1, Tuple2 <Assignment, Double> tuple2) {
                if (tuple1.f1 > tuple2.f1) {
                    return tuple1;
                }
                else {
                    return tuple2;
                }
            };
        }).collect().get(0);

        System.out.println(MAPResult.f0.outputString(MAPVariables));
        System.out.println(MAPResult.f1);


    }

    static class LocalMAPInference implements MapFunction<Long, Tuple2<Assignment, Double>> {

        private BayesianNetwork model;
        private List<Variable> MAPVariables;
        private int seed;

        public LocalMAPInference(BayesianNetwork model, List<Variable> MAPVariables, int seed) {
            this.model = model;
            this.MAPVariables = MAPVariables;
            this.seed = seed;
        }

        @Override
        public Tuple2<Assignment, Double> map(Long value) throws Exception {

            MAPInference localMAPInference = new MAPInference();
            localMAPInference.setModel(model);
            localMAPInference.setMAPVariables(MAPVariables);
            localMAPInference.setSeed(seed + value.intValue());
            localMAPInference.setSampleSize(1);

            localMAPInference.runInference();

            Assignment MAPEstimate = localMAPInference.getEstimate();
            double logProbMAPEstimate = localMAPInference.getLogProbabilityOfEstimate();

            return new Tuple2<>(MAPEstimate, logProbMAPEstimate);
        }
    }

}
