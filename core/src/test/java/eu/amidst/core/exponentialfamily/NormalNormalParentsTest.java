package eu.amidst.core.exponentialfamily;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.ConditionalLinearGaussian;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.HashMapAssignment;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Hanen on 06/02/15.
 */
public class NormalNormalParentsTest {


    @Test
    public void testingProbabilities_Normal1NormalParent() throws IOException, ClassNotFoundException  {


        BayesianNetwork testnet = BayesianNetworkLoader.loadFromFile("networks/Normal_1NormalParents.bn");

        ConditionalLinearGaussian dist = (ConditionalLinearGaussian) testnet.getConditionalDistributions().get(1);

        //dist.getCoeffParents()[0]=0;
        //dist.setIntercept(0.1);
        //dist.setSd(2.234);

        System.out.println(testnet.toString());

        System.out.println("\nNormal_1NormalParents probabilities comparison \n ");

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(testnet);
        sampler.setSeed(0);
        DataStream<DataInstance> data = sampler.sampleToDataStream(100000);

        //Compare predictions between distributions and EF distributions.

        EF_BayesianNetwork ef_testnet = new EF_BayesianNetwork(testnet);
        HashMapAssignment dataTmp = new HashMapAssignment(2);
        dataTmp.setValue(testnet.getStaticVariables().getVariableByName("A"), 1.0);
        dataTmp.setValue(testnet.getStaticVariables().getVariableByName("B"), 1.0);


        System.out.println(testnet.getConditionalDistributions().get(1).getLogConditionalProbability(dataTmp));
        System.out.println(ef_testnet.getDistributionList().get(1).computeLogProbabilityOf(dataTmp));


        for(DataInstance e: data){
            double ef_logProb = 0,logProb = 0;
            for(EF_ConditionalDistribution ef_dist: ef_testnet.getDistributionList()){
                ef_logProb += ef_dist.computeLogProbabilityOf(e);
            }
            logProb = testnet.getLogProbabiltyOf(e);
            //System.out.println("Distributions: "+ logProb + " = EF-Distributions: "+ ef_logProb);
            Assert.assertEquals(logProb, ef_logProb, 0.0001);

        }
    }


    @Test
    public void testingProbabilities_Normal2NormalParents() throws IOException, ClassNotFoundException  {

        BayesianNetwork testnet = BayesianNetworkLoader.loadFromFile("networks/Normal_NormalParents.bn");

        System.out.println(testnet.toString());

        System.out.println("\nNormal_2NormalParents probabilities comparison \n ");

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(testnet);
        sampler.setSeed(0);
        DataStream<DataInstance> data = sampler.sampleToDataStream(100000);

        //Compare predictions between distributions and EF distributions.
        EF_BayesianNetwork ef_testnet = new EF_BayesianNetwork(testnet);

        HashMapAssignment dataTmp = new HashMapAssignment(3);
        dataTmp.setValue(testnet.getStaticVariables().getVariableByName("A"), 1.0);
        dataTmp.setValue(testnet.getStaticVariables().getVariableByName("B"), 1.0);
        dataTmp.setValue(testnet.getStaticVariables().getVariableByName("C"), 1.0);

        System.out.println(testnet.getConditionalDistributions().get(2).getLogConditionalProbability(dataTmp));
        System.out.println(ef_testnet.getDistributionList().get(2).computeLogProbabilityOf(dataTmp));

        for(DataInstance e: data){
            double ef_logProb = 0,logProb = 0;
            for(EF_ConditionalDistribution ef_dist: ef_testnet.getDistributionList()){
                ef_logProb += ef_dist.computeLogProbabilityOf(e);
            }
            logProb = testnet.getLogProbabiltyOf(e);
            //System.out.println("Distributions: "+ logProb + " = EF-Distributions: "+ ef_logProb);
            Assert.assertEquals(logProb, ef_logProb, 0.0001);
        }
    }

}
