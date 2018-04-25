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

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class offers some AMIDST util methods.
 */
public final class Utils {

    /**
     * Normalizes a given vector.
     * @param <E> a class extending {@link Vector}.
     * @param vector the vector to be normalized.
     * @return a normalized vector.
     */
    public static <E extends Vector> E normalize(E vector){

        double sum = Utils.sum(vector);
        for (int i = 0; i < vector.size(); i++) {
            vector.set(i,vector.get(i)/sum);
        }

        return vector;
    }

    /**
     * Normalizes a given vector.
     * @param <E> a class extending {@link Vector}.
     * @param vector the vector to be normalized.
     * @return a normalized vector.
     */
    public static <E extends Vector> E logNormalize(E vector){

        int maxIndex = Utils.maxIndex(vector);
        double maxValue = vector.get(maxIndex);
        for (int i = 0; i < vector.size(); i++) {
            vector.set(i,vector.get(i)-maxValue);
        }

        return vector;
    }

    /**
     * Returns the sumNonStateless of the elements in a given vector.
     * @param vector a {@link Vector} object.
     * @return a {@code double} that represents the sumNonStateless of the vector elements.
     */
    public static double sum(Vector vector){
        double sum = 0;
        for (int i=0; i<vector.size(); i++){
            sum += vector.get(i);
        }
        return sum;
    }

    /**
     * Returns the index of the maximum element in a given vector.
     * @param vector a {@link Vector} object.
     * @return an {@code int} that represents the index of the maximum element in the vector.
     */
    public static int maxIndex(Vector vector){
        double max = Double.NEGATIVE_INFINITY;
        int index = -1;
        for (int i=0; i<vector.size(); i++){
            if (vector.get(i)>max) {
                max = vector.get(i);
                index = i;
            }
        }
        if (index==-1)
            throw new IllegalStateException("There is no maximum. Probably a NaN value.");

        return index;
    }


    /**
     * Returns the index of the minimum element in a given vector.
     * @param vector a {@link Vector} object.
     * @return an {@code int} that represents the index of the minimum element in the vector.
     */
    public static int minIndex(Vector vector){
        double min = Double.POSITIVE_INFINITY;
        int index = -1;
        for (int i=0; i<vector.size(); i++){
            if (vector.get(i)<min) {
                min = vector.get(i);
                index = i;
            }
        }
        if (index==-1)
            throw new IllegalStateException("There is no maximum. Probably a NaN value.");

        return index;
    }

    /**
     * Sets a missing value as a Double.NaN.
     * @return a Double.NaN.
     */
    public static double missingValue(){
        return Double.NaN;
    }

    /**
     * Tests if a given value is missing or not.
     * @param val a {@code double} value.
     * @return {@code true} is the value is missing, {@code false} otherwise.
     */
    public static boolean isMissingValue(double val){
        return Double.isNaN(val);
    }

    public static void accumulatedSumVectors(double[] a, double[] b){
        for (int i=0; i<a.length; i++){
            a[i]+=b[i];
        }
    }

    /**
     * Returns the index of the min element in a given array of doubles.
     * @param vals an {@code array} of {@code double}.
     * @return an {@code int} that represents the index of the maximum element in the array.
     */
    public static int minIndex(double[] vals){
        double min = Double.POSITIVE_INFINITY;
        int index = -1;
        for (int i=0; i<vals.length; i++){
            if (vals[i]<min) {
                min = vals[i];
                index = i;
            }
        }
        return index;
    }
    /**
     * Returns the index of the maximum element in a given array of doubles.
     * @param vals an {@code array} of {@code double}.
     * @return an {@code int} that represents the index of the maximum element in the array.
     */
    public static int maxIndex(double[] vals){
        double max = Double.NEGATIVE_INFINITY;
        int index = -1;
        for (int i=0; i<vals.length; i++){
            if (vals[i]>max) {
                max = vals[i];
                index = i;
            }
        }
        return index;
    }

    /**
     * Normalizes an array of doubles.
     * @param vals an {@code array} of {@code double}.
     * @return a normalized array of doubles.
     */
    public static double[] normalize(double[] vals) {
        double sum = 0;
        for (int i=0; i<vals.length; i++) {
            sum+=vals[i];
        }

        for (int i=0; i<vals.length; i++) {
            vals[i] /= sum;
        }
        return vals;
    }

