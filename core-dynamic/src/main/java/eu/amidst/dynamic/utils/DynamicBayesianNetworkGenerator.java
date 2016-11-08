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

package eu.amidst.dynamic.utils;

import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class defines a random {@link DynamicBayesianNetwork} generator.
 */
public class DynamicBayesianNetworkGenerator {

    /** Represents the number of Multinomial variables in the {@link DynamicBayesianNetwork} to be generated. */
    private static int numberOfDiscreteVars = 10;

    /** Represents the number of states for each Multinomial variables in the {@link DynamicBayesianNetwork} to be generated. */
    private static int numberOfStates = 2;


    /** Represents the number of Gaussian variables in the {@link DynamicBayesianNetwork} to be generated. */
    private static int numberOfContinuousVars = 0;

    /** Represents the number of links in the {@link DynamicBayesianNetwork} to be generated. */
    private static int numberOfLinks = 3;

    /** Represents the seed for random generation. */
    private static int seed=0;


    private static String classVarName = "ClassVar";
    private static String discreteVarName = "DiscreteVar";
    private static String continuousVarName = "ContinuousVar";


    public static void setSeed(int seed) {
        DynamicBayesianNetworkGenerator.seed = seed;
    }

    public static void setClassVarName(String classVarName) {
        DynamicBayesianNetworkGenerator.classVarName = classVarName;
    }

    public static void setDiscreteVarName(String discreteVarName) {
        DynamicBayesianNetworkGenerator.discreteVarName = discreteVarName;
    }

    /**
     * Sets the number of links for this DynamicBayesianNetworkGenerator.
     * @param numberOfLinks an {@code int} that represents the number of links.
     */
    public static void setNumberOfLinks(int numberOfLinks) {
        DynamicBayesianNetworkGenerator.numberOfLinks = numberOfLinks;
    }

    /**
     * Sets the number of Multinomial variables for this DynamicBayesianNetworkGenerator.
     * @param numberOfDiscreteVars an {@code int} that represents the number of Multinomial variables.
     */
    public static void setNumberOfDiscreteVars(int numberOfDiscreteVars) {
        DynamicBayesianNetworkGenerator.numberOfDiscreteVars = numberOfDiscreteVars;
    }

    /**
     * Sets the number of Gaussian variables for this DynamicBayesianNetworkGenerator.
     * @param numberOfContinuousVars an {@code int} that represents the number of Gaussian variables.
     */
    public static void setNumberOfContinuousVars(int numberOfContinuousVars) {
        DynamicBayesianNetworkGenerator.numberOfContinuousVars = numberOfContinuousVars;
    }

    /**
     * Sets the number of the number of states of the Multinomial variables.
     * @param numberOfStates an {@code int} that represents the number of states.
     */
    public static void setNumberOfStates(int numberOfStates) {
        //the same number of states is assigned for each discrete variable
        DynamicBayesianNetworkGenerator.numberOfStates = numberOfStates;
    }

    /**
     * Generates a Dynamic Naive Bayes DAG with a Multinomial class variable.
     * @param numberClassStates the number of states for the class variable.
     * @param connectChildrenTemporally a {@code boolean} that indicates whether the children are connected temporally or not.
     * @return a {@link DynamicDAG} object.
     */
    public static DynamicDAG generateDynamicNaiveBayesDAG(int numberClassStates, boolean connectChildrenTemporally){
        DynamicVariables dynamicVariables  = new DynamicVariables();

        //class variable which is always discrete
        Variable classVar = dynamicVariables.newMultinomialDynamicVariable(classVarName, numberClassStates);

        //Discrete variables
        IntStream.range(1, numberOfDiscreteVars+1)
                .forEach(i -> dynamicVariables.newMultinomialDynamicVariable(discreteVarName + i,
                        DynamicBayesianNetworkGenerator.numberOfStates));

        //Continuous variables
        IntStream.range(1,numberOfContinuousVars+1)
                .forEach(i -> dynamicVariables.newGaussianDynamicVariable(continuousVarName + i));

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

        return dag;
    }

