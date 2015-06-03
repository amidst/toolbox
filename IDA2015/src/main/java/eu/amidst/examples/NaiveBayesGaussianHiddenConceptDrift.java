/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.examples;

import eu.amidst.examples.core.datastream.Attribute;
import eu.amidst.examples.core.datastream.DataInstance;
import eu.amidst.examples.core.datastream.DataOnMemory;
import eu.amidst.examples.core.datastream.DataStream;
import eu.amidst.examples.core.distribution.Multinomial;
import eu.amidst.examples.core.distribution.Normal;
import eu.amidst.examples.core.inference.InferenceEngineForBN;
import eu.amidst.examples.core.io.DataStreamLoader;
import eu.amidst.examples.core.learning.StreamingVariationalBayesVMP;
import eu.amidst.examples.core.models.BayesianNetwork;
import eu.amidst.examples.core.models.DAG;
import eu.amidst.examples.core.utils.Utils;
import eu.amidst.examples.core.variables.StaticVariables;
import eu.amidst.examples.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andresmasegosa on 21/4/15.
 */
public class NaiveBayesGaussianHiddenConceptDrift {

    public enum DriftDetector {GLOBAL, LOCAL, GLOBAL_LOCAL};

    DataStream<DataInstance> data;
    int windowsSize;
    double transitionVariance;
    int classIndex = -1;
    DriftDetector conceptDriftDetector;
    int seed = 0;
    StreamingVariationalBayesVMP svb;
    List<Variable> hiddenVars;
    double fading = 1.0;
    int numberOfGlobalVars = 1;

    boolean globalHidden = true;

    public Variable getClassVariable(){
        return this.svb.getLearntBayesianNetwork().getStaticVariables().getVariableById(this.classIndex);
    }
    public void setNumberOfGlobalVars(int numberOfGlobalVars) {
        this.numberOfGlobalVars = numberOfGlobalVars;
    }

    public void setGlobalHidden(boolean globalHidden) {
        this.globalHidden = globalHidden;
    }

    public void setFading(double fading) {
        this.fading = fading;
    }

    public void setClassIndex(int classIndex) {
        this.classIndex = classIndex;
    }

    public int getClassIndex(){return classIndex;}

    public void setData(DataStream<DataInstance> data) {
        this.data = data;
    }

    public void setWindowsSize(int windowsSize) {
        this.windowsSize = windowsSize;
    }

    public void setTransitionVariance(double transitionVariance) {
        this.transitionVariance = transitionVariance;
    }

