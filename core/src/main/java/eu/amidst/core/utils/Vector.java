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

/**
 * This interface defines some Vector utility methods.
 */
public interface Vector {

    /**
     * Returns the value of the element in a given position i.
     * @param i an {@code int} that represents the position.
     * @return a {@code double} that represents the extracted value.
     */
    double get(int i);

    /**
     * Sets the value of the element in a given position i to the input {@code double} value.
     * @param i an {@code int} that represents the position.
     * @param val an {@code double} value.
     */
    void set(int i, double val);

    /**
     * Returns the size.
     * @return an {@code int} that represents the size.
     */
    int size();

    /**
     * Returns the sumNonStateless of all elements.
     * @return a {@code double} that represents the sumNonStateless of all elements.
     */
    default double sum() {
        double sum=0;
        for (int i = 0; i < size(); i++) {
            sum+=this.get(i);
        }
        return sum;
    }

    /**
     * Updates the values of this Vector as a sumNonStateless of its initial values and the input vector values.
     * @param vector an input Vector.
     */
    default void sum(Vector vector){
        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors do not have same size.");

        for (int i = 0; i < vector.size(); i++) {
            this.set(i,this.get(i)+vector.get(i));
        }
    }

    /**
     * Updates the values of this Vector as a subtraction of the input vector values from its initial values.
     * @param vector an input Vector.
     */
    default void substract(Vector vector){
        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors do not have same size.");

        for (int i = 0; i < vector.size(); i++) {
            this.set(i,this.get(i)-vector.get(i));
        }
    }

    /**
     * Copies the input source Vector to this Vector.
     * @param vector an input Vector.
     */
    default void copy(Vector vector){
        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors do not have same size.");

        for (int i = 0; i < vector.size(); i++) {
            this.set(i,vector.get(i));
        }
    }

    /**
     * Updates the values of this Vector via dividing its initial values by an input {@code double} value.
     * @param val an input {@code double} value.
     */
    default void divideBy(double val){
        for (int i = 0; i < this.size(); i++) {
            this.set(i,this.get(i)/val);
        }
    }

    /**
     * Updates the values of this Vector via multiplying its initial values by an input {@code double} value.
     * @param val an input {@code double} value.
     */
    default void multiplyBy(double val){
        for (int i = 0; i < this.size(); i++) {
            this.set(i,this.get(i)*val);
        }
    }

    /**
     * Updates the values of this Vector via summing its initial values by an input {@code double} value.
     * @param val an input {@code double} value.
     */
    default void sumConstant(double val){
        for (int i = 0; i < this.size(); i++) {
            this.set(i,this.get(i)+val);
        }
    }

    /**
     * Returns the dot product of this Vector and an input vector, defined as
     * the sumNonStateless of the pairwise products of the values of the two vectors.
     * @param vector an input vector.
     * @return a double that represents the dot product of the two vectors.
     */
    default double dotProduct(Vector vector){
        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors do not have same size.");

        double sum=0;
        for (int i = 0; i < vector.size(); i++) {
            sum+=this.get(i)*vector.get(i);
        }

        return sum;
    }

    /**
     * Tests if this Vector is equal to an input vector given a threshold.
     * @param vector an input vector.
     * @param threshold a threshold.
     * @return {@code true} if the two vectors ae equal, {@code false} otherwise.
     */
    default boolean equalsVector(Vector vector, double threshold){

        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors do not have same size.");

        for (int i = 0; i < vector.size(); i++) {
            if (Math.abs(this.get(i) - vector.get(i)) > threshold)
                return false;
        }

        return true;
    }

    /**
     * Sums two vectors.
     * @param vec1 an input Vector.
     * @param vec2 a destination Vector.
     * @return the destination vector containing the sumNonStateless of the two vectors.
     */
    static Vector sumVector(Vector vec1, Vector vec2){
        vec2.sum(vec1);
        return vec2;
    }

    default String output(){
        StringBuilder stringBuilder = new StringBuilder("{");
        for (int i = 0; i < this.size(); i++) {
            stringBuilder.append(this.get(i));
            stringBuilder.append(", ");
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

}