    /**
     * Normalizes an array of doubles.
     * @param vals an {@code array} of {@code double}.
     * @return a normalized array of doubles.
     */
    public static double[] logs2probs(double[] vals){
        double max = vals[Utils.maxIndex(vals)];
        double[] normalizedVals = new double[vals.length];
        for (int i=0; i<vals.length; i++) {
            normalizedVals[i] = Math.exp(vals[i]+max);
        }
        return Utils.normalize(normalizedVals);
    }

    /**
     * Returns the topological order of {@link Variables} of a given {@link DAG}.
     * @param dag a given {@link DAG}.
     * @return a topological ordered list of {@link Variables}.
     */
    public static List<Variable> getTopologicalOrder(DAG dag){
        Variables variables = dag.getVariables();
        int nNrOfAtts = variables.getNumberOfVars();
        List<Variable> order = new ArrayList();
        boolean[] bDone = new boolean[variables.getNumberOfVars()];

        for (Variable var: variables){
            bDone[var.getVarID()] = false;
        }

        for (int iAtt = 0; iAtt < nNrOfAtts; iAtt++) {
            boolean allParentsDone = false;
            for (Variable var2 : variables){
                if (!bDone[var2.getVarID()]) {
                    allParentsDone = true;
                    int iParent = 0;
                    for (Variable parent: dag.getParentSet(var2))
                        allParentsDone = allParentsDone && bDone[parent.getVarID()];

                    if (allParentsDone){
                        order.add(var2);
                        bDone[var2.getVarID()] = true;
                    }
                }
            }
        }
        return order;
    }

    /**
     * Returns the conditional distribution type for a given {@link Variable} in a given {@link BayesianNetwork}.
     * @param amidstVar a given {@link Variable}.
     * @param amidstBN a given {@link BayesianNetwork}.
     * @return an {@code int} that represents the conditional distribution type.
     */
    public static int getConditionalDistributionType(Variable amidstVar, BayesianNetwork amidstBN) {

        int type = -1;
        List<Variable> conditioningVariables = amidstBN.getConditionalDistribution(amidstVar).getConditioningVariables();

        if (amidstVar.isMultinomial() && conditioningVariables.size()>0){
            return 0;
        }else if (amidstVar.isMultinomial() && conditioningVariables.size()==0){
            return 4;
        }else if (amidstVar.isNormal()) {

            boolean multinomialParents = false;
            boolean normalParents = false;

            for (Variable v : conditioningVariables) {
                if (v.isMultinomial()) {
                    multinomialParents = true;
                } else if (v.isNormal()) {
                    normalParents = true;
                } else {
                    throw new IllegalArgumentException("Unrecognized DistributionType. ");
                }
            }
            if (normalParents && !multinomialParents) {
                return 1;
            } else if (conditioningVariables.size() == 0){
                return 5;
            } else if (!normalParents && multinomialParents) {
                return 2;
            } else if (normalParents && multinomialParents) {
                return 3;
            } else {
                throw new IllegalArgumentException("Unrecognized DistributionType. ");
            }
        }
        return type;
    }

    /**
     * Returns the Inverse digamma of a given {@code double} value.
     * <p> The digamma function is the derivative of the log gamma function.
     * It calculates the value Y &gt; 0 for a value X such that digamma(Y) = X.
     * This algorithm is from Paul Fackler and Harvard Univesity:
     * http://www4.ncsu.edu/~pfackler/
     * http://hips.seas.harvard.edu/content/inverse-digamma-function-matlab </p>
     * @param X a {@code double} value.
     * @return a {@code double} value Y, such as Digamma(Y) = X.
     */
    public static double invDigamma(double X){
        double L = 1;
        double Y = Math.exp(X);
        while (L > 10e-8) {
            Y = Y + L * Math.signum(X - org.apache.commons.math3.special.Gamma.digamma(Y));
            L = L / 2;
        }
        return Y;
    }

    public static void shuffleData(String inputPath, String outputPath){

        DataOnMemory<DataInstance> dataOnMemory = DataStreamLoader.loadDataOnMemoryFromFile(inputPath);
        Collections.shuffle(dataOnMemory.getList());
        try {
            DataStreamWriter.writeDataToFile(dataOnMemory, outputPath);
        }catch(IOException e){}
    }

}
