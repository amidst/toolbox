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

package eu.amidst.dynamic.examples.inference;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.utils.AmidstOptionsHandler;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.inference.FactoredFrontierForDBN;
import eu.amidst.dynamic.inference.InferenceEngineForDBN;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkSampler;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This example shows the scalability with respect to the number of cores of the Factored Frontier algorithm
 * with the Importance Sampling inference engine, as described in Deliverable 3.4 (Section 6).
 * (Note that the different number of cores has to be set from the operating system and run this experiment
 * at least once with each configuration)
 * Created by ana@cs.aau.dk on 25/11/15.
 */
public class DynamicIS_Scalability implements AmidstOptionsHandler {

    int numberOfDiscreteVars = 30;
    int numberOfContinuousVars = 0;
    int numberOfDiscreteHiddenVars = 10;
    int numberOfStates = 2;
    int sequenceLength = 1000;
    int numOfSequences = 2;
    boolean connectChildrenTemporally = false;
    boolean activateMiddleLayer = true;
    int seed = 1;
    int numberOfSamples = 1000;


    public int getNumOfSequences() {
        return numOfSequences;
    }

    public void setNumOfSequences(int numOfSequences) {
        this.numOfSequences = numOfSequences;
    }

    public int getNumberOfContinuousVars() {
        return numberOfContinuousVars;
    }

    public void setNumberOfContinuousVars(int numberOfContinuousVars) {
        this.numberOfContinuousVars = numberOfContinuousVars;
    }

    public int getNumberOfDiscreteHiddenVars() {
        return numberOfDiscreteHiddenVars;
    }

    public void setNumberOfDiscreteHiddenVars(int numberOfDiscreteHiddenVars) {
        this.numberOfDiscreteHiddenVars = numberOfDiscreteHiddenVars;
    }

    public int getNumberOfStates() {
        return numberOfStates;
    }

    public void setNumberOfStates(int numberOfStates) {
        this.numberOfStates = numberOfStates;
    }

    public int getSequenceLength() {
        return sequenceLength;
    }

    public void setSequenceLength(int sequenceLength) {
        this.sequenceLength = sequenceLength;
    }

    public int getNumberOfDiscreteVars() {
        return numberOfDiscreteVars;
    }

    public void setNumberOfDiscreteVars(int numberOfDiscreteVars) {
        this.numberOfDiscreteVars = numberOfDiscreteVars;
    }

    public boolean isConnectChildrenTemporally() {
        return connectChildrenTemporally;
    }

    public void setConnectChildrenTemporally(boolean connectChildrenTemporally) {
        this.connectChildrenTemporally = connectChildrenTemporally;
    }

    public boolean isActivateMiddleLayer() {
        return activateMiddleLayer;
    }

