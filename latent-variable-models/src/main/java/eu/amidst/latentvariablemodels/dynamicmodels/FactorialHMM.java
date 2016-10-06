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
 * This class implements a Factorial Hidden Markov Model. HMM with (unconnected) binary hidden parents. See e.g.:
 *
 * Kevin P. Murphy. 2012. Machine Learning: A Probabilistic Perspective. The MIT Press. Page 627
 *
 * Created by ana@cs.aau.dk on 05/03/16.
 */
public class FactorialHMM extends DynamicModel {

    private int numHidden = 2;
    private boolean diagonal = true;

    public int getNumHidden() {
        return numHidden;
    }

    public FactorialHMM setNumHidden(int numHidden) {
        this.numHidden = numHidden;
        resetModel();
        return this;
    }

    public boolean isDiagonal() {
        return diagonal;
    }

    public FactorialHMM setDiagonal(boolean diagonal) {
        this.diagonal = diagonal;
        resetModel();
        return this;
    }

    public FactorialHMM(Attributes attributes) {
        super(attributes);
    }

    @Override
    protected void buildDAG() {

        List<Variable> binaryHiddenVars = new ArrayList<>();

        IntStream.range(0, getNumHidden()).forEach(i -> {
            Variable binaryHiddenVar = this.variables.newMultinomialDynamicVariable("binaryHiddenVar" + i,2);
            binaryHiddenVars.add(binaryHiddenVar);
        });

        dynamicDAG = new DynamicDAG(this.variables);
        dynamicDAG.getParentSetsTimeT()
                .stream()
                .filter(w -> !binaryHiddenVars.contains(w.getMainVar()))
                .forEach(y -> {
                    binaryHiddenVars.stream()
                            .forEach(h -> y.addParent(h));
                });

        for (Variable gaussianHiddenVar : binaryHiddenVars) {
            dynamicDAG.getParentSetTimeT(gaussianHiddenVar).addParent(gaussianHiddenVar.getInterfaceVariable());
        }

        /*
         * Learn full covariance matrix
         */
        if(!isDiagonal()) {
            List<Variable> observedVars = this.variables.getListOfDynamicVariables()
                    .stream()
                    .filter(w -> !binaryHiddenVars.contains(w))
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
    public boolean isValidConfiguration(){
        return true;
    }

    public static void main(String[] args) {

        DataStream<DynamicDataInstance> dataHybrid= DataSetGenerator.generate(1,1000,3,10);
        DataStream<DynamicDataInstance> dataGaussians = DataSetGenerator.generate(1,1000,0,10);
        //DataStream<DynamicDataInstance> data = DynamicDataStreamLoader
        //        .loadFromFile("datasets/syntheticDataVerdandeScenario3.arff");

        System.out.println("------------------Factorial HMM (diagonal matrix) from streaming------------------");
        FactorialHMM factorialHMM = new FactorialHMM(dataHybrid.getAttributes());
        System.out.println(factorialHMM.getDynamicDAG());
        factorialHMM.updateModel(dataHybrid);
        System.out.println(factorialHMM.getModel());

        System.out.println("------------------Factorial HMM (full cov. matrix) from streaming------------------");
        factorialHMM = new FactorialHMM(dataGaussians.getAttributes());
        factorialHMM.setDiagonal(false);
        System.out.println(factorialHMM.getDynamicDAG());
        factorialHMM.updateModel(dataGaussians);
        System.out.println(factorialHMM.getModel());

        System.out.println("------------------Factorial HMM (diagonal matrix) from batches------------------");
        factorialHMM = new FactorialHMM(dataHybrid.getAttributes());
        System.out.println(factorialHMM.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : dataHybrid.iterableOverBatches(100)) {
            factorialHMM.updateModel(batch);
        }
        System.out.println(factorialHMM.getModel());

    }

}
