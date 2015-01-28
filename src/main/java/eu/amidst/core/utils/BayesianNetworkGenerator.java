package eu.amidst.core.utils;

import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetworkWriter;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.*;

import java.io.IOException;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Created by andresmasegosa on 09/01/15.
 */
public final class BayesianNetworkGenerator {

    private static int numberOfVars = 10;
    private static int numberOfLinks = 3;
    private static int numberOfDiscreteVars = 10;
    private static int numberOfContinuousVars = 0;
    private static int numberOfStates = 2;

    public static void setNumberOfVars(int numberOfVars) {
        BayesianNetworkGenerator.numberOfVars = numberOfVars;
    }

    public static void setNumberOfLinks(int numberOfLinks) {
        BayesianNetworkGenerator.numberOfLinks = numberOfLinks;
    }

    public static void setNumberOfDiscreteVars(int numberOfDiscreteVars) {
        BayesianNetworkGenerator.numberOfDiscreteVars = numberOfDiscreteVars;
    }

    public static void setNumberOfContinuousVars(int numberOfContinuousVars) {
        BayesianNetworkGenerator.numberOfContinuousVars = numberOfContinuousVars;
    }

    public static void setNumberOfStates(int numberOfStates) {
        BayesianNetworkGenerator.numberOfStates = numberOfStates;
    }

    public static BayesianNetwork generateNaiveBayes(Random random, int nClassLabels){

        StaticVariables staticVariables  = new StaticVariables();


        IntStream.range(0,numberOfDiscreteVars-1)
                .forEach(i -> staticVariables.addHiddenVariable(generateDiscreteVariable("DiscreteVar" + i, BayesianNetworkGenerator.numberOfStates)));

        IntStream.range(0,numberOfContinuousVars)
                .forEach(i -> staticVariables.addHiddenVariable(generateContinuousVariable("GaussianVar" + i)));

        Variable classVar = staticVariables.addHiddenVariable(generateDiscreteVariable("ClassVar", nClassLabels));

        DAG dag = new DAG(staticVariables);

        dag.getParentSets().stream()
                .filter(var -> var.getMainVar().getVarID()!=classVar.getVarID())
                .forEach(w -> w.addParent(classVar));

        BayesianNetwork network = BayesianNetwork.newBayesianNetwork(dag);

        network.randomInitialization(random);

        return network;
    }

    private static VariableBuilder generateDiscreteVariable(String name, int numberOfStates){
        VariableBuilder builder = new VariableBuilder();
        builder.setName(name);
        builder.setDistributionType(DistType.MULTINOMIAL);
        builder.setStateSpace(new FiniteStateSpace(numberOfStates));
        builder.setObservable(false);

        return builder;
    }

    private static VariableBuilder generateContinuousVariable(String name){
        VariableBuilder builder = new VariableBuilder();
        builder.setName(name);
        builder.setDistributionType(DistType.GAUSSIAN);
        builder.setStateSpace(new RealStateSpace());
        builder.setObservable(false);

        return builder;
    }

    public static void main(String[] agrs) throws IOException, ClassNotFoundException {

        BayesianNetworkGenerator.setNumberOfContinuousVars(0);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(10);
        BayesianNetworkGenerator.setNumberOfStates(2);

        BayesianNetwork naiveBayes = BayesianNetworkGenerator.generateNaiveBayes(new Random(0), 2);

        BayesianNetworkWriter.saveToFile(naiveBayes, "networks/NB-10.ser");

        BayesianNetwork naiveBayes2 = BayesianNetworkLoader.loadFromFile("networks/NB-10.ser");
        System.out.println(naiveBayes2.toString());
    }
}
