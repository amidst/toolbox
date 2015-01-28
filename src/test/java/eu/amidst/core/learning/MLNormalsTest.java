package eu.amidst.core.learning;

import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.filereaders.StaticDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.BayesianNetworkLoader;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Created by ana@cs.aau.dk on 22/01/15.
 * TODO: test probability dist of EF is equal to regular dist.
 *
 */
public class MLNormalsTest {

    @Test
    public void testingML_Gaussians1Parent() throws IOException, ClassNotFoundException {


        BayesianNetwork testnet = BayesianNetworkLoader.loadFromFile("networks/Normal_1NormalParents.ser");

        System.out.println("\nNormal_1NormalParents network \n ");

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(testnet);
        sampler.setSeed(0);
        sampler.setParallelMode(true);
        try{
            sampler.sampleToAnARFFFile("./data/Normal_1NormalParents.arff", 100000);
        } catch (IOException ex){
        }

        //Load the sampled data
        DataBase data = new StaticDataOnDiskFromFile(new ARFFDataReader(new String("data/Normal_1NormalParents.arff")));


        //Parameter Learning
        MaximumLikelihood.setBatchSize(1000);
        MaximumLikelihood.setParallelMode(true);
        BayesianNetwork bnet = MaximumLikelihood.learnParametersStaticModel(testnet.getDAG(), data);

        //Check the probability distributions of each node
        for (Variable var : testnet.getStaticVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            System.out.println("\nTrue distribution:\n"+ testnet.getDistribution(var));
            System.out.println("\nLearned distribution:\n"+ bnet.getDistribution(var));
            assertTrue(bnet.getDistribution(var).equalDist(testnet.getDistribution(var), 0.05));
        }

        //Or check directly if the true and learned networks are equals
        assertTrue(bnet.equalBNs(testnet,0.05));
    }

    @Test
    public void testingML_GaussiansTwoParents() throws  IOException, ClassNotFoundException {

        BayesianNetwork testnet = BayesianNetworkLoader.loadFromFile("networks/Normal_NormalParents.ser");
        System.out.println("\nNormal_NormalParents network \n ");

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(testnet);
        sampler.setSeed(0);
        sampler.setParallelMode(true);
        try{
            sampler.sampleToAnARFFFile("./data/Normal_NormalParents.arff", 100000);
        } catch (IOException ex){
        }

        //Load the sampled data
        DataBase data = new StaticDataOnDiskFromFile(new ARFFDataReader(new String("data/Normal_NormalParents.arff")));


        //Parameter Learning
        MaximumLikelihood.setBatchSize(1000);
        MaximumLikelihood.setParallelMode(true);
        BayesianNetwork bnet = MaximumLikelihood.learnParametersStaticModel(testnet.getDAG(), data);

        //Check the probability distributions of each node
        for (Variable var : testnet.getStaticVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            System.out.println("\nTrue distribution:\n"+ testnet.getDistribution(var));
            System.out.println("\nLearned distribution:\n"+ bnet.getDistribution(var));
            assertTrue(bnet.getDistribution(var).equalDist(testnet.getDistribution(var), 0.05));
        }

        //Or check directly if the true and learned networks are equals
        assertTrue(bnet.equalBNs(testnet,0.05));
    }
}
