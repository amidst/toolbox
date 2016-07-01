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

package eu.amidst.core.variables;

import java.util.List;
import java.util.Set;

/**
 * This interface defines a collection of assignments to variables.
 * For example, "(A=0.1, B = True)" is an assignment with A being a continuous variable and B a binary variable.
 * The assignment values are always encoded as double values. In case of finite state space distribution, the
 * double value can be seen as an integer indexing the different states.
 */
public interface Assignment {

    /**
     * Returns the value assigned to a given variable.
     * @param var the Variable object we want to query.
     * @return the assigned value to the given variable. Returns a Double.NaN if
     * the variable does not exist in the assignment.
     */
    double getValue(Variable var);

    /**
     * Sets the value assigned to a variable.
     * If the value already exists, then the value is updated accordingly.
     * @param var the Variable object to which the value will be assigned.
     * @param value the value that will be assigned to the Variable.
     */
    void setValue(Variable var, double value);

    /**
     * Returns the set of variables included in the assignment.
     * @return a valid set of variables.
     */
    Set<Variable> getVariables();

    /**
     * Defines a default implementation of the "toString()" method.
     * It produces a String that details all the variables and their corresponding values stored in this Assignment.
     * The String starts with "{" and ends with "}". E.g., {A=0, B=2, C=0}.
     * @return a String object describing this Assignment.
     */
    default String outputString(){
        StringBuilder builder = new StringBuilder(this.getVariables().size()*2);
        builder.append("{");
        this.getVariables().stream().forEach(var -> builder.append(var.getName() + " = " + (var.isMultinomial() ? (int) this.getValue(var) : String.format("%1$,.3f", this.getValue(var))) + ", "));
        //builder.deleteCharAt(builder.lastIndexOf(","));
        if (builder.length()>1) {
            builder.delete(builder.lastIndexOf(","), builder.lastIndexOf(",") + 2);
        }
        builder.append("}");
        return builder.toString();
    }

    /**
     * Contains a default implementation of a "toString()" method.
     * It produces a String that details the given variables and their corresponding values stored in this Assignment.
     * The String keeps the order given in the argument and displays 3 decimal places for the continuous variables.
     * The String starts with "{" and ends with "}" E.g., {A=0, B=1.217}.
     * @param vars an ordered list of Variables to display.
     * @return a String object describing a subset of variables in this Assignment.
     */
    default String outputString(List<Variable> vars){
        StringBuilder builder = new StringBuilder(vars.size()*2);
        builder.append("{");
//        vars.stream().limit(vars.size() - 1).filter(var -> !Double.isNaN(this.getValue(var))).forEach(var -> builder.append(var.getName() + " = " + (var.isMultinomial() ? (int) this.getValue(var) : String.format("%1$,.3f", this.getValue(var))) + ", "));
//        if(!Double.isNaN(this.getValue(vars.get(vars.size()-1)))) {
//            builder.append(vars.get(vars.size() - 1).getName() + " = " + (vars.get(vars.size() - 1).isMultinomial() ? (int) this.getValue(vars.get(vars.size() - 1)) : String.format("%1$,.3f", this.getValue(vars.get(vars.size() - 1)))));
//        }
        vars.stream().filter(var -> !Double.isNaN(this.getValue(var))).forEach(var -> builder.append(var.getName() + " = " + (var.isMultinomial() ? (int) this.getValue(var) : String.format("%1$,.3f", this.getValue(var))) + ", "));
        //builder.deleteCharAt(builder.lastIndexOf(","));
        if (builder.length()>1) {
            builder.delete(builder.lastIndexOf(","), builder.lastIndexOf(",") + 2);
        }
        builder.append("}");

        return builder.toString();
    }
}