    /**
     * Generates a Dynamic Naive Bayes model with randomly initialized distributions.
     * @param random an object of type {@link java.util.Random}.
     * @param numberClassStates the number of states for the class variable.
     * @param connectChildrenTemporally a {@code boolean} that indicates whether the children are connected temporally or not.
     * @return a {@link DynamicBayesianNetwork} object.
     */
    public static DynamicBayesianNetwork generateDynamicNaiveBayes(Random random, int numberClassStates,
                                                                   boolean connectChildrenTemporally){
        DynamicBayesianNetwork network = new DynamicBayesianNetwork(
                DynamicBayesianNetworkGenerator.generateDynamicNaiveBayesDAG(numberClassStates,
                        connectChildrenTemporally));

        network.randomInitialization(random);

        return network;
    }


    /**
     * Generates a Dynamic TAN model with randomly initialized distributions.
     * @param random an object of type {@link java.util.Random}.
     * @param numberClassStates the number of states for the class variable.
     * @param connectChildrenTemporally a {@code boolean} that indicates whether the children are connected temporally or not.
     * @return a {@link DynamicBayesianNetwork} object.
     */
    public static DynamicBayesianNetwork generateDynamicTAN(Random random, int numberClassStates, boolean connectChildrenTemporally) {

        DynamicBayesianNetwork dynamicNB = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(random, numberClassStates, connectChildrenTemporally);

        DynamicVariables variables = dynamicNB.getDynamicVariables();
        DynamicDAG dynamicDAG = Serialization.deepCopy(dynamicNB.getDynamicDAG());

        int numberOfVariables = variables.getNumberOfVars();

        if(numberOfVariables>2) {

        Variable treeRoot;
        do {
            treeRoot = variables.getVariableById(random.nextInt(numberOfVariables));
        } while (!treeRoot.isMultinomial() || treeRoot.getName().equals(classVarName));

        List<Variable> variablesLevel2 = new ArrayList<>(0);


            Variable level2Var;
            do {
                level2Var = variables.getVariableById(random.nextInt(numberOfVariables));
            }
            while (!level2Var.isMultinomial() || level2Var.getName().equals(classVarName) || level2Var.equals(treeRoot));

            dynamicDAG.getParentSetTime0(level2Var).addParent(treeRoot);
            dynamicDAG.getParentSetTimeT(level2Var).addParent(treeRoot);
            variablesLevel2.add(level2Var);

            for (Variable currentVar : variables) {
                if (currentVar.equals(treeRoot) || currentVar.getName().equals(classVarName) || currentVar.equals(level2Var)) {
                    continue;
                }

                int aux = random.nextInt(10);
                int currentVarLevel = (aux < 5) ? 2 : 3;

                if (currentVarLevel == 3 && variablesLevel2.size() == 0) {
                    currentVarLevel = 2;
                }

                Variable possibleParent;

                do {
                    if (currentVarLevel == 2) {
                        possibleParent = treeRoot;
                    } else {
                        possibleParent = variablesLevel2.get(random.nextInt(variablesLevel2.size()));
                    }
                } while (currentVar.isMultinomial() && possibleParent.isNormal());

                dynamicDAG.getParentSetTime0(currentVar).addParent(possibleParent);
                dynamicDAG.getParentSetTimeT(currentVar).addParent(possibleParent);

                if (currentVarLevel == 2) {
                    variablesLevel2.add(currentVar);
                }
            }
        }
        if (dynamicDAG.toDAGTime0().containCycles() || dynamicDAG.toDAGTimeT().containCycles()) {

            System.out.println("ERROR: DAG WITH CYCLES");
            System.out.println(dynamicDAG.toString());
            System.exit(-1);
        }

        DynamicBayesianNetwork dynamicTAN = new DynamicBayesianNetwork(dynamicDAG);
        dynamicTAN.randomInitialization(random);
        return dynamicTAN;
    }

