package eu.amidst.core.utils;

import eu.amidst.core.io.DynamicBayesianNetworkWriter;
import eu.amidst.core.models.*;
import eu.amidst.core.variables.*;

import java.io.IOException;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Created by Hanen on 16/01/15.
 */
public class DynamicBayesianNetworkGenerator {

    private static int numberOfDiscreteVars = 10;
    private static int numberOfStates = 2;
    private static int numberOfContinuousVars = 0;
    private static int numberOfLinks = 3;

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
        //the same number of states is assigned for each discrete variable
        DynamicBayesianNetworkGenerator.numberOfStates = numberOfStates;
    }

    public static DynamicBayesianNetwork generateDynamicNaiveBayes(Random random, int numberClassStates, boolean connectChildrenTemporally){

        DynamicVariables dynamicVariables  = new DynamicVariables();

        //class variable which is always discrete
        Variable classVar = dynamicVariables.newMultinomialDynamicVariable("ClassVar", numberClassStates);

        //Discrete variables
        IntStream.range(1, numberOfDiscreteVars+1)
                .forEach(i -> dynamicVariables.newMultinomialDynamicVariable("DiscreteVar" + i, DynamicBayesianNetworkGenerator.numberOfStates));

        //Continuous variables
        IntStream.range(1,numberOfContinuousVars+1)
                .forEach(i -> dynamicVariables.newGaussianDynamicVariable("ContinuousVar" + i));

        DynamicDAG dag = new DynamicDAG(dynamicVariables);

        dag.getParentSetsTimeT().stream()
                .filter(var -> var.getMainVar().getVarID()!=classVar.getVarID())
                .forEach(w -> {
                            w.addParent(classVar);
                            if (connectChildrenTemporally) {
                                w.addParent(dynamicVariables.getInterfaceVariable(w.getMainVar()));
                            }
                        }
                );

        dag.getParentSetTimeT(classVar).addParent(dynamicVariables.getInterfaceVariable(classVar));

        DynamicBayesianNetwork network = DynamicBayesianNetwork.newDynamicBayesianNetwork(dag);

        network.randomInitialization(random);

        return network;
    }

    public static void main(String[] agrs) throws IOException, ClassNotFoundException {

        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(0);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfStates(2);
        DynamicBayesianNetworkGenerator.setNumberOfLinks(5);

        DynamicBayesianNetwork dynamicNaiveBayes = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);

        System.out.println(dynamicNaiveBayes.getDynamicDAG().toString());
        System.out.println(dynamicNaiveBayes.toString());

        DynamicBayesianNetworkWriter.saveToFile(dynamicNaiveBayes, "networks/DynamicNB-10.dbn");

    }
}
