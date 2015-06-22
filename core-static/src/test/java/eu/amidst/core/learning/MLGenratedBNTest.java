package eu.amidst.core.learning;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.learning.parametric.LearningEngine;
import eu.amidst.core.learning.parametric.MaximumLikelihood;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
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

        BayesianNetworkGenerator.setNumberOfGaussianVars(10);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(1, 2);
        BayesianNetworkGenerator.setSeed(0);
        BayesianNetwork naiveBayes = BayesianNetworkGenerator.generateNaiveBayes(2);
        System.out.println(naiveBayes.toString());

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(naiveBayes);
        sampler.setSeed(0);

        DataStream<DataInstance> data = sampler.sampleToDataStream(1000000);


        //Parameter Learning
        MaximumLikelihood maximumLikelihood = new MaximumLikelihood();
        maximumLikelihood.setBatchSize(1000);
        maximumLikelihood.setParallelMode(true);
        LearningEngine.setParameterLearningAlgorithm(maximumLikelihood);
        BayesianNetwork bnet = LearningEngine.learnParameters(naiveBayes.getDAG(), data);

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
