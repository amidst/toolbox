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

package eu.amidst.cim2015.examples;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Hanen on 12/06/15.
 */
public class MyIntegerUtils {


    public interface NbrPredicate{
        boolean test(Integer integer);
    }

    public static boolean isEven(Integer i){
        return i%2==0;
    }

    public static boolean isOdd(Integer i){
        return i%2==1;
    }

    public static int filterList(List<Integer> input, NbrPredicate predicate){
        int count = 0;
        for (Integer i : input){
            if (predicate.test(i))
               count++;
        }
        return count;
    }

    public static void main(String[] args) throws Exception {

        NbrPredicate preEven = new NbrPredicate() {
            @Override
            public boolean test(Integer integer) {
                return isEven(integer);
            }
        };

        NbrPredicate preOdd = new NbrPredicate() {
            @Override
            public boolean test(Integer integer) {
                return isOdd(integer);
            }
        };



        List<Integer> list= Arrays.asList(0, 3, 5, 6, 8, 10, 12, 15);

        System.out.println("The count of Even numbers is " + MyIntegerUtils.filterList(list, preEven));
        System.out.println("The count of Odd numbers is " +  MyIntegerUtils.filterList(list, preOdd));

        System.out.println("The count of Even numbers is " + MyIntegerUtils.filterList(list, MyIntegerUtils::isEven));
        System.out.println("The count of Odd numbers is " +  MyIntegerUtils.filterList(list, MyIntegerUtils::isOdd));
    }
}
