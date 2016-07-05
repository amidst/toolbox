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

package eu.amidst.latentvariablemodels;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.variables.Variable;
import eu.amidst.latentvariablemodels.staticmodels.BayesianLinearRegression;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;
import junit.framework.TestCase;

/**
 * Created by rcabanas on 28/03/16.
 */
public class BayesianLinearRegressionTest extends TestCase {

    protected BayesianLinearRegression model;
    DataStream<DataInstance> data;

    protected void setUp() throws WrongConfigurationException {
        data = DataSetGenerator.generate(0,1000, 0, 3);
        System.out.println(data.getAttributes().toString());

        String className = "GaussianVar0";


        model = new BayesianLinearRegression(data.getAttributes());
        model.setClassName(className);
        model.setDiagonal(false);

        model.updateModel(data);
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {
            model.updateModel(batch);
        }
        System.out.println(model.getModel());
        System.out.println(model.getDAG());

    }


    //////// test methods

    public void testClassVariable() {
        boolean passedTest = true;

        Variable classVar = model.getClassVar();

        // class variable is a REAL
        boolean isReal = classVar.isNormal();

        //is child of all the attributes
        boolean allAttrChildren = model.getModel().getVariables().getListOfVariables().stream()
                .filter(v-> !v.equals(classVar))
                .allMatch(v -> model.getDAG().getParentSet(classVar).contains(v));

        assertTrue(isReal && allAttrChildren);
    }





//    public void testBLR() {
//
//
//        boolean passed = false;
//
//        List<DataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,1);
//
//
//
//        InferenceAlgorithm infer = new VMP();
//        infer.setModel(model.getModel());
//
//        for(DataInstance d : dataTest) {
//
//            Assignment assignment = new HashMapAssignment(model.getModel().getNumberOfVars()-1);
//            for (int i=0; i<model.getModel().getNumberOfVars(); i++) {
//                Variable v = model.getModel().getVariables().getVariableById(i);
//                if(!v.equals(model.getClassVar()))
//                    assignment.setValue(v,d.getValue(v));
//            }
//
//            UnivariateDistribution posterior = InferenceEngine.getPosterior(model.getClassVar(), model.getModel(),assignment);
//            double[] param = posterior.getParameters();
//            System.out.println(param[0]);
//
//            if(param[0] == 0.9700097820861373)
//                passed = true;
//
//
//        }
//
//
//        assertTrue(passed);
//
//
//    }



}
