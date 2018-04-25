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

package eu.amidst.latentvariablemodels.dynamicmodels;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.utils.DataSetGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements an Input-Output Hidden Markov Model. HMM with input/control observed variables on top.
 * See e.g.:
 *
 * Kevin P. Murphy. 2012. Machine Learning: A Probabilistic Perspective. The MIT Press. Page 625
 *
 * Created by ana@cs.aau.dk on 08/03/16.
 */
public class InputOutputHMM  extends DynamicModel<InputOutputHMM> {

    private boolean diagonal = true;
    private int numStates = 2;

    private List<Attribute> inputAtts;
    private List<Attribute> outputAtts;

    public boolean isDiagonal() {
        return diagonal;
    }

    public InputOutputHMM setDiagonal(boolean diagonal) {
        this.diagonal = diagonal;
        resetModel();
        return this;
    }

    public int getNumStates() {
        return numStates;
    }

    public InputOutputHMM setNumStates(int numStates) {
        this.numStates = numStates;
        resetModel();
        return this;
    }

    public InputOutputHMM(Attributes attributes, List<Attribute> inputs,List<Attribute> outputs ) {
        super(attributes);
        this.inputAtts = inputs;
        this.outputAtts = outputs;
    }

    @Override
    protected void buildDAG() {

        List<Variable> inputVars = this.variables.getVariablesForListOfAttributes(inputAtts);
        List<Variable> outputVars = this.variables.getVariablesForListOfAttributes(outputAtts);
        Variable discreteHiddenVar = this.variables.newMultinomialDynamicVariable("discreteHiddenVar", getNumStates());


        dynamicDAG = new DynamicDAG(this.variables);

        for (Variable inputVar : inputVars) {
            if (inputVar.isNormal())
                throw new UnsupportedOperationException("Invalid configuration: all the input variables must be " +
                        "discrete");
            dynamicDAG.getParentSetTimeT(discreteHiddenVar).addParent(inputVar);
            for (Variable outputVar : outputVars) {
                dynamicDAG.getParentSetTimeT(outputVar).addParent(inputVar);
            }
        }

        for (Variable outputVar : outputVars) {
            dynamicDAG.getParentSetTimeT(outputVar).addParent(discreteHiddenVar);
        }

        System.out.println(dynamicDAG);
        dynamicDAG.getParentSetTimeT(discreteHiddenVar).addParent(discreteHiddenVar.getInterfaceVariable());


        /*
         * Learn full covariance matrix
         */
        if(!isDiagonal()) {

            for (int i=0; i<inputVars.size()-1; i++){
                for(int j=i+1; j<inputVars.size(); j++) {
                    dynamicDAG.getParentSetTime0(inputVars.get(i)).addParent(inputVars.get(j));
                    dynamicDAG.getParentSetTimeT(inputVars.get(i)).addParent(inputVars.get(j));
                }
            }

            for (int i=0; i<outputVars.size()-1; i++){
                for(int j=i+1; j<outputVars.size(); j++) {
                    dynamicDAG.getParentSetTime0(outputVars.get(i)).addParent(outputVars.get(j));
                    dynamicDAG.getParentSetTimeT(outputVars.get(i)).addParent(outputVars.get(j));
                }
            }
        }
    }

    @Override
    public boolean isValidConfiguration() {
        return true;
    }

    public static void main(String[] args) {

        DataStream<DynamicDataInstance> data = DataSetGenerator.generate(1,1000,3,3);

        //DataStream<DynamicDataInstance> data = DynamicDataStreamLoader
        //        .loadFromFile("datasets/WasteIncineratorSample.arff");

        Attributes dataAttributes = data.getAttributes();

        List<Attribute> inputAtts = new ArrayList<>();
        inputAtts.add(dataAttributes.getAttributeByName("DiscreteVar0"));
        inputAtts.add(dataAttributes.getAttributeByName("DiscreteVar1"));
        inputAtts.add(dataAttributes.getAttributeByName("DiscreteVar2"));
        List<Attribute> outputAtts = new ArrayList<>();
        outputAtts.add(dataAttributes.getAttributeByName("GaussianVar0"));
        outputAtts.add(dataAttributes.getAttributeByName("GaussianVar1"));
        outputAtts.add(dataAttributes.getAttributeByName("GaussianVar2"));

        System.out.println("------------------Input-Output HMM (diagonal matrix) from streaming------------------");
        InputOutputHMM IOHMM = new InputOutputHMM(dataAttributes,inputAtts,outputAtts);
        IOHMM.setNumStates(2);
        System.out.println(IOHMM.getDynamicDAG());
        IOHMM.updateModel(data);
        System.out.println(IOHMM.getModel());

        System.out.println("------------------Input-Output HMM (full cov. matrix) from streaming------------------");
        IOHMM = new InputOutputHMM(dataAttributes,inputAtts,outputAtts);
        IOHMM.setDiagonal(false);
        System.out.println(IOHMM.getDynamicDAG());
        IOHMM.updateModel(data);
        System.out.println(IOHMM.getModel());

        System.out.println("------------------Input-Output HMM (diagonal matrix) from batches------------------");
        IOHMM = new InputOutputHMM(dataAttributes,inputAtts,outputAtts);
        System.out.println(IOHMM.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : data.iterableOverBatches(100)) {
            IOHMM.updateModel(batch);
        }
        System.out.println(IOHMM.getModel());

    }
}
