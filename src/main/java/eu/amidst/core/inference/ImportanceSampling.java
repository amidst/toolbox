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
            ConditionalDistribution samplingDistribution = this.samplingModel.getDistribution(samplingVar);
            UnivariateDistribution univariateSamplingDistribution = samplingDistribution.getUnivariateDistribution(samplingAssignment);

            double simulatedValue;
            simulatedValue = univariateSamplingDistribution.sample(random);
            denominator = denominator/univariateSamplingDistribution.getProbability(simulatedValue);
            UnivariateDistribution univariateModelDistribution = this.model.getDistribution(modelVar).getUnivariateDistribution(modelAssignment);
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
    public BayesianNetwork getModel() {
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
        else if (var.isGaussian()) {
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

        Distribution posteriorDistribution = EF_DistributionBuilder.toUnivariateDistribution(ef_univariateDistribution);

        //Normalize Multinomial distributions
        if(var.isMultinomial()) {
            ((Multinomial) posteriorDistribution).
                    setProbabilities(Utils.normalize(((Multinomial) posteriorDistribution).getProbabilities()));
        }

        return (E)posteriorDistribution;
    }

    private static BayesianNetwork getNoisyModel() throws IOException, ClassNotFoundException {

        BayesianNetwork samplingBN = BayesianNetworkLoader.loadFromFile("networks/IS.bn");
        StaticVariables variables = samplingBN.getStaticVariables();
        Variable A = variables.getVariableByName("A");
        Variable B = variables.getVariableByName("B");
        Variable C = variables.getVariableByName("C");
        Variable D = variables.getVariableByName("D");
        Variable E = variables.getVariableByName("E");

        // Variable A
        Multinomial_MultinomialParents distA = samplingBN.getDistribution(A);
        distA.getProbabilities()[0].setProbabilities(new double[]{0.15, 0.85});

        // Variable B
        Multinomial_MultinomialParents distB = samplingBN.getDistribution(B);
        distB.getMultinomial(0).setProbabilities(new double[]{0.15,0.85});
        distB.getMultinomial(1).setProbabilities(new double[]{0.75,0.25});

        // Variable C
        Normal_MultinomialParents distC = samplingBN.getDistribution(C);
        distC.getNormal(0).setMean(3.1);
        distC.getNormal(0).setSd(0.9660254037);
        distC.getNormal(1).setMean(2.1);
        distC.getNormal(1).setSd(0.848683);

        //Variable D
        Normal_MultinomialNormalParents distD = samplingBN.getDistribution(D);
        distD.getNormal_NormalParentsDistribution(0).setIntercept(2.1);
        distD.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{2.1});
        distD.getNormal_NormalParentsDistribution(0).setSd(1.1);

        distD.getNormal_NormalParentsDistribution(1).setIntercept(0.6);
        distD.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{1.6});
        distD.getNormal_NormalParentsDistribution(1).setSd(1.5142);

        //Variable E
        Normal_NormalParents distE  = samplingBN.getDistribution(E);
        distE.setIntercept(2.4);
        distE.setCoeffParents(new double[]{4.1});
        distE.setSd(1.2832);

        return(samplingBN);
    }


    public static void main(String args[]) throws IOException, ClassNotFoundException {

        //-------------------------------------------------------------------------------------

        BayesianNetwork model = BayesianNetworkLoader.loadFromFile("networks/IS.bn");

        System.out.println("============================================================");
        System.out.println("================= MODEL DISTRIBUTIONS ======================");
        System.out.println("============================================================");


        model.getDistributions().stream().forEach(e-> {
            System.out.println(e.getVariable().getName());
            System.out.println(e);
        });

        StaticVariables variables = model.getStaticVariables();

        Variable varA = variables.getVariableByName("A");
        Variable varB = variables.getVariableByName("B");
        Variable varC = variables.getVariableByName("C");
        Variable varD = variables.getVariableByName("D");
        Variable varE = variables.getVariableByName("E");

        HashMapAssignment evidence = new HashMapAssignment(0);

        //evidence.setValue(varA,1.0);
        //evidence.setValue(varB,1.0);



        //-------------------------------------------------------------------------------------

        BayesianNetwork samplingModel = ImportanceSampling.getNoisyModel();

        System.out.println("============================================================");
        System.out.println("== SAMPLING DISTRIBUTIONS (MODEL DISTRIBUTIONS WITH NOISE) =");
        System.out.println("============================================================");

        samplingModel.getDistributions().stream().forEach(e-> {
            System.out.println(e.getVariable().getName());
            System.out.println(e);
        });

        //-------------------------------------------------------------------------------------

        System.out.println("============================================================");
        System.out.println("================= IMPORTANCE SAMPLING ======================");
        System.out.println("============================================================");

        ImportanceSampling IS = new ImportanceSampling();
        IS.setModel(model);
        IS.setSamplingModel(samplingModel);
        IS.setSampleSize(200000);
        IS.setEvidence(evidence);
        IS.setParallelMode(false);


        IS.runInference();
        UnivariateDistribution posteriorDistribution = IS.getPosterior(varA);
        System.out.println("A: " + posteriorDistribution.toString());

        IS.runInference();
        posteriorDistribution = IS.getPosterior(varB);
        System.out.println("B: " + posteriorDistribution.toString());

        IS.runInference();
        posteriorDistribution = IS.getPosterior(varC);
        System.out.println("C: " + posteriorDistribution.toString());

        IS.runInference();
        posteriorDistribution = IS.getPosterior(varD);
        System.out.println("D: " + posteriorDistribution.toString());

        IS.runInference();
        posteriorDistribution = IS.getPosterior(varE);
        System.out.println("E: " + posteriorDistribution.toString());


    }

}
