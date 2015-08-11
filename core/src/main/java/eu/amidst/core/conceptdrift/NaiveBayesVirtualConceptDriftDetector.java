/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */
package eu.amidst.core.conceptdrift;



import eu.amidst.core.conceptdrift.utils.GaussianHiddenTransitionMethod;
import eu.amidst.core.conceptdrift.utils.PlateuHiddenVariableConceptDrift;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/#nbconceptdriftexample"> http://amidst.github.io/toolbox/#nbconceptdriftexample </a>  </p>
 *
 */
public class NaiveBayesVirtualConceptDriftDetector implements FadingLearner{

    public enum DriftDetector {GLOBAL};

    DataStream<DataInstance> data;
    int windowsSize;
    double transitionVariance;
    int classIndex = -1;
    DriftDetector conceptDriftDetector = DriftDetector.GLOBAL;
    int seed = 0;
    SVB svb;
    List<Variable> hiddenVars;
    double fading = 1.0;
    int numberOfGlobalVars = 1;

    boolean globalHidden = true;

    public Variable getClassVariable(){
        return this.svb.getLearntBayesianNetwork().getVariables().getVariableById(this.classIndex);
    }
    public void setNumberOfGlobalVars(int numberOfGlobalVars) {
        this.numberOfGlobalVars = numberOfGlobalVars;
    }

    @Override
    public void setFadingFactor(double fading) {
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

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public SVB getSvb() {
        return svb;
    }

    private void buildGlobalDAG(){
        Variables variables = new Variables(data.getAttributes());
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

        //System.out.println(dag.toString());

        svb = new SVB();
        svb.setSeed(this.seed);
        svb.setPlateuStructure(new PlateuHiddenVariableConceptDrift(hiddenVars, true));
        GaussianHiddenTransitionMethod gaussianHiddenTransitionMethod = new GaussianHiddenTransitionMethod(hiddenVars, 0, this.transitionVariance);
        gaussianHiddenTransitionMethod.setFading(fading);
        svb.setTransitionMethod(gaussianHiddenTransitionMethod);
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
        }
    }

    public double[] updateModel(DataOnMemory<DataInstance> batch){
        svb.updateModel(batch);
        double[] out = new double[hiddenVars.size()];
        for (int i = 0; i < out.length; i++) {
            Variable hiddenVar = this.hiddenVars.get(i);
            Normal normal = svb.getPlateuStructure().getEFVariablePosterior(hiddenVar, 0).toUnivariateDistribution();
            out[i] = normal.getMean();
        }
        return out;
    }

    public List<Variable> getHiddenVars() {
        return hiddenVars;
    }

    public BayesianNetwork getLearntBayesianNetwork(){
        return svb.getLearntBayesianNetwork();
    }

}
