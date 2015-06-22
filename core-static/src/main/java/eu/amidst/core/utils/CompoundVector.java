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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CompoundVector implements SufficientStatistics, MomentParameters, NaturalParameters {

    int size;
    List<IndexedVector> baseVectors;

    public CompoundVector(int nVectors, int size1){
        baseVectors = new ArrayList(nVectors);
        this.size = size1;
        for (int i = 0; i < nVectors; i++) {
            baseVectors.add(i, new IndexedVector(i, null));
        }
    }

    public CompoundVector(List<Vector> vectors) {
        baseVectors = new ArrayList(vectors.size());

        for (int i = 0; i < vectors.size(); i++) {
            baseVectors.add(i, new IndexedVector(i, vectors.get(i)));
            size += vectors.get(i).size();
        }
    }

    public static <T> CompoundVector newCompoundVector(List<Map.Entry<Integer,T>> vectors){
        return null;
    }
    public void setVectorByPosition(int position, Vector vec) {
        baseVectors.get(position).setVector(vec);
    }

    public Vector getVectorByPosition(int position) {
        return this.baseVectors.get(position).getVector();
    }

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

    @Override
    public int size() {
        return size;
    }

    @Override
    public void sum(Vector vector) {
        this.sum((CompoundVector) vector);
    }

    @Override
    public void copy(Vector vector) {
        this.copy((CompoundVector) vector);
    }

    @Override
    public void divideBy(double val) {
        this.baseVectors.stream().forEach(w -> w.getVector().divideBy(val));
    }

    @Override
    public double dotProduct(Vector vec) {
        return this.dotProduct((CompoundVector) vec);
    }

    public double dotProduct(CompoundVector vec) {
        return this.baseVectors.stream().mapToDouble(w -> w.getVector().dotProduct(vec.getVectorByPosition(w.getIndex()))).sum();
    }

    public void copy(CompoundVector vector) {
        if (vector.size() != this.size())
            throw new IllegalArgumentException("Error in variable Vector. Method copy. The parameter vec has a different size. ");

        this.baseVectors.stream().forEach(w -> w.getVector().copy(vector.getVectorByPosition(w.getIndex())));

    }

    public void sum(CompoundVector vector) {
        this.baseVectors.stream().forEach(w -> w.getVector().sum(vector.getVectorByPosition(w.getIndex())));
    }


    static class IndexedVector {
        Vector vector;
        int index;

        IndexedVector(int index1, Vector vec1) {
            this.vector = vec1;
            this.index = index1;
        }

        public Vector getVector() {
            return vector;
        }

        public int getIndex() {
            return index;
        }

        public void setVector(Vector vector) {
            this.vector = vector;
        }
    }

    @FunctionalInterface
    public interface VectorBuilder {
        public Vector createZeroedVector();
    }

}