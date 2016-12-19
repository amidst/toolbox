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

package eu.amidst.icdm2016.smoothing;/*
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


import eu.amidst.core.datastream.*;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.icdm2016.GaussianHiddenTransitionMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains the functionality for using the concept drift apporoach based on probabilitic graphical models
 * detailed in the following paper,
 *
 * <i> Borchani et al. Modeling concept drift: A probabilistic graphical model based approach. IDA 2015. </i>
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#nbconceptdriftexample"> http://amidst.github.io/toolbox/CodeExamples.html#nbconceptdriftexample </a>  </p>
 *
 */
public class Smooth_NaiveBayesVirtualConceptDriftDetector {

    /** Represents the drift detection mode. Only the global mode is currently provided.*/
    public enum DriftDetector {GLOBAL};

    /** Represents the data stream used for detecting the concepts drifts*/
    DataStream<DataInstance> data;

    Attributes attributes;

    /** Represents the size of the window used by the {@link SVB} class*/
    int windowsSize;

    /** Represents the variance added when making a transition*/
    double transitionVariance;

    /** Represents the index of the class variable of the classifier*/
    int classIndex = -1;

    /** Represents the drift detection mode. Only the global mode is currently provided.*/
    DriftDetector conceptDriftDetector = DriftDetector.GLOBAL;

    /** Represents the seed of the class*/
    int seed = 0;

    /** Represents the underlying learning engine*/
    SVB svb;

    /** Represents the list of hidden vars modelling concept drift*/
    List<Variable> hiddenVars;

    /** Represents the fading factor.*/
    double fading = 1.0;

    /** Represents the number of global hidden variables*/
    int numberOfGlobalVars = 1;

    /** Represents whether there is or not a global hidden variable modelling concept drift*/
    boolean globalHidden = true;

    boolean includeUR = false;

    boolean includeIndicators = false;

    boolean output = false;

    double elbo=0;
    /**
     * Returns the class variable of the classifier
     * @return A <code>Variable</code> object
     */
    public Variable getClassVariable(){
        return this.svb.getLearntBayesianNetwork().getVariables().getVariableById(this.classIndex);
    }

    /**
     * Sets the number of global hidden variables modelling concept drift
     * @param numberOfGlobalVars A positive integer value.
     */
    public void setNumberOfGlobalVars(int numberOfGlobalVars) {
        this.numberOfGlobalVars = numberOfGlobalVars;
    }


    /**
     * Sets which is class variable of the model,
     * @param classIndex, a positive integer defining the index of the class variable.
     */
    public void setClassIndex(int classIndex) {
        this.classIndex = classIndex;
    }

    /**
     * Gets the index of the class variable of the model
     * @return A positive integer value.
     */
    public int getClassIndex(){return classIndex;}

    /**
     * Sets the data stream where the concept drift will be detected
     * @param data, a <code>DataStream</code> object
     */
    public void setData(DataStream<DataInstance> data) {
        //this.data = data;
        this.attributes = data.getAttributes();
    }

    public void setAttributes(Attributes attributes){
        this.attributes = attributes;
    }

    /**
     * Sets the window size of the concept drift detection model
     * @param windowsSize, a positive integer value
     */
    public void setWindowsSize(int windowsSize) {
        this.windowsSize = windowsSize;
    }

    public void setOutput(boolean output) {
        this.output = output;
    }

    /**
     * Sets the transition variance of the concept drift detection model
     * @param transitionVariance, a positive double value
     */
    public void setTransitionVariance(double transitionVariance) {
        this.transitionVariance = transitionVariance;
        if(this.svb!=null)
            ((Smooth_GaussianHiddenTransitionMethod)this.svb.getTransitionMethod()).setTransitionVariance(transitionVariance);
    }

    /**
     * Set the seed of the class
     * @param seed, an integer value
     */
    public void setSeed(int seed) {
        this.seed = seed;
    }

    /**
     * Retuns the SVB learningn engine
     * @return A <code>SVB</code> object.
     */
    public SVB getSvb() {
        return svb;
    }


    public void deactivateTransitionMethod(){
        svb.setTransitionMethod(null);
    }

    public void activateTransitionMethod(){
        GaussianHiddenTransitionMethod gaussianHiddenTransitionMethod = new GaussianHiddenTransitionMethod(hiddenVars,
                0, this.transitionVariance);
        gaussianHiddenTransitionMethod.setFading(fading);
        svb.setTransitionMethod(gaussianHiddenTransitionMethod);
    }

    public boolean isIncludeIndicators() {
        return includeIndicators;
    }

    public void setIncludeIndicators(boolean includeIndicators) {
        this.includeIndicators = includeIndicators;
    }

    public boolean isIncludeUR() {
        return includeUR;
    }

    public void setIncludeUR(boolean includeUR) {
        this.includeUR = includeUR;
    }


