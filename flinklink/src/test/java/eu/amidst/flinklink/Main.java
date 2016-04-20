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

package eu.amidst.flinklink;

import java.text.DecimalFormat;
import java.util.Random;

/**
 * Created by andresmasegosa on 29/9/15.
 */
public class Main {

    public static void main(String[] args){

        Random random = new Random();
        DecimalFormat format = new DecimalFormat("#");
        for (int i = 0; i < 100; i++) {
            int val = random.nextInt();
            double value = (double) random.nextDouble();
            System.out.println(val);
            System.out.println(value);
            System.out.println(format.format(value));
            System.out.println();
        }
    }
}
