/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.ida2015;

import eu.amidst.core.ModelFactory;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andresmasegosa on 21/4/15.
 */
public class NaiveBayesMultinomialHiddenConceptDrift {

    public enum DriftDetector {GLOBAL, LOCAL, GLOBAL_LOCAL};

    DataStream<DataInstance> data;
    int windowsSize;
    double transitionProbability;
    int classIndex = -1;
    DriftDetector conceptDriftDetector;
    int seed = 0;
    SVB svb;
    List<Variable> hiddenVars;
    int numberOfStatesHiddenVar =  5;

    public void setNumberOfStatesHiddenVar(int numberOfStatesHiddenVar) {
        this.numberOfStatesHiddenVar = numberOfStatesHiddenVar;
    }

    public void setClassIndex(int classIndex) {
        this.classIndex = classIndex;
    }

    public void setData(DataStream<DataInstance> data) {
        this.data = data;
    }

    public void setWindowsSize(int windowsSize) {
        this.windowsSize = windowsSize;
    }

    public void setTransitionProbability(double transitionProbability) {
        this.transitionProbability = transitionProbability;
    }

    public void setConceptDriftDetector(DriftDetector conceptDriftDetector) {
        this.conceptDriftDetector = conceptDriftDetector;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }


    private void buildGlobalDAG(){
        Variables variables = ModelFactory.newVariables(data.getAttributes());
        String className = data.getAttributes().getFullListOfAttributes().get(classIndex).getName();
        hiddenVars = new ArrayList<Variable>();

        Variable globalHidden  = variables.newMultionomialVariable("GlobalHidden", this.numberOfStatesHiddenVar);
        hiddenVars.add(globalHidden);

        Variable classVariable = variables.getVariableByName(className);

        DAG dag = ModelFactory.newDAG(variables);

        for (Attribute att : data.getAttributes()) {
            if (att.getName().equals(className))
                continue;

            Variable variable = variables.getVariableByName(att.getName());
            //dag.getParentSet(variable).addParent(classVariable);
            dag.getParentSet(variable).addParent(globalHidden);
        }

        System.out.println(dag.toString());

        svb = new SVB();
        svb.setSeed(this.seed);
        svb.setPlateuStructure(new PlateuHiddenVariableConceptDrift(hiddenVars, true));
        svb.setTransitionMethod(new MultinomialHiddenTransitionMethod(hiddenVars, this.transitionProbability));
        svb.setWindowsSize(this.windowsSize);
        svb.setDAG(dag);
        svb.initLearning();
    }

    public void learnDAG() {
        if (classIndex == -1)
            classIndex = data.getAttributes().getNumberOfAttributes()-1;


        switch (this.conceptDriftDetector){
            case GLOBAL:
                this.buildGlobalDAG();
                break;
        }


        System.out.print("Sample");
        for (Variable hiddenVar : this.hiddenVars) {
            System.out.print("\t" + hiddenVar.getName());
        }
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
        }


        System.out.print("Sample");
        for (Variable hiddenVar : this.hiddenVars) {
            System.out.print("\t" + hiddenVar.getName());
        }
        System.out.println();


        int count = windowsSize;
        double avACC = 0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowsSize)) {

            //System.out.println(svb.getLearntBayesianNetwork());
            double accuracy = computeAccuracy(svb.getLearntBayesianNetwork(), batch);
            svb.updateModel(batch);

            System.out.print(count);

            for (Variable hiddenVar : this.hiddenVars) {
                Multinomial multinomial = svb.getPlateuStructure().getEFVariablePosterior(hiddenVar, 0).toUnivariateDistribution();
                for (int i = 0; i < hiddenVar.getNumberOfStates(); i++) {
                    System.out.print("\t" + multinomial.getProbabilityOfState(i));
                }
            }
            System.out.print("\t" + accuracy);
            System.out.println();

            count += windowsSize;
            avACC+= accuracy;
        }

        System.out.println("Average Accuracy: " + avACC/(count/windowsSize));

    }

    public BayesianNetwork getLearntBayesianNetwork(){
        return svb.getLearntBayesianNetwork();
    }

    private double computeAccuracy(BayesianNetwork bn, DataOnMemory<DataInstance> data){

        Variable classVariable = bn.getVariables().getVariableById(classIndex);
        double predictions = 0;
        VMP vmp = new VMP();
        vmp.setModel(bn);
        for (DataInstance instance : data) {
            double realValue = instance.getValue(classVariable);
            instance.setValue(classVariable, Utils.missingValue());
            vmp.setEvidence(instance);
            vmp.runInference();
            Multinomial posterior = vmp.getPosterior(classVariable);
            if (Utils.maxIndex(posterior.getProbabilities())==realValue)
                predictions++;

            instance.setValue(classVariable, realValue);
        }

        return predictions/data.getNumberOfDataInstances();
    }



    public static void main(String[] args) {

        DataStream<DataInstance> data = DataStreamLoader.openFromFile("./IDA2015/DriftSets/sea.arff");
        NaiveBayesMultinomialHiddenConceptDrift nb = new NaiveBayesMultinomialHiddenConceptDrift();
        nb.setClassIndex(-1);
        nb.setData(data);
        nb.setWindowsSize(10);
        nb.setTransitionProbability(0.5);
        nb.setNumberOfStatesHiddenVar(2);
        nb.setConceptDriftDetector(DriftDetector.GLOBAL);

        nb.learnModel();

    }
}
