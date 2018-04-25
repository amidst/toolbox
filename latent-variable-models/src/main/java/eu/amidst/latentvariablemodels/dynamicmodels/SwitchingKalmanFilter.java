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
 * This class implements a Switching Kalman Filter (SKF). See e.g.:
 *
 * Kevin P. Murphy. 2012. Machine Learning: A Probabilistic Perspective. The MIT Press. Page 640
 *
 * Created by ana@cs.aau.dk on 07/03/16.
 */
public class SwitchingKalmanFilter  extends DynamicModel<SwitchingKalmanFilter> {

    private int numStates = 2;
    private boolean diagonal = true;

    public int getNumStates() {
        return numStates;
    }

    public SwitchingKalmanFilter setNumStates(int numStates) {
        this.numStates = numStates;
        resetModel();
        return this;
    }

    public boolean isDiagonal() {
        return diagonal;
    }

    public SwitchingKalmanFilter setDiagonal(boolean diagonal) {
        this.diagonal = diagonal;
        resetModel();
        return this;
    }

    public SwitchingKalmanFilter(Attributes attributes) {
        super(attributes);
    }

    @Override
    protected void buildDAG() {

        Variable discreteHiddenVar = this.variables.newMultinomialDynamicVariable("discreteHiddenVar", getNumStates());
        Variable gaussianHiddenVar = this.variables.newGaussianDynamicVariable("gaussianHiddenVar");

        dynamicDAG = new DynamicDAG(this.variables);
        dynamicDAG.getParentSetsTimeT()
                .stream()
                .filter(w -> w.getMainVar() != discreteHiddenVar)
                .filter(w -> w.getMainVar() != gaussianHiddenVar)
                .forEach(w -> {
                    w.addParent(discreteHiddenVar);
                    w.addParent(gaussianHiddenVar);
                });

        dynamicDAG.getParentSetTimeT(gaussianHiddenVar).addParent(discreteHiddenVar);
        dynamicDAG.getParentSetTimeT(gaussianHiddenVar).addParent(gaussianHiddenVar.getInterfaceVariable());
        dynamicDAG.getParentSetTimeT(discreteHiddenVar).addParent(discreteHiddenVar.getInterfaceVariable());

        /*
         * Learn full covariance matrix
         */
        if(!isDiagonal()) {
            List<Variable> attrVars = this.variables.getListOfDynamicVariables()
                    .stream()
                    .filter(v -> !v.equals(discreteHiddenVar))
                    .filter(v -> !v.equals(gaussianHiddenVar))
                    .peek(v-> {
                        if(v.isMultinomial())
                            throw new UnsupportedOperationException("Full covariance matrix cannot be used with" +
                                    " multinomial attributes");
                    })
                    .collect(Collectors.toList());

            for (int i=0; i<attrVars.size()-1; i++){
                for(int j=i+1; j<attrVars.size(); j++) {
                    dynamicDAG.getParentSetTime0(attrVars.get(i)).addParent(attrVars.get(j));
                    dynamicDAG.getParentSetTimeT(attrVars.get(i)).addParent(attrVars.get(j));
                }

            }
        }

    }

    @Override
    public boolean isValidConfiguration() {

        boolean isValid = this.variables.getListOfDynamicVariables()
                .stream().allMatch(var -> var.isNormal());

        if(!isValid)
            setErrorMessage("Invalid configuration: all the variables must be real");

        return isValid;


    }


    public static void main(String[] args) {

        DataStream<DynamicDataInstance> dataGaussians = DataSetGenerator.generate(1,1000,0,10);
        //DataStream<DynamicDataInstance> data = DynamicDataStreamLoader
        //        .loadFromFile("datasets/syntheticDataVerdandeScenario3.arff");

        System.out.println("------------------SKF (diagonal matrix) from streaming------------------");
        SwitchingKalmanFilter SKF = new SwitchingKalmanFilter(dataGaussians.getAttributes());
        System.out.println(SKF.getDynamicDAG());
        SKF.updateModel(dataGaussians);
        System.out.println(SKF.getModel());

        System.out.println("------------------SKF (full cov. matrix) from streaming------------------");
        SKF = new SwitchingKalmanFilter(dataGaussians.getAttributes());
        SKF.setDiagonal(false);
        System.out.println(SKF.getDynamicDAG());
        SKF.updateModel(dataGaussians);
        System.out.println(SKF.getModel());

        System.out.println("------------------SKF (diagonal matrix) from batches------------------");
        SKF = new SwitchingKalmanFilter(dataGaussians.getAttributes());
        System.out.println(SKF.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : dataGaussians.iterableOverBatches(100)) {
            SKF.updateModel(batch);
        }
        System.out.println(SKF.getModel());

    }
}
