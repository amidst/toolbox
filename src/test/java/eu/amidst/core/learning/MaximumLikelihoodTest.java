package eu.amidst.core.learning;

import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import com.google.common.base.Stopwatch;
import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.filereaders.StaticDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.huginlink.ConverterToHugin;
import eu.amidst.core.huginlink.ParallelTAN;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.BayesianNetworkLoader;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by Hanen on 08/01/15.
 */
public class MaximumLikelihoodTest {

    @Test
    public void testingML() throws ExceptionHugin {

        // load the true Asia Bayesian network
        BayesianNetwork asianet = BayesianNetworkLoader.loadFromHugin("./networks/asia.net");

        System.out.println("\nAsia network \n ");
        System.out.println(asianet.getDAG().toString());
        System.out.println(asianet.toString());

        //Sampling 5000 instances from Asia BN
        Stopwatch watch = Stopwatch.createStarted();
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        sampler.setParallelMode(true);
        try{
        sampler.sampleToAnARFFFile("./data/asia5000samples.arff", 5000);
        } catch (IOException ex){
        }
        System.out.println(watch.stop());
        //sampler.getSampleStream(10).forEach( e -> System.out.println(e.toString(asianet.getStaticVariables().getListOfVariables())));

        //Load the sampled data
        DataBase data = new StaticDataOnDiskFromFile(new ARFFDataReader(new String("data/asia5000samples.arff")));

        //Structure learning is excluded from the test, i.e., so we provide here the Asia network structure

        StaticVariables variables = new StaticVariables(data.getAttributes());

        Variable a = variables.getVariableByName("A");
        Variable b = variables.getVariableByName("B");
        Variable d = variables.getVariableByName("D");
        Variable e = variables.getVariableByName("E");
        Variable l = variables.getVariableByName("L");
        Variable t = variables.getVariableByName("T");
        Variable s = variables.getVariableByName("S");
        Variable x = variables.getVariableByName("X");

        DAG dag = new DAG(variables);
        dag.getParentSet(x).addParent(e);
        dag.getParentSet(b).addParent(s);
        dag.getParentSet(d).addParent(b);
        dag.getParentSet(d).addParent(e);
        dag.getParentSet(l).addParent(s);
        dag.getParentSet(t).addParent(a);
        dag.getParentSet(e).addParent(t);
        dag.getParentSet(e).addParent(l);
        System.out.println(dag.toString());

        assertTrue(dag.equals(asianet.getDAG()));

        //Parameter Learning
        MaximumLikelihood.setBatchSize(1000);

        //using Maximum likelihood parallelLearnStatic
        BayesianNetwork bn = MaximumLikelihood.parallelLearnStatic(dag, data);
        System.out.println(bn.toString());

        //using Maximum likelihood serialLearnStatic
        BayesianNetwork bn2 = MaximumLikelihood.serialLearnStatic(dag, data);
        System.out.println(bn2.toString());

        //Check if the probability distributions of the true and learned networks are equals
        for (Variable var : asianet.getStaticVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            ConditionalDistribution trueCD = asianet.getDistribution(var);
            System.out.println("\nThe true distribution:\n"+ trueCD);

            ConditionalDistribution learnedCD = bn.getDistribution(var);
            System.out.println("\nThe learned distribution:\n"+ learnedCD);

        }

    }

}