    public void setConceptDriftDetector(DriftDetector conceptDriftDetector) {
        this.conceptDriftDetector = conceptDriftDetector;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public StreamingVariationalBayesVMP getSvb() {
        return svb;
    }

    private void buildGlobalDAG(){
        StaticVariables variables = new StaticVariables(data.getAttributes());
        String className = data.getAttributes().getList().get(classIndex).getName();
        hiddenVars = new ArrayList<Variable>();

        for (int i = 0; i < this.numberOfGlobalVars ; i++) {
            hiddenVars.add(variables.newGaussianVariable("GlobalHidden_"+i));
        }

        Variable classVariable = variables.getVariableByName(className);

        DAG dag = new DAG(variables);

        for (Attribute att : data.getAttributes()) {
            if (att.getName().equals(className))
                continue;

            Variable variable = variables.getVariableByName(att.getName());
            dag.getParentSet(variable).addParent(classVariable);
            if (this.globalHidden) {
                for (int i = 0; i < this.numberOfGlobalVars ; i++) {
                    dag.getParentSet(variable).addParent(hiddenVars.get(i));
                }
            }
        }

        System.out.println(dag.toString());

        svb = new StreamingVariationalBayesVMP();
        svb.setSeed(this.seed);
        svb.setPlateuStructure(new PlateuHiddenVariableConceptDrift(hiddenVars, true));
        GaussianHiddenTransitionMethod gaussianHiddenTransitionMethod = new GaussianHiddenTransitionMethod(hiddenVars, 0, this.transitionVariance);
        gaussianHiddenTransitionMethod.setFading(fading);
        svb.setTransitionMethod(gaussianHiddenTransitionMethod);
        svb.setWindowsSize(this.windowsSize);
        svb.setDAG(dag);
        svb.initLearning();
    }


    private void buildLocalDAG(){

        StaticVariables variables = new StaticVariables(data.getAttributes());
        String className = data.getAttributes().getList().get(classIndex).getName();
        hiddenVars = new ArrayList<Variable>();

        for (Attribute att : data.getAttributes()){
            if (att.getName().equals(className))
                continue;
            hiddenVars.add(variables.newGaussianVariable("Local_"+att.getName()));
        }

        Variable classVariable = variables.getVariableByName(className);

        DAG dag = new DAG(variables);

        for (Attribute att : data.getAttributes()) {
            if (att.getName().equals(className))
                continue;

            Variable variable = variables.getVariableByName(att.getName());
            dag.getParentSet(variable).addParent(classVariable);
            if (this.globalHidden) dag.getParentSet(variable).addParent(variables.getVariableByName("Local_"+att.getName()));
        }

        System.out.println(dag.toString());

        svb = new StreamingVariationalBayesVMP();
        svb.setSeed(this.seed);
        svb.setPlateuStructure(new PlateuHiddenVariableConceptDrift(hiddenVars, true));
        svb.setTransitionMethod(new GaussianHiddenTransitionMethod(hiddenVars, 0, this.transitionVariance));
        svb.setWindowsSize(this.windowsSize);
        svb.setDAG(dag);
        svb.initLearning();
    }

    private void buildGlobalLocalDAG(){

        StaticVariables variables = new StaticVariables(data.getAttributes());
        String className = data.getAttributes().getList().get(classIndex).getName();
        hiddenVars = new ArrayList<Variable>();

        Variable globalHidden  = variables.newGaussianVariable("GlobalHidden");
        hiddenVars.add(globalHidden);

        for (Attribute att : data.getAttributes()){
            if (att.getName().equals(className))
                continue;
            hiddenVars.add(variables.newGaussianVariable("Local_"+att.getName()));
        }

        Variable classVariable = variables.getVariableByName(className);

        DAG dag = new DAG(variables);

        for (Attribute att : data.getAttributes()) {
            if (att.getName().equals(className))
                continue;

            Variable variable = variables.getVariableByName(att.getName());
            dag.getParentSet(variable).addParent(classVariable);
            dag.getParentSet(variable).addParent(variables.getVariableByName("Local_"+att.getName()));
            if (this.globalHidden) dag.getParentSet(variable).addParent(globalHidden);

        }

        System.out.println(dag.toString());

        svb = new StreamingVariationalBayesVMP();
        svb.setSeed(this.seed);
        svb.setPlateuStructure(new PlateuHiddenVariableConceptDrift(hiddenVars, true));
        svb.setTransitionMethod(new GaussianHiddenTransitionMethod(hiddenVars, 0, this.transitionVariance));
        svb.setWindowsSize(this.windowsSize);
        svb.setDAG(dag);
        svb.initLearning();
    }

    public void initLearning() {
        if (classIndex == -1)
            classIndex = data.getAttributes().getNumberOfAttributes()-1;


        switch (this.conceptDriftDetector){
            case GLOBAL:
                this.buildGlobalDAG();
                break;
            case LOCAL:
                this.buildLocalDAG();
                break;
            case GLOBAL_LOCAL:
                this.buildGlobalLocalDAG();
                break;
        }


        System.out.print("Sample");
        for (Variable hiddenVar : this.hiddenVars) {
            System.out.print("\t" + hiddenVar.getName());
        }
        System.out.print("\t Accuracy");

        System.out.println();
    }

    public void updateModel(DataOnMemory<DataInstance> batch){
        svb.updateModel(batch);
    }

    void learnModel() {
        if (classIndex == -1)
            classIndex = data.getAttributes().getNumberOfAttributes()-1;


        switch (this.conceptDriftDetector){
            case GLOBAL:
                this.buildGlobalDAG();
                break;
            case LOCAL:
                this.buildLocalDAG();
                break;
            case GLOBAL_LOCAL:
                this.buildGlobalLocalDAG();
                break;
        }


        System.out.print("Sample");
        for (Variable hiddenVar : this.hiddenVars) {
            System.out.print("\t" + hiddenVar.getName());
        }
        System.out.println();


        int count = windowsSize;
        double avACC = 0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowsSize)) {

            double accuracy = computeAccuracy(svb.getLearntBayesianNetwork(), batch);
            svb.updateModel(batch);

            System.out.print(count);

            for (Variable hiddenVar : this.hiddenVars) {
                Normal normal = svb.getPlateuStructure().getEFVariablePosterior(hiddenVar, 0).toUnivariateDistribution();
                System.out.print("\t" + normal.getMean());
            }
            System.out.print("\t" + accuracy);
            System.out.println();

            count += windowsSize;
            avACC+= accuracy;
        }

