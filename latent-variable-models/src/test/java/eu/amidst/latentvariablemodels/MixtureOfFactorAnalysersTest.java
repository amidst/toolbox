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
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.latentvariablemodels.staticmodels.MixtureOfFactorAnalysers;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;
import junit.framework.TestCase;

/**
 * Created by rcabanas on 29/03/16.
 */
public class MixtureOfFactorAnalysersTest extends TestCase {

    protected MixtureOfFactorAnalysers model;
    DataStream<DataInstance> data;

    protected void setUp() throws WrongConfigurationException {
        int seed=6236;
        int nSamples=5000;
        int nContinuousVars=3;

        data = DataSetGenerator.generate(seed,nSamples,0,nContinuousVars);

        model = new MixtureOfFactorAnalysers(data.getAttributes());

        System.out.println(model.getDAG());

        model.updateModel(data);

    }


    //////// test methods

    public void testAttributes() {



        // each observable variable has a number of parents equal to the number of continuous hidden variables plus 1
        boolean numParentsCond = model.getModel().getVariables().getListOfVariables().stream()
                .filter(v -> v.isObservable())
                .allMatch(v -> model.getDAG().getParentSet(v).getNumberOfParents() == model.getNumberOfLatentVariables()+1);


        // the observable variables only have hidden parents
        boolean allHidenParents = model.getModel().getVariables().getListOfVariables().stream()
                .filter(v -> v.isObservable())
                .allMatch(v -> model.getDAG().getParentSet(v).getParents().stream()
                            .allMatch(p -> !p.isObservable()));

        assertTrue(numParentsCond && allHidenParents);
    }





//    public void testMFA() {
//
//
//        boolean passed = false;
//
//
//        /*
//        P(DiscreteLatentVar) follows a Multinomial
//            [ 3.996802557953637E-4, 0.9996003197442046 ]
//        P(LatentVar0) follows a Normal
//            Normal [ mu = 0.6322849166799489, var = 306.16068625831326 ]
//         */
//
//
//        ConditionalDistribution pHD0 = model.getModel().getConditionalDistribution(model.getModel().getVariables().getVariableByName("DiscreteLatentVar"));
//        double[] values = pHD0.getParameters();
//
//        if(values[0] == 3.996802557953637E-4 && values[1] == 0.9996003197442046 )
//            passed = true;
//        else
//            passed = false;
//
//
//        ConditionalDistribution pH0 = model.getModel().getConditionalDistribution(model.getModel().getVariables().getVariableByName("LatentVar0"));
//
//        double[] params = pH0.getParameters();
//
//        if(params[0] == 0.6322849166799489 && params[1] == 306.16068625831326)
//            passed = true;
//        else
//            passed = false;
//
//
//        assertTrue(passed);
//
//
//    }


}