    /**
     * Generates a Dynamic FAN model with randomly initialized distributions.
     * @param random an object of type {@link java.util.Random}.
     * @param numberClassStates the number of states for the class variable.
     * @param connectChildrenTemporally a {@code boolean} that indicates whether the children are connected temporally or not.
     * @return a {@link DynamicBayesianNetwork} object.
     */
    public static DynamicBayesianNetwork generateDynamicFAN(Random random, int numberClassStates, boolean connectChildrenTemporally) {

        DynamicBayesianNetwork dynamicNB = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(random, numberClassStates, connectChildrenTemporally);

        DynamicVariables variables = dynamicNB.getDynamicVariables();
        DynamicDAG dynamicDAG = Serialization.deepCopy(dynamicNB.getDynamicDAG());

        int numberOfVariables = variables.getNumberOfVars();

        Variable level1Var;
        do {
            level1Var = variables.getVariableById(random.nextInt(numberOfVariables));
        } while (!level1Var.isMultinomial() || level1Var.getName().equals(classVarName));

        List<Variable> variablesLevel1 = new ArrayList<>(0);
        List<Variable> variablesLevel2 = new ArrayList<>(0);

        variablesLevel1.add(level1Var);

        Variable level2Var;
        do {
            level2Var = variables.getVariableById(random.nextInt(numberOfVariables));
        } while (!level2Var.isMultinomial() || level2Var.getName().equals(classVarName) || level2Var.equals(level1Var));

        dynamicDAG.getParentSetTime0(level2Var).addParent(level1Var);
        dynamicDAG.getParentSetTimeT(level2Var).addParent(level1Var);
        variablesLevel2.add(level2Var);

        for(Variable currentVar : variables) {
            if (currentVar.equals(level1Var) || currentVar.getName().equals(classVarName) || currentVar.equals(level2Var)) {
                continue;
            }

            int aux = random.nextInt(10);
            int currentVarLevel = (aux<4) ? 1 : (aux<8 ? 2 : 3);

            if (currentVarLevel==3 && variablesLevel2.size()==0) {
                currentVarLevel=2;
            }

            if (currentVarLevel==2 && variablesLevel1.size()==0) {
                currentVarLevel=1;
            }

            if (currentVarLevel==1) {
                variablesLevel1.add(currentVar);
                continue;
            }
            else {

                Variable possibleParent;

                do {
                    if (currentVarLevel==2) {
                        possibleParent = variablesLevel1.get(random.nextInt(variablesLevel1.size()));
                    }
                    else {
                        possibleParent = variablesLevel2.get(random.nextInt(variablesLevel2.size()));
                    }
                } while ( currentVar.isMultinomial() && possibleParent.isNormal());

                dynamicDAG.getParentSetTime0(currentVar).addParent(possibleParent);
                dynamicDAG.getParentSetTimeT(currentVar).addParent(possibleParent);

                if (currentVarLevel==2) {
                    variablesLevel2.add(currentVar);
                }
            }
        }

        if (dynamicDAG.toDAGTime0().containCycles() || dynamicDAG.toDAGTimeT().containCycles()) {
            System.out.println("ERROR: DAG WITH CYCLES");
            System.out.println(dynamicDAG.toString());
            System.exit(-1);
        }

        DynamicBayesianNetwork dynamicFAN = new DynamicBayesianNetwork(dynamicDAG);
        dynamicFAN.randomInitialization(random);
        return dynamicFAN;
    }


