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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by andresmasegosa on 2/5/16.
 */
public class SparseVector implements Vector {

    Map<Integer,Double> values;
    final int dimension;

    public SparseVector(int dimension) {
        this.dimension = dimension;
        this.values = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double get(int i) {
        if (this.values.containsKey(i))
            return this.values.get(i);
        else
            return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void set(int i, double val) {
        this.values.put(i,val);
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
        double sum =0;
        for (Double aDouble : values.values()) {
            sum+=aDouble;
        }
        return sum;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sum(Vector vector) {
        SparseVector sparseVector = (SparseVector)vector;

        for (Integer integer : this.getNonZeroEntries()) {
            this.set(integer,this.get(integer)+sparseVector.get(integer));
        }

        for (Integer integer : sparseVector.getNonZeroEntries()) {
            if (!this.values.containsKey(integer))
                this.set(integer,sparseVector.get(integer));
        }
    }

    @Override
    public void substract(Vector vector) {
        SparseVector sparseVector = (SparseVector)vector;

        for (Integer integer : this.getNonZeroEntries()) {
            this.set(integer,this.get(integer)-sparseVector.get(integer));
        }

        for (Integer integer : sparseVector.getNonZeroEntries()) {
            if (!this.values.containsKey(integer))
                this.set(integer,-sparseVector.get(integer));
        }
    }

    @Override
    public void copy(Vector vector) {
        SparseVector sparseVector = (SparseVector)vector;

        for (Integer integer : sparseVector.getNonZeroEntries()) {
            this.set(integer,sparseVector.get(integer));
        }
    }

    @Override
    public void divideBy(double val) {
        for (Integer integer : this.getNonZeroEntries()) {
            this.set(integer,this.get(integer)/val);
        }
    }

    @Override
    public void multiplyBy(double val) {
        for (Integer integer : this.getNonZeroEntries()) {
            this.set(integer,this.get(integer)*val);
        }
    }

    @Override
    public void sumConstant(double val) {
        for (Integer integer : this.getNonZeroEntries()) {
            this.set(integer,this.get(integer) + val);
        }
    }

    @Override
    public double dotProduct(Vector vector) {
        SparseVector sparseVector = (SparseVector)vector;

        double sum = 0;
        for (Integer integer : this.getNonZeroEntries()) {
            sum += this.get(integer)*sparseVector.get(integer);
        }

        return sum;
    }

    @Override
    public boolean equalsVector(Vector vector, double threshold) {
        SparseVector sparseVector = (SparseVector)vector;

        for (Integer integer : this.getNonZeroEntries()) {
            if (Math.abs(this.get(integer)-sparseVector.get(integer))>threshold)
                return false;
        }

        for (Integer integer : sparseVector.getNonZeroEntries()) {
            if (!this.values.containsKey(integer))
                if (Math.abs(-sparseVector.get(integer))>threshold)
                    return false;
        }

        return true;
    }

    @Override
    public String output() {
        StringBuilder stringBuilder = new StringBuilder("{");
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

    public Set<Integer> getNonZeroEntries(){
        return this.values.keySet();
    }
}
