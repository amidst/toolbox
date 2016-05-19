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
import eu.amidst.latentvariablemodels.dynamicmodels.SwitchingKalmanFilter;
import junit.framework.TestCase;

/**
 * Created by ana@cs.aau.dk on 09/03/16.
 */
public class SwitchingKalmanFilterTest extends TestCase{
    private static DataStream<DynamicDataInstance> dataGaussians;
    private static boolean setUpIsDone = false;

    protected void setUp(){
        if (setUpIsDone) {
            return;
        }
        dataGaussians = DataSetGenerator.generate(1,1000,0,10);
        setUpIsDone = true;
    }

    public void test1(){
        System.out.println("------------------SKF (diagonal matrix) from streaming------------------");
        SwitchingKalmanFilter SKF = new SwitchingKalmanFilter(dataGaussians.getAttributes());
        System.out.println(SKF.getDynamicDAG());
        SKF.updateModel(dataGaussians);
        System.out.println(SKF.getModel());

    }
    public void test2(){
        System.out.println("------------------SKF (full cov. matrix) from streaming------------------");
        SwitchingKalmanFilter SKF = new SwitchingKalmanFilter(dataGaussians.getAttributes());
        SKF.setDiagonal(false);
        System.out.println(SKF.getDynamicDAG());
        SKF.updateModel(dataGaussians);
        System.out.println(SKF.getModel());
    }

    public void test3(){
        System.out.println("------------------SKF (diagonal matrix) from batches------------------");
        SwitchingKalmanFilter SKF = new SwitchingKalmanFilter(dataGaussians.getAttributes());
        System.out.println(SKF.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : dataGaussians.iterableOverBatches(100)) {
            SKF.updateModel(batch);
        }
        System.out.println(SKF.getModel());
    }
}