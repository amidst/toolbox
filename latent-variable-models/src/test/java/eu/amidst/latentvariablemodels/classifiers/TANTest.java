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
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.latentvariablemodels.staticmodels.classifiers.TAN;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;
import junit.framework.TestCase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rcabanas on 10/03/16.
 */
public class TANTest extends TestCase {

    protected TAN model;
    String rootVarName;

    DataStream<DataInstance> data;

    protected void setUp() throws WrongConfigurationException {

        int seed=6236;
        int nSamples=5000;
        int nDiscreteVars=5;
        int nContinuousVars=10;




        data = DataSetGenerator.generate(seed,nSamples,nDiscreteVars,nContinuousVars);

        String classVarName="DiscreteVar0";
        rootVarName="DiscreteVar1";


        model = new TAN(data.getAttributes());


        model.setClassName(classVarName);
        model.setRootVarName(rootVarName);

        model.updateModel(data);

        System.out.println(model.getDAG());
        System.out.println();
        System.out.println(model.getModel());


    }


    //////// test methods

    public void testClassVariable() {

        boolean passedTest = true;

        Variable classVar = model.getClassVar();

        // class variable is a multinomial
        boolean isMultinomial = classVar.isMultinomial();

        //has not parents
        boolean noParents = model.getDAG().getParentSet(classVar).getParents().isEmpty();

        //all the attributes have the class in their parent set
        boolean allAttrChildren = model.getModel().getVariables().getListOfVariables().stream()
                .filter(v-> !v.equals(classVar))
                .allMatch(v -> model.getDAG().getParentSet(v).contains(classVar));

        assertTrue(isMultinomial && noParents && allAttrChildren);
    }



    public void testAttributes(){
        Variable classVar = model.getClassVar();



        // all the attributes but the root have a 2 parents
        boolean numParents = model.getModel().getVariables().getListOfVariables().stream()
                .filter(v-> !v.equals(classVar) && !v.getName().equals(rootVarName))
                .allMatch(v -> {
                    System.out.println(v.getName()+" "+model.getDAG().getParentSet(v).getNumberOfParents());
                    return model.getDAG().getParentSet(v).getNumberOfParents()==2;
                });

        assertTrue(numParents);


    }



    public void testPrediction() {


        List<DataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,10);


        double hits = 0;

        for(DataInstance d : dataTest) {

            double realValue = d.getValue(model.getClassVar());
            double predValue;

            d.setValue(model.getClassVar(), Utils.missingValue());
            Multinomial posteriorProb = model.predict(d);


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