    public void setActivateMiddleLayer(boolean activateMiddleLayer) {
        this.activateMiddleLayer = activateMiddleLayer;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public int getNumberOfSamples() {
        return numberOfSamples;
    }

    public void setNumberOfSamples(int numberOfSamples) {
        this.numberOfSamples = numberOfSamples;
    }

    public void runExperiment(){
        Random random = new Random(this.getSeed());

        //We first generate a dynamic Bayesian network with two latent nodes
        DynamicVariables dynamicVariables  = new DynamicVariables();

        //Upper layer
        Variable varH1 = dynamicVariables.newMultinomialDynamicVariable("H1",this.getNumberOfStates());

        //Middle layer
        if(this.isActivateMiddleLayer()) {
            IntStream.range(2, this.getNumberOfDiscreteHiddenVars()+2)
                    .forEach(i -> dynamicVariables.newMultinomialDynamicVariable("H" + i, this.getNumberOfStates()));
        }

        //Discrete leaf variables (lower layer)
        IntStream.range(1, this.getNumberOfDiscreteVars()+1)
                .forEach(i -> dynamicVariables.newMultinomialDynamicVariable("A" + i, this.getNumberOfStates()));

        //Continuous leaf variables (lower layer)
        IntStream.range(1,this.getNumberOfContinuousVars()+1)
                .forEach(i -> dynamicVariables.newGaussianDynamicVariable("C" + i));

        DynamicDAG dag = new DynamicDAG(dynamicVariables);


        //Connect H1 in consecutive time steps.
        dag.getParentSetTimeT(varH1).addParent(varH1.getInterfaceVariable());

        //Connect varH1 as parent of hidden nodes in the second layer
        //and the latter in consecutive time steps.
        if(this.isActivateMiddleLayer()) {
            dag.getParentSetsTimeT().stream()
                    .filter(v -> !v.getMainVar().getName().contains("A") &&
                                 !v.getMainVar().getName().contains("C") &&
                                 v.getMainVar().getVarID()!=varH1.getVarID())
                    .forEach(w -> {
                        w.addParent(varH1);
                        w.addParent(w.getMainVar().getInterfaceVariable());
                    });
        }

        //Connect hidden vars in middle layer (varH2 and varH3) with all leaves,
        //connect leaf variables in time if set.
        if(this.isActivateMiddleLayer()) {
            dag.getParentSetsTimeT().stream()
                    .filter(var -> !var.getMainVar().getName().contains("H"))
                    .forEach(w -> {
                                dag.getDynamicVariables().getListOfDynamicVariables().stream()
                                        .filter(x -> x.getName().contains("H") &&
                                                     x.getVarID()!=varH1.getVarID())
                                        .forEach(v -> w.addParent(v));
                                if(this.isConnectChildrenTemporally())
                                    w.addParent(dynamicVariables.getInterfaceVariable(w.getMainVar()));
                    });
        //Connect hidden var in top layer (varH1) with all leaves
        //Connect leaf variables in time if set
        }else{
            dag.getParentSetsTimeT().stream()
                    .filter(var -> var.getMainVar().getVarID()!=varH1.getVarID())
                    .forEach(w -> {
                                w.addParent(varH1);
                                if(this.isConnectChildrenTemporally())
                                    w.addParent(dynamicVariables.getInterfaceVariable(w.getMainVar()));
                            }
                    );
        }

        System.out.println(dag);

        //Creat the dynamic Bayesian network
        DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(dag);

        //We initialize the parameters of the network randomly
        dbn.randomInitialization(random);

        //We create a dynamic dataset with 3 sequences for prediction
        DynamicBayesianNetworkSampler dynamicSampler = new DynamicBayesianNetworkSampler(dbn);
        dynamicSampler.setHiddenVar(varH1);
        if(this.isActivateMiddleLayer()) {
            dag.getDynamicVariables().getListOfDynamicVariables().stream()
                    .filter(x -> x.getName().contains("H") &&
                            x.getVarID()!=varH1.getVarID())
                    .forEach(v -> dynamicSampler.setHiddenVar(v));
        }
        DataStream<DynamicDataInstance> dataPredict = dynamicSampler.sampleToDataBase(this.getNumOfSequences(),
                this.getSequenceLength());

        List<DynamicDataInstance> dataPredictList = dataPredict.stream().collect(Collectors.toList());


        //********************************************************************************************
        //                   DYNAMIC IS WITH FACTORED FRONTIER ALGORITHM
        //********************************************************************************************

        //We select DynamicVMP as the Inference Algorithm
        ImportanceSampling importanceSampling = new ImportanceSampling();
        importanceSampling.setParallelMode(true);
        importanceSampling.setKeepDataOnMemory(true);
        importanceSampling.setSampleSize(this.getNumberOfSamples());
        FactoredFrontierForDBN factoredFrontierForDBN = new FactoredFrontierForDBN(importanceSampling);
        InferenceEngineForDBN.setInferenceAlgorithmForDBN(factoredFrontierForDBN);
        //Then, we set the DBN model
        InferenceEngineForDBN.setModel(dbn);

        UnivariateDistribution posterior = null;
        int time = 0 ;

        double average = 0;
        for (int j = 0; j < 2; j++) {
            long start = System.nanoTime();
            for (DynamicDataInstance instance : dataPredictList) {
                //The InferenceEngineForDBN must be reset at the begining of each Sequence.
                if (instance.getTimeID() == 0 && posterior != null) {
                    InferenceEngineForDBN.reset();
                    time = 0;
                }
                factoredFrontierForDBN.setSeed(j);

                //We also set the evidence.
                InferenceEngineForDBN.addDynamicEvidence(instance);

                //Then we run inference
                InferenceEngineForDBN.runInference();

                //Then we query the posterior of the target variable
                posterior = InferenceEngineForDBN.getFilteredPosterior(varH1);

                //We show the output
                //System.out.println("P(varH1|e[0:" + (time++) + "]) = " + posterior);
            }
            long duration = (System.nanoTime() - start) / 1;
            double seconds = duration / 1000000000.0;
            if (j > 1) {
                average += seconds;
            }
        }

        System.out.println("Time for Dynamic IS = "+average/10+" secs");
    }

    public static void main(String[] args) throws IOException {
        DynamicIS_Scalability exp = new DynamicIS_Scalability();
        exp.setOptions(args);
        exp.runExperiment();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String classNameID() {
        return "DynamicIS_Scalability";
    }



    @Override
    public String listOptions() {
        return  this.classNameID() +",\\"+
                "-d, 30, Number of discrete leaf vars\\" +
                "-c, 0, Number of continuous leaf vars\\" +
                "-h, 10, Number of discrete hidden leaf vars\\" +
                "-s, 2, Number of states for all the discrete vars\\" +
                "-l, 1000, Length for each sequence\\" +
                "-q, 2, Number of sequences\\" +
                "-linkNodes, false, Connects leaf nodes in consecutive time steps.\\"+
                "-activateMiddleLayer, true, Create middle layer with two (temporaly connected) " +
                                            "discrete hidden nodes.\\"+
                "-seed, 1, seed to generate random numbers\\"+
                "-samples, 1000, Number of samples for IS";
    }

    @Override
    public String listOptionsRecursively() {
        return this.listOptions();
    }

    @Override
    public void loadOptions() {
        this.setNumberOfDiscreteVars(this.getIntOption("-d"));
        this.setNumberOfContinuousVars(this.getIntOption("-c"));
        this.setNumberOfDiscreteHiddenVars(this.getIntOption("-h"));
        this.setNumberOfStates(this.getIntOption("-s"));
        this.setSequenceLength(this.getIntOption("-l"));
        this.setNumOfSequences(this.getIntOption("-q"));
        this.setConnectChildrenTemporally(getBooleanOption("-linkNodes"));
        this.setActivateMiddleLayer(getBooleanOption("-activateMiddleLayer"));
        this.setSeed(this.getIntOption("-seed"));
        this.setNumberOfSamples(this.getIntOption("-samples"));
    }
}
