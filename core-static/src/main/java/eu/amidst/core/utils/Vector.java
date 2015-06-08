/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.utils;

/**
 * Created by andresmasegosa on 12/11/14.
 */
public interface Vector {

    public double get(int i);

    public void set(int i, double val);

    public int size();

    public default double sum() {
        double sum=0;
        for (int i = 0; i < size(); i++) {
            sum+=this.get(i);
        }
        return sum;
    }

    public default void sum(Vector vector){
        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors do not have same size.");

        for (int i = 0; i < vector.size(); i++) {
            this.set(i,this.get(i)+vector.get(i));
        }
    }


    public default void substract(Vector vector){
        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors do not have same size.");

        for (int i = 0; i < vector.size(); i++) {
            this.set(i,this.get(i)-vector.get(i));
        }
    }

    public default void copy(Vector vector){
        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors do not have same size.");

        for (int i = 0; i < vector.size(); i++) {
            this.set(i,vector.get(i));
        }
    }

    public default void divideBy(double val){
        for (int i = 0; i < this.size(); i++) {
            this.set(i,this.get(i)/val);
        }
    }

    public default void multiplyBy(double val){
        for (int i = 0; i < this.size(); i++) {
            this.set(i,this.get(i)*val);
        }
    }

    public default double dotProduct(Vector vector){
        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors do not have same size.");

        double sum=0;
        for (int i = 0; i < vector.size(); i++) {
            sum+=this.get(i)*vector.get(i);
        }

        return sum;
    }

    public default boolean equalsVector(Vector vector, double threshold){

        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors do not have same size.");

        for (int i = 0; i < vector.size(); i++) {
            if (Math.abs(this.get(i) - vector.get(i)) > threshold)
                return false;
        }

        return true;
    }

    public static Vector sumVector(Vector vec1, Vector vec2){
        vec2.sum(vec1);
        return vec2;
    }


}
