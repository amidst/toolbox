package eu.amidst.core.utils;

import eu.amidst.core.models.*;
import eu.amidst.core.variables.*;

import java.io.IOException;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Created by Hanen on 16/01/15.
 */
public class DynamicBayesianNetworkGenerator {

    private static int numberOfVars = 10;
    private static int numberOfLinks = 3;
    private static int numberOfDiscreteVars = 10;
    private static int numberOfContinuousVars = 0;
    private static int numberOfStates = 2;

    public static void setNumberOfVars(int numberOfVars) {
        DynamicBayesianNetworkGenerator.numberOfVars = numberOfVars;
    }

    public static void setNumberOfLinks(int numberOfLinks) {
        DynamicBayesianNetworkGenerator.numberOfLinks = numberOfLinks;
    }

    public static void setNumberOfDiscreteVars(int numberOfDiscreteVars) {
        DynamicBayesianNetworkGenerator.numberOfDiscreteVars = numberOfDiscreteVars;
    }

    public static void setNumberOfContinuousVars(int numberOfContinuousVars) {
        DynamicBayesianNetworkGenerator.numberOfContinuousVars = numberOfContinuousVars;
    }

    public static void setNumberOfStates(int numberOfStates) {
        DynamicBayesianNetworkGenerator.numberOfStates = numberOfStates;
    }

    public static DynamicBayesianNetwork generateDynamicNaiveBayes(Random random, int nClassLabels){

        DynamicVariables dynamicVariables  = new DynamicVariables();


        IntStream.range(0, numberOfDiscreteVars - 1)
                .forEach(i -> dynamicVariables.addHiddenDynamicVariable(generateDiscreteVariable("DiscreteVar" + i, DynamicBayesianNetworkGenerator.numberOfStates)));

        IntStream.range(0,numberOfContinuousVars)
                .forEach(i -> dynamicVariables.addHiddenDynamicVariable(generateContinuousVariable("GaussianVar" + i)));

        Variable classVar = dynamicVariables.addHiddenDynamicVariable(generateDiscreteVariable("ClassVar", nClassLabels));

        DynamicDAG dag = new DynamicDAG(dynamicVariables);

        /*dag.getParentSetsTime0().stream()
                .filter(var -> var.getMainVar().getVarID()!=classVar.getVarID())
                .forEach(w -> w.addParent(classVar));*/

        dag.getParentSetsTimeT().stream()
                .filter(var -> var.getMainVar().getVarID()!=classVar.getVarID())
                .forEach(w -> {
                            w.addParent(classVar);
                            w.addParent(dynamicVariables.getTemporalClone(w.getMainVar()));
                        }
                );

        dag.getParentSetTimeT(classVar).addParent(dynamicVariables.getTemporalClone(classVar));

        DynamicBayesianNetwork network = DynamicBayesianNetwork.newDynamicBayesianNetwork(dag);

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

        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(0);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(10);
        DynamicBayesianNetworkGenerator.setNumberOfStates(2);

        DynamicBayesianNetwork dynamicNaiveBayes = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2);

        System.out.println(dynamicNaiveBayes.getDynamicDAG().toString());
        System.out.println(dynamicNaiveBayes.toString());

        DynamicBayesianNetworkWriter.saveToFile(dynamicNaiveBayes, "networks/DynamicNB-10.ser");

    }
}
