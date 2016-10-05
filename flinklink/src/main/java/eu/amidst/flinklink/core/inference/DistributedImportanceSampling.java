package eu.amidst.flinklink.core.inference;

import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.inference.ImportanceSamplingCLG;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.*;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.log4j.BasicConfigurator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dario on 27/4/16.
 */
public class DistributedImportanceSampling {

    private static final long serialVersionUID = 369279637623979372L;

    private BayesianNetwork model;
    private BayesianNetwork samplingModel;
    private boolean sameSamplingModel;

    private List<Variable> causalOrder;

    private int seed = 0;
    private int sampleSize = 10000;

    private List<Variable> variablesAPosteriori;
    private List<SufficientStatistics> SSvariablesAPosteriori;

    private Assignment evidence;
    private double logProbOfEvidence;

    public void setModel(BayesianNetwork model) {
        this.model = model;
        this.samplingModel = model;
        this.sameSamplingModel = true;
    }

    public void setSamplingModel(BayesianNetwork samplingModel) {
        this.samplingModel = samplingModel;
        this.sameSamplingModel = samplingModel.equalBNs(this.model,1E-10);
    }

    public void setEvidence(Assignment evidence) {
        this.evidence = evidence;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public void setVariablesAPosteriori(List<Variable> variablesAPosteriori) {
        this.variablesAPosteriori = variablesAPosteriori;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public void runInference() throws Exception {

        BasicConfigurator.configure();
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        final int numberOfFlinkNodes = env.getParallelism();
        System.out.println("Flink parallel nodes: " + numberOfFlinkNodes);

        SSvariablesAPosteriori = env.generateSequence(0,numberOfFlinkNodes)
                .map(new LocalImportanceSampling(model, variablesAPosteriori, evidence, sampleSize/numberOfFlinkNodes, seed))
                .reduce(new ReduceFunction<List<SufficientStatistics>>() {
                    public List<SufficientStatistics> reduce(List<SufficientStatistics> ssList1, List<SufficientStatistics> ssList2) {

                        List<SufficientStatistics> result = new ArrayList<>(ssList1.size());

                        for (int i = 0; i < ssList1.size(); i++) {
                            SufficientStatistics sufficientStatistics = Serialization.deepCopy(ssList1.get(i));
                            sufficientStatistics.sum(ssList2.get(i));

                            ArrayVector result2 = RobustOperations.robustSumOfMultinomialLogSufficientStatistics((ArrayVector)ssList1.get(i),(ArrayVector)ssList2.get(i));
                            result.add(result2);

//                            if(i==0) {
//                                System.out.println(Arrays.toString(ImportanceSamplingCLG.robustNormalizationOfLogProbabilitiesVector((ArrayVector)ssList1.get(0)).toArray()));
//                                System.out.println(Arrays.toString(ImportanceSamplingCLG.robustNormalizationOfLogProbabilitiesVector((ArrayVector)ssList2.get(0)).toArray()));
//                                System.out.println(Arrays.toString(ImportanceSamplingCLG.robustNormalizationOfLogProbabilitiesVector((ArrayVector)result.get(0)).toArray()));
//                            }
                        }

                        return  result;

                    }
                }).collect().get(0);

    }

    static class LocalImportanceSampling implements MapFunction<Long, List<SufficientStatistics>> {

        private BayesianNetwork model;
        private List<Variable> variablesAPosteriori;
        private Assignment evidence;
        private int seed;
        private int numberOfSamples;

        LocalImportanceSampling(BayesianNetwork model, List<Variable> variablesAPosteriori, Assignment evidence, int numberOfSamples, int seed) {
            this.model = model;
            this.variablesAPosteriori = variablesAPosteriori;
            this.evidence = evidence;
            this.numberOfSamples = numberOfSamples;
            this.seed = seed;
        }

        @Override
        public List<SufficientStatistics> map(Long value) throws Exception {

            ImportanceSamplingCLG localImportanceSampling = new ImportanceSamplingCLG();
            localImportanceSampling.setModel(model);
            localImportanceSampling.setSeed(seed + value.intValue());
            localImportanceSampling.setSampleSize(numberOfSamples);

            localImportanceSampling.setVariablesAPosteriori(variablesAPosteriori);
            localImportanceSampling.setEvidence(evidence);

            localImportanceSampling.runInference();

            return localImportanceSampling.getSSMultinomialVariablesAPosteriori();
        }
    }

    public <E extends UnivariateDistribution> E getPosterior(Variable variable) {

        EF_UnivariateDistribution ef_univariateDistribution = variable.newUnivariateDistribution().toEFUnivariateDistribution();

        // TODO Could we build this object in a general way for Multinomial and Normal?
        if (!variable.isMultinomial()) {
            throw new UnsupportedOperationException("DistributedImportanceSampling.getPosterior() not supported yet for non-multinomial distributions");
        }

        ArrayVector sumSS = new ArrayVector(ef_univariateDistribution.sizeOfSufficientStatistics());
        sumSS.copy(SSvariablesAPosteriori.get(variablesAPosteriori.indexOf(variable)));
        //System.out.println(Arrays.toString(sumSS.toArray()));

        sumSS = RobustOperations.robustNormalizationOfLogProbabilitiesVector(sumSS);

        ef_univariateDistribution.setMomentParameters((SufficientStatistics)sumSS);
        Multinomial posteriorDistribution = ef_univariateDistribution.toUnivariateDistribution();
        posteriorDistribution.setProbabilities(Utils.normalize(posteriorDistribution.getParameters()));

        return (E)posteriorDistribution;
    }

    public static void main(String[] args) throws Exception {

        // Bayesian network model
        BayesianNetworkGenerator.setNumberOfMultinomialVars(8, 2);
        BayesianNetworkGenerator.setNumberOfGaussianVars(30);
        BayesianNetwork model = BayesianNetworkGenerator.generateNaiveBayes(2);
        System.out.println(model);

        DistributedImportanceSampling distributedImportanceSampling =  new DistributedImportanceSampling();
        distributedImportanceSampling.setModel(model);

        // Variables for querying the a posteriori distribution

        Variable classVar = model.getVariables().getVariableByName("ClassVar");
        Variable discreteVar1 = model.getVariables().getVariableByName("DiscreteVar1");

        List<Variable> variablesAPosteriori = new ArrayList<>(1);
        variablesAPosteriori.add(classVar);
        variablesAPosteriori.add(discreteVar1);

        distributedImportanceSampling.setVariablesAPosteriori(variablesAPosteriori);

        // Evidence
        Assignment evidence = new HashMapAssignment(2);
        Variable discreteVar0 = model.getVariables().getVariableByName("DiscreteVar0");
        Variable gaussianVar0 = model.getVariables().getVariableByName("GaussianVar0");

        evidence.setValue(discreteVar0,0);
        evidence.setValue(gaussianVar0,-6);

        distributedImportanceSampling.setEvidence(evidence);


        // Set parameters and run inference
        distributedImportanceSampling.setSeed(28235);
        distributedImportanceSampling.setSampleSize(50000);

        distributedImportanceSampling.runInference();

        // Show results
        System.out.println(distributedImportanceSampling.getPosterior(classVar));
        System.out.println(distributedImportanceSampling.getPosterior(discreteVar1));


    }
}
