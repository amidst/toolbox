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
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * This class implements various useful methods for indexing arrays of distributions involving multinomial variables.
 */
public final class MultinomialIndex {

    /**
     * Returns the index of an assignment given a set of multinomial variables and a corresponding assignment.
     * Example: Let X, Y and Z three multinomial variables with states {0,1}, {0,1} and {0,1,2} respectively. Then, they
     * are indexed as:
     *
     *       X Y Z Index
     *       0 0 0   0
     *       1 0 0   1
     *       0 1 0   2
     *       1 1 0   3
     *       0 0 1   4
     *       1 0 1   5
     *       0 1 1   6
     *       1 1 1   7
     *       0 0 2   8
     *       1 0 2   9
     *       0 1 2  10
     *       1 1 2  11
     *
     * So, for instance Index(0,0,2) = 8.
     *
     * @param vars a {@code List} of {@link Variable}.
     * @param assignment an {@link Assignment} for a set of variables.
     * @return the index of the corresponding input assignment.
     */
    public static int getIndexFromVariableAssignment (List<Variable> vars, Assignment assignment) {

        int lastPhiStride = 1;
        int index = 0;

        for (Variable var: vars){
            index = index + (int)assignment.getValue(var)*lastPhiStride;
            lastPhiStride=lastPhiStride*var.getNumberOfStates();
        }
        return index;
    }

    /**
     * Returns the index of an assignment given the set of multinomial variables and their corresponding values.
     * @param vars a {@code List} of {@link Variable}.
     * @param assignment a {@code List} of double values for the variables in the same order.
     * @return the index of the corresponding input assignment.
     */
    public static int getIndexFromVariableAssignment (List<Variable> vars, List<Double> assignment) {

        int n = vars.size();
        int lastPhiStride = 1;
        int index = 0;

        for (int i=0; i<n; i++){
            index = index + (int)assignment.get(i).doubleValue()*lastPhiStride;
            lastPhiStride=lastPhiStride*vars.get(i).getNumberOfStates();
        }
        return index;
    }

    /**
     * Returns the index of an assignment given a set of multinomial variables and a {@link DataInstance}.
     * @param vars a {@code List} of {@link Variable}.
     * @param dataInstance a {@link DataInstance} including values for the input set of variables.
     * @return the index of the corresponding input dataInstance.
     */
    public static int getIndexFromDataInstance (List<Variable> vars, DataInstance dataInstance) {

        int lastPhiStride = 1;
        int index = 0;

        for (Variable var: vars){
            index = index + (int)dataInstance.getValue(var)*lastPhiStride;
            lastPhiStride=lastPhiStride*var.getNumberOfStates();
        }
        return index;
    }

    /**
     * Returns the index of an assignment given a set of multinomial variables and their corresponding values.
     * @param vars a {@code List} of {@link Variable}.
     * @param assignment an array of double including the values of variables in the same order.
     * @return the index of the corresponding input assignment.
     */
    public static int getIndexFromVariableAssignment (List<Variable> vars, double[] assignment) {

        int n = vars.size();
        int lastPhiStride = 1;
        int index = 0;

        for (int i=0; i<n; i++){
            index = index + (int)assignment[i]*lastPhiStride;
            lastPhiStride=lastPhiStride*vars.get(i).getNumberOfStates();
        }
        return index;
    }

    /**
     * Returns an assignment given a list of variables and an input index.
     * @param vars a {@code List} of {@link Variable}.
     * @param index the index of the {@link Assignment}.
     * @return an array of {@code double} with the values of the input variables, i.e., the assignment.
     */
    public static double[] getVariableArrayAssignmentFromIndex(List<Variable> vars, int index) {

        double[] assignment = new double[vars.size()];
        int n = vars.size();
        int lastPhiStride = 1;

        for (int i=0; i<n; i++){
            assignment[i]=Math.floor(index/(double)lastPhiStride) % vars.get(i).getNumberOfStates();
            lastPhiStride=lastPhiStride*vars.get(i).getNumberOfStates();
        }
        return assignment;
    }

    /**
     * Returns an assignment given a list of variables and an input index.
     * @param vars a {@code List} of {@link Variable}.
     * @param index the index of the {@link Assignment}.
     * @return an {@link Assignment}.
     */
    public static Assignment getVariableAssignmentFromIndex(List<Variable> vars, int index) {

        HashMapAssignment assignment = new HashMapAssignment(vars.size());
        int n = vars.size();
        int lastPhiStride = 1;

        for (int i=0; i<n; i++){
            assignment.setValue(vars.get(i),Math.floor(index/(double)lastPhiStride) % vars.get(i).getNumberOfStates());
            lastPhiStride=lastPhiStride*vars.get(i).getNumberOfStates();
        }
        return assignment;
    }
    /**
     * Returns the number of possible assignments for a list of variables.
     * @param vars a {@code List} of {@link Variable}.
     * @return an {@code integer} representing the number of possible assignments.
     */
    public static int getNumberOfPossibleAssignments (List<Variable> vars) {

        int n = 1;
        for (Variable v : vars) {
            n = n * v.getNumberOfStates();
        }
        return n;
    }
}
