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
import eu.amidst.latentvariablemodels.staticmodels.classifiers.HODE;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;
import junit.framework.TestCase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ana@cs.aau.dk/rcabanas on 11/03/16.
 */
public class HODETest extends TestCase{
    protected HODE hode;
    DataStream<DataInstance> data;

    protected void setUp() throws WrongConfigurationException {
        data = DataSetGenerator.generate(1234,500, 2, 10);

        System.out.println(data.getAttributes().toString());

        String classVarName = "DiscreteVar0";

        hode = new HODE(data.getAttributes());
        hode.setClassName(classVarName);

        hode.updateModel(data);
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {

            hode.updateModel(batch);
        }
        System.out.println(hode.getModel());
        System.out.println(hode.getDAG());

    }


    //////// test methods

    public void testClassVariable() {
        boolean passedTest = true;

        Variable classVar = hode.getClassVar();

        // class variable is a multinomial
        boolean isMultinomial = classVar.isMultinomial();

        //has not parents
        boolean noParents = hode.getDAG().getParentSet(classVar).getParents().isEmpty();

        //all the attributes are their children
        boolean allAttrChildren = hode.getModel().getVariables().getListOfVariables().stream()
                .filter(v-> !v.equals(classVar))
                .allMatch(v -> hode.getDAG().getParentSet(v).contains(classVar));

        assertTrue(isMultinomial && noParents && allAttrChildren);
    }



    public void testAttributes(){
        Variable classVar = hode.getClassVar();

        // the attributes have two parents
        boolean numParents = hode.getModel().getVariables().getListOfVariables().stream()
                .filter(v-> !v.equals(classVar))
                .filter(v-> !v.equals(hode.getModel().getVariables().getVariableByName("superParentVar")))
                .allMatch(v -> hode.getDAG().getParentSet(v).getNumberOfParents()==2);

        assertTrue(numParents);
    }



    public void testPrediction() {

        List<DataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,10);


        double hits = 0;

        for(DataInstance d : dataTest) {

            double realValue = d.getValue(hode.getClassVar());
            double predValue;

            d.setValue(hode.getClassVar(), Utils.missingValue());
            Multinomial posteriorProb = hode.predict(d);


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

}