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

package eu.amidst.core.utils;

import junit.framework.TestCase;

/**
 * Created by andresmasegosa on 2/5/16.
 */
public class SparseVectorDefaultValueTest extends TestCase {

    public static void test1() {
        SparseVectorDefaultValue vectorA = new SparseVectorDefaultValue(10,0.0);
        SparseVectorDefaultValue vectorB = new SparseVectorDefaultValue(10,0.0);

        vectorA.set(0,100.0);
        vectorA.set(1,100.0);

        vectorB.set(1,100.0);
        vectorB.set(8,100.0);

        assertEquals(100.0,vectorA.get(0));
        assertEquals(100.0,vectorA.get(1));
        assertEquals(100.0,vectorB.get(1));
        assertEquals(100.0,vectorB.get(8));
        assertEquals(0.0,vectorB.get(5));

        assertEquals(200.0,vectorB.sum());

        vectorA.sum(vectorB);

        assertEquals(100.0,vectorA.get(0));
        assertEquals(200.0,vectorA.get(1));
        assertEquals(100.0,vectorA.get(8));
        assertEquals(0.0,vectorA.get(5));

        vectorA.substract(vectorB);

        assertEquals(100.0,vectorA.get(0));
        assertEquals(100.0,vectorA.get(1));
        assertEquals(100.0,vectorB.get(1));
        assertEquals(100.0,vectorB.get(8));
        assertEquals(0.0,vectorB.get(5));

        vectorA.multiplyBy(10);
        assertEquals(1000.0,vectorA.get(0));
        assertEquals(1000.0,vectorA.get(1));
        assertEquals(0.0,vectorA.get(5));


        vectorA.divideBy(10);
        assertEquals(100.0,vectorA.get(0));
        assertEquals(100.0,vectorA.get(1));
        assertEquals(0.0,vectorA.get(5));

        assertEquals(10000.0,vectorA.dotProduct(vectorB));





        vectorA.copy(vectorB);
        assertEquals(100.0,vectorB.get(1));
        assertEquals(100.0,vectorB.get(8));
        assertEquals(0.0,vectorB.get(5));





    }


    public static void test2() {
        SparseVectorDefaultValue vectorA = new SparseVectorDefaultValue(10,1.0);
        SparseVectorDefaultValue vectorB = new SparseVectorDefaultValue(10,1.0);

        vectorA.set(0,100.0);
        vectorA.set(1,100.0);

        vectorB.set(1,100.0);
        vectorB.set(8,100.0);

        assertEquals(100.0,vectorA.get(0));
        assertEquals(100.0,vectorA.get(1));
        assertEquals(100.0,vectorB.get(1));
        assertEquals(100.0,vectorB.get(8));
        assertEquals(1.0,vectorA.get(5));
        assertEquals(1.0,vectorB.get(5));

        assertEquals(208.0,vectorB.sum());

        vectorA.sum(vectorB);

        assertEquals(101.0,vectorA.get(0));
        assertEquals(200.0,vectorA.get(1));
        assertEquals(101.0,vectorA.get(8));
        assertEquals(2.0,vectorA.get(5));

        vectorA.substract(vectorB);

        assertEquals(100.0,vectorA.get(0));
        assertEquals(100.0,vectorA.get(1));
        assertEquals(100.0,vectorB.get(1));
        assertEquals(100.0,vectorB.get(8));
        assertEquals(1.0,vectorA.get(5));
        assertEquals(1.0,vectorB.get(5));

        vectorA.multiplyBy(10);
        assertEquals(1000.0,vectorA.get(0));
        assertEquals(1000.0,vectorA.get(1));
        assertEquals(10.0,vectorA.get(5));


        vectorA.divideBy(10);
        assertEquals(100.0,vectorA.get(0));
        assertEquals(100.0,vectorA.get(1));
        assertEquals(1.0,vectorA.get(5));

        assertEquals(10000.0 + 100 + 100 + 7,vectorA.dotProduct(vectorB));



        vectorA.copy(vectorB);
        assertEquals(100.0,vectorB.get(1));
        assertEquals(100.0,vectorB.get(8));
        assertEquals(1.0,vectorB.get(5));





    }

}