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
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.latentvariablemodels.staticmodels.classifiers.GaussianDiscriminantAnalysis;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;
import junit.framework.TestCase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rcabanas on 10/03/16.
 */
public class GaussianDiscriminantAnalysisTest extends TestCase {

    protected GaussianDiscriminantAnalysis gda;
    DataStream<DataInstance> data;

    protected void setUp() throws WrongConfigurationException {

        data = DataSetGenerator.generate(1234,500, 1, 3);

        gda = new GaussianDiscriminantAnalysis(data.getAttributes());
        gda.setDiagonal(false);
        gda.setClassName("DiscreteVar0");

        gda.updateModel(data);
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {
            gda.updateModel(batch);
        }

    }


    //////// test methods

    public void testClassVariable() {
        boolean passedTest = true;

        Variable classVar = gda.getClassVar();

        // class variable is a multinomial
        boolean isMultinomial = classVar.isMultinomial();

        //has not parents
        boolean noParents = gda.getDAG().getParentSet(classVar).getParents().isEmpty();

        //all the attributes are their children
        boolean allAttrChildren = gda.getModel().getVariables().getListOfVariables().stream()
                .filter(v-> !v.equals(classVar))
                .allMatch(v -> gda.getDAG().getParentSet(v).contains(classVar));

        assertTrue(isMultinomial && noParents && allAttrChildren);
    }



    public void testAttributes(){
        Variable classVar = gda.getClassVar();

        // the attributes have a single parent
        boolean numParents = gda.getModel().getVariables().getListOfVariables().stream()
                .filter(v-> !v.equals(classVar))
                .allMatch(v -> gda.getDAG().getParentSet(v).getNumberOfParents()==1);

        assertTrue(!gda.isDiagonal() || numParents);
    }



    public void testPrediction() {

        List<DataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,10);


        double hits = 0;

        for(DataInstance d : dataTest) {

            double realValue = d.getValue(gda.getClassVar());
            double predValue;

            d.setValue(gda.getClassVar(), Utils.missingValue());
            Multinomial posteriorProb = gda.predict(d);


            double[] values = posteriorProb.getProbabilities();
            if (values[0]>values[1]) {
                predValue = 0;
            }else {
                predValue = 1;

            }

            if(realValue == predValue) hits++;


        }


        System.out.println(hits);
        assertTrue(hits==10);


    }




}
