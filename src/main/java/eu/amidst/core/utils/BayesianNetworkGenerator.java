package eu.amidst.core.utils;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.*;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;

import java.io.IOException;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Created by andresmasegosa on 09/01/15.
 */
public final class BayesianNetworkGenerator{

    private static int numberOfVars;
    private static int numberOfLinks;
    private static int numberOfDiscreteVars = 10;
    private static int numberOfContinuousVars = 10;
    private static int numberOfStates = 2;
    private static int seed = 0;

    public static void setSeed(int seed) {
        BayesianNetworkGenerator.seed = seed;
    }

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

    public static BayesianNetwork generateNaiveBayesWithGlobalHiddenVar(int nClassLabels, String nameGlobalHiddenVar){

        StaticVariables staticVariables  = new StaticVariables();


        IntStream.range(0,numberOfDiscreteVars-1)
                .forEach(i -> staticVariables.newMultionomialVariable("DiscreteVar" + i, BayesianNetworkGenerator.numberOfStates));

        IntStream.range(0,numberOfContinuousVars)
                .forEach(i -> staticVariables.newGaussianVariable("GaussianVar" + i));

        Variable globalHiddenVar =  staticVariables.newGaussianVariable(nameGlobalHiddenVar);

        Variable classVar = staticVariables.newMultionomialVariable("ClassVar", nClassLabels);

        DAG dag = new DAG(staticVariables);

        dag.getParentSets().stream()
                .filter(parentSet -> !parentSet.getMainVar().equals(classVar) && !parentSet.getMainVar().equals(globalHiddenVar))
                .forEach(w -> {w.addParent(classVar); w.addParent(globalHiddenVar);});

        BayesianNetwork network = BayesianNetwork.newBayesianNetwork(dag);

        network.randomInitialization(new Random(seed));

        return network;
    }


    public static BayesianNetwork generateNaiveBayes(int nClassLabels){

        StaticVariables staticVariables  = new StaticVariables();


        IntStream.range(0,numberOfDiscreteVars-1)
                .forEach(i -> staticVariables.newMultionomialVariable("DiscreteVar" + i, BayesianNetworkGenerator.numberOfStates));

        IntStream.range(0,numberOfContinuousVars)
                .forEach(i -> staticVariables.newGaussianVariable("GaussianVar" + i));

        Variable classVar = staticVariables.newMultionomialVariable("ClassVar", nClassLabels);

        DAG dag = new DAG(staticVariables);

        dag.getParentSets().stream()
                .filter(parentSet -> parentSet.getMainVar().getVarID()!=classVar.getVarID())
                .forEach(w -> w.addParent(classVar));

        BayesianNetwork network = BayesianNetwork.newBayesianNetwork(dag);

        network.randomInitialization(new Random(seed));

        return network;
    }

    public static String listOptions(){
        return  classNameID() +", "+
                "-numberOfVars, 10, Total number of variables\\" +
                "-numberOfLinks, 3, Number of links\\" +
                "-numberOfDiscreteVars, 10, Number of discrete variables\\"+
                "-numberOfContinuousVars, 0, Number of continuous variables.\\" +
                "-numberOfStates, 2, Number of states per discrete variable\\" +
                "-seed, 0, seed for random number generator\\";
    }

    public static void loadOptions(){
        numberOfVars = getIntOption("-numberOfVars");
        numberOfLinks = getIntOption("-numberOfLinks");
        numberOfDiscreteVars = getIntOption("-numberOfDiscreteVars");
        numberOfContinuousVars = getIntOption("-numberOfContinuousVars");
        numberOfStates = getIntOption("-numberOfStates");
        seed = getIntOption("-seed");
    }

    public static String classNameID(){
        return "eu.amidst.core.utils.BayesianNetworkGenerator";
    }

    public static void setOptions(String[] args) {
        OptionParser.setArgsOptions(classNameID(),args);
        loadOptions();
    }

    public static void loadOptionsFromFile(String fileName){
        OptionParser.setConfFileName(fileName);
        OptionParser.loadFileOptions();
        OptionParser.loadDefaultOptions(classNameID());
        loadOptions();
    }

    public static String getOption(String optionName) {
        return OptionParser.parse(classNameID(), listOptions(), optionName);
    }

    public static int getIntOption(String optionName){
        return Integer.parseInt(getOption(optionName));
    }

    public static void main(String[] agrs) throws IOException, ClassNotFoundException {

        BayesianNetworkGenerator.loadOptions();

        BayesianNetworkGenerator.setNumberOfContinuousVars(0);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(10);
        BayesianNetworkGenerator.setNumberOfStates(2);
        BayesianNetworkGenerator.setSeed(0);

        BayesianNetwork naiveBayes = BayesianNetworkGenerator.generateNaiveBayes(2);

        BayesianNetworkWriter.saveToFile(naiveBayes, "networks/NB-10.bn");

        BayesianNetwork naiveBayes2 = BayesianNetworkLoader.loadFromFile("networks/NB-10.bn");

        System.out.println(naiveBayes2.toString());
    }

}
