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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class implements a Kalman Filter (KF) or State Space Model (SSM). See e.g.:
 *
 * Kevin P. Murphy. 2012. Machine Learning: A Probabilistic Perspective. The MIT Press. Page 640
 *
 * Created by ana@cs.aau.dk on 05/03/16.
 */
public class KalmanFilter extends DynamicModel<KalmanFilter> {
    private boolean diagonal = true;
    private int numHidden = 2;

    public boolean isDiagonal() {
        return diagonal;
    }

    public KalmanFilter setDiagonal(boolean diagonal) {
        this.diagonal = diagonal;
        resetModel();
        return this;
    }

    public int getNumHidden() {
        return numHidden;
    }

    public KalmanFilter setNumHidden(int numHidden) {
        this.numHidden = numHidden;
        resetModel();
        return this;
    }

    public KalmanFilter(Attributes attributes) {
        super(attributes);
    }

    @Override
    protected void buildDAG() {

        List<Variable> gaussianHiddenVars = new ArrayList<>();

        IntStream.range(0,getNumHidden()).forEach(i -> {
            Variable gaussianHiddenVar = this.variables.newGaussianDynamicVariable("gaussianHiddenVar" + i);
            gaussianHiddenVars.add(gaussianHiddenVar);
        });

        dynamicDAG = new DynamicDAG(this.variables);

        dynamicDAG.getParentSetsTimeT()
                .stream()
                .filter(w -> !gaussianHiddenVars.contains(w.getMainVar()))
                .forEach(y -> {
                    gaussianHiddenVars.stream()
                            .forEach(h -> y.addParent(h));
                });

        for (Variable gaussianHiddenVar : gaussianHiddenVars) {
            dynamicDAG.getParentSetTimeT(gaussianHiddenVar).addParent(gaussianHiddenVar.getInterfaceVariable());
        }


        for (int i=0; i<gaussianHiddenVars.size()-1; i++){
            for(int j=i+1; j<gaussianHiddenVars.size(); j++) {
                dynamicDAG.getParentSetTime0(gaussianHiddenVars.get(i)).addParent(gaussianHiddenVars.get(j));
                dynamicDAG.getParentSetTimeT(gaussianHiddenVars.get(i)).addParent(gaussianHiddenVars.get(j));
            }

        }



        /*
         * Learn full covariance matrix
         */
        if(!isDiagonal()) {
            List<Variable> observedVars = this.variables.getListOfDynamicVariables()
                    .stream()
                    .filter(w -> !gaussianHiddenVars.contains(w))
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

        System.out.println("------------------KF (diagonal matrix) from streaming------------------");
        KalmanFilter KF = new KalmanFilter(dataGaussians.getAttributes());
        KF.setNumHidden(2);
        System.out.println(KF.getDynamicDAG());
        KF.updateModel(dataGaussians);
        System.out.println(KF.getModel());

        System.out.println("------------------KF (full cov. matrix) from streaming------------------");
        KF = new KalmanFilter(dataGaussians.getAttributes());
        KF.setDiagonal(false);
        System.out.println(KF.getDynamicDAG());
        KF.updateModel(dataGaussians);
        System.out.println(KF.getModel());

        System.out.println("------------------KF (diagonal matrix) from batches------------------");
        KF = new KalmanFilter(dataGaussians.getAttributes());
        System.out.println(KF.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : dataGaussians.iterableOverBatches(100)) {
            KF.updateModel(batch);
        }
        System.out.println(KF.getModel());



    }
}