    /**
     * Generates a tree {@link DAG} given a set of variables.
     * @param variables a set of {@link Variables}.
     * @return a {@link DAG} object.
     */
    public static DynamicDAG generateTreeDAG(DynamicVariables variables) {
        DynamicDAG dag = new DynamicDAG(variables);

        //TREE FOR TIME 0
        List<Variable> connectedVars = new ArrayList();

        List<Variable> nonConnectedVars = variables.getListOfDynamicVariables().stream().collect(Collectors.toList());

        Random random = new Random(seed);


        connectedVars.add(nonConnectedVars.remove(random.nextInt(nonConnectedVars.size())));

        while (nonConnectedVars.size()>0){
            Variable var1 = connectedVars.get(random.nextInt(connectedVars.size()));
            Variable var2 = nonConnectedVars.get(random.nextInt(nonConnectedVars.size()));

            if (var1.getVarID()<var2.getVarID() && dag.getParentSetTime0(var2).getNumberOfParents()==0 && var2.getDistributionType().isParentCompatible(var1))
                dag.getParentSetTime0(var2).addParent(var1);
            else if (var2.getVarID()<var1.getVarID() && dag.getParentSetTime0(var1).getNumberOfParents()==0 && var1.getDistributionType().isParentCompatible(var2))
                dag.getParentSetTime0(var1).addParent(var2);
            else
                continue;

            nonConnectedVars.remove(var2);
            connectedVars.add(var2);
        }

        //TREE FOR TIME T
        connectedVars = new ArrayList();

        nonConnectedVars = variables.getListOfDynamicVariables().stream().collect(Collectors.toList());

        connectedVars.add(nonConnectedVars.remove(random.nextInt(nonConnectedVars.size())));

        while (nonConnectedVars.size()>0){
            Variable var1 = connectedVars.get(random.nextInt(connectedVars.size()));
            Variable var2 = nonConnectedVars.get(random.nextInt(nonConnectedVars.size()));

            if (var1.getVarID()<var2.getVarID() && dag.getParentSetTimeT(var2).getNumberOfParents()==0 && var2.getDistributionType().isParentCompatible(var1))
                dag.getParentSetTimeT(var2).addParent(var1);
            else if (var2.getVarID()<var1.getVarID() && dag.getParentSetTimeT(var1).getNumberOfParents()==0 && var1.getDistributionType().isParentCompatible(var2))
                dag.getParentSetTimeT(var1).addParent(var2);
            else
                continue;

            nonConnectedVars.remove(var2);
            connectedVars.add(var2);
        }

        return dag;
    }


    /**
     * Generates a {@link DynamicBayesianNetwork} randomly where each multinomial variable
     * Xi contains DynamicBayesianNetworkGenerator.numberOfStates+n[i] states.
     * @return a {@link DynamicBayesianNetwork} object.
     */
    public static DynamicBayesianNetwork generateDynamicBayesianNetwork(int n[]) {


        if (n.length != numberOfDiscreteVars) {
            System.out.println("ERROR: wrong size of n[] in generateDynamicBayesianNetwork");
            System.exit(-1);
        }


        DynamicVariables variables = new DynamicVariables();

        IntStream.range(0, DynamicBayesianNetworkGenerator.numberOfDiscreteVars)
                .forEach(i -> variables.newMultinomialDynamicVariable("DiscreteVar" + i, DynamicBayesianNetworkGenerator.numberOfStates+n[i]));

        IntStream.range(0, DynamicBayesianNetworkGenerator.numberOfContinuousVars)
                .forEach(i -> variables.newGaussianDynamicVariable("GaussianVar" + i));

        DynamicDAG dag = generateTreeDAG(variables);


        Random random = new Random(seed);

        //DAG TIME 0
        int dagLinks = variables.getNumberOfVars()-1;
        while (dagLinks< numberOfLinks){
            Variable var1 = variables.getVariableById(random.nextInt(variables.getNumberOfVars()));
            int max = variables.getNumberOfVars() - var1.getVarID() - 1;
            if (max == 0)
                continue;

            Variable var2 = variables.getVariableById(var1.getVarID() + 1 + random.nextInt(max));

            if (dag.getParentSetTime0(var2).contains(var1) || !var2.getDistributionType().isParentCompatible(var1) || dag.getParentSetTime0(var2).getNumberOfParents()>=3)
                continue;

            dag.getParentSetTime0(var2).addParent(var1);
            dagLinks++;
        }

        //DAG TIME T
        dagLinks = variables.getNumberOfVars()-1;
        while (dagLinks< numberOfLinks){
            Variable var1 = variables.getVariableById(random.nextInt(variables.getNumberOfVars()));
            int max = variables.getNumberOfVars() - var1.getVarID() - 1;
            if (max == 0)
                continue;

            Variable var2 = variables.getVariableById(var1.getVarID() + 1 + random.nextInt(max));

            if (dag.getParentSetTimeT(var2).contains(var1) || !var2.getDistributionType().isParentCompatible(var1) || dag.getParentSetTimeT(var2).getNumberOfParents()>=3)
                continue;

            dag.getParentSetTimeT(var2).addParent(var1);
            dagLinks++;
        }

        //Finally we connected over time.
        for (Variable variable : variables) {
            dag.getParentSetTimeT(variable).addParent(variable.getInterfaceVariable());
        }

        if (dag.containCycles())
            throw new IllegalStateException("DAG with cycles");

        DynamicBayesianNetwork network = new DynamicBayesianNetwork(dag);

        network.randomInitialization(new Random(seed));

        return network;
    }


