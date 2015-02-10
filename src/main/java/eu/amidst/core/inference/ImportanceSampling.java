package eu.amidst.core.inference;

import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.DataOnMemoryListContainer;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.EF_DistributionBuilder;
import eu.amidst.core.exponentialfamily.EF_Multinomial;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
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
    private boolean parallelMode = true;

    public boolean isParallelMode() {
        return parallelMode;
    }

    public void setParallelMode(boolean parallelMode_) {
        this.parallelMode = parallelMode_;
    }



    public ImportanceSampling(BayesianNetwork model_, Assignment evidence_, long sampleSize_) {

        this.model = model_;
        this.evidence = evidence_;
        this.sampleSize = sampleSize_;
        Attributes attributes= this.model.getDAG().getStaticVariables().getAttributes();
        this.simulatedSample = new DataOnMemoryListContainer(attributes);

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


    //This method will consist in sampling instances to estimate probabilities afterwards in method getPosterior(var), right?
    //CHECK IF THE SIMULATED VALUE MATCHES WITH THE EVIDENCE!!!!!!
    @Override
    public void compileModel() {

        HashMapAssignment assignment;

        for(int i=0;i<sampleSize;i++) {
            assignment = new HashMapAssignment(1);
            for (ConditionalDistribution dist : this.getSamplingDistributions()) {
                UnivariateDistribution univariateDistribution = dist.getUnivariateDistribution(assignment);
                double simulatedValue = univariateDistribution.sample(new Random());
                assignment.putValue(dist.getVariable(),simulatedValue);
            }
            simulatedSample.add(assignment);
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
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(model);
        sampler.setSeed(0);
        sampler.setParallelMode(false);
        evidence = sampler.sampleToDataBase(1).stream().findFirst().get();
    }

    @Override
    //For continuous variables, obtain a Gaussian distributions. In theory we should go for a Mixture of Gaussians instead!!!
    public <E extends UnivariateDistribution> E getPosterior(Variable var) {

        EF_Multinomial ef_multinomial = new EF_Multinomial(var);
        ef_multinomial.createZeroedSufficientStatistics();

        Stream<HashMapAssignment> samples = this.simulatedSample.stream();

        AtomicInteger dataInstanceCount = new AtomicInteger(0);

        SufficientStatistics sumSS = samples
                .peek(w -> {
                    dataInstanceCount.getAndIncrement();
                })
                .map(ef_multinomial::getSufficientStatistics)
                .reduce(SufficientStatistics::sumVector).get();

        //Normalize the sufficient statistics
        sumSS.divideBy(dataInstanceCount.get());

        ef_multinomial.setMomentParameters(sumSS);

        Multinomial dist = EF_DistributionBuilder.toDistribution(ef_multinomial);

        return (E)dist;
    }


    public static void main(String args[]) throws IOException, ClassNotFoundException {

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("networks/IS.bn");

        StaticVariables variables = bn.getStaticVariables();

        Variable A = variables.getVariableByName("A");
        Variable B = variables.getVariableByName("B");
        Variable C = variables.getVariableByName("C");
        Variable D = variables.getVariableByName("D");
        Variable E = variables.getVariableByName("E");


        // TOPOLOGICAL ORDER
        //---------------------------------------------------------------------------------
        List<ConditionalDistribution> distributions = new ArrayList(bn.getDistributions());
        Collections.reverse(distributions);

        distributions.stream().forEach(e-> {
            System.out.println(e.getVariable().getName());
            System.out.println(e);
        });
        //---------------------------------------------------------------------------------

        ImportanceSampling inferenceEngine = new ImportanceSampling(bn,null,1000);
        inferenceEngine.setSamplingDistributions(distributions);
        inferenceEngine.compileModel();



        System.out.println("\n A -> " + inferenceEngine.getPosterior(A).toString());











    }

}
