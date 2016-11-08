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
import eu.amidst.latentvariablemodels.dynamicmodels.KalmanFilter;
import junit.framework.TestCase;

/**
 * Created by ana@cs.aau.dk on 08/03/16.
 */
public class KalmanFilterTest extends TestCase{
    private static DataStream<DynamicDataInstance> dataGaussians;
    private static boolean setUpIsDone = false;

    protected void setUp(){
        if (setUpIsDone) {
            return;
        }
        dataGaussians = DataSetGenerator.generate(1,1000,0,3);
        setUpIsDone = true;
    }

    public void test1(){
        System.out.println("------------------KF (diagonal matrix) from streaming------------------");
        KalmanFilter KF = new KalmanFilter(dataGaussians.getAttributes());
        KF.setNumHidden(2);
        System.out.println(KF.getDynamicDAG());
        KF.updateModel(dataGaussians);
        System.out.println(KF.getModel());
    }
    public void test2(){
        System.out.println("------------------KF (full cov. matrix) from streaming------------------");
        KalmanFilter KF = new KalmanFilter(dataGaussians.getAttributes());
        KF.setDiagonal(false);
        System.out.println(KF.getDynamicDAG());
        KF.updateModel(dataGaussians);
        System.out.println(KF.getModel());
    }

    public void test3(){
        System.out.println("------------------KF (diagonal matrix) from batches------------------");
        KalmanFilter KF = new KalmanFilter(dataGaussians.getAttributes());
        System.out.println(KF.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : dataGaussians.iterableOverBatches(100)) {
            KF.updateModel(batch);
        }
        System.out.println(KF.getModel());
    }

}