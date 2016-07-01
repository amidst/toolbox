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
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the interfaces {@link MomentParameters}, {@link NaturalParameters}, and {@link SufficientStatistics}.
 * It handles some compound vector utility methods.
 */
public class CompoundVector implements MomentParameters, NaturalParameters, SufficientStatistics, Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = -3436599636425587512L;

    /** Represents the total size of the CompoundVector, defined as the sumNonStateless of its baseVector sizes. */
    int size;

    /** Represents the list of base vectors. */
    List<IndexedVector> baseVectors;

    /**
     * Creates a new CompoundVector for a given size and number of vectors.
     * @param nVectors an {@code int} that represents the number of vectors in this CompoundVector.
     * @param size1 an {@code int} that represents the size of this CompoundVector.
     */
    public CompoundVector(int nVectors, int size1){
        baseVectors = new ArrayList(nVectors);
        this.size = size1;
        for (int i = 0; i < nVectors; i++) {
            baseVectors.add(i, new IndexedVector(i, null));
        }
    }

    /**
     * Creates a new CompoundVector from a given list of vectors.
     * @param vectors a {@code List} of {@link eu.amidst.core.utils.Vector}s.
     */
    public CompoundVector(List<Vector> vectors) {
        baseVectors = new ArrayList(vectors.size());

        for (int i = 0; i < vectors.size(); i++) {
            baseVectors.add(i, new IndexedVector(i, vectors.get(i)));
            size += vectors.get(i).size();
        }
    }

    /**
     * Returns the number of base vectors
     * @return a positive integer number.
     */
    public int getNumberOfBaseVectors(){
        return this.baseVectors.size();
    }

    /**
     * Sets a vector at a given position in this CompoundVector.
     * @param position an {@code int} that represents the position of the vector to be set.
     * @param vec the {@link Vector} object to be set.
     */
    public void setVectorByPosition(int position, Vector vec) {
        baseVectors.get(position).setVector(vec);
    }

    /**
     * Returns a vector located at a given position in this CompoundVector.
     * @param position an {@code int} that represents the position of the vector to be extracted.
     * @return the extracted {@link Vector} object.
     */
    public Vector getVectorByPosition(int position) {
        return this.baseVectors.get(position).getVector();
    }


    /**
     * Returns a list of Vectors.
     */
    public List<Vector> getVectors(){
        List<Vector> vectors = new ArrayList();
        for (int i = 0; i < baseVectors.size(); i++) {
            vectors.add(this.getVectorByPosition(i));
        }
        return vectors;
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
                return;
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
    public void substract(Vector vector) {
        this.substract((CompoundVector) vector);
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
        this.baseVectors.stream().forEach(w -> w.getVector().divideBy(val));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void multiplyBy(double val){
        this.baseVectors.stream().forEach(w -> w.getVector().multiplyBy(val));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double dotProduct(Vector vec) {
        return this.dotProduct((CompoundVector) vec);
    }

    /**
     * Returns the dot product of this CompoundVector and an input CompoundVector, defined as
     * the sumNonStateless of the pairwise products of the values of the two CompoundVectors.
     * @param vec an input CompoundVector.
     * @return a double that represents the dot product of the two CompoundVectors.
     */
    public double dotProduct(CompoundVector vec) {
        return this.baseVectors.stream().mapToDouble(w -> w.getVector().dotProduct(vec.getVectorByPosition(w.getIndex()))).sum();
    }

     /**
     * Copies the input source CompoundVector object to this CompoundVector.
     * @param vector an input CompoundVector object.
     */
    public void copy(CompoundVector vector) {
        if (vector.size() != this.size())
            throw new IllegalArgumentException("Error in variable Vector. Method copy. The input parameter vector has a different size.");

        this.baseVectors.stream().forEach(w -> w.getVector().copy(vector.getVectorByPosition(w.getIndex())));
    }

    /**
     * Updates the values of this CompoundVector as a sumNonStateless of its initial values and the input CompoundVector values.
     * @param vector an input CompoundVector.
     */
    public void sum(CompoundVector vector) {
        this.baseVectors.stream().forEach(w -> w.getVector().sum(vector.getVectorByPosition(w.getIndex())));
    }

    /**
     * Updates the values of this CompoundVector as a substractNonStateless of its initial values and the input CompoundVector values.
     * @param vector an input CompoundVector.
     */
    public void substract(CompoundVector vector) {
        this.baseVectors.stream().forEach(w -> w.getVector().substract(vector.getVectorByPosition(w.getIndex())));
    }

    /**
     * This class handles some utils for an Indexed Vector.
     */
    static class IndexedVector implements Serializable{

        /** Represents the serial version ID for serializing the object. */
        private static final long serialVersionUID = -3436599636425587512L;

        /** Represents a {@link Vector} object. */
        Vector vector;

        /** Represents the {@code int} index of this Vector. */
        int index;

        /**
         * Creates a new IndexedVector for a given vector and index.
         * @param index1 an input index.
         * @param vec1 an input  vector.
         */
        IndexedVector(int index1, Vector vec1) {
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
         * @return an {@code int} that represents the index.
         */
        public int getIndex() {
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