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

package eu.amidst.latentvariablemodels;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.latentvariablemodels.staticmodels.FactorAnalysis;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;
import junit.framework.TestCase;

/**
 * Created by rcabanas on 28/03/16.
 */
public class FactorAnalysisTest extends TestCase {

    protected FactorAnalysis model;
    DataStream<DataInstance> data;

    protected void setUp() throws WrongConfigurationException {
        int seed=6236;
        int nSamples=5000;
        int nContinuousVars=3;

        data = DataSetGenerator.generate(seed,nSamples,0,nContinuousVars);

        model = new FactorAnalysis(data.getAttributes());

        System.out.println(model.getDAG());

        model.updateModel(data);

    }


    //////// test methods

    public void testAttributes() {



        // each observable variable has a number of parents equal to the number of hidden variables
        boolean numParentsCond = model.getModel().getVariables().getListOfVariables().stream()
                .filter(v -> v.isObservable())
                .allMatch(v -> model.getDAG().getParentSet(v).getNumberOfParents() == model.getNumberOfLatentVariables());


        // the observable variables only have hidden parents
        boolean allHidenParents = model.getModel().getVariables().getListOfVariables().stream()
                .filter(v -> v.isObservable())
                .allMatch(v -> model.getDAG().getParentSet(v).getParents().stream()
                            .allMatch(p -> !p.isObservable()));

        assertTrue(numParentsCond && allHidenParents);
    }





//    public void testFA() {
//
//
//        boolean passed = false;
//
//        // Normal [ mu = -0.11391269298981004, var = 47.58199351242742 ]
//
//        ConditionalDistribution pH0 = model.getModel().getConditionalDistribution(model.getModel().getVariables().getVariableByName("LatentVar0"));
//
//        double[] params = pH0.getParameters();
//
//        assertEquals(params[0], -0.11391269298981004, 0.00001);
//        assertEquals(params[1], 47.58199351242742,0.00001);
//
//    }



}
