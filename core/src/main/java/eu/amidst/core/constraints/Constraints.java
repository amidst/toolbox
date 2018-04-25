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

package eu.amidst.core.constraints;

import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.learning.parametric.bayesian.utils.PlateuStructure;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 13/01/2018.
 */
public class Constraints implements Serializable{

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

    public void setPlateuStructure(PlateuStructure plateuStructure) {
        nodes = constraintMap.keySet()
                .stream()
                .map( var -> plateuStructure.getNodeOfVar(var,0))
                .collect(Collectors.toList());
    }

    public void buildConstrains(){
        nodes.forEach(node -> this.setConstraint(node));
    }

    public void setConstraint(Node node){
        for ( Constraint constraint : this.constraintMap.get(node.getMainVariable())) {
            Optional<Node> optional = node.getParents().stream().filter(nodeParent -> match(node.getPDist(), nodeParent.getMainVariable(), constraint)).findFirst();
            if (!optional.isPresent())
                throw new IllegalStateException("No constraint for the given parameter");

            Node nodeParent = optional.get();

            nodeParent.setActive(false);

            this.fixValue(nodeParent.getQDist(), constraint.getValue());
        }
    }

    public static boolean match(EF_ConditionalDistribution dist, Variable parent, Constraint constraint) {
        String x = dist.getClass().getName();

        if (x.contains("EF_Normal_Normal_Gamma")) {
            return match((EF_Normal_Normal_Gamma)dist, parent, constraint);
        } else if (x.contains("EF_BaseDistribution_MultinomialParents")) {
            return match((EF_BaseDistribution_MultinomialParents)dist, parent, constraint);
        } else {
            throw new UnsupportedOperationException("No Fix Value mehtod for this distribution");
        }
    }

    public static boolean match(EF_Normal_Normal_Gamma dist, Variable parent, Constraint constraint){
        return parent.getName().contains(constraint.getParamaterName());
    }

    public static boolean match(EF_BaseDistribution_MultinomialParents dist, Variable parent, Constraint constraint){
        String constraintNanme = constraint.getParamaterName().trim().replace(" ", "");
        String parentName = parent.getName().trim().replace(" ", "");
        String[] names = constraintNanme.split("\\|");

        if (parentName.contains(names[0]) && parentName.contains(names[1]))
            return true;
        else
            return false;
    }

    public static void fixValue(EF_UnivariateDistribution dist, double val){
        String x = dist.getClass().getName();

        if (x.contains("EF_NormalParameter")) {
            fixValue((EF_NormalParameter)dist, val);
        } else if (x.contains("EF_Gamma")) {
            fixValue((EF_Gamma)dist, val);
        } else {
            throw new UnsupportedOperationException("No Fix Value mehtod for this distribution");
        }
    }
    public static void fixValue(EF_NormalParameter normal, double val){
        normal.setNaturalWithMeanPrecision(val, 1000);
        normal.fixNumericalInstability();
        normal.updateMomentFromNaturalParameters();
    }

    public static void fixValue(EF_Gamma gamma, double val){
        gamma.getNaturalParameters().set(0,val*1000-1);
        gamma.getNaturalParameters().set(0,-val*1000);
        gamma.fixNumericalInstability();
        gamma.updateMomentFromNaturalParameters();
    }


}
