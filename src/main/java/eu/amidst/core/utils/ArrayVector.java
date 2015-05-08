/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.utils;

import eu.amidst.core.exponentialfamily.MomentParameters;
import eu.amidst.core.exponentialfamily.NaturalParameters;
import eu.amidst.core.exponentialfamily.SufficientStatistics;

/**
 * Created by andresmasegosa on 12/11/14.
 */
public class ArrayVector implements MomentParameters, NaturalParameters, SufficientStatistics{

    private double[] array;

    public ArrayVector(int size){
        this.array = new double[size];
    }

    public ArrayVector(double[] vec){
        this.array=vec;
    }

    @Override
    public double get(int i){
        return this.array[i];
    }

    @Override
    public void set(int i, double val){
        this.array[i]=val;
    }

    @Override
    public int size(){
        return this.array.length;
    }

    @Override
    public void sum(Vector vector) {
        for (int i = 0; i < vector.size(); i++) {
            this.array[i]+=vector.get(i);
        }
    }

    @Override
    public void substract(Vector vector) {
        for (int i = 0; i < vector.size(); i++) {
            this.array[i]-=vector.get(i);
        }
    }

    @Override
    public void copy(Vector vector){
        this.copy((ArrayVector)vector);
    }

    public double[] toArray(){
        return this.array;
    }

    public void copy(ArrayVector vector){
        if (vector.size()!=vector.size())
            throw new IllegalArgumentException("Vectors with different sizes");
        System.arraycopy(vector.toArray(),0,this.array,0,vector.toArray().length);
    }

    @Override
    public void divideBy(double val){
        for (int i = 0; i < this.array.length ; i++) {
            this.array[i]/=val;
        }
    }

    public void multiplyBy(double val){
        for (int i = 0; i < this.array.length ; i++) {
            this.array[i]*=val;
        }
    }
    @Override
    public double dotProduct(Vector vector) {
        double sum=0;
        for (int i = 0; i < vector.size(); i++) {
            sum+=this.array[i]*vector.get(i);
        }
        return sum;
    }

}
