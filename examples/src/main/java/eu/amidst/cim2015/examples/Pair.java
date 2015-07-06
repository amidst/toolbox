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

package eu.amidst.cim2015.examples;


import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemoryListContainer;

import java.util.Comparator;
import java.util.stream.IntStream;

/**
 * Created by andresmasegosa on 6/7/15.
 */
public class Pair
{
    private DataInstance centroid;
    private DataInstance dataInstance;

    public DataInstance getCentroid() {
        return centroid;
    }

    public void setCentroid(DataInstance centroid) {
        this.centroid = centroid;
    }

    public DataInstance getDataInstance() {
        return dataInstance;
    }

    public void setDataInstance(DataInstance dataInstance) {
        this.dataInstance = dataInstance;
    }

    public Pair(DataInstance centroid_, DataInstance dataInstance_){
        centroid = centroid_;
        dataInstance = dataInstance_;
    }

    public static Pair newPair(DataOnMemoryListContainer<DataInstance> centroids, DataInstance dataInstance){
        DataInstance centroid = centroids.stream().min(Comparator.comparing(c -> getED(c, dataInstance))).get();
        return new Pair(centroid,dataInstance);
    }

    public double[] getDataInstanceToArray(){
        return dataInstance.toArray();
    }

    public double[] getCentroidToArray(){
        return centroid.toArray();
    }

    public double[] getAverage(double[] aux){
        return null;//new double[atts.getNumberOfAttributes()];
    }

    /*Calculate Euclidean Distance*/
    public static double getED(DataInstance e1, DataInstance e2){

        double sum =
                IntStream.rangeClosed(0, e1.getAttributes().getNumberOfAttributes() - 1)
                        .mapToDouble(i ->
                                    {Attribute att = e1.getAttributes().getList().get(i);
                                        return Math.pow(e1.getValue(att)-e2.getValue(att),2);})
                        .sum();

        return Math.sqrt(sum);
    }

}