    private int getNumberOfGlobalVars(){



        return 0;
    }
    /**
     * Builds the DAG structure of a Naive Bayes classifier with a global hidden Gaussian variable.
     */
    private void buildGlobalDAG(){
        Variables variables = new Variables(this.attributes);

        String className = this.attributes.getFullListOfAttributes().get(classIndex).getName().split("__")[0];

        Variable unemploymentRateVar = null;
        String unemploymentRateAttName = "UNEMPLOYMENT_RATE_ALMERIA";

        /**
         * Create indicator variables if includeIndicators is set to true
         */
        if(this.isIncludeIndicators()){
            for (Attribute att : this.attributes.getListOfNonSpecialAttributes()) {
                if(!att.getName().startsWith(className) && !att.getName().equals(unemploymentRateAttName)) {
                    variables.newIndicatorVariable(variables.getVariableByName(att.getName()), 0.0);
                }
            }
        }

        hiddenVars = new ArrayList<>();
        List<Variable> hiddenVarsWithUR = new ArrayList<>();
/*
        for (int i = 0; i < this.numberOfGlobalVars ; i++) {
            Variable globalHidden = variables.newGaussianVariable("GlobalHidden_"+i);
            this.hiddenVars.add(globalHidden);
            hiddenVarsWithUR.add(globalHidden);
        }
*/


        int previousIndex = -1;
        for (Attribute attribute : this.attributes) {
            if(!attribute.getName().startsWith(className) && !attribute.getName().equals(unemploymentRateAttName)) {
                int index = Integer.parseInt(attribute.getName().split("__")[1]);
                if (previousIndex!=-1 && index==previousIndex)
                    continue;

                Variable globalHidden = variables.newGaussianVariable("GlobalHidden_" + index);
                this.hiddenVars.add(globalHidden);
                hiddenVarsWithUR.add(globalHidden);
                previousIndex=index;
            }
        }

        /**
         * Include indicator variable if includeIndicators is set to true
         */

        if(this.isIncludeUR()) {
            try {
                unemploymentRateVar = variables.getVariableByName(unemploymentRateAttName);
                hiddenVarsWithUR.add(unemploymentRateVar);
            } catch (UnsupportedOperationException e) {
            }
        }

        DAG dag = new DAG(variables);

        for (Attribute att : this.attributes.getListOfNonSpecialAttributes()) {
            if (att.getName().startsWith(className) || att.getName().equals(unemploymentRateAttName))
                continue;

            int index = Integer.parseInt(att.getName().split("__")[1]);

            Variable variable = variables.getVariableByName(att.getName());
            dag.getParentSet(variable).addParent(variables.getVariableByName(className+"__"+index));
            if (this.globalHidden) {
                dag.getParentSet(variable).addParent(variables.getVariableByName("GlobalHidden_" + index));
            }
            if(unemploymentRateVar!=null)
                dag.getParentSet(variable).addParent(unemploymentRateVar);
        }

        for (int i = 1; i < hiddenVars.size(); i++) {
            dag.getParentSet(hiddenVars.get(i)).addParent(hiddenVars.get(i-1));
        }

        /**
         * Include indicator variables as parents of its corresponding variables if includeIndicators is set to true
         */
        if(this.isIncludeIndicators()){
            for (Attribute att : this.attributes.getListOfNonSpecialAttributes()) {
                if(!att.getName().startsWith(className) && !att.getName().equals(unemploymentRateAttName)) {
                    Variable predictiveVar = variables.getVariableByName(att.getName());
                    dag.getParentSet(predictiveVar).addParent(variables.getVariableByName(att.getName() + "_INDICATOR"));
                }
            }
        }

        System.out.println(dag.toString());

        svb = new SVB();
        svb.setSeed(this.seed);
        svb.setPlateuStructure(new Smooth_PlateuStructureGlobalAsInIDA2015(hiddenVarsWithUR));
        Smooth_GaussianHiddenTransitionMethod gaussianHiddenTransitionMethod = new Smooth_GaussianHiddenTransitionMethod(this.hiddenVars, 0, this.transitionVariance);
        gaussianHiddenTransitionMethod.setFading(fading);
        svb.setTransitionMethod(gaussianHiddenTransitionMethod);
        svb.setWindowsSize(this.windowsSize);
        svb.setDAG(dag);

        svb.setOutput(this.output);
        svb.getPlateuStructure().getVMP().setMaxIter(100);
        svb.getPlateuStructure().getVMP().setThreshold(0.001);

        // svb.setParallelMode(true);



        svb.initLearning();
    }

    /**
     * Initialises the class for concept drift detection.
     */
    public void initLearning() {

        elbo=0;

        if (classIndex == -1)
            classIndex = this.attributes.getNumberOfAttributes()-1;


        switch (this.conceptDriftDetector){
            case GLOBAL:
                this.buildGlobalDAG();
                break;
        }
    }


    /**
     * Update the model with a new data stream of instances.
     * @param batch, a <code>DataOnMemory</code> object containing a batch of data instances.
     * @return An array of double values containing the expected value of the global hidden variables.
     */
    public double[] updateModel(DataStream<DataInstance> batch){

        double[] meanHiddenVars = new double[this.getHiddenVars().size()];

        double out[];
        for (DataOnMemory<DataInstance> minibatch : batch.iterableOverBatches(this.windowsSize)) {
            out = this.updateModel(minibatch);


            for (int i = 0; i < meanHiddenVars.length; i++) {
                meanHiddenVars[i] = out[i];
            }

            break;
        }
        return meanHiddenVars;
    }

    /**
     * Update the model with a new batch of instances. The size of the batch should be equal
     * to the size of the window of the class
     * @param batch, a <code>DataOnMemory</code> object containing a batch of data instances.
     * @return An array of double values containing the expected value of the global hidden variables.
     */
    public double[] updateModel(DataOnMemory<DataInstance> batch){
        elbo+=svb.updateModel(batch);
        double[] out = new double[hiddenVars.size()];
        for (int i = 0; i < out.length; i++) {
            Variable hiddenVar = this.hiddenVars.get(i);
            Normal normal = svb.getParameterPosterior(hiddenVar);
            out[i] = normal.getMean();
        }
        return out;
    }

    public double getElbo() {
        return elbo;
    }

    /**
     * Returns the list of global hidden variables
     * @return A list of <code>Variable</code> objects
     */
    public List<Variable> getHiddenVars() {
        return hiddenVars;
    }

    /**
     * Returns the Bayesian network learnt with the concept drift adaptation method.
     * @return A <code>BayesianNetwork</code> object.
     */
    public BayesianNetwork getLearntBayesianNetwork(){
        return svb.getLearntBayesianNetwork();
    }

}
