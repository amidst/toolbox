/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.corestatic.variables;

import java.util.List;

/**
 * Created by ana@cs.aau.dk on 03/11/14.
 */
public interface Assignment {

    double getValue(Variable var);

    void setValue(Variable var, double value);

    // TODO Check THIS!!
    default String toString(List<Variable> vars){
        StringBuilder builder = new StringBuilder(vars.size()*2);
        builder.append("{");
        vars.stream().limit(vars.size()-1).forEach(var -> builder.append(var.getName()+ " = "+(int)this.getValue(var)+", "));
        builder.append(vars.get(vars.size()-1).getName()+ " = "+ (int)this.getValue(vars.get(vars.size()-1)));
        builder.append("}");
        return builder.toString();
    }

}
