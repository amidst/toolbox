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

package eu.amidst.dynamic.constraints;

import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.learning.parametric.bayesian.PlateauStructure;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 17/01/2018.
 */
public class Constraints {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    protected Map<Variable, List<Constraint>> constraintMap = new HashMap();
    transient protected List<Node> nodes = new ArrayList<>();

    public void addConstraint(Constraint constraint){
        if (constraintMap.containsKey(constraint.getVariable()))
            constraintMap.get(constraint.getVariable()).add(constraint);
        else{
            List<Constraint> list = new ArrayList<>();
            list.add(constraint);
            constraintMap.put(constraint.getVariable(), list);
        }
    }

    public void setPlateuStructure(PlateauStructure plateuStructure) {
         nodes = constraintMap.values()
                .stream()
                .flatMap(list -> list.stream())
                .filter( constraint ->  constraint.isTime0())
                .map( constraint -> plateuStructure.getNodeOfVarTime0(constraint.getVariable()))
                .collect(Collectors.toList());

         nodes.addAll(constraintMap.values()
                 .stream()
                 .flatMap(list -> list.stream())
                 .filter( constraint ->  !constraint.isTime0())
                 .map( constraint -> plateuStructure.getNodeOfVarTimeT(constraint.getVariable(),0))
                 .collect(Collectors.toList()));
    }

    public void buildConstrains(){
        nodes.forEach(node -> this.setConstraint(node));
    }

    public void setConstraint(Node node){
        for ( Constraint constraint : this.constraintMap.get(node.getMainVariable())) {
            Optional<Node> optional = node.getParents().stream().filter(nodeParent -> eu.amidst.core.constraints.Constraints.match(node.getPDist(), nodeParent.getMainVariable(), constraint)).findFirst();
            if (!optional.isPresent())
                throw new IllegalStateException("No constraint for the given parameter");

            Node nodeParent = optional.get();

            nodeParent.setActive(false);

            eu.amidst.core.constraints.Constraints.fixValue(nodeParent.getQDist(), constraint.getValue());
        }
    }
}