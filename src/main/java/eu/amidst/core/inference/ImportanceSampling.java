package eu.amidst.core.inference;

import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnMemoryListContainer;
import eu.amidst.core.distribution.*;
import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.BayesianNetworkLoader;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Created by afa on 10/2/15.
 */
public class ImportanceSampling implements InferenceAlgorithmForBN {

    private BayesianNetwork model;
    private DataOnMemoryListContainer simulatedSample;
    private long sampleSize;
    private Assignment evidence;
    private List<ConditionalDistribution> samplingDistributions;
    private ArrayList<Double> weights;

    private boolean parallelMode = true;

    public Assignment getEvidence() {
        return evidence;
    }

    public boolean isParallelMode() {
        return parallelMode;
    }

    public void setParallelMode(boolean parallelMode_) {
        this.parallelMode = parallelMode_;
    }

    public ImportanceSampling(BayesianNetwork model_, long sampleSize_) {

        this.model = model_;
        this.sampleSize = sampleSize_;
        Attributes attributes= this.model.getDAG().getStaticVariables().getAttributes();
        this.simulatedSample = new DataOnMemoryListContainer(attributes);
        this.weights = new ArrayList<>((int)this.sampleSize);
    }

    public List<ConditionalDistribution> getSamplingDistributions() {
        return samplingDistributions;
    }

    //We assume that we have these sampling distributions in a topological order!!!
    public void setSamplingDistributions(List<ConditionalDistribution> samplingDistributions_) {
        this.samplingDistributions = samplingDistributions_;
    }

    public long getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(long sampleSize) {
        this.sampleSize = sampleSize;
    }

    @Override
    public void runInference() {

        HashMapAssignment samplingAssignment;
        HashMapAssignment modelAssignment;

        Random random = new Random();
        for(int i=0;i<sampleSize;i++) {
            samplingAssignment = new HashMapAssignment(1);
            modelAssignment = new HashMapAssignment(1);
            double numerator = 1.0;
            double denominator = 1.0;

            for (ConditionalDistribution samplingDistribution : this.getSamplingDistributions()) {

                Variable samplingVar = samplingDistribution.getVariable();
                Variable modelVar = this.model.getStaticVariables().getVariableById(samplingVar.getVarID());

//                System.out.println("----------------------------");
//                System.out.println("SAMPLING VAR: " +samplingVar.getName());
//                System.out.println("MODEL VAR: " + modelVar.getName());

                //Denominator
                UnivariateDistribution univariateSamplingDistribution = samplingDistribution.getUnivariateDistribution(samplingAssignment);
                double simulatedValue = univariateSamplingDistribution.sample(random);


//                System.out.println("\nSAMPLING DISTRIBUTION ("+univariateSamplingDistribution.getVariable().getName()+ ")");
//                System.out.println(univariateSamplingDistribution);
//                System.out.println("\nSimulated value:" + simulatedValue);

                denominator = denominator/univariateSamplingDistribution.getProbability(simulatedValue);

                //Numerator
//                System.out.println("ASSIGNMENT");
//                for(Variable v:this.model.getStaticVariables().getListOfVariables()){
//                    System.out.println(modelAssignment.getValue(v));
//                }



                UnivariateDistribution univariateModelDistribution = this.model.getDistributions().get(samplingVar.getVarID()).getUnivariateDistribution(modelAssignment);

//                System.out.println("\nMODEL DISTRIBUTION ("+univariateModelDistribution.getVariable().getName()+ ")");
//                System.out.println(univariateModelDistribution);
//                System.out.println("\nProbability: "+univariateModelDistribution.getProbability(simulatedValue));


                numerator = numerator * univariateModelDistribution.getProbability(simulatedValue);
                modelAssignment.setValue(this.model.getStaticVariables().getVariableById(samplingVar.getVarID()),simulatedValue);
                samplingAssignment.setValue(samplingVar,simulatedValue);
            }

            double weight = numerator*denominator;
            this.weights.add(weight);
            simulatedSample.add(modelAssignment);
        }
    }