        System.out.println("Average Accuracy: " + avACC / (count / windowsSize));

    }

    public List<Variable> getHiddenVars() {
        return hiddenVars;
    }

    public BayesianNetwork getLearntBayesianNetwork(){
        return svb.getLearntBayesianNetwork();
    }

    public double computeAccuracy(BayesianNetwork bn, DataOnMemory<DataInstance> data){

        Variable classVariable = bn.getStaticVariables().getVariableById(classIndex);
        double predictions = 0;
        InferenceEngineForBN.setModel(bn);
        for (DataInstance instance : data) {
            double realValue = instance.getValue(classVariable);
            instance.setValue(classVariable, Utils.missingValue());
            InferenceEngineForBN.setEvidence(instance);
            InferenceEngineForBN.runInference();
            Multinomial posterior = InferenceEngineForBN.getPosterior(classVariable);
            if (Utils.maxIndex(posterior.getProbabilities())==realValue)
                predictions++;

            instance.setValue(classVariable, realValue);
        }

        return predictions/data.getNumberOfDataInstances();

    }

    public double[] computePredictions(BayesianNetwork bn, DataOnMemory<DataInstance> data){

        double[] output = new double[4];
        int TP = 0, TN = 0, FP  = 0, FN = 0;
        Variable classVariable = bn.getStaticVariables().getVariableById(classIndex);
        double predictions = 0;
        InferenceEngineForBN.setModel(bn);
        for (DataInstance instance : data) {
            double realValue = instance.getValue(classVariable);
            instance.setValue(classVariable, Utils.missingValue());
            InferenceEngineForBN.setEvidence(instance);
            InferenceEngineForBN.runInference();
            Multinomial posterior = InferenceEngineForBN.getPosterior(classVariable);
            if (Utils.maxIndex(posterior.getProbabilities())==realValue) {
                predictions++;
                if(realValue==1){
                    TP++;
                }else{
                    TN++;
                }
            }else{
                if(realValue==1){
                    FN++;
                }else{
                    FP++;
                }
            }

            instance.setValue(classVariable, realValue);
        }
        output[0] = predictions/data.getNumberOfDataInstances();
        output[1] = TP;
        output[2] = TN;
        output[3] = FP;
        output[4] = FN;

        return output;

    }



    public static void main(String[] args) {

        DataStream<DataInstance> data = DataStreamLoader.loadFromFile("./IDA2015/DriftSets/hyperplane9.arff");
        NaiveBayesGaussianHiddenConceptDrift nb = new NaiveBayesGaussianHiddenConceptDrift();
        nb.setClassIndex(-1);
        nb.setData(data);
        nb.setWindowsSize(100);
        nb.setTransitionVariance(0.1);
        nb.setConceptDriftDetector(DriftDetector.GLOBAL_LOCAL);

        nb.learnModel();

    }
}
