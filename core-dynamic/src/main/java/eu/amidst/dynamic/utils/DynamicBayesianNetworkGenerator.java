/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.dynamic.utils;

import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
        Variable classVar = dynamicVariables.newMultinomialDynamicVariable("ClassVar", numberClassStates);

        //Discrete variables
        IntStream.range(1, numberOfDiscreteVars+1)
                .forEach(i -> dynamicVariables.newMultinomialDynamicVariable("DiscreteVar" + i,
                        DynamicBayesianNetworkGenerator.numberOfStates));

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

        Variable treeRoot;
        do {
            treeRoot = variables.getVariableById(random.nextInt(numberOfVariables));
        } while (!treeRoot.isMultinomial() || treeRoot.getName().equals("ClassVar"));

        List<Variable> variablesLevel2 = new ArrayList<>(0);

        Variable level2Var;
        do {
            level2Var = variables.getVariableById(random.nextInt(numberOfVariables));
        } while (!level2Var.isMultinomial() || level2Var.getName().equals("ClassVar") || level2Var.equals(treeRoot));

        dynamicDAG.getParentSetTime0(level2Var).addParent(treeRoot);
        dynamicDAG.getParentSetTimeT(level2Var).addParent(treeRoot);
        variablesLevel2.add(level2Var);

        for(Variable currentVar : variables) {
            if (currentVar.equals(treeRoot) || currentVar.getName().equals("ClassVar") || currentVar.equals(level2Var)) {
                continue;
            }

            int aux = random.nextInt(10);
            int currentVarLevel = (aux<5) ? 2 : 3;

            if (currentVarLevel==3 && variablesLevel2.size()==0) {
                currentVarLevel=2;
            }

            Variable possibleParent;

            do {
                if (currentVarLevel==2) {
                    possibleParent = treeRoot;
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
        } while (!level1Var.isMultinomial() || level1Var.getName().equals("ClassVar"));

        List<Variable> variablesLevel1 = new ArrayList<>(0);
        List<Variable> variablesLevel2 = new ArrayList<>(0);

        variablesLevel1.add(level1Var);

        Variable level2Var;
        do {
            level2Var = variables.getVariableById(random.nextInt(numberOfVariables));
        } while (!level2Var.isMultinomial() || level2Var.getName().equals("ClassVar") || level2Var.equals(level1Var));

        dynamicDAG.getParentSetTime0(level2Var).addParent(level1Var);
        dynamicDAG.getParentSetTimeT(level2Var).addParent(level1Var);
        variablesLevel2.add(level2Var);

        for(Variable currentVar : variables) {
            if (currentVar.equals(level1Var) || currentVar.getName().equals("ClassVar") || currentVar.equals(level2Var)) {
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

    }
}
