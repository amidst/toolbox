package learning;

import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.StaticDataInstance;
import eu.amidst.core.database.filereaders.StaticDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.distribution.Normal_NormalParents;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.learning.MaximumLikelihoodForBN;
import eu.amidst.core.models.BayesianNetwork;
//import eu.amidst.core.models.BayesianNetworkLoader;
import eu.amidst.huginlink.BayesianNetworkLoader;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;

import org.junit.Test;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by ana@cs.aau.dk on 22/01/15.
 * TODO: test probability dist of EF is equal to regular dist.
 *
 */
public class MLNormalsTest {

    @Test
    public void testingProbabilities_NormalNormal1Parent() throws IOException, ClassNotFoundException, ExceptionHugin  {


        BayesianNetwork testnet = BayesianNetworkLoader.loadFromHugin("networks/Normal_1NormalParents.net");

        Normal_NormalParents dist = (Normal_NormalParents) testnet.getDistributions().get(1);

        dist.getCoeffParents()[0]=1.0;
        dist.setSd(1.0);

        System.out.println(testnet.toString());

        System.out.println("\nNormal_1NormalParents probabilities comparison \n ");

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(testnet);
        sampler.setSeed(0);
        sampler.setParallelMode(true);
        try{
            sampler.sampleToAnARFFFile("./data/Normal_1NormalParents.arff", 10);
        } catch (IOException ex){
        }

        //Load the sampled data
        DataBase<StaticDataInstance> data = new StaticDataOnDiskFromFile(new ARFFDataReader(new String("data/Normal_1NormalParents.arff")));


        //Compare predictions between distributions and EF distributions.

        EF_BayesianNetwork ef_testnet = new EF_BayesianNetwork(testnet);



        for(DataInstance e: data){
            double ef_logProb = 0,logProb = 0;
            for(EF_ConditionalDistribution ef_dist: ef_testnet.getDistributionList()){
                ef_logProb += ef_dist.computeLogProbabilityOf(e);
            }
            logProb = testnet.getLogProbabiltyOfFullAssignment(e);
            System.out.println("Distributions: "+ logProb + " = EF-Distributions: "+ ef_logProb);
            assertEquals(logProb, ef_logProb, 0.05);

        }
    }

    /*
    @Test
    public void testingProbabilities_NormalMultinomial() throws IOException, ClassNotFoundException, ExceptionHugin  {


        BayesianNetwork testnet = BayesianNetworkLoader.loadFromHugin("networks/Normal_MultinomialParents.net");

        System.out.println(testnet.toString());
        System.out.println("\nNormal_MultinomialParents probabilities comparison \n ");

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(testnet);
        sampler.setSeed(0);
        sampler.setParallelMode(true);
        try{
            sampler.sampleToAnARFFFile("./data/Normal_MultinomialParents.arff", 10);
        } catch (IOException ex){
        }

        //Load the sampled data
        DataBase<StaticDataInstance> data = new StaticDataOnDiskFromFile(new ARFFDataReader(new String("data/Normal_MultinomialParents.arff")));


        //Compare predictions between distributions and EF distributions.

        EF_BayesianNetwork ef_testnet = new EF_BayesianNetwork(testnet);



        for(DataInstance e: data){
            double ef_logProb = 0,logProb = 0;
            for(EF_ConditionalDistribution ef_dist: ef_testnet.getDistributionList()){
                ef_logProb += ef_dist.computeLogProbabilityOf(e);
            }
            logProb = testnet.getLogProbabiltyOfFullAssignment(e);
            System.out.println("Distributions: "+ logProb + " = EF-Distributions: "+ ef_logProb);
            assertEquals(logProb, ef_logProb, 0.05);

        }
    }
    */

}

