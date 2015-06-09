/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.corestatic.utils;

import eu.amidst.corestatic.io.BayesianNetworkLoader;
import eu.amidst.corestatic.io.BayesianNetworkWriter;
import eu.amidst.corestatic.models.BayesianNetwork;
import eu.amidst.corestatic.models.DAG;
import eu.amidst.corestatic.variables.StaticVariables;
import eu.amidst.corestatic.variables.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by andresmasegosa on 09/01/15.
 */
public final class BayesianNetworkGenerator{

    private static int numberOfVars;
    private static int numberOfLinks;
    private static int numberOfDiscreteVars;
    private static int numberOfContinuousVars;
    private static int numberOfStates;
    private static int seed;

    public static void setSeed(int seed) {
        BayesianNetworkGenerator.seed = seed;
    }

    public static void setNumberOfVars(int numberOfVars) {
        BayesianNetworkGenerator.numberOfVars = numberOfVars;
    }

    public static void setNumberOfLinks(int numberOfLinks) {
        if (numberOfLinks<(numberOfVars-1) || numberOfLinks>numberOfVars*(numberOfVars-1)/2)
            throw new IllegalArgumentException("Number of links is not between " + (numberOfVars-1) + " and " + numberOfVars*(numberOfVars-1)/2);
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

    public static DAG generateTreeDAG(StaticVariables staticVariables) {
        DAG dag = new DAG(staticVariables);

        List<Variable> connectedVars = new ArrayList();

        List<Variable> nonConnectedVars = staticVariables.getListOfVariables().stream().collect(Collectors.toList());

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

    public static BayesianNetwork generateBayesianNetwork(){

        StaticVariables staticVariables  = new StaticVariables();


        IntStream.range(0,numberOfDiscreteVars)
                .forEach(i -> staticVariables.newMultionomialVariable("DiscreteVar" + i, BayesianNetworkGenerator.numberOfStates));

        IntStream.range(0,numberOfContinuousVars)
                .forEach(i -> staticVariables.newGaussianVariable("GaussianVar" + i));

        DAG dag = generateTreeDAG(staticVariables);

        int dagLinks = staticVariables.getNumberOfVars()-1;

        Random random = new Random(seed);
        while (dagLinks< numberOfLinks){
            Variable var1 = staticVariables.getVariableById(random.nextInt(staticVariables.getNumberOfVars()));
            int max = staticVariables.getNumberOfVars() - var1.getVarID() - 1;
            if (max == 0)
                continue;

            Variable var2 = staticVariables.getVariableById(var1.getVarID() + 1 + random.nextInt(max));

            if (dag.getParentSet(var2).contains(var1) || !var2.getDistributionType().isParentCompatible(var1) || dag.getParentSet(var2).getNumberOfParents()>=3)
                continue;

            dag.getParentSet(var2).addParent(var1);
            dagLinks++;
        }

        if (dag.containCycles())
            throw new IllegalStateException("DAG with cycles");

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
        return "BayesianNetworkGenerator";
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

    public static void generateBNtoFile(int nDiscrete, int nStates, int nContin, int nLinks, int seed_, String filename) throws IOException {


        numberOfLinks=nLinks;
        numberOfDiscreteVars=nDiscrete;
        numberOfContinuousVars=nContin;
        numberOfStates =nStates;
        numberOfVars=nDiscrete+nContin;
        seed = seed_;

        BayesianNetwork bayesianNetwork = BayesianNetworkGenerator.generateBayesianNetwork();
        BayesianNetworkWriter.saveToFile(bayesianNetwork, filename);
    }

    public static void main(String[] agrs) throws IOException, ClassNotFoundException {

        BayesianNetworkGenerator.loadOptions();

        BayesianNetworkGenerator.setNumberOfContinuousVars(5);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(5);
        BayesianNetworkGenerator.setNumberOfStates(2);
        BayesianNetworkGenerator.setNumberOfLinks(15);
        BayesianNetworkGenerator.setSeed(0);

        BayesianNetwork bayesianNetwork = BayesianNetworkGenerator.generateBayesianNetwork();

        BayesianNetworkWriter.saveToFile(bayesianNetwork, "networks/Bayesian10Vars15Links.bn");

        BayesianNetwork bayesianNetwork2 = BayesianNetworkLoader.loadFromFile("networks/Bayesian10Vars15Links.bn");

        System.out.println(bayesianNetwork2.getDAG().toString());
    }

}
