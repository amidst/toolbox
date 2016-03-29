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

package eu.amidst.dynamic.utils;

import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ana@cs.aau.dk on 09/06/15.
 */
public final class Utils {

    public static List<Variable> getCausalOrderTime0(DynamicDAG dag){
        DynamicVariables variables = dag.getDynamicVariables();
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
                    for (Variable parent: dag.getParentSetTime0(var2))
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

    public static List<Variable> getCausalOrderTimeT(DynamicDAG dag){
        DynamicVariables variables = dag.getDynamicVariables();
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
                    for (Variable parent: dag.getParentSetTimeT(var2)) {
                        if (parent.isInterfaceVariable())
                            continue;
                        allParentsDone = allParentsDone && bDone[parent.getVarID()];
                    }

                    if (allParentsDone){
                        order.add(var2);
                        bDone[var2.getVarID()] = true;
                    }
                }
            }
        }
        return order;
    }

}
