/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.variables;

import java.util.Set;

/**
 * This interface defines a collection of assignments to variables. I.e.
 * "(A=0.1, B = True)", assuming A is continuous variable and B a binary variable.
 *
 * Created by ana@cs.aau.dk on 03/11/14.
 */
public interface Assignment {

    /**
     * This method return the value assigned to a given variable
     * @param var, the Variable object we want to query
     * @return The assigned value to the given variable. Returns a Double.NaN if
     * the variable is not included in the assignment.
     */
    double getValue(Variable var);

    /**
     * This method set the value assigned to a variable. If the value is already include,
     * then update the value accordingly.
     *
     * @param var, the Variable object we want to assign
     * @param value, the assigned value
     */
    void setValue(Variable var, double value);


    Set<Variable> getVariables();

    default String outputString(){

        StringBuilder builder = new StringBuilder(this.getVariables().size()*2);
        builder.append("{");
        this.getVariables().stream().forEach(var -> builder.append(var.getName()+ " = "+(int)this.getValue(var)+", "));
        builder.append("}");
        return builder.toString();
    }

}
