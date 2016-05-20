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
import eu.amidst.latentvariablemodels.staticmodels.classifiers.LatentClassificationModel;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;
import junit.framework.TestCase;

import java.util.List;
import java.util.stream.Collectors;


public class LatentClassificationModelTest extends TestCase {

    protected LatentClassificationModel lcm;
    DataStream<DataInstance> data;

    protected void setUp() throws WrongConfigurationException {
        data = DataSetGenerator.generate(1234,500, 1, 5);

        System.out.println(data.getAttributes().toString());

        String classVarName = "DiscreteVar0";

        lcm = new LatentClassificationModel(data.getAttributes());
        lcm.setClassName(classVarName);

        lcm.updateModel(data);
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {

            lcm.updateModel(batch);
        }
        System.out.println(lcm.getModel());
        System.out.println(lcm.getDAG());


    }


    //////// test methods

    public void testClassVariable() {
        boolean passedTest = true;

        Variable classVar = lcm.getClassVar();

        // class variable is a multinomial
        boolean isMultinomial = classVar.isMultinomial();

        //has not parents
        boolean noParents = lcm.getDAG().getParentSet(classVar).getParents().isEmpty();


        //only the latent variables are children of the class


        boolean classChildren = lcm.getModel().getVariables().getListOfVariables().stream()     // class only have hidden children
                .filter(v -> lcm.getDAG().getParentSet(v).contains(lcm.getClassVar()))
                .allMatch( v -> lcm.getContHiddenList().contains(v) || v.equals(lcm.getHiddenMultinomial()))
                &&
                lcm.getDAG().getParentSet(lcm.getHiddenMultinomial()).getParents().stream().allMatch(v->v.equals(lcm.getClassVar())) // multi. hidden only has class as parents
                &&
                lcm.getContHiddenList().stream()    // continuous hidden only have class as parent
                        .allMatch(hc -> lcm.getDAG().getParentSet(hc).getParents().stream()
                                .allMatch(v->v.equals(lcm.getClassVar())));




        assertTrue(isMultinomial && noParents && classChildren);
    }



    public void testAttributes(){
        Variable classVar = lcm.getClassVar();

        // the attributes have a single parent
        boolean numParents = lcm.getModel().getVariables().getListOfVariables().stream()
                .filter(v-> !v.equals(classVar) && !v.equals(lcm.getHiddenMultinomial()) && !lcm.getContHiddenList().contains(v))
                .allMatch(v -> !lcm.getDAG().getParentSet(v).contains(classVar));

        assertTrue(numParents);
    }



    public void testPrediction() {

        List<DataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,10);


        double hits = 0;

        for(DataInstance d : dataTest) {

            double realValue = d.getValue(lcm.getClassVar());
            double predValue;

            d.setValue(lcm.getClassVar(), Utils.missingValue());
            Multinomial posteriorProb = lcm.predict(d);


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





/*
    public void testDAG() {
        boolean passedTest = true;



        assertTrue(passedTest);
    }
*/


}
