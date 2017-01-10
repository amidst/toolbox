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

package weka.classifiers.bayes;

import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variables;
import weka.core.*;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;
import java.util.stream.IntStream;

/**
 * This class extends the {@link weka.classifiers.AbstractClassifier} and defines the AMIDST Classifier that could be run using the MOA’s graphical user interface.
 * MOA (Massive Online Analysis) is an open source software available at http://moa.cms.waikato.ac.nz
 */
public class NaiveBayesWithHiddenVars extends AmidstGeneralClassifier implements OptionHandler, Randomizable{
    /** Represents the number of Gaussian hidden variables in this AmidstClassifier. */
    protected  int nOfGaussianHiddenVars_ = 0;

    /** Represents the number of Multinomial hidden variables in this AmidstClassifier. */
    protected  int nOfStatesMultHiddenVar_ = 0;

    public int getnOfGaussianHiddenVars_() {
        return nOfGaussianHiddenVars_;
    }

    public void setnOfGaussianHiddenVars_(int nOfGaussianHiddenVars_) {
        this.nOfGaussianHiddenVars_ = nOfGaussianHiddenVars_;
    }

    public int getnOfStatesMultHiddenVar_() {
        return nOfStatesMultHiddenVar_;
    }

    public void setnOfStatesMultHiddenVar_(int nOfStatesMultHiddenVar_) {
        this.nOfStatesMultHiddenVar_ = nOfStatesMultHiddenVar_;
    }

    /**
     * Returns default capabilities of the classifier.
     *
     * @return the capabilities of this classifier
     */
    @Override
    public Capabilities getCapabilities() {
        Capabilities result = super.getCapabilities();
        result.disableAll();

        // attributes
        result.enable(Capabilities.Capability.NOMINAL_ATTRIBUTES);
        result.enable(Capabilities.Capability.NUMERIC_ATTRIBUTES);
        result.enable(Capabilities.Capability.MISSING_VALUES);

        // class
        result.enable(Capabilities.Capability.NOMINAL_CLASS);
        result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);

        // instances
        result.setMinimumNumberInstances(0);

        return result;
    }


    @Override
    public DAG buildDAG(){

        Variables modelHeader = new Variables(attributes_);

        /* Create both Gaussian and Multinomial hidden variables. */
        if(getnOfGaussianHiddenVars_() > 0)
            IntStream.rangeClosed(0, getnOfGaussianHiddenVars_()-1)
                    .forEach(i -> modelHeader.newGaussianVariable("HiddenG_" + i));
        if(getnOfStatesMultHiddenVar_() > 0)
            modelHeader.newMultinomialVariable("HiddenM", getnOfStatesMultHiddenVar_());

        DAG dag = new DAG(modelHeader);

        /* Set DAG structure. */
        /* 1. Add classVar as parent of all Gaussian and Multinomial hidden variables. */
        if(getnOfGaussianHiddenVars_() > 0)
            IntStream.rangeClosed(0, getnOfGaussianHiddenVars_()-1).parallel()
                    .forEach(hv -> dag.getParentSet(modelHeader.getVariableByName("HiddenG_" + hv)).addParent(classVar_));
        if(getnOfStatesMultHiddenVar_() > 0)
            dag.getParentSet(modelHeader.getVariableByName("HiddenM")).addParent(classVar_);

        /* 2. Add classVar and all hidden variables as parents of all predictive attributes. */
        /* Note however that Gaussian hidden variables can be only parents of Gaussian predictive attributes. */
        dag.getParentSets().stream()
                .filter(w -> w.getMainVar().getVarID() != classVar_.getVarID())
                .filter(w -> w.getMainVar().isObservable())
                .forEach(w -> {
                    w.addParent(classVar_);
                    if (getnOfStatesMultHiddenVar_() != 0)
                        w.addParent(modelHeader.getVariableByName("HiddenM"));
                    if (w.getMainVar().isNormal() && getnOfGaussianHiddenVars_() != 0)
                        IntStream.rangeClosed(0, getnOfGaussianHiddenVars_()-1)
                                .forEach(hv -> w.addParent(modelHeader.getVariableByName("HiddenG_" + hv)));});


        System.out.println(dag.toString());


        return dag;
    }

    /**
     * Returns an enumeration describing the available options.
     *
     * @return an enumeration of all the available options.
     */
    @Override
    public Enumeration<Option> listOptions() {

        Vector<Option> newVector = new Vector<Option>(2);

        newVector.addElement(new Option(
                "\tNumber of Gaussian hidden variables.\n"+
                        "\t(Default = 0)",
                "G", 1,"-G <number of Gaussian hidden>"));
        newVector.addElement(new Option(
                "\tNumber of states of the discrete hidden variable.\n"+
                        "\t(Default = 0)",
                "S", 1,"-S <number of states>"));

        newVector.addAll(Collections.list(super.listOptions()));

        return newVector.elements();
    }

    /**
     * Parses a given list of options.
     * <p/>
     *
     * <!-- options-start --> Valid options are:
     * <p/>
     *
     * <pre>
     * -G
     *  Number of hidden Gaussian variables
     * </pre>
     *
     * <pre>
     * -S
     *  Number of states of the hidden discrete variables
     * </pre>
     *
     * <!-- options-end -->
     *
     * @param options the list of options as an array of strings
     * @exception Exception if an option is not supported
     */
    @Override
    public void setOptions(String[] options) throws Exception {
        String numGaussString = Utils.getOption('G', options);
        if (numGaussString.length() != 0) {
            setnOfGaussianHiddenVars_(Integer.parseInt(numGaussString));
        } else {
            setnOfGaussianHiddenVars_(0);
        }

        String numStatesString = Utils.getOption('S', options);
        if (numStatesString.length() != 0) {
            setnOfStatesMultHiddenVar_(Integer.parseInt(numStatesString));
        } else {
            setnOfStatesMultHiddenVar_(0);
        }
    }

    /**
     * Gets the current settings of the classifier.
     *
     * @return an array of strings suitable for passing to setOptions
     */
    @Override
    public String[] getOptions() {

        Vector<String> options = new Vector<String>();
        options.add("-G"); options.add("" + getnOfGaussianHiddenVars_());
        options.add("-S"); options.add("" + getnOfStatesMultHiddenVar_());

        Collections.addAll(options, super.getOptions());

        return options.toArray(new String[0]);
    }

    /**
     * Main method for testing this class.
     *
     * @param argv the options
     */
    public static void main(String[] argv) {
        runClassifier(new NaiveBayesWithHiddenVars(), argv);
    }
}
