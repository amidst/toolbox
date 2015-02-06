package eu.amidst.core.learning;

import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.StaticDataInstance;
import eu.amidst.core.distribution.Normal_NormalParents;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.learning.MaximumLikelihoodForBN;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.models.BayesianNetworkLoader;
import eu.amidst.core.variables.Variable;

import org.junit.Test;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by ana@cs.aau.dk on 22/01/15.
 *
 */
public class MLNormalsTest {

    //TODO: Test more than 1 parent

    @Test
    public void testingProbabilities_NormalNormal1Parent() throws IOException, ClassNotFoundException  {


        BayesianNetwork testnet = BayesianNetworkLoader.loadFromFile("networks/Normal_1NormalParents.bn");

        Normal_NormalParents dist = (Normal_NormalParents) testnet.getDistributions().get(1);

        //dist.getCoeffParents()[0]=0;
        //dist.setIntercept(0.1);
        //dist.setSd(2.234);

        System.out.println(testnet.toString());

        System.out.println("\nNormal_1NormalParents probabilities comparison \n ");

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(testnet);
        sampler.setSeed(0);
        sampler.setParallelMode(true);
        DataBase<StaticDataInstance> data = sampler.sampleToDataBase(100000);

        //Compare predictions between distributions and EF distributions.

        EF_BayesianNetwork ef_testnet = new EF_BayesianNetwork(testnet);
        HashMapAssignment dataTmp = new HashMapAssignment(2);
        dataTmp.setValue(testnet.getStaticVariables().getVariableByName("A"), 1.0);
        dataTmp.setValue(testnet.getStaticVariables().getVariableByName("B"), 1.0);


        System.out.println(testnet.getDistributions().get(1).getLogConditionalProbability(dataTmp));
        System.out.println(ef_testnet.getDistributionList().get(1).computeLogProbabilityOf(dataTmp));


        for(DataInstance e: data){
            double ef_logProb = 0,logProb = 0;
            for(EF_ConditionalDistribution ef_dist: ef_testnet.getDistributionList()){
                ef_logProb += ef_dist.computeLogProbabilityOf(e);
            }
            logProb = testnet.getLogProbabiltyOfFullAssignment(e);
            //System.out.println("Distributions: "+ logProb + " = EF-Distributions: "+ ef_logProb);
            assertEquals(logProb, ef_logProb, 0.0001);

        }
    }


    @Test
    public void testingML_NormalNormal1Parent() throws IOException, ClassNotFoundException  {


        BayesianNetwork testnet = BayesianNetworkLoader.loadFromFile("networks/Normal_1NormalParents.bn");
        //BayesianNetwork testnet = eu.amidst.core.models.BayesianNetworkLoader.loadFromFile("networks/Normal_NormalParents.ser");
        System.out.println("\nNormal_NormalParents network \n ");

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(testnet);
        sampler.setSeed(0);
        sampler.setParallelMode(true);
        DataBase<StaticDataInstance> data = sampler.sampleToDataBase(100000);


        //Parameter Learning
        MaximumLikelihoodForBN.setBatchSize(1000);
        MaximumLikelihoodForBN.setParallelMode(true);
        BayesianNetwork bnet = MaximumLikelihoodForBN.learnParametersStaticModel(testnet.getDAG(), data);

        EF_BayesianNetwork ef_testnet = new EF_BayesianNetwork(testnet);

        EF_BayesianNetwork ef_bnet = new EF_BayesianNetwork(bnet);

        assertTrue(ef_bnet.equal_efBN(ef_testnet,0.05));
    }

    @Test
    public void testingML_GaussiansTwoParents() throws  IOException, ClassNotFoundException {

        BayesianNetwork testnet = BayesianNetworkLoader.loadFromFile("networks/Normal_NormalParents.bn");
        System.out.println("\nNormal_NormalParents network \n ");

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(testnet);
        sampler.setSeed(0);
        sampler.setParallelMode(true);
        DataBase<StaticDataInstance> data = sampler.sampleToDataBase(100000);


        //Parameter Learning
        MaximumLikelihoodForBN.setBatchSize(1000);
        MaximumLikelihoodForBN.setParallelMode(true);
        BayesianNetwork bnet = MaximumLikelihoodForBN.learnParametersStaticModel(testnet.getDAG(), data);

        //Check the probability distributions of each node
        for (Variable var : testnet.getStaticVariables()) {
            //System.out.println("\n------ Variable " + var.getName() + " ------");
            //System.out.println("\nTrue distribution:\n"+ testnet.getDistribution(var));
            //System.out.println("\nLearned distribution:\n"+ bnet.getDistribution(var));
            assertTrue(bnet.getDistribution(var).equalDist(testnet.getDistribution(var), 0.05));
        }

        //Or check directly if the true and learned networks are equals
        assertTrue(bnet.equalBNs(testnet,0.05));
    }

    @Test
    public void testingProbabilities_NormalMultinomial() throws IOException, ClassNotFoundException  {


        BayesianNetwork testnet = BayesianNetworkLoader.loadFromFile("networks/Normal_MultinomialParents.bn");

        System.out.println(testnet.toString());
        System.out.println("\nNormal_MultinomialParents probabilities comparison \n ");

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(testnet);
        sampler.setSeed(0);
        sampler.setParallelMode(true);
        DataBase<StaticDataInstance> data = sampler.sampleToDataBase(100000);


        //Compare predictions between distributions and EF distributions.

        EF_BayesianNetwork ef_testnet = new EF_BayesianNetwork(testnet);



        for(DataInstance e: data){
            double ef_logProb = 0,logProb = 0;
            for(EF_ConditionalDistribution ef_dist: ef_testnet.getDistributionList()){
                ef_logProb += ef_dist.computeLogProbabilityOf(e);
            }
            logProb = testnet.getLogProbabiltyOfFullAssignment(e);
            //System.out.println("Distributions: "+ logProb + " = EF-Distributions: "+ ef_logProb);
            assertEquals(logProb, ef_logProb, 0.05);

        }
    }


}