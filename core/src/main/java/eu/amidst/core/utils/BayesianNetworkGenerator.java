/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.core.utils;

import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variables;
import eu.amidst.core.variables.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class defines a random {@link BayesianNetwork} generator.
 */
public final class BayesianNetworkGenerator{

    /** Represents the number of links in the {@link BayesianNetwork} to be generated. */
    private static int numberOfLinks;

    /** Represents the number of Multinomial variables in the {@link BayesianNetwork} to be generated. */
    private static int numberOfMultinomialVars;

    /** Represents the number of Gaussian variables in the {@link BayesianNetwork} to be generated. */
    private static int numberOfGaussianVars;

    /** Represents the number of states for the multinomial variables in the {@link BayesianNetwork} to be generated. */
    private static int numberOfStates;

    /** Represents the seed for random generation. */
    private static int seed=0;

    /**
     * Sets the seed for this BayesianNetworkGenerator.
     * @param seed an {@code Integer} that represents the seed.
     */
    public static void setSeed(int seed) {
        BayesianNetworkGenerator.seed = seed;
    }

    /**
     * Sets the number of links for this BayesianNetworkGenerator.
     * @param numberOfLinks an {@code int} that represents the number of links.
     */
    public static void setNumberOfLinks(int numberOfLinks) {
        int numberOfVars = numberOfMultinomialVars + numberOfGaussianVars;
        if (numberOfLinks<(numberOfVars-1) || numberOfLinks>numberOfVars*(numberOfVars-1)/2)
            throw new IllegalArgumentException("Number of links is not between " + (numberOfVars-1) + " and " + numberOfVars*(numberOfVars-1)/2);
        BayesianNetworkGenerator.numberOfLinks = numberOfLinks;
    }

    /**
     * Sets the number of Multinomial variables and their corresponding states for this BayesianNetworkGenerator.
     * @param numberOfMultinomialVars_ an {@code int} that represents the number of Multinmail variables.
     * @param numberOfStates_ an {@code int} that represents the number of states.
     */
    public static void setNumberOfMultinomialVars(int numberOfMultinomialVars_, int numberOfStates_) {
        BayesianNetworkGenerator.numberOfMultinomialVars = numberOfMultinomialVars_;
        BayesianNetworkGenerator.numberOfStates = numberOfStates_;
    }

    /**
     * Sets the number of Gaussian variables for this BayesianNetworkGenerator.
     * @param numberOfGaussianVars an {@code int} that represents the number of Gaussian variables.
     */
    public static void setNumberOfGaussianVars(int numberOfGaussianVars) {
        BayesianNetworkGenerator.numberOfGaussianVars = numberOfGaussianVars;
    }

    //TODO Check this method!! Maybe also moved to ida2015 package
    /**
     * Generates a NaiveBayesClassifier with a global Gaussian hidden variable.
     * The global Gaussian hidden variable is a child of the class variable and a parent of all the remaining variables.
     * @param nClassLabels the number of states for the class variable.
     * @param nameGlobalHiddenVar a {@code String} that represents the name of the global hidden variable.
     * @return a {@link BayesianNetwork} object.
     */
    public static BayesianNetwork generateNaiveBayesWithGlobalHiddenVar(int nClassLabels, String nameGlobalHiddenVar){

        Variables variables = new Variables();

        IntStream.range(0, numberOfMultinomialVars -1)
                .forEach(i -> variables.newMultinomialVariable("DiscreteVar" + i, BayesianNetworkGenerator.numberOfStates));

        IntStream.range(0, numberOfGaussianVars)
                .forEach(i -> variables.newGaussianVariable("GaussianVar" + i));

        Variable globalHiddenVar =  variables.newGaussianVariable(nameGlobalHiddenVar);

        Variable classVar = variables.newMultinomialVariable("ClassVar", nClassLabels);

        DAG dag = new DAG(variables);

        dag.getParentSets().stream()
                .filter(parentSet -> !parentSet.getMainVar().equals(classVar) && !parentSet.getMainVar().equals(globalHiddenVar))
                .forEach(w -> {w.addParent(classVar); w.addParent(globalHiddenVar);});

        BayesianNetwork network = new BayesianNetwork(dag);

        network.randomInitialization(new Random(seed));

        return network;
    }

