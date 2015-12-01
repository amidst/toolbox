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
import eu.amidst.dynamic.io.DynamicBayesianNetworkWriter;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
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

    public static DynamicBayesianNetwork generateDynamicNaiveBayes(Random random, int numberClassStates,
                                                                   boolean connectChildrenTemporally){



        DynamicBayesianNetwork network = new DynamicBayesianNetwork(
                DynamicBayesianNetworkGenerator.generateDynamicNaiveBayesDAG(numberClassStates,
                        connectChildrenTemporally));

        network.randomInitialization(random);

        return network;
    }

    // TODO: CHECK THIS FUNCTION, NO GUARANTEE OF OBTAINING A RESULT
    public static DynamicBayesianNetwork generateDynamicTAN(Random random, int numberClassStates, boolean connectChildrenTemporally){

        DynamicBayesianNetwork dynamicNB = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(random, numberClassStates, connectChildrenTemporally);

        DynamicVariables variables = dynamicNB.getDynamicVariables();
        DynamicDAG dynamicDAG = Serialization.deepCopy(dynamicNB.getDynamicDAG());

        int numberOfVariables = variables.getNumberOfVars();
        int numberOfLinks = numberOfVariables-1;

        Variable treeRoot;
        do {
            treeRoot = variables.getVariableById(random.nextInt(numberOfVariables));
        } while (!treeRoot.isMultinomial() && treeRoot.getName()=="ClassVar");

        List<Variable> variablesPreviousLevels = new ArrayList<>(1);
        variablesPreviousLevels.add(variables.getVariableByName("ClassVar"));
        List<Variable> variablesCurrentLevel = new ArrayList<>(1);
        variablesCurrentLevel.add(treeRoot);
        List<Variable> variablesNextLevel = new ArrayList<>(0);
        while (numberOfLinks < DynamicBayesianNetworkGenerator.numberOfLinks) {


//            int indexVarThisLevel=0;
//            while(indexVarThisLevel < variablesCurrentLevel.size() && numberOfLinks < DynamicBayesianNetworkGenerator.numberOfLinks) {

            while (!variablesCurrentLevel.isEmpty() && numberOfLinks < DynamicBayesianNetworkGenerator.numberOfLinks) {
                List<Variable> possibleParents = variablesCurrentLevel.stream().filter(variable -> !variablesPreviousLevels.contains(variable)).collect(Collectors.toList());
                Collections.shuffle(possibleParents,random);
                Variable possibleParent;
                try {
                    possibleParent = possibleParents.stream().filter(var -> var.isMultinomial()).findAny().get();
                }
                catch (Exception e) {
                    variablesCurrentLevel=variables.getListOfDynamicVariables().stream().filter(variable -> !variablesPreviousLevels.contains(variable)).collect(Collectors.toList());
                    continue;
                }

                //variablesCurrentLevel.add(possibleParent);
//                System.out.println("Parent var: " + possibleParent.getName());
                //int maxChildren=2+random.nextInt( (int)Math.ceil(DynamicBayesianNetworkGenerator.numberOfLinks-numberOfLinks/2) );
                int maxChildren = 3 + random.nextInt(10);
                int currentChildren = 0;

                while (currentChildren < maxChildren && numberOfLinks < DynamicBayesianNetworkGenerator.numberOfLinks) {
//                    System.out.println("Links: " + numberOfLinks + ", children: " + currentChildren);
                    Variable possibleChild;

                    try {
                        List<Variable> possibleChildren = variables.getListOfDynamicVariables().stream().filter(variable -> !variablesPreviousLevels.contains(variable)).collect(Collectors.toList());
                        Collections.shuffle(possibleChildren,random);
                        possibleChild = possibleChildren.stream().filter(var -> !var.equals(possibleParent)).findAny().get();
//                        System.out.println("Possible child: " + possibleChild.getName());
                    } catch (Exception e) {
//                        System.out.println(e.getMessage());
                        currentChildren = maxChildren;
                        continue;
                    }

                    if (variablesPreviousLevels.contains(possibleChild) || variablesCurrentLevel.contains(possibleChild) || dynamicDAG.getParentSetTime0(possibleChild).contains(possibleParent) || dynamicDAG.getParentSetTimeT(possibleChild).contains(possibleParent)) {
//                        System.out.println("Children in previous levels");
                        continue;
                    }

                    if (possibleChild.isMultinomial() && !possibleParent.isMultinomial()) {
//                        System.out.println("Unsuitable children");
                        continue;
                    }

                    DynamicDAG possibleDynamicDAG = Serialization.deepCopy(dynamicDAG);
                    possibleDynamicDAG.getParentSetTime0(possibleChild).addParent(possibleParent);
                    possibleDynamicDAG.getParentSetTimeT(possibleChild).addParent(possibleParent);

                    if (possibleDynamicDAG.toDAGTime0().containCycles() || possibleDynamicDAG.toDAGTimeT().containCycles()) {
//                        System.out.println(possibleDynamicDAG.toString());
//                        System.out.println("DAG with cycles");
//                        System.exit(-1);
                        continue;
                    }

                    dynamicDAG.getParentSetTime0(possibleChild).addParent(possibleParent);
                    dynamicDAG.getParentSetTimeT(possibleChild).addParent(possibleParent);

                    currentChildren++;
                    numberOfLinks++;
                    variablesNextLevel.add(possibleChild);
                    //variablesPreviousLevels.add(possibleChild);
                    //variablesCurrentLevel.add(possibleChild);

                }
                variablesCurrentLevel.remove(possibleParent);
                variablesPreviousLevels.add(possibleParent);
            }

            variablesCurrentLevel.stream().filter(var -> !variablesPreviousLevels.contains(var)).forEach(variablesPreviousLevels::add);
            variablesCurrentLevel = variablesNextLevel;
            variablesNextLevel = new ArrayList<>(0);
        }

        DynamicBayesianNetwork dynamicTAN = new DynamicBayesianNetwork(dynamicDAG);
        dynamicTAN.randomInitialization(random);
        return dynamicTAN;
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
