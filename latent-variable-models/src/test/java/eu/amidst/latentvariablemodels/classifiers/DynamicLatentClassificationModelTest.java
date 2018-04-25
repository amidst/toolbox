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

import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.utils.DataSetGenerator;
import eu.amidst.latentvariablemodels.dynamicmodels.classifiers.DynamicLatentClassificationModel;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;
import junit.framework.TestCase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ana@cs.aau.dk/rcabanas on 11/03/16.
 */
public class DynamicLatentClassificationModelTest extends TestCase {

    protected DynamicLatentClassificationModel dLCM;
    DataStream<DynamicDataInstance> data;

    protected void setUp() throws WrongConfigurationException {
        data = DataSetGenerator.generate(1234,500, 1, 5);

        System.out.println(data.getAttributes().toString());

        String classVarName = "DiscreteVar0";

        dLCM = new DynamicLatentClassificationModel(data.getAttributes());
        dLCM.setClassName(classVarName);


        dLCM.updateModel(data);
        for (DataOnMemory<DynamicDataInstance> batch : data.iterableOverBatches(100)) {

            dLCM.updateModel(batch);
        }

        System.out.println(dLCM.getDynamicDAG());
        System.out.println(dLCM.getModel());



    }


    //////// test methods

    public void testClassVariable() {
        boolean passedTest = true;

        Variable classVar = dLCM.getClassVar();

        // class variable is a multinomial
        boolean isMultinomial = classVar.isMultinomial();

        //has not parents
        boolean noParents = dLCM.getDynamicDAG().getParentSetTime0(classVar).getParents().isEmpty();


        //only the latent variables are children of the class


        boolean classChildren = dLCM.getModel().getDynamicVariables().getListOfDynamicVariables().stream()     // class only have hidden children
                .filter(v -> dLCM.getDynamicDAG().getParentSetTimeT(v).contains(dLCM.getClassVar()))
                .allMatch( v -> dLCM.getContHiddenList().contains(v) || v.equals(dLCM.getHiddenMultinomial()))
                &&
                dLCM.getDynamicDAG().getParentSetTimeT(dLCM.getHiddenMultinomial()).getParents().stream().allMatch(v->v.equals(dLCM.getClassVar())) // multi. hidden only has class as parents
                &&
                dLCM.getContHiddenList().stream()    // continuous hidden only have class as parent
                        .allMatch(hc -> dLCM.getDynamicDAG().getParentSetTime0(hc).getParents().stream()
                                .allMatch(v->v.equals(dLCM.getClassVar())));




        assertTrue(isMultinomial && noParents && classChildren);
    }



    public void testAttributes(){
        Variable classVar = dLCM.getClassVar();

        // the attributes have a single parent
        boolean numParents = dLCM.getModel().getDynamicVariables().getListOfDynamicVariables().stream()
                .filter(v-> !v.equals(classVar) && !v.equals(dLCM.getHiddenMultinomial()) && !dLCM.getContHiddenList().contains(v))
                .allMatch(v -> !dLCM.getDynamicDAG().getParentSetTime0(v).contains(classVar));

        assertTrue(numParents);
    }



    public void testPrediction() {

        List<DynamicDataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,10);


        double hits = 0;

        for(DynamicDataInstance d : dataTest) {

            double realValue = d.getValue(dLCM.getClassVar());
            double predValue;

            d.setValue(dLCM.getClassVar(), Utils.missingValue());
            Multinomial posteriorProb = dLCM.predict(d);


            double[] values = posteriorProb.getProbabilities();
            if (values[0]>values[1]) {
                predValue = 0;
            }else {
                predValue = 1;

            }

            if(realValue == predValue) hits++;


        }
        System.out.println(hits);

    }
}
