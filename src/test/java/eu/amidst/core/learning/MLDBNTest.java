package eu.amidst.core.learning;

import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.filereaders.StaticDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.BayesianNetworkLoader;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicBayesianNetworkWriter;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.core.utils.DynamicBayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import org.junit.Test;
import scala.util.DynamicVariable;

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * Created by Hanen on 27/01/15.
 */
public class MLDBNTest {

    @Test
    public void testingMLforDBN() throws ExceptionHugin, IOException, ClassNotFoundException {

        //Generate a dynamic Naive Bayes with only multinomials variables

        DynamicBayesianNetworkGenerator dbnGenerator = new DynamicBayesianNetworkGenerator();
        dbnGenerator.setNumberOfContinuousVars(0);
        dbnGenerator.setNumberOfDiscreteVars(2);
        dbnGenerator.setNumberOfStates(2);

        DynamicBayesianNetwork dynamicNB = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2);

        System.out.println(dynamicNB.getDynamicDAG().toString());
        System.out.println(dynamicNB.toString());

        //Sampling from the generated Dynamic NB

        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicNB);
        sampler.setSeed(0);
        sampler.setParallelMode(true);

        //Sample from the dynamic NB given as inputs both nSequences and sequenceLength
        DataBase data = sampler.sampleToDataBase(10000,100);

        //Structure learning is excluded from the test, i.e., we use directly the initial Dynamic Naive Bayes network structure
        // and just apply then test parameter learning

        //Parameter Learning
        MaximumLikelihood.setBatchSize(1000);
        MaximumLikelihood.setParallelMode(false);
        DynamicBayesianNetwork bnet = MaximumLikelihood.learnDynamic(dynamicNB.getDynamicDAG(), data);

        //Check if the probability distributions of each node
        for (Variable var : dynamicNB.getDynamicVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            // time 0
            System.out.println("\nTrue distribution:\n"+ dynamicNB.getDistributionTime0(var));
            System.out.println("\nLearned distribution:\n"+ bnet.getDistributionTime0(var));
            //assertTrue(bnet.getDistributionTime0(var).equalDist(dynamicNB.getDistributionTime0(var), 0.05));
            // time T
            System.out.println("\nTrue distribution:\n"+ dynamicNB.getDistributionTimeT(var));
            System.out.println("\nLearned distribution:\n"+ bnet.getDistributionTimeT(var));
            //assertTrue(bnet.getDistributionTimeT(var).equalDist(dynamicNB.getDistributionTimeT(var), 0.05));
        }

        //Or check directly if the true and learned networks are equals
        assertTrue(bnet.equalDBNs(dynamicNB,0.05));
    }

}
