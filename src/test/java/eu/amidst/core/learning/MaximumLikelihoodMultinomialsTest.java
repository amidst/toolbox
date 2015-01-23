package eu.amidst.core.learning;

import COM.hugin.HAPI.ExceptionHugin;
import com.google.common.base.Stopwatch;
import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.filereaders.StaticDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.BayesianNetworkLoader;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;
import org.junit.Test;
import eu.amidst.core.distribution.ConditionalDistribution;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Hanen on 08/01/15.
 */
public class MaximumLikelihoodMultinomialsTest {

    @Test
    public void testingML() throws ExceptionHugin {

        // load the true Asia Bayesian network
        BayesianNetwork asianet = BayesianNetworkLoader.loadFromHugin("./networks/asia.net");

        System.out.println("\nAsia network \n ");
        //System.out.println(asianet.getDAG().toString());
        //System.out.println(asianet.toString());

        //Sampling from Asia BN
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        sampler.setParallelMode(true);
        try{
        sampler.sampleToAnARFFFile("./data/asiaSamples.arff", 10000);
        } catch (IOException ex){
        }

        //Load the sampled data
        DataBase data = new StaticDataOnDiskFromFile(new ARFFDataReader(new String("data/asiaSamples.arff")));

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        MaximumLikelihood.setBatchSize(1000);
        MaximumLikelihood.setParallelMode(true);
        BayesianNetwork bnet = MaximumLikelihood.learnParametersStaticModel(asianet.getDAG(), data);

        //Check if the probability distributions of each node
        for (Variable var : asianet.getStaticVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            System.out.println("\nTrue distribution:\n"+ asianet.getDistribution(var));
            System.out.println("\nLearned distribution:\n"+ bnet.getDistribution(var));
            assertTrue(bnet.getDistribution(var).equalDist(asianet.getDistribution(var), 0.05));
        }

        //Or check directly if the true and learned networks are equals
        assertTrue(bnet.equalBNs(asianet,0.05));
    }

}
