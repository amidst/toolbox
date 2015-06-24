package eu.amidst.core.learning;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.learning.parametric.LearningEngine;
import eu.amidst.core.learning.parametric.MaximumLikelihood;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Hanen on 08/01/15.
 */
public class MLMultinomialsTest {

    @Test
    public void testingMLSequential() throws IOException, ClassNotFoundException {

        // load the true Asia Bayesian network
        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("networks/asia.bn");

        System.out.println("\nAsia network \n ");
        //System.out.println(asianet.getDAG().outputString());
        //System.out.println(asianet.outputString());

        //Sampling from Asia BN
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);

        //Load the sampled data
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);
        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        MaximumLikelihood maximumLikelihood = new MaximumLikelihood();
        maximumLikelihood.setBatchSize(1000);
        LearningEngine.setParameterLearningAlgorithm(maximumLikelihood);
        BayesianNetwork bnet = LearningEngine.learnParameters(asianet.getDAG(), data);

        //Check if the probability distributions of each node
        for (Variable var : asianet.getStaticVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            System.out.println("\nTrue distribution:\n"+ asianet.getConditionalDistribution(var));
            System.out.println("\nLearned distribution:\n"+ bnet.getConditionalDistribution(var));
            Assert.assertTrue(bnet.getConditionalDistribution(var).equalDist(asianet.getConditionalDistribution(var), 0.05));
        }

        //Or check directly if the true and learned networks are equals
        Assert.assertTrue(bnet.equalBNs(asianet, 0.05));
    }


    @Test
    public void testingMLParallel() throws IOException, ClassNotFoundException {

        // load the true Asia Bayesian network
        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("networks/asia.bn");

        System.out.println("\nAsia network \n ");
        //System.out.println(asianet.getDAG().outputString());
        //System.out.println(asianet.outputString());

        //Sampling from Asia BN
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);

        //Load the sampled data
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);
        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        MaximumLikelihood maximumLikelihood = new MaximumLikelihood();
        maximumLikelihood.setBatchSize(1000);
        maximumLikelihood.setParallelMode(true);
        LearningEngine.setParameterLearningAlgorithm(maximumLikelihood);
        BayesianNetwork bnet = LearningEngine.learnParameters(asianet.getDAG(), data);

        //Check if the probability distributions of each node
        for (Variable var : asianet.getStaticVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            System.out.println("\nTrue distribution:\n"+ asianet.getConditionalDistribution(var));
            System.out.println("\nLearned distribution:\n"+ bnet.getConditionalDistribution(var));
            Assert.assertTrue(bnet.getConditionalDistribution(var).equalDist(asianet.getConditionalDistribution(var), 0.05));
        }

        //Or check directly if the true and learned networks are equals
        Assert.assertTrue(bnet.equalBNs(asianet, 0.05));
    }

    @Test
    public void testingMLBatches() throws IOException, ClassNotFoundException {

        // load the true Asia Bayesian network
        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("networks/asia.bn");

        System.out.println("\nAsia network \n ");
        //System.out.println(asianet.getDAG().outputString());
        //System.out.println(asianet.outputString());

        //Sampling from Asia BN
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);

        //Load the sampled data
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);
        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        MaximumLikelihood maximumLikelihood = new MaximumLikelihood();
        maximumLikelihood.setDAG(asianet.getDAG());
        maximumLikelihood.initLearning();

        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)){
            maximumLikelihood.updateModel(batch);
        }

        BayesianNetwork bnet = maximumLikelihood.getLearntBayesianNetwork();

        //Check if the probability distributions of each node
        for (Variable var : asianet.getStaticVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            System.out.println("\nTrue distribution:\n"+ asianet.getConditionalDistribution(var));
            System.out.println("\nLearned distribution:\n"+ bnet.getConditionalDistribution(var));
            Assert.assertTrue(bnet.getConditionalDistribution(var).equalDist(asianet.getConditionalDistribution(var), 0.05));
        }

        //Or check directly if the true and learned networks are equals
        Assert.assertTrue(bnet.equalBNs(asianet, 0.05));
    }
}
