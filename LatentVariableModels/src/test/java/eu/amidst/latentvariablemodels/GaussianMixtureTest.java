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
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;
import eu.amidst.latentvariablemodels.staticmodels.GaussianMixture;
import junit.framework.TestCase;

/**
 * Created by rcabanas on 10/03/16.
 */
public class GaussianMixtureTest extends TestCase {

    protected GaussianMixture gmm;
    DataStream<DataInstance> data;

    protected void setUp() throws WrongConfigurationException {


        DataStream<DataInstance> data = DataSetGenerator.generate(1234,500, 0, 4);



        gmm = new GaussianMixture(data.getAttributes());
        gmm.setDiagonal(false);
        gmm.setNumStatesHiddenVar(3);

        gmm.updateModel(data);
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {
            gmm.updateModel(batch);
        }


    }


    //////// test methods

    public void testHiddenVar() {
        boolean passedTest = true;

        Variable classVar = gmm.getHiddenVar();

        // class variable is a multinomial
        boolean isMultinomial = classVar.isMultinomial();

        //has not parents
        boolean noParents = gmm.getDAG().getParentSet(classVar).getParents().isEmpty();

        //all the attributes are their children
        boolean allAttrChildren = gmm.getModel().getVariables().getListOfVariables().stream()
                .filter(v-> !v.equals(classVar))
                .allMatch(v -> gmm.getDAG().getParentSet(v).contains(classVar));

        assertTrue(isMultinomial && noParents && allAttrChildren);
    }



    public void testAttributes(){
        Variable classVar = gmm.getHiddenVar();

        // the attributes have a single parent
        boolean numParents = gmm.getModel().getVariables().getListOfVariables().stream()
                .filter(v-> !v.equals(classVar))
                .allMatch(v -> gmm.getDAG().getParentSet(v).getNumberOfParents()==1);

        assertTrue(!gmm.isDiagonal() || numParents);
    }





}
