package eu.amidst.core.utils;

import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.BayesianNetworkWriter;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.*;

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

    public static BayesianNetwork generateNaiveBayes(Random random){

        StaticVariables staticVariables  = new StaticVariables();

        Variable classVar = staticVariables.addHiddenVariable(generateDiscreteVariable("ClassVar"));

        IntStream.range(0,numberOfDiscreteVars-1)
                .forEach(i -> staticVariables.addHiddenVariable(generateDiscreteVariable("DiscreteVar" + i)));

        IntStream.range(0,numberOfContinuousVars)
                .forEach(i -> staticVariables.addHiddenVariable(generateContinuousVariable("GaussianVar" + i)));

        DAG dag = new DAG(staticVariables);

        dag.getParentSets().stream()
                .filter(var -> var.getMainVar().getVarID()!=classVar.getVarID())
                .forEach(w -> w.addParent(classVar));

        BayesianNetwork network = BayesianNetwork.newBayesianNetwork(dag);

        network.randomInitialization(random);

        return network;
    }

    private static VariableBuilder generateDiscreteVariable(String name){
        VariableBuilder builder = new VariableBuilder();
        builder.setName(name);
        builder.setDistributionType(DistType.MULTINOMIAL);
        builder.setStateSpace(new FiniteStateSpace(BayesianNetworkGenerator.numberOfStates));
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

    public static void main(String[] agrs) throws ExceptionHugin {

        BayesianNetworkGenerator.setNumberOfContinuousVars(0);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(10000);
        BayesianNetworkGenerator.setNumberOfStates(10);

        BayesianNetwork naiveBayes = BayesianNetworkGenerator.generateNaiveBayes(new Random(0));

        BayesianNetworkWriter.saveToHuginFile(naiveBayes,"./networks/NB-1000.net");

    }
}
