package eu.amidst.core.utils;

import COM.hugin.HAPI.ExceptionHugin;
import com.google.common.base.Stopwatch;
import eu.amidst.core.database.*;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.BayesianNetworkLoader;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.openjdk.jmh.annotations.*;


/**
 * Created by andresmasegosa on 11/12/14.
 */
public class BayesianNetworkSampler {

    public static Stream<Assignment> parallelSampling(int seed, BayesianNetwork network, int nSamples){

        List<Variable> causalOrder = getCausalOrder(network.getDAG());

        LocalRandomGenerator randomGenerator = new LocalRandomGenerator(seed);

        return IntStream.iterate(0, i -> i + 1).limit(nSamples).parallel().mapToObj(i -> sample(network, causalOrder, randomGenerator.current()));

    }

    public static Stream<Assignment> serialSampling(int seed, BayesianNetwork network, int nSamples){

        List<Variable> causalOrder = getCausalOrder(network.getDAG());

        Random rand = new Random(seed);

        return IntStream.iterate(0, i -> i + 1).limit(nSamples).mapToObj(e -> sample(network, causalOrder, rand));
    }


    public static Assignment sample(BayesianNetwork network, List<Variable> causalOrder, Random random) {
        HashMapAssignment assignment = new HashMapAssignment(network.getNumberOfVars());
        for (Variable var : causalOrder) {
            double sampledValue = network.getDistribution(var).getUnivariateDistribution(assignment).sample(random);
            assignment.putValue(var, sampledValue);
        }
        return assignment;
    }


    public static List<Variable> getCausalOrder(DAG dag){
        StaticVariables variables = dag.getStaticVariables();
        int nNrOfAtts = variables.getNumberOfVars();
        List<Variable> order = new ArrayList();
        boolean[] bDone = new boolean[variables.getNumberOfVars()];

        for (Variable var: variables){
            bDone[var.getVarID()] = false;
        }

        for (int iAtt = 0; iAtt < nNrOfAtts; iAtt++) {
                boolean allParentsDone = false;
                for (Variable var2 : variables){
                    if (!bDone[var2.getVarID()]) {
                        allParentsDone = true;
                        int iParent = 0;
                        for (Variable parent: dag.getParentSet(var2))
                            allParentsDone = allParentsDone && bDone[parent.getVarID()];

                        if (allParentsDone){
                            order.add(var2);
                            bDone[var2.getVarID()] = true;
                        }
                    }
                }
            }
            return order;
    }
    public static void writeToARFFFile(String path, StaticVariables variables, Stream<Assignment> samples) throws IOException{

        FileWriter fw = new FileWriter(path);
        fw.write("@relation\n\n");

        for (Variable v : variables){
            fw.write(v.toARFFString()+"\n");
        }

        fw.write("\n\n@data\n\n");

        samples.forEach(e -> {
            try {
                fw.write(e.toString(variables.getVariableList()) + "\n");
            } catch (Exception ex){
                ex.printStackTrace();
                throw new UnsupportedOperationException("Error Writting Samples.");
            }
        });

        fw.close();
    }

    public static void main(String[] args) throws Exception{


        BayesianNetwork network = BayesianNetworkLoader.loadFromHugin("./networks/asia.net");

        Stream<Assignment> samples = BayesianNetworkSampler.parallelSampling(0, network, 1000);

        BayesianNetworkSampler.writeToARFFFile("./data/asisa-samples.arff", network.getStaticVariables(), samples);

    }
}
