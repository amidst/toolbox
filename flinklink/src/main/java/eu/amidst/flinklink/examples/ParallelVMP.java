package eu.amidst.flinklink.examples;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Variable;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.learning.parametric.ParallelVB;
import org.apache.flink.api.java.ExecutionEnvironment;

/**
 * Created by Hanen on 08/10/15.
 */
public class ParallelVMP {

    public static void main(String[] args) throws Exception {

        // load the true Asia Bayesian network
        BayesianNetwork originalBnet = BayesianNetworkLoader.loadFromFile(args[0]);
        System.out.println("\n Network \n " + args[0]);

        //originalBnet.randomInitialization(new Random(0));
        //System.out.println(originalBnet.toString());

        //Sampling from Asia BN
        //BayesianNetworkSampler sampler = new BayesianNetworkSampler(originalBnet);
        //sampler.setSeed(0);

        //Load the sampled data
        //int sizeData = Integer.parseInt(args[1]);
        //DataStream<DataInstance> data = sampler.sampleToDataStream(sizeData);

        //DataStreamWriter.writeDataToFile(data, "./tmp.arff");

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        //DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadData(env, "./tmp.arff");
        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadData(env,"hdfs:///tmp.arff", false);


        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        long start = System.nanoTime();

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setMaximumGlobalIterations(1);
        parallelVB.setSeed(5);
        parallelVB.setBatchSize(1000);
        VMP vmp = parallelVB.getSVB().getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        parallelVB.setDAG(originalBnet.getDAG());
        parallelVB.setDataFlink(dataFlink);
        parallelVB.runLearning();
        BayesianNetwork LearnedBnet = parallelVB.getLearntBayesianNetwork();

        //Check if the probability distributions of each node
        for (Variable var : originalBnet.getVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            System.out.println("\nTrue distribution:\n"+ originalBnet.getConditionalDistribution(var));
            System.out.println("\nLearned distribution:\n" + LearnedBnet.getConditionalDistribution(var));
        }

        if (LearnedBnet.equalBNs(originalBnet, 0.1))
            System.out.println("\n The true and learned networks are equals :-) \n ");
        else
            System.out.println("\n The true and learned networks are NOT equals!!! \n ");

        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;
        System.out.println("Running time: \n" + seconds + " secs");

    }

    }
