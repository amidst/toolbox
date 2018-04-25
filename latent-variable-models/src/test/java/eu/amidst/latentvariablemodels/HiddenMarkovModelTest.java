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

import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.utils.DataSetGenerator;
import eu.amidst.latentvariablemodels.dynamicmodels.HiddenMarkovModel;
import junit.framework.TestCase;

/**
 * Created by ana@cs.aau.dk on 08/03/16.
 */
public class HiddenMarkovModelTest extends TestCase{

    private static DataStream<DynamicDataInstance> dataHybrid;
    private static DataStream<DynamicDataInstance> dataGaussians;
    private static boolean setUpIsDone = false;

    protected void setUp(){
        if (setUpIsDone) {
            return;
        }
        dataHybrid = DataSetGenerator.generate(1,1000,3,3);
        dataGaussians = DataSetGenerator.generate(1,1000,0,3);
        setUpIsDone = true;
    }

    public void test1(){
        System.out.println("------------------HMM (diagonal matrix) from streaming------------------");
        HiddenMarkovModel HMM = new HiddenMarkovModel(dataHybrid.getAttributes());
        System.out.println(HMM.getDynamicDAG());
        HMM.updateModel(dataHybrid);
        System.out.println(HMM.getModel());
    }
    public void test2(){
        System.out.println("------------------HMM (full cov. matrix) from streaming------------------");
        HiddenMarkovModel HMM = new HiddenMarkovModel(dataGaussians.getAttributes());
        HMM.setDiagonal(false);
        System.out.println(HMM.getDynamicDAG());
        HMM.updateModel(dataGaussians);
        System.out.println(HMM.getModel());
    }

    public void test3(){
        System.out.println("------------------HMM (diagonal matrix) from batches------------------");
        HiddenMarkovModel HMM = new HiddenMarkovModel(dataHybrid.getAttributes());
        System.out.println(HMM.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : dataHybrid.iterableOverBatches(100)) {
            HMM.updateModel(batch);
        }
        System.out.println(HMM.getModel());
    }

}
