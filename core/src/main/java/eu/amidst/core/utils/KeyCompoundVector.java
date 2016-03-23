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
import java.util.HashMap;
import java.util.Map;

/**
 * This class implements the interfaces {@link MomentParameters}, {@link NaturalParameters}, and {@link SufficientStatistics}.
 * It handles some key compound vector utility methods.
 */
public class KeyCompoundVector<E> implements MomentParameters, NaturalParameters, SufficientStatistics, Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = -3436599636425587512L;

    /** Represents the total size of this KeyCompoundVector. */
    int size;

    /** Represents the collection of base vectors. */
    Map<E,IndexedVector<E>> baseVectors;

    /**
     * Creates a new KeyCompoundVector.
     */
    public KeyCompoundVector() {
        baseVectors = new HashMap();
        this.size=0;
    }

    /**
     * Adds a vector to this KeyCompoundVector.
     * @param key a value of the corresponding type of elements.
     * @param vector a {@link Vector} object.
     */
    public void addVector(E key, Vector vector){
        this.baseVectors.put(key,new IndexedVector<E>(key, vector));
        this.size+=vector.size();
    }

    /**
     * Sets a vector at a given position in this KeyCompoundVector.
     * @param key the position of the vector to be set.
     * @param vec the {@link Vector} object to be set.
     */
    public void setVectorByPosition(E key, Vector vec) {
        baseVectors.get(key).setVector(vec);
    }

    /**
     * Returns a vector located at a given position in this KeyCompoundVector.
     * @param key the position of the vector to be extracted.
     * @return the extracted {@link Vector} object.
     */
    public Vector getVectorByPosition(E key) {
        return this.baseVectors.get(key).getVector();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double get(int i) {
        int total = 0;
        for (int j = 0; j < this.baseVectors.size(); j++) {
            if (i < (total + this.baseVectors.get(j).getVector().size())) {
                return this.baseVectors.get(j).getVector().get(i - total);
            } else {
                total += this.baseVectors.get(j).getVector().size();
            }
        }
        return Double.NaN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void set(int i, double val) {
        int total = 0;
        for (int j = 0; j < this.baseVectors.size(); j++) {
            if (i < (total + this.baseVectors.get(j).getVector().size())) {
                this.baseVectors.get(j).getVector().set(i - total, val);
            } else {
                total += this.baseVectors.get(j).getVector().size();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sum(Vector vector) {
        this.sum((CompoundVector) vector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(Vector vector) {
        this.copy((CompoundVector) vector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void divideBy(double val) {
        this.baseVectors.values().stream().forEach(w -> w.getVector().divideBy(val));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double dotProduct(Vector vec) {
        return this.dotProduct((CompoundVector) vec);
    }

    /**
     * Returns the dot product of this KeyCompoundVector and an input KeyCompoundVector, defined as
     * the sumNonStateless of the pairwise products of the values of the two KeyCompoundVectors.
     * @param vec an input KeyCompoundVector.
     * @return a double that represents the dot product of the two KeyCompoundVectors.
     */
    public double dotProduct(KeyCompoundVector vec) {
        return this.baseVectors.values().stream().mapToDouble(w -> w.getVector().dotProduct(vec.getVectorByPosition(w.getIndex()))).sum();
    }

    /**
     * Copies the input source KeyCompoundVector object to this KeyCompoundVector.
     * @param vector an input KeyCompoundVector object.
     */
    public void copy(KeyCompoundVector vector) {
        if (vector.size() != this.size())
            throw new IllegalArgumentException("Error in variable Vector. Method copy. The parameter vec has a different size. ");

        this.baseVectors.values().stream().forEach(w -> w.getVector().copy(vector.getVectorByPosition(w.getIndex())));

    }

    /**
     * Updates the values of this KeyCompoundVector as a sumNonStateless of its initial values and the input KeyCompoundVector values.
     * @param vector an input KeyCompoundVector.
     */
    public void sum(KeyCompoundVector vector) {
        this.baseVectors.values().stream().forEach(w -> w.getVector().sum(vector.getVectorByPosition(w.getIndex())));
    }

    /**
     * This generic class handles some utils for an Indexed Vector.
     * @param <E> the type of elements or indices.
     */
    static class IndexedVector<E> {
        /** Represents a {@link Vector} object. */
        Vector vector;

        /** Represents the index of this Vector, defined as an instance of a generic element E. */
        E index;

        /**
         * Creates a new IndexedVector for a given vector and index.
         * @param index1 an input index.
         * @param vec1 an input  vector.
         */
        IndexedVector(E index1, Vector vec1) {
            this.vector = vec1;
            this.index = index1;
        }

        /**
         * Returns the vector of this IndexedVector.
         * @return a {@link Vector}.
         */
        public Vector getVector() {
            return vector;
        }

         /**
         * Returns the index of this IndexedVector.
         * @return the index of generic element E.
         */
        public E getIndex() {
            return index;
        }

        /**
         * Sets a vector in this IndexedVector.
         * @param vector the {@link Vector} object to be set.
         */
        public void setVector(Vector vector) {
            this.vector = vector;
        }
    }

}