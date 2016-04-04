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

package eu.amidst.ecml2016;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicBayesianNetworkLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkSampler;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.dynamic.variables.HashMapDynamicAssignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Created by dario on 24/02/16.
 */
public class DaimlerSimulatedDynamicModel {

    private DynamicBayesianNetwork model;

    private List<Variable> observableVars;

    private int seed=23664;

    private Random random;

    private String classVarName = "LaneChange";

    private List<DynamicAssignment> observableEvidence;

    private List<DynamicAssignment> fullEvidence;

    private double probabilityKeepClassState;

    /**
     * Sets the seed for model generation repeatability.
     * @param seed an {@code int} that represents the seed.
     */
    public void setSeed(int seed) {
        this.seed = seed;
        random = new Random(seed);
    }

    public void generateModel() {

        random = new Random(seed);

        try {
            model = DynamicBayesianNetworkLoader.loadFromFile("./networks/DaimlerSimulatedNetwork.dbn");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }

        observableVars = new ArrayList<>();
        observableVars.add(model.getDynamicVariables().getVariableByName("sensorVelocity1"));
        observableVars.add(model.getDynamicVariables().getVariableByName("sensorVelocity2"));

    }

    public List<DynamicAssignment> generateEvidence(int sequenceLength) {

        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(model);
        sampler.setSeed(random.nextInt());

        //sampler.setHiddenVar(model.getDynamicVariables().getVariableByName("Discrete" + hiddenVarName));

        DataStream<DynamicDataInstance> fullSample = sampler.sampleToDataBase(1,sequenceLength);

//        model.getDynamicVariables().getListOfDynamicVariables().forEach(variable -> System.out.println(variable.getName() + fullSample.stream().findFirst().get().getValue(variable)));

        //fullSample.stream().forEach(dynamicDataInstance1 -> System.out.println(dynamicDataInstance1.outputString(model.getDynamicVariables().getListOfDynamicVariables())));

        List<DynamicAssignment> sample = new ArrayList<>();
        List<DynamicAssignment> fullEvidence = new ArrayList<>();

        fullSample.stream().forEachOrdered(dynamicDataInstance -> {
            DynamicAssignment dynamicAssignment = new HashMapDynamicAssignment(observableVars.size());
            ((HashMapDynamicAssignment)dynamicAssignment).setTimeID((int)dynamicDataInstance.getTimeID());
            observableVars.stream().forEach(var1 -> {
                dynamicAssignment.setValue(var1,dynamicDataInstance.getValue(var1));
//                System.out.println(dynamicDataInstance.getValue(var1));
            });
            sample.add(dynamicAssignment);

            DynamicAssignment fullDynamicAssignment = new HashMapDynamicAssignment(observableVars.size());
            ((HashMapDynamicAssignment)fullDynamicAssignment).setTimeID((int)dynamicDataInstance.getTimeID());
            model.getDynamicVariables().getListOfDynamicVariables().stream().forEach(var1 -> {
                fullDynamicAssignment.setValue(var1,dynamicDataInstance.getValue(var1));
//                System.out.println(dynamicDataInstance.getValue(var1));
            });
            fullEvidence.add(fullDynamicAssignment);
        });

        this.observableEvidence =sample;
        this.fullEvidence=fullEvidence;

        return sample;
    }


    public DynamicBayesianNetwork getModel() {
        return model;
    }

    public List<DynamicAssignment> getEvidence() {
        return observableEvidence;
    }

    public List<DynamicAssignment> getFullEvidence() {
        return fullEvidence;
    }

    public List<DynamicAssignment> getEvidenceNoClass() {

        List<DynamicAssignment> evidenceNoClass = new ArrayList<>();

        this.observableEvidence.forEach(dynamicAssignment -> {
            DynamicAssignment dynamicAssignmentNoClass = new HashMapDynamicAssignment(dynamicAssignment.getVariables().size()-1);
            ((HashMapDynamicAssignment)dynamicAssignmentNoClass).setTimeID((int)dynamicAssignment.getTimeID());
            dynamicAssignment.getVariables().stream()
                .filter(variable -> !variable.equals(this.getClassVariable()))
                .forEach(variable -> dynamicAssignmentNoClass.setValue(variable,dynamicAssignment.getValue(variable)));
            evidenceNoClass.add(dynamicAssignmentNoClass);
        });

        return evidenceNoClass;
    }

    public int[] getClassSequence() {

        int[] classSequence = new int[fullEvidence.size()];

        this.fullEvidence.forEach(dynamicAssignment -> {

            classSequence[(int) dynamicAssignment.getTimeID()] = (int) dynamicAssignment.getValue(this.getClassVariable());

        });
        return classSequence;
    }

    public Variable getClassVariable() {
        return model.getDynamicVariables().getVariableByName(classVarName);
    }

    public void setProbabilityOfKeepingClass(double probKeeping) {
        if (model==null) {
            this.probabilityKeepClassState=probKeeping;
            return;
        }
        Variable classVar = model.getDynamicVariables().getVariableByName(classVarName);
        Multinomial_MultinomialParents classVarCondDistribution = model.getConditionalDistributionTimeT(classVar);

        double probOtherStates = (1-probKeeping)/(classVar.getNumberOfStates()-1);

        IntStream.range(0,classVar.getNumberOfStates()).forEach(k -> {
            double[] probabilities = new double[classVar.getNumberOfStates()];
            for (int i = 0; i <classVar.getNumberOfStates(); i++) {
                if(i==k) {
                    probabilities[i] = probKeeping;
                }
                else {
                    probabilities[i] = probOtherStates;
                }

            }
            classVarCondDistribution.getMultinomial(k).setProbabilities(probabilities);
        });
        this.probabilityKeepClassState=probKeeping;
    }

    public void printDAG() {
        System.out.println(this.model.getDynamicDAG().toString());
    }

    public void printHiddenLayerModel() {
        System.out.println(this.model.toString());
    }

    public static void main(String[] args) {
        DaimlerSimulatedDynamicModel hiddenModel = new DaimlerSimulatedDynamicModel();

        hiddenModel.generateModel();

        System.out.println(hiddenModel.model.getDynamicDAG().toString());


//        System.out.println(hiddenModel.model.toString());

//        System.out.println("\nDYNAMIC VARIABLES");
//        hiddenModel.model.getDynamicVariables().forEach(var -> System.out.println(var.getName()));
//
//        System.out.println("\nOBSERVABLE VARIABLES");
//        hiddenModel.observableVars.forEach(var -> System.out.println(var.getName()));


        //hiddenModel.setProbabilityOfKeepingClass(0.98);
        System.out.println(hiddenModel.model.toString());


        System.out.println("\nEVIDENCE");
        List<DynamicAssignment> evidence = hiddenModel.generateEvidence(100);

        evidence.forEach(dynamicAssignment -> System.out.println(dynamicAssignment.outputString(hiddenModel.observableVars)));



    }
}

