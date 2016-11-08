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

package eu.amidst.core.utils;

import eu.amidst.core.exponentialfamily.MomentParameters;
import eu.amidst.core.exponentialfamily.NaturalParameters;
import eu.amidst.core.exponentialfamily.SufficientStatistics;

import java.io.Serializable;

/**
 * This class implements the interfaces {@link MomentParameters}, {@link NaturalParameters}, and {@link SufficientStatistics}.
 * It handles some array vector utility methods.
 */
public class ArrayVector implements MomentParameters, NaturalParameters, SufficientStatistics, Serializable{

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = -3436599636425587512L;

    /** Represents an array of {@code double}. */
    private double[] array;

    /**
     * Creates a new array vector given an {@code int} size.
     * @param size the size of the array vector.
     */
    public ArrayVector(int size){
        this.array = new double[size];
    }

    /**
     * Creates a new array vector given an array of {@code double}.
     * @param vec an array of {@code double}.
     */
    public ArrayVector(double[] vec){
        this.array=vec;
    }


    /**
     * Converts this ArrayVector to an array of {@code double}.
     * @return an array of {@code double}.
     */
    public double[] toArray(){
        return this.array;
    }

    /**
     * Copies the input source vector to this ArrayVector.
     * @param vector an input source ArrayVector object.
     */
    public void copy(ArrayVector vector){
        if (vector.size()!=vector.size())
            throw new IllegalArgumentException("Vectors with different sizes");
        System.arraycopy(vector.toArray(),0,this.array,0,vector.toArray().length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double get(int i){
        return this.array[i];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void set(int i, double val){
        this.array[i]=val;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size(){
        return this.array.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sum(Vector vector) {
        if (vector.getClass().isAssignableFrom(ArrayVector.class))
            sum((ArrayVector)vector);
        else if (vector.getClass().isAssignableFrom(SparseVectorDefaultValue.class))
            sum((SparseVectorDefaultValue)vector);
        else
            throw new UnsupportedOperationException("Non supported operation");    }


    public void sum(SparseVectorDefaultValue vector) {

        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors has different sizes");

        if (vector.getDefaultValue()!=0)
            throw new UnsupportedOperationException("Non supported operation");

        for (Integer i : vector.getNonZeroEntries()) {
            this.array[i]+=vector.get(i);
        }

    }

    public void sum(ArrayVector vector) {
        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors has different sizes");

        for (int i = 0; i < vector.size(); i++) {
            this.array[i]+=vector.array[i];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void substract(Vector vector) {
        substract((ArrayVector)vector);
    }


    public void substract(ArrayVector vector) {
        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors has different sizes");

        for (int i = 0; i < vector.size(); i++) {
            this.array[i]-=vector.array[i];
        }
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(Vector vector){
        this.copy((ArrayVector)vector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void divideBy(double val){
        for (int i = 0; i < this.array.length ; i++) {
            this.array[i]/=val;
        }
    }

    /**
     /**
     * {@inheritDoc}
     */
    @Override
    public void multiplyBy(double val){
        for (int i = 0; i < this.array.length ; i++) {
            this.array[i]*=val;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double dotProduct(Vector vector) {
        if (vector.getClass().isAssignableFrom(ArrayVector.class))
            return dotProduct((ArrayVector)vector);
        else if (vector.getClass().isAssignableFrom(SparseVectorDefaultValue.class))
            return dotProduct((SparseVectorDefaultValue)vector);
        else
            throw new UnsupportedOperationException("Non supported operation");
    }


    public double dotProduct(SparseVectorDefaultValue vector) {

        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors has different sizes");

        if (vector.getDefaultValue()!=0)
            throw new UnsupportedOperationException("Non supported operation");

        double sum=0;
        for (Integer i : vector.getNonZeroEntries()) {
            sum+=this.array[i]*vector.get(i);
        }

        return sum;
    }

    public double dotProduct(ArrayVector vector) {
        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors has different sizes");

        double sum=0;
        for (int i = 0; i < vector.size(); i++) {
            sum+=this.array[i]*vector.array[i];
        }
        return sum;
    }
}