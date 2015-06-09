package eu.amidst.corestatic.learning;

import eu.amidst.corestatic.datastream.DataInstance;
import eu.amidst.corestatic.datastream.DataStream;
import eu.amidst.corestatic.learning.parametric.MaximumLikelihoodForBN;
import eu.amidst.corestatic.models.BayesianNetwork;
import eu.amidst.corestatic.utils.BayesianNetworkGenerator;
import eu.amidst.corestatic.utils.BayesianNetworkSampler;
import eu.amidst.corestatic.variables.Variable;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Hanen on 28/01/15.
 */
public class MLGenratedBNTest {

    @Test
    public void testingMLGeneratedBN() throws IOException, ClassNotFoundException {

        BayesianNetworkGenerator.loadOptions();

        BayesianNetworkGenerator.setNumberOfContinuousVars(10);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(1);
        BayesianNetworkGenerator.setNumberOfStates(2);
        BayesianNetworkGenerator.setSeed(0);
        BayesianNetwork naiveBayes = BayesianNetworkGenerator.generateNaiveBayes(2);
        System.out.println(naiveBayes.toString());

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(naiveBayes);
        sampler.setSeed(0);

        DataStream<DataInstance> data = sampler.sampleToDataStream(1000000);


        //Parameter Learning
        MaximumLikelihoodForBN.setBatchSize(1000);
        MaximumLikelihoodForBN.setParallelMode(true);
        BayesianNetwork bnet = MaximumLikelihoodForBN.learnParametersStaticModel(naiveBayes.getDAG(), data);

        //Check the probability distributions of each node
        for (Variable var : naiveBayes.getStaticVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            System.out.println("\nTrue distribution:\n"+ naiveBayes.getConditionalDistribution(var));
            System.out.println("\nLearned distribution:\n"+ bnet.getConditionalDistribution(var));
            Assert.assertTrue(bnet.getConditionalDistribution(var).equalDist(naiveBayes.getConditionalDistribution(var), 0.05));
        }

        //Or check directly if the true and learned networks are equals
        Assert.assertTrue(bnet.equalBNs(naiveBayes, 0.05));
    }

}
