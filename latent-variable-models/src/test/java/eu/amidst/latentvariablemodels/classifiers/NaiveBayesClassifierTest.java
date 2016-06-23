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

package eu.amidst.latentvariablemodels.classifiers;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.latentvariablemodels.staticmodels.classifiers.NaiveBayesClassifier;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;
import junit.framework.TestCase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rcabanas on 10/03/16.
 */
public class NaiveBayesClassifierTest extends TestCase {

    protected NaiveBayesClassifier nb;
    DataStream<DataInstance> data;

    protected void setUp() throws WrongConfigurationException {
        data = DataSetGenerator.generate(1234,500, 2, 10);

        System.out.println(data.getAttributes().toString());

        String classVarName = "DiscreteVar0";

        nb = new NaiveBayesClassifier(data.getAttributes());
        nb.setClassName(classVarName);

        nb.updateModel(data);
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {

            nb.updateModel(batch);
        }
        System.out.println(nb.getModel());
        System.out.println(nb.getDAG());

    }


    //////// test methods

    public void testClassVariable() {
        boolean passedTest = true;

        Variable classVar = nb.getClassVar();

        // class variable is a multinomial
        boolean isMultinomial = classVar.isMultinomial();

        //has not parents
        boolean noParents = nb.getDAG().getParentSet(classVar).getParents().isEmpty();

        //all the attributes are their children
        boolean allAttrChildren = nb.getModel().getVariables().getListOfVariables().stream()
                .filter(v-> !v.equals(classVar))
                .allMatch(v -> nb.getDAG().getParentSet(v).contains(classVar));

        assertTrue(isMultinomial && noParents && allAttrChildren);
    }



    public void testAttributes(){
        Variable classVar = nb.getClassVar();

        // the attributes have a single parent
        boolean numParents = nb.getModel().getVariables().getListOfVariables().stream()
                .filter(v-> !v.equals(classVar))
                .allMatch(v -> nb.getDAG().getParentSet(v).getNumberOfParents()==1);

        assertTrue(numParents);
    }



    public void testPrediction() {

        List<DataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,10);


        double hits = 0;

        for(DataInstance d : dataTest) {

            double realValue = d.getValue(nb.getClassVar());
            double predValue;

            d.setValue(nb.getClassVar(), Utils.missingValue());
            Multinomial posteriorProb = nb.predict(d);


            double[] values = posteriorProb.getProbabilities();
            if (values[0]>values[1]) {
                predValue = 0;
            }else {
                predValue = 1;

            }

            if(realValue == predValue) hits++;


        }

        assertTrue(hits==10);


    }


    public void testNBClassifier() {

        long time = System.nanoTime();
        nb.updateModel(data);
        BayesianNetwork nbClassifier = nb.getModel();
        System.out.println(nbClassifier.toString());

        System.out.println("Time: " + (System.nanoTime() - time) / 1000000000.0);


        ParallelMaximumLikelihood parallelMaximumLikelihood = new ParallelMaximumLikelihood();

        parallelMaximumLikelihood.setWindowsSize(100);
        parallelMaximumLikelihood.setDAG(nbClassifier.getDAG());
        parallelMaximumLikelihood.setLaplace(true);
        parallelMaximumLikelihood.setDataStream(DataSetGenerator.generate(1234,500, 2, 10));

        parallelMaximumLikelihood.runLearning();
        BayesianNetwork nbML = parallelMaximumLikelihood.getLearntBayesianNetwork();
        System.out.println(nb.toString());
        assertTrue(nbML.equalBNs(nbClassifier, 0.2));

    }



}
