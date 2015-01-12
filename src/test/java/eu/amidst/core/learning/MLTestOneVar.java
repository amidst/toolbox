package eu.amidst.core.learning;

import COM.hugin.HAPI.ExceptionHugin;
import com.google.common.base.Stopwatch;
import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.filereaders.StaticDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.distribution.ConditionalDistribution;
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
 * Created by Hanen on 12/01/15.
 */
public class MLTestOneVar {

    @Test
    public void MLTest() throws ExceptionHugin {

        // load the true Asia Bayesian network

        BayesianNetwork net = BayesianNetworkLoader.loadFromHugin("./networks/One.net");
        System.out.println("\nOne network \n ");
        System.out.println(net.getDAG().toString());
        System.out.println(net.toString());

        //Sampling 5000 instances from Asia BN
        Stopwatch watch = Stopwatch.createStarted();
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(net);
        sampler.setSeed(0);
        sampler.setParallelMode(true);
        try{
            sampler.sampleToAnARFFFile("./data/OneVar10samples.arff", 10);
        } catch (IOException ex){
        }
        System.out.println(watch.stop());
        sampler.getSampleStream(10).forEach( e -> System.out.println(e.toString(net.getStaticVariables().getListOfVariables())));

        //Load the sampled data
        DataBase data = new StaticDataOnDiskFromFile(new ARFFDataReader(new String("data/OneVar10samples.arff")));

        //Structure learning is excluded from the test, i.e., so we use here the same initial network structure net.getDAG()

        //Parameter Learning
        MaximumLikelihood.setBatchSize(10);
        MaximumLikelihood.setParallelMode(false);

        //using Maximum likelihood learnParametersStaticModel
        BayesianNetwork bn = MaximumLikelihood.learnParametersStaticModel(net.getDAG(), data);
        System.out.println(bn.toString());


        //Check if the probability distributions of the true and learned networks are equals
        for (Variable var : net.getStaticVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            ConditionalDistribution trueCD = net.getDistribution(var);
            System.out.println("\nThe true distribution:\n"+ trueCD);

            ConditionalDistribution learnedCD = bn.getDistribution(var);
            System.out.println("\nThe learned distribution:\n"+ learnedCD);

        }

    }

}
