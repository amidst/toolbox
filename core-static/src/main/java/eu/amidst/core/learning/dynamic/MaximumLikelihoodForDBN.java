/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.learning.dynamic;

import com.google.common.base.Stopwatch;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.exponentialfamily.EF_DynamicBayesianNetwork;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 06/01/15.
 */
public final class MaximumLikelihoodForDBN {

    private static int batchSize = 1000;
    private static boolean parallelMode = true;

    public static int getBatchSize() {
        return batchSize;
    }

    public static void setBatchSize(int batchSize) {
        MaximumLikelihoodForDBN.batchSize = batchSize;
    }

    public static boolean isParallelMode() {
        return parallelMode;
    }

    public static void setParallelMode(boolean parallelMode) {
        MaximumLikelihoodForDBN.parallelMode = parallelMode;
    }

    public static DynamicBayesianNetwork learnDynamic(DynamicDAG dag, DataStream<DynamicDataInstance> dataStream) {

        EF_DynamicBayesianNetwork efDynamicBayesianNetwork = new EF_DynamicBayesianNetwork(dag);

        Stream<DynamicDataInstance> stream = null;
        if (parallelMode){
            stream = dataStream.parallelStream(batchSize);
        }else{
            stream = dataStream.stream();
        }

        AtomicInteger dataInstanceCount = new AtomicInteger(0);

        SufficientStatistics sumSS = stream
                .peek(w -> {
                    if (w.getTimeID()==0)
                        dataInstanceCount.getAndIncrement();
                })
                .map(efDynamicBayesianNetwork::getSufficientStatistics)
                .reduce(SufficientStatistics::sumVector).get();
                //.reduce(efDynamicBayesianNetwork.createZeroedSufficientStatistics(), SufficientStatistics::sumVector);

        //Normalize the sufficient statistics
        sumSS.divideBy(dataInstanceCount.get());

        efDynamicBayesianNetwork.setMomentParameters(sumSS);
        return efDynamicBayesianNetwork.toDynamicBayesianNetwork(dag);
    }

    public static void main(String[] args){

        List<ArrayVector> vectorList = IntStream.range(0,10).mapToObj(i -> {
            ArrayVector vec = new ArrayVector(2);
            vec.set(0, 1);
            vec.set(1, 1);
            return vec;
        }).collect(Collectors.toList());


        Vector out1 = vectorList.parallelStream()
                .reduce(new ArrayVector(2), (u, v) -> {
                    ArrayVector outvec = new ArrayVector(2);
                    outvec.sum(v);
                    outvec.sum(u);
                    return outvec;});


        Vector out2 = vectorList.parallelStream().reduce(new ArrayVector(2), (u, v) -> {u.sum(v); return u;});
        Vector out3 = vectorList.parallelStream().reduce(new ArrayVector(2), (u, v) -> {v.sum(u); return v;});

        System.out.println(out1.get(0) + ", " + out1.get(1));
        System.out.println(out2.get(0) + ", " + out2.get(1));
        System.out.println(out3.get(0) + ", " + out3.get(1));

        /*
        BayesianNetwork bn=null;

        int nlinks = bn.getDAG().getParentSets()
                .parallelStream()
                .mapToInt(parentSet -> parentSet.getNumberOfParents()).sum();

        nlinks=0;

        for (ParentSet parentSet: bn.getDAG().getParentSets()){
            nlinks+=parentSet.getNumberOfParents();
        }

        for (int i = 0; i < bn.getDAG().getParentSets().size(); i++) {
            nlinks+=bn.getDAG().getParentSets().get(i).getNumberOfParents();
        }*/


        int nSamples = 4000000;
        int sizeSS=1000000;
        int sizeSS2=100;

        double[][] sum = new double[sizeSS][];
        double[][] ss = new double[sizeSS][];

        for (int i = 0; i < 100; i++) {
            /*for (int j = 0; j < ss.length; j++) {
                    ss[j]=new double[sizeSS];
            }*/
            /*
            for (int j = 0; j < ss.length; j++) {
                for (int k = 0; k < ss[j].length; k++) {
                    ss[j][k]=1.0;//Math.random();
                }
            }


            for (int j = 0; j < ss.length; j++) {
                for (int k = 0; k < ss[j].length; k++) {
                    sum[j][k]+=ss[j][k];
                }
            }
*/

            class ArrayVector{
                double[] array;
                public ArrayVector(int size){
                    array = new double[size];
                }
                public double[] getArray(){
                    return this.array;
                }
            }

            Stopwatch watch = Stopwatch.createStarted();
            for (int j = 0; j < sizeSS ; j++) {
                    ArrayVector vex = new ArrayVector(sizeSS2);
                    ss[j]=vex.getArray();
            }
            System.out.println(watch.stop());

            watch = Stopwatch.createStarted();
            for (int j = 0; j < sizeSS ; j++) {
                ss[j]= new double[sizeSS2];
            }
            System.out.println(watch.stop());
            System.out.println();
        }


    }
}