    @Override
    public void setModel(BayesianNetwork model_) {
        this.model=model_;
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
    //TODO For continuous variables, instead of returning a Gaussian distributions, we should implement a Mixture of Gaussians instead!!!
    public <E extends UnivariateDistribution> E getPosterior(Variable var) {

        // TODO Could we build this object in a general way for Multinomial and Normal
        EF_UnivariateDistribution ef_univariateDistribution=null;
        if (var.isMultinomial()){
            ef_univariateDistribution = new EF_Multinomial(var);
        }
        else if (var.isGaussian()) {
            ef_univariateDistribution = new EF_Normal(var);
        }
        else {
            throw new IllegalArgumentException("Variable type not allowed.");
        }
        //---------------------------------------------------------
        AtomicInteger dataInstanceCount = new AtomicInteger(0);
        Stream<HashMapAssignment> samples = this.simulatedSample.stream();
        SufficientStatistics sumSS = samples
                .peek(w -> {
                    dataInstanceCount.getAndIncrement();
                })
                .map(ef_univariateDistribution::getSufficientStatistics)
                .reduce(SufficientStatistics::sumVector).get();
        sumSS.divideBy(dataInstanceCount.get());
        ef_univariateDistribution.setMomentParameters(sumSS);
        return (E)EF_DistributionBuilder.toUnivariateDistribution(ef_univariateDistribution);

    }


    public static void main(String args[]) throws IOException, ClassNotFoundException {


        /*******************************************************************************************************/
        /************************************* MODEL DISTRIBUTIONS  ********************************************/
        /*******************************************************************************************************/

        BayesianNetwork bn2 = BayesianNetworkLoader.loadFromFile("networks/IS.bn");
        StaticVariables variables2 = bn2.getStaticVariables();

        Variable varA = variables2.getVariableByName("A");
        Variable varB = variables2.getVariableByName("B");
        Variable varC = variables2.getVariableByName("C");
        Variable varD = variables2.getVariableByName("D");
        Variable varE = variables2.getVariableByName("E");

        List<ConditionalDistribution> modelDistributions = new ArrayList(bn2.getDistributions());
        Collections.reverse(modelDistributions);

        System.out.println("============================================================");
        System.out.println("================= MODEL DISTRIBUTIONS ======================");
        System.out.println("============================================================");

        modelDistributions.stream().forEach(e-> {
            System.out.println(e.getVariable().getName());
            System.out.println(e);
        });

        /*******************************************************************************************************/
        /************************************ SAMPLING DISTRIBUTIONS *******************************************/
        /*******************************************************************************************************/

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("networks/IS.bn");
        StaticVariables variables = bn.getStaticVariables();
        Variable A = variables.getVariableByName("A");
        Variable B = variables.getVariableByName("B");
        Variable C = variables.getVariableByName("C");
        Variable D = variables.getVariableByName("D");
        Variable E = variables.getVariableByName("E");

        // Variable A
        Multinomial_MultinomialParents distA = bn.getDistribution(A);
        distA.getProbabilities()[0].setProbabilities(new double[]{0.15, 0.85});

        // Variable B
        Multinomial_MultinomialParents distB = bn.getDistribution(B);
        distB.getMultinomial(0).setProbabilities(new double[]{0.15,0.85});
        distB.getMultinomial(1).setProbabilities(new double[]{0.75,0.25});

        // Variable C
        Normal_MultinomialParents distC = bn.getDistribution(C);
        distC.getNormal(0).setMean(3.1);
        distC.getNormal(0).setSd(0.9660254037);
        distC.getNormal(1).setMean(2.1);
        distC.getNormal(1).setSd(0.848683);

        //Variable D
        Normal_MultinomialNormalParents distD = bn.getDistribution(D);
        distD.getNormal_NormalParentsDistribution(0).setIntercept(2.1);
        distD.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{2.1});
        distD.getNormal_NormalParentsDistribution(0).setSd(1.1);

        distD.getNormal_NormalParentsDistribution(1).setIntercept(0.6);
        distD.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{1.6});
        distD.getNormal_NormalParentsDistribution(1).setSd(1.5142);

        //Variable E
        Normal_NormalParents distE  = bn.getDistribution(E);
        distE.setIntercept(2.4);
        distE.setCoeffParents(new double[]{4.1});
        distE.setSd(1.2832);

        List<ConditionalDistribution> samplingDistributions = new ArrayList(bn.getDistributions());
        Collections.reverse(samplingDistributions);

        System.out.println("============================================================");
        System.out.println("== SAMPLING DISTRIBUTIONS (MODEL DISTRIBUTIONS WITH NOISE) =");
        System.out.println("============================================================");

        samplingDistributions.stream().forEach(e-> {
            System.out.println(e.getVariable().getName());
            System.out.println(e);
        });

        System.out.println("============================================================");
        System.out.println("==================== IMPORTANCE SAMPLING ===================");
        System.out.println("============================================================");

        ImportanceSampling inferenceEngine = new ImportanceSampling(bn2,1000);
        inferenceEngine.setEvidence(null);
        inferenceEngine.setSamplingDistributions(samplingDistributions);
        inferenceEngine.runInference();

        //inferenceEngine.weights.stream().forEach(System.out::println);

        System.out.println("\n varA -> " + inferenceEngine.getPosterior(varA).toString());



    }

}