    /**
     * Generates a {@link DynamicBayesianNetwork} randomly.
     * @return a {@link DynamicBayesianNetwork} object.
     */
    public static DynamicBayesianNetwork generateDynamicBayesianNetwork(){

        DynamicVariables variables = new DynamicVariables();

        IntStream.range(0, DynamicBayesianNetworkGenerator.numberOfDiscreteVars)
                .forEach(i -> variables.newMultinomialDynamicVariable("DiscreteVar" + i, DynamicBayesianNetworkGenerator.numberOfStates));

        IntStream.range(0, DynamicBayesianNetworkGenerator.numberOfContinuousVars)
                .forEach(i -> variables.newGaussianDynamicVariable("GaussianVar" + i));

        DynamicDAG dag = generateTreeDAG(variables);


        Random random = new Random(seed);

        //DAG TIME 0
        int dagLinks = variables.getNumberOfVars()-1;
        int iter = 0;
        while (dagLinks< numberOfLinks && iter<1000){
            Variable var1 = variables.getVariableById(random.nextInt(variables.getNumberOfVars()));
            int max = variables.getNumberOfVars() - var1.getVarID() - 1;
            if (max == 0)
                continue;

            Variable var2 = variables.getVariableById(var1.getVarID() + 1 + random.nextInt(max));

            if (dag.getParentSetTime0(var2).contains(var1) || !var2.getDistributionType().isParentCompatible(var1) || dag.getParentSetTime0(var2).getNumberOfParents()>=3) {
                iter++;
                continue;
            }

            dag.getParentSetTime0(var2).addParent(var1);
            dagLinks++;
        }

        //DAG TIME T
        dagLinks = variables.getNumberOfVars()-1;
        iter = 0;
        while (dagLinks< numberOfLinks && iter<1000){
            Variable var1 = variables.getVariableById(random.nextInt(variables.getNumberOfVars()));
            int max = variables.getNumberOfVars() - var1.getVarID() - 1;
            if (max == 0)
                continue;

            Variable var2 = variables.getVariableById(var1.getVarID() + 1 + random.nextInt(max));

            if (dag.getParentSetTimeT(var2).contains(var1) || !var2.getDistributionType().isParentCompatible(var1) || dag.getParentSetTimeT(var2).getNumberOfParents()>=3){
                iter++;
                continue;
            }
            dag.getParentSetTimeT(var2).addParent(var1);
            dagLinks++;
            iter++;
        }

        //Finally we connected over time.
        for (Variable variable : variables) {
            dag.getParentSetTimeT(variable).addParent(variable.getInterfaceVariable());
        }

        if (dag.containCycles())
            throw new IllegalStateException("DAG with cycles");

        DynamicBayesianNetwork network = new DynamicBayesianNetwork(dag);

        network.randomInitialization(new Random(seed));

        return network;
    }

    public static void main(String[] agrs) throws IOException, ClassNotFoundException {

        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(2);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(8);
        DynamicBayesianNetworkGenerator.setNumberOfStates(2);
        DynamicBayesianNetworkGenerator.setNumberOfLinks(5);

        DynamicBayesianNetwork dynamicBayesianNetwork;

        dynamicBayesianNetwork = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);
        System.out.println("DYNAMIC NAIVE BAYES");
        System.out.println(dynamicBayesianNetwork.getDynamicDAG().toString());
        dynamicBayesianNetwork = DynamicBayesianNetworkGenerator.generateDynamicFAN(new Random(0), 2, true);
        System.out.println("DYNAMIC FAN");
        System.out.println(dynamicBayesianNetwork.getDynamicDAG().toString());
        dynamicBayesianNetwork = DynamicBayesianNetworkGenerator.generateDynamicTAN(new Random(0), 2, true);
        System.out.println("DYNAMIC TAN");
        System.out.println(dynamicBayesianNetwork.getDynamicDAG().toString());

        dynamicBayesianNetwork = DynamicBayesianNetworkGenerator.generateDynamicBayesianNetwork();
        System.out.println("DYNAMIC BN");
        System.out.println(dynamicBayesianNetwork.getDynamicDAG().toString());

    }
}
