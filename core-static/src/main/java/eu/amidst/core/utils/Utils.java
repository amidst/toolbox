/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.utils;

import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variables;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andresmasegosa on 28/08/14.
 */
public final class Utils {

    private Utils(){
        //Not called
    }

    public static <E extends Vector> E normalize(E vector){

        double sum = Utils.sum(vector);
        for (int i = 0; i < vector.size(); i++) {
            vector.set(i,vector.get(i)/sum);
        }

        return vector;
    }


    public static <E extends Vector> E logNormalize(E vector){

        int maxIndex = Utils.maxIndex(vector);
        double maxValue = vector.get(maxIndex);
        for (int i = 0; i < vector.size(); i++) {
            vector.set(i,vector.get(i)-maxValue);
        }

        return vector;
    }

    public static double sum(Vector vector){
        double sum = 0;
        for (int i=0; i<vector.size(); i++){
            sum += vector.get(i);
        }
        return sum;
    }

    public static int maxIndex(Vector vector){
        double max = Double.NEGATIVE_INFINITY;
        int index = -1;
        for (int i=0; i<vector.size(); i++){
            if (vector.get(i)>max) {
                max = vector.get(i);
                index = i;
            }
        }
        return index;
    }

    public static double missingValue(){
        return Double.NaN;
    }

    public static boolean isMissingValue(double val){
        return Double.isNaN(val);
    }

    public static void accumulatedSumVectors(double[] a, double[] b){
        for (int i=0; i<a.length; i++){
            a[i]+=b[i];
        }
    }

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

    public static double[] newNormalize(double[] vals) {
        double sum = 0;
        for (int i=0; i<vals.length; i++) {
            sum+=vals[i];
        }

        double[] normalizedVals = new double[vals.length];

        for (int i=0; i<vals.length; i++) {
            normalizedVals[i] = vals[i]/sum;
        }

        return normalizedVals;

    }

    public static double[] logs2probs(double[] vals){
        double max = vals[Utils.maxIndex(vals)];
        double[] normalizedVals = new double[vals.length];
        for (int i=0; i<vals.length; i++) {
            normalizedVals[i] = Math.exp(vals[i]+max);
        }
        return Utils.normalize(normalizedVals);
    }

    public static List<Variable> getCausalOrder(DAG dag){
        Variables variables = dag.getStaticVariables();
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
     *
     * Inverse digamma function.  The digamma function is the
     * derivative of the log gamma function.  This calculates the value
     * Y > 0 for a value X such that digamma(Y) = X.
     *
     * This algorithm is from Paul Fackler & Harvard Univesity:
     * http://www4.ncsu.edu/~pfackler/
     * http://hips.seas.harvard.edu/content/inverse-digamma-function-matlab
     *
     * @param X
     * @return Y value such as Digamma(Y) = X
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

    //*********************************************************************************
//            //Simulate a sample from a Hugin network
//            int nsamples = 100;
//            for (int j=0;j< nodeList.size();j++) {
//                System.out.print(((Node)nodeList.get(j)).getName());
//                if(j<nodeList.size()-1)
//                    System.out.print(",");
//            }
//            System.out.println();
//            for (int i=0;i<nsamples;i++){
//                domain.simulate();
//                for (int j=0;j<nodeList.size();j++){
//                    System.out.print(((ContinuousChanceNode)nodeList.get(j)).getSampledValue());
//                    if(j<nodeList.size()-1)
//                        System.out.print(",");
//                }
//                System.out.println();
//            }
//            //*********************************************************************************



}
