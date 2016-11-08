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

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.utils.DataSetGenerator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class implements a Hidden Markov Model (HMM). See e.g.:
 *
 * Kevin P. Murphy. 2012. Machine Learning: A Probabilistic Perspective. The MIT Press. Page 603
 *
 */
public class HiddenMarkovModel extends DynamicModel<HiddenMarkovModel>{

    private int numStatesHiddenVar = 2;
    private boolean diagonal = true;

    public int getNumStatesHiddenVar() {
        return numStatesHiddenVar;
    }

    public HiddenMarkovModel setNumStatesHiddenVar(int numStates) {
        this.numStatesHiddenVar = numStates;
        resetModel();
        return this;
    }

    public boolean isDiagonal() {
        return diagonal;
    }

    public HiddenMarkovModel setDiagonal(boolean diagonal) {
        this.diagonal = diagonal;
        resetModel();
        return this;
    }

    public HiddenMarkovModel(Attributes attributes) {
        super(attributes);
    }

    @Override
    protected void buildDAG() {

        Variable discreteHiddenVar = this.variables.newMultinomialDynamicVariable("discreteHiddenVar", getNumStatesHiddenVar());
        dynamicDAG = new DynamicDAG(this.variables);
        dynamicDAG.getParentSetsTimeT()
                .stream()
                .filter(w -> w.getMainVar() != discreteHiddenVar)
                .forEach(w -> w.addParent(discreteHiddenVar));

        dynamicDAG.getParentSetTimeT(discreteHiddenVar).addParent(discreteHiddenVar.getInterfaceVariable());

        /*
         * Learn full covariance matrix
         */
        if(!isDiagonal()) {
            List<Variable> observedVars = this.variables.getListOfDynamicVariables()
                    .stream()
                    .filter(v -> !v.equals(discreteHiddenVar))
                    .peek(v-> {
                        if(v.isMultinomial())
                            throw new UnsupportedOperationException("Full covariance matrix cannot be used with" +
                                    " multinomial attributes");
                    })
                    .collect(Collectors.toList());

            for (int i=0; i<observedVars.size()-1; i++){
                for(int j=i+1; j<observedVars.size(); j++) {
                    dynamicDAG.getParentSetTime0(observedVars.get(i)).addParent(observedVars.get(j));
                    dynamicDAG.getParentSetTimeT(observedVars.get(i)).addParent(observedVars.get(j));
                }

            }
        }

    }


    @Override
    public boolean isValidConfiguration() {

        boolean isValid = true;
        return isValid;


    }

    public static void main(String[] args) {


        DataStream<DynamicDataInstance> dataGaussians = DataSetGenerator.generate(1,1000,0,10);
        DataStream<DynamicDataInstance> dataHybrid = DataSetGenerator.generate(1,1000,2,10);

        //DataStream<DynamicDataInstance> data = DynamicDataStreamLoader
        //        .loadFromFile("datasets/syntheticDataVerdandeScenario3.arff");

        System.out.println("------------------HMM (diagonal matrix) from streaming------------------");
        HiddenMarkovModel HMM = new HiddenMarkovModel(dataHybrid.getAttributes());
        System.out.println(HMM.getDynamicDAG());
        HMM.updateModel(dataHybrid);
        System.out.println(HMM.getModel());

        System.out.println("------------------HMM (full cov. matrix) from streaming------------------");
        HMM = new HiddenMarkovModel(dataGaussians.getAttributes());
        HMM.setDiagonal(false);
        System.out.println(HMM.getDynamicDAG());
        HMM.updateModel(dataGaussians);
        System.out.println(HMM.getModel());

        System.out.println("------------------HMM (diagonal matrix) from batches------------------");
        HMM = new HiddenMarkovModel(dataHybrid.getAttributes());
        System.out.println(HMM.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : dataHybrid.iterableOverBatches(100)) {
            HMM.updateModel(batch);
        }
        System.out.println(HMM.getModel());



    }


}