    /**
     * Generates a NaiveBayesClassifier.
     * @param nClassLabels the number of states for the class variable.
     * @return a {@link BayesianNetwork} object.
     */
    public static BayesianNetwork generateNaiveBayes(int nClassLabels){

        Variables variables = new Variables();


        IntStream.range(0, numberOfMultinomialVars -1)
                .forEach(i -> variables.newMultinomialVariable("DiscreteVar" + i, BayesianNetworkGenerator.numberOfStates));

        IntStream.range(0, numberOfGaussianVars)
                .forEach(i -> variables.newGaussianVariable("GaussianVar" + i));

        Variable classVar = variables.newMultinomialVariable("ClassVar", nClassLabels);

        DAG dag = new DAG(variables);

        dag.getParentSets().stream()
                .filter(parentSet -> parentSet.getMainVar().getVarID()!=classVar.getVarID())
                .forEach(w -> w.addParent(classVar));

        BayesianNetwork network = new BayesianNetwork(dag);

        network.randomInitialization(new Random(seed));

        return network;
    }

    /**
     * Generates a tree {@link DAG} given a set of variables.
     * @param variables a set of {@link Variables}.
     * @return a {@link DAG} object.
     */
    public static DAG generateTreeDAG(Variables variables) {
        DAG dag = new DAG(variables);

        List<Variable> connectedVars = new ArrayList();

        List<Variable> nonConnectedVars = variables.getListOfVariables().stream().collect(Collectors.toList());

        Random random = new Random(seed);


        connectedVars.add(nonConnectedVars.remove(random.nextInt(nonConnectedVars.size())));

        while (nonConnectedVars.size()>0){
            Variable var1 = connectedVars.get(random.nextInt(connectedVars.size()));
            Variable var2 = nonConnectedVars.get(random.nextInt(nonConnectedVars.size()));

            if (var1.getVarID()<var2.getVarID() && dag.getParentSet(var2).getNumberOfParents()==0 && var2.getDistributionType().isParentCompatible(var1))
                dag.getParentSet(var2).addParent(var1);
            else if (var2.getVarID()<var1.getVarID() && dag.getParentSet(var1).getNumberOfParents()==0 && var1.getDistributionType().isParentCompatible(var2))
                dag.getParentSet(var1).addParent(var2);
            else
                continue;

            nonConnectedVars.remove(var2);
            connectedVars.add(var2);
        }

        return dag;
    }

    /**
     * Generates a {@link BayesianNetwork} randomly.
     * @return a {@link BayesianNetwork} object.
     */
    public static BayesianNetwork generateBayesianNetwork(){

        Variables variables = new Variables();

        IntStream.range(0, numberOfMultinomialVars)
                .forEach(i -> variables.newMultinomialVariable("DiscreteVar" + i, BayesianNetworkGenerator.numberOfStates));

        IntStream.range(0, numberOfGaussianVars)
                .forEach(i -> variables.newGaussianVariable("GaussianVar" + i));

        DAG dag = generateTreeDAG(variables);

        int dagLinks = variables.getNumberOfVars()-1;

        Random random = new Random(seed);
        while (dagLinks< numberOfLinks){
            Variable var1 = variables.getVariableById(random.nextInt(variables.getNumberOfVars()));
            int max = variables.getNumberOfVars() - var1.getVarID() - 1;
            if (max == 0)
                continue;

            Variable var2 = variables.getVariableById(var1.getVarID() + 1 + random.nextInt(max));

            if (dag.getParentSet(var2).contains(var1) || !var2.getDistributionType().isParentCompatible(var1) || dag.getParentSet(var2).getNumberOfParents()>=3)
                continue;

            dag.getParentSet(var2).addParent(var1);
            dagLinks++;
        }

        if (dag.containCycles())
            throw new IllegalStateException("DAG with cycles");

        BayesianNetwork network = new BayesianNetwork(dag);

        network.randomInitialization(new Random(seed));

        return network;
    }

