package eu.amidst.corestatic.learning;


import com.google.common.base.Stopwatch;
import eu.amidst.corestatic.datastream.DataInstance;
import eu.amidst.corestatic.datastream.DataStream;
import eu.amidst.corestatic.distribution.Distribution;
import eu.amidst.corestatic.io.BayesianNetworkLoader;
import eu.amidst.corestatic.learning.parametric.LearningEngineForBN;
import eu.amidst.corestatic.learning.parametric.MaximumLikelihood;
import eu.amidst.corestatic.models.BayesianNetwork;
import eu.amidst.corestatic.utils.BayesianNetworkSampler;
import eu.amidst.corestatic.variables.Variable;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Hanen on 12/01/15.
 */
public class MLTestOneVar {

    @Test
    public void MLTest() throws  IOException, ClassNotFoundException {

        // load the true Asia Bayesian network
        BayesianNetwork net = BayesianNetworkLoader.loadFromFile("./networks/One.bn");

        System.out.println("\nOne network \n ");
        System.out.println(net.getDAG().toString());
        System.out.println(net.toString());

        //Sampling 5000 instances from Asia BN
        Stopwatch watch = Stopwatch.createStarted();
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(net);
        sampler.setSeed(0);
        System.out.println(watch.stop());

        DataStream<DataInstance> data = sampler.sampleToDataStream(10);
        data.stream().forEach( e -> System.out.println(e.toString(net.getStaticVariables().getListOfVariables())));

        //Load the sampled data
        data = sampler.sampleToDataStream(10);
        //Structure learning is excluded from the test, i.e., so we use here the same initial network structure net.getDAG()

        //Parameter Learning
        MaximumLikelihood maximumLikelihood = new MaximumLikelihood();
        maximumLikelihood.setBatchSize(1000);
        maximumLikelihood.setParallelMode(true);
        LearningEngineForBN.setParameterLearningAlgorithm(maximumLikelihood);


        //using Maximum likelihood learnParametersStaticModel
        BayesianNetwork bn = LearningEngineForBN.learnParameters(net.getDAG(), data);
        System.out.println(bn.toString());


        //Check if the probability distributions of the true and learned networks are equals
        for (Variable var : net.getStaticVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            Distribution trueCD = net.getConditionalDistribution(var);
            System.out.println("\nThe true distribution:\n"+ trueCD);

            Distribution learnedCD = bn.getConditionalDistribution(var);
            System.out.println("\nThe learned distribution:\n"+ learnedCD);

        }

    }

}
