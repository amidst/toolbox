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

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 08/07/15.
 */
public class ParallelKMeans {

    static int k = 3;
    static final Double epsilon = 0.00001;
    static int batchSize = 100;


    public static int getK() {
        return k;
    }

    public static void setK(int k) {
        ParallelKMeans.k = k;
    }

    public static int getBatchSize() {
        return batchSize;
    }

    public static void setBatchSize(int batchSize) {
        ParallelKMeans.batchSize = batchSize;
    }

    public static double[][] learnKMeans(int k, DataStream<DataInstance> data){

        setK(k);
        Attributes atts = data.getAttributes();

        double[][] centroids = new double[getK()][atts.getNumberOfAttributes()];

        AtomicInteger index = new AtomicInteger();
        data.stream().limit(getK()).forEach(dataInstance -> centroids[index.getAndIncrement()]=dataInstance.toArray());
        data.restart();

        boolean change = true;

        while(change){

            Map<Integer, Averager> newCentroidsAv =
                    data.parallelStream(batchSize)
                            .map(instance -> Pair.newPair(centroids, instance))
                            .collect(Collectors.groupingByConcurrent(Pair::getClusterID,
                                    Collectors.reducing(new Averager(atts.getNumberOfAttributes()), p -> new Averager(p.getDataInstance()), Averager::combine)));


            double error = IntStream.rangeClosed(0, centroids.length - 1).mapToDouble( i -> {
                double distance = Pair.getED(centroids[i], newCentroidsAv.get(i).average());
                centroids[i]=newCentroidsAv.get(i).average();
                return distance;
            }).average().getAsDouble();


            if (error<epsilon)
                change = false;

            data.restart();
        }

        return centroids;
    }



    public static class Pair
    {

        private Integer clusterID;

        private DataInstance dataInstance;

        public Integer getClusterID() {
            return clusterID;
        }

        public DataInstance getDataInstance() {
            return dataInstance;
        }


        public Pair(Integer clusterID_, DataInstance dataInstance_){
            clusterID = clusterID_;
            dataInstance = dataInstance_;
        }


        public static Pair newPair(double[][] centroids, DataInstance dataInstance){

            int clusterID = IntStream.rangeClosed(0,centroids.length-1)
                    .mapToObj(i -> new Integer(i))
                    .min(Comparator.comparing(i -> getED(centroids[i], dataInstance.toArray()))).get().intValue();

            return new Pair(clusterID,dataInstance);
        }


        /*Calculate Euclidean Distance*/
        public static double getED(double[] e1, double[] e2){

            double sum =
                    IntStream.rangeClosed(0, e1.length - 1)
                            .mapToDouble(i -> Math.pow(e1[i] - e2[i], 2))
                            .sum();

            return Math.sqrt(sum);
        }

    }

    public static class Averager
    {
        private double[] total;
        private int count = 0;

        public Averager(int nAtts){
            total= new double[nAtts];
            count = 0;
        }

        public Averager(DataInstance dataInstance){
            total=dataInstance.toArray();
            count = 1;
        }

        public double[] average() {
            return count > 0? Arrays.stream(total).map(val -> val / count).toArray() : new double[0];
        }


        public Averager combine(Averager other) {
            Averager combination = new Averager(this.total.length);
            combination.total = IntStream.rangeClosed(0,other.total.length-1).mapToDouble(i->this.total[i]+other.total[i]).toArray();
            combination.count = this.count + other.count;
            return combination;
        }
    }
}