    /**
     * Returns the list of options used by this BayesianNetworkGenerator.
     * @return a {@code String} that represents the list of options.
     */
    public static String listOptions(){
        return  classNameID() +", "+
                "-numberOfVars, 10, Total number of variables\\" +
                "-numberOfLinks, 3, Number of links\\" +
                "-numberOfMultinomialVars, 10, Number of discrete variables\\"+
                "-numberOfGaussianVars, 0, Number of continuous variables.\\" +
                "-numberOfStates, 2, Number of states per discrete variable\\" +
                "-seed, 0, seed for random number generator\\";
    }

    /**
     * Loads the list of options to be used by this BayesianNetworkGenerator.
     */
    public static void loadOptions(){
        numberOfLinks = getIntOption("-numberOfLinks");
        numberOfMultinomialVars = getIntOption("-numberOfMultinomialVars");
        numberOfGaussianVars = getIntOption("-numberOfGaussianVars");
        numberOfStates = getIntOption("-numberOfStates");
        seed = getIntOption("-seed");
    }

    /**
     * Returns the name of this class.
     * @return the name of this class.
     */
    public static String classNameID(){
        return "BayesianNetworkGenerator";
    }

    /**
     * Sets the list of options to be used by this BayesianNetworkGenerator.
     * @param args the list of options given as an array of Strings.
     */
    public static void setOptions(String[] args) {
        OptionParser.setArgsOptions(classNameID(),args);
        loadOptions();
    }

    /**
     * Loads the list of options to be used by this BayesianNetworkGenerator from a given file name.
     * @param fileName a {@code String} that represents the file name.
     */
    public static void loadOptionsFromFile(String fileName){
        OptionParser.setConfFileName(fileName);
        OptionParser.loadFileOptions();
        OptionParser.loadDefaultOptions(classNameID());
        loadOptions();
    }

    /**
     * Returns the {@code String} value of an option given its name.
     * @param optionName the option name.
     * @return an {@code String} containing the value of the option.
     */
    public static String getOption(String optionName) {
        return OptionParser.parse(classNameID(), listOptions(), optionName);
    }

    /**
     * Returns the {@code int} value of an option given its name.
     * @param optionName the option name.
     * @return an {@code int} containing the value of the option.
     */
    public static int getIntOption(String optionName){
        return Integer.parseInt(getOption(optionName));
    }

    /**
     * Generates a {@link eu.amidst.core.models.BayesianNetwork} then saves it in a file.
     * @param nDiscrete an {@code int} that represents the number of Multinomial variables.
     * @param nStates an {@code int} that represents the number of states.
     * @param nContin an {@code int} that represents the number of Gaussian variables.
     * @param nLinks an {@code int} that represents the number of links.
     * @param seed_ an {@code int} that represents the seed.
     * @param filename an {@code String} that represents the name of the file where the generated BN will be saved.
     * @throws IOException signals that an I/O exception has occurred.
     */
    public static void generateBNtoFile(int nDiscrete, int nStates, int nContin, int nLinks, int seed_, String filename) throws IOException {

        numberOfLinks=nLinks;
        numberOfMultinomialVars =nDiscrete;
        numberOfGaussianVars =nContin;
        numberOfStates =nStates;
        seed = seed_;

        BayesianNetwork bayesianNetwork = BayesianNetworkGenerator.generateBayesianNetwork();
        BayesianNetworkWriter.save(bayesianNetwork, filename);
    }


    public static void main(String[] agrs) throws IOException, ClassNotFoundException {

        BayesianNetworkGenerator.loadOptions();
        BayesianNetworkGenerator.setNumberOfGaussianVars(5);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(5, 2);
        BayesianNetworkGenerator.setNumberOfLinks(15);
        BayesianNetworkGenerator.setSeed(0);

        BayesianNetwork bayesianNetwork = BayesianNetworkGenerator.generateBayesianNetwork();
        BayesianNetworkWriter.save(bayesianNetwork, "networks/simulated/Bayesian10Vars15Links.bn");

        BayesianNetwork bayesianNetwork2 = BayesianNetworkLoader.loadFromFile("networks/simulated/Bayesian10Vars15Links.bn");
        System.out.println(bayesianNetwork2.getDAG().toString());
    }

}
