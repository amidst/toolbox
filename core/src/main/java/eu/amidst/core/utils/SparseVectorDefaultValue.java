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

package eu.amidst.core.utils;

import eu.amidst.core.exponentialfamily.MomentParameters;
import eu.amidst.core.exponentialfamily.NaturalParameters;
import eu.amidst.core.exponentialfamily.SufficientStatistics;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Created by andresmasegosa on 2/5/16.
 */
public class SparseVectorDefaultValue implements Vector, NaturalParameters, MomentParameters, SufficientStatistics, Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    Map<Integer, Double> values;
    final int dimension;
    double defaultValue = 0;


    public SparseVectorDefaultValue(int dimension, double defaultValue) {
        this.dimension = dimension;
        this.values = new HashMap<>();
        this.defaultValue = defaultValue;
    }

    public double getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(double defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void reset() {
        this.values = new HashMap<>();
    }

    public int getNDefaultValues() {
        return this.dimension - this.values.keySet().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double get(int i) {
        if (this.values.containsKey(i))
            return this.values.get(i);
        else
            return this.defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void set(int i, double val) {
        this.values.put(i, val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return this.dimension;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double sum() {
        double sum = 0;
        for (Double aDouble : values.values()) {
            sum += aDouble;
        }

        sum += this.defaultValue * this.getNDefaultValues();

        return sum;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sum(Vector vector) {
        SparseVectorDefaultValue sparseVector = (SparseVectorDefaultValue) vector;

        if (sparseVector.getDefaultValue()==0){
            for (Integer integer : sparseVector.getNonZeroEntries()) {
                if (Math.abs(sparseVector.get(integer))>0.00001)
                    this.set(integer, this.get(integer) + sparseVector.get(integer));
            }
        }else {
            for (Integer integer : this.getNonZeroEntries()) {
                this.set(integer, this.get(integer) + sparseVector.get(integer));
            }

            for (Integer integer : sparseVector.getNonZeroEntries()) {
                if (!this.values.containsKey(integer))
                    this.set(integer, this.getDefaultValue() + sparseVector.get(integer));
            }

            this.setDefaultValue(this.getDefaultValue() + sparseVector.getDefaultValue());
        }
    }

    @Override
    public void substract(Vector vector) {
        SparseVectorDefaultValue sparseVector = (SparseVectorDefaultValue) vector;

        for (Integer integer : this.getNonZeroEntries()) {
            this.set(integer, this.get(integer) - sparseVector.get(integer));
        }

        for (Integer integer : sparseVector.getNonZeroEntries()) {
            if (!this.values.containsKey(integer))
                this.set(integer, this.defaultValue - sparseVector.get(integer));
        }

        this.setDefaultValue(this.getDefaultValue() - sparseVector.getDefaultValue());

    }

    @Override
    public void copy(Vector vector) {
        SparseVectorDefaultValue sparseVector = (SparseVectorDefaultValue) vector;
        this.values= new HashMap<>(sparseVector.values);
        this.defaultValue = sparseVector.getDefaultValue();
    }

    @Override
    public void divideBy(double val) {
        for (Integer integer : this.getNonZeroEntries()) {
            this.set(integer, this.get(integer) / val);
        }

        this.setDefaultValue(this.getDefaultValue() / val);
    }

    @Override
    public void multiplyBy(double val) {
        for (Integer integer : this.getNonZeroEntries()) {
            this.set(integer, this.get(integer) * val);
        }

        this.setDefaultValue(this.getDefaultValue() * val);

    }

    @Override
    public void sumConstant(double val) {
        for (Integer integer : this.getNonZeroEntries()) {
            this.set(integer, this.get(integer) + val);
        }

        this.setDefaultValue(this.getDefaultValue() + val);

    }

    @Override
    public double dotProduct(Vector vector) {
        SparseVectorDefaultValue sparseVector = (SparseVectorDefaultValue) vector;


        if (sparseVector.getDefaultValue()==0){
            double sum = 0;
            for (Integer integer : sparseVector.getNonZeroEntries()) {
                    sum += this.get(integer) * sparseVector.get(integer);
            }
            return sum;
        }else if (this.getDefaultValue()==0){
            double sum = 0;
            for (Integer integer : this.getNonZeroEntries()) {
                sum += this.get(integer) * sparseVector.get(integer);
            }
            return sum;
        }else{
            double sum = 0;
            double cont = this.dimension;
            for (Integer integer : this.getNonZeroEntries()) {
                if (!sparseVector.values.containsKey(integer)) {
                    sum += this.get(integer) * sparseVector.getDefaultValue();
                    cont--;
                } else {
                    sum += this.get(integer) * sparseVector.get(integer);
                    cont--;
                }
            }

            for (Integer integer : sparseVector.getNonZeroEntries()) {
                if (!this.values.containsKey(integer)) {
                    sum += this.getDefaultValue() * sparseVector.get(integer);
                    cont--;
                }

            }

            sum += cont * this.getDefaultValue() * sparseVector.getDefaultValue();
            return sum;
        }
    }

    @Override
    public boolean equalsVector(Vector vector, double threshold) {
        SparseVectorDefaultValue sparseVector = (SparseVectorDefaultValue) vector;

        if (Math.abs(this.getDefaultValue() - sparseVector.getDefaultValue()) > threshold)
            return false;

        if (Math.abs(this.getNDefaultValues() - sparseVector.getNDefaultValues()) > threshold)
            return false;

        for (Integer integer : this.getNonZeroEntries()) {
            if (Math.abs(this.get(integer) - sparseVector.get(integer)) > threshold)
                return false;
        }

        for (Integer integer : sparseVector.getNonZeroEntries()) {
            if (!this.values.containsKey(integer))
                if (Math.abs(-sparseVector.get(integer)) > threshold)
                    return false;
        }


        return true;
    }

    @Override
    public String output() {
        StringBuilder stringBuilder = new StringBuilder("{");
        stringBuilder.append(this.defaultValue);
        stringBuilder.append(",");
        for (Integer integer : this.getNonZeroEntries()) {
            stringBuilder.append("(");
            stringBuilder.append(integer);
            stringBuilder.append(",");
            stringBuilder.append(this.get(integer));
            stringBuilder.append(")");
            stringBuilder.append(", ");
        }

        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public Set<Integer> getNonZeroEntries() {
        return this.values.keySet();
    }

    public Map<Integer, Double> getValues() {
        return values;
    }

    /**
     * Log-Normalizes a given vector.
     *
     * @param vector the vector to be normalized.
     * @return a normalized vector.
     */
    public static SparseVectorDefaultValue logNormalize(SparseVectorDefaultValue vector) {

        double maxValue = vector.defaultValue;
        for (Integer index : vector.getNonZeroEntries()) {
            double val = vector.get(index);
            if (val > maxValue) {
                maxValue = val;
            }
        }

        for (Integer index : vector.getNonZeroEntries()) {
            vector.set(index, vector.get(index) - maxValue);
        }

        vector.setDefaultValue(vector.getDefaultValue() - maxValue);

        return vector;
    }

    /**
     * Normalizes a given vector.
     *
     * @param vector the vector to be normalized.
     * @return a normalized vector.
     */
    public static SparseVectorDefaultValue normalize(SparseVectorDefaultValue vector) {

        double sum = vector.sum();

        for (Integer index : vector.getNonZeroEntries()) {
            vector.set(index, vector.get(index) / sum);
        }

        vector.setDefaultValue(vector.getDefaultValue() / sum);

        return vector;
    }

    public void apply(Function<Double,Double> function) {
        for (Integer integer : this.getNonZeroEntries()) {
            this.set(integer, function.apply(this.get(integer)));
        }

        this.setDefaultValue(function.apply(this.getDefaultValue()));

    }

    public double sumApply(Function<Double,Double> function) {
        double sum = 0;
        for (Integer integer : this.getNonZeroEntries()) {
            sum+=function.apply(this.get(integer));
        }

        sum += function.apply(this.defaultValue) * this.getNDefaultValues();

        return sum;
    }
}


