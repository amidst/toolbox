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

package gps;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andresmasegosa on 17/2/16.
 */
public class DAGsGeneration {

    static public double maxTrain = 50000;

    public static DAG getBCCFullMixtureDAG(Attributes attributes, int nstates) {
        // Create a Variables object from the attributes of the input data stream.
        Variables variables = new Variables(attributes.subSet(2,3,4,13));

        // Define the class variable.
        Variable classVar = variables.getVariableByName("DEFAULTING");

        // Define the global hidden variable.
        Variable globalHiddenVar = variables.newMultinomialVariable("GlobalHidden",nstates);

        // Create an empty DAG object with the defined variables.
        DAG dag = new DAG(variables);

        // Link the class as parent of all attributes
        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .forEach(w -> w.addParent(classVar));


        // Link the global hidden as parent of all predictive attributes
        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .filter(w -> w.getMainVar() != globalHiddenVar)
                .forEach(w -> w.addParent(globalHiddenVar));

        // Show the new dynamic DAG structure
        System.out.println(dag.toString());

        return dag;
    }

    public static DAG getBCCNB(Attributes attributes) {

        // Create a Variables object from the attributes of the input data stream.
        Variables variables = new Variables(attributes);

        // Define the class variable.
        Variable classVar = variables.getVariableByName("DEFAULTING");



        for (Attribute att : attributes) {
            if (att.isSpecialAttribute() || att.getName().compareTo(classVar.getName())==0)
                continue;

            Variable variable = variables.getVariableByName(att.getName());
            if (variable!=classVar){
                variables.newIndicatorVariable(variable,0);
            }
        }
        // Create an empty DAG object with the defined variables.
        DAG dag = new DAG(variables);

        // Link the class as parent of all attributes
        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .filter(w -> !w.getMainVar().getName().endsWith("_INDICATOR"))
                .forEach(w -> w.addParent(classVar));


        // Link the class as parent of all attributes
        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .filter(w -> !w.getMainVar().getName().endsWith("_INDICATOR"))
                .forEach(w -> w.addParent(variables.getVariableByName(w.getMainVar().getName()+"_INDICATOR")));

        return dag;
    }

    public static DAG getBCCNBNoClass(Attributes attributes) {

        attributes = attributes.subList(3,4);

        // Create a Variables object from the attributes of the input data stream.
        Variables variables = new Variables(attributes);

        // Define the class variable.


        for (Attribute att : attributes) {
            if (att.isSpecialAttribute())
                continue;

            Variable variable = variables.getVariableByName(att.getName());
            variables.newIndicatorVariable(variable,0);
        }
        // Create an empty DAG object with the defined variables.
        DAG dag = new DAG(variables);


        // Link the class as parent of all attributes
        dag.getParentSets()
                .stream()
                .filter(w -> !w.getMainVar().getName().endsWith("_INDICATOR"))
                .forEach(w -> w.addParent(variables.getVariableByName(w.getMainVar().getName()+"_INDICATOR")));

        return dag;
    }

    public static DAG getBCCMixtureDAG(Attributes attributes, int nstates) {

        // Create a Variables object from the attributes of the input data stream.
        Variables variables = new Variables(attributes);

        // Define the class variable.
        Variable classVar = variables.getVariableByName("DEFAULTING");

        // Define a local hidden variable.
        List<Variable> localHiddenVars = new ArrayList<>();
        List<Attribute> attributesList = attributes.getListOfNonSpecialAttributes();
        for (Attribute attribute : attributesList) {
            if (attribute.getName().compareTo("DEFAULTING")==0)
                continue;
            localHiddenVars.add(variables.newMultinomialVariable("LocalHidden_"+attribute.getName(),nstates));
        }

        // Define the global hidden variable.
        Variable globalHiddenVar = variables.newMultinomialVariable("GlobalHidden",1);

        // Create an empty DAG object with the defined variables.
        DAG dag = new DAG(variables);

        // Link the class as parent of all attributes
        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                //.filter(w -> w.getMainVar() != globalHiddenVar)
                .filter(w -> !w.getMainVar().getName().startsWith("Local"))
                .forEach(w -> w.addParent(classVar));

        // Link the global hidden as parent of all predictive attributes
        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .filter(w -> w.getMainVar() != globalHiddenVar)
                .filter(w -> !w.getMainVar().getName().startsWith("Local"))
                .forEach(w -> w.addParent(globalHiddenVar));

        // Link the local hidden as parent of all predictive attributes

        for (Attribute attribute : attributesList) {
            if (attribute.getName().compareTo("DEFAULTING")==0)
                continue;

            dag.getParentSet(variables.getVariableByName(attribute.getName())).addParent(variables.getVariableByName("LocalHidden_"+attribute.getName()));
        }

        // Show the new dynamic DAG structure
        System.out.println(dag.toString());

        return dag;
    }


    public static DAG getBCCLocalMixtureDAG(Attributes attributes, int nstates) {

        attributes = attributes.subList(3,4);

        // Create a Variables object from the attributes of the input data stream.
        Variables variables = new Variables(attributes);

        // Define the class variable.
        Variable classVar = null;//variables.getVariableByName("DEFAULTING");

        // Define a local hidden variable.
        List<Variable> localHiddenVars = new ArrayList<>();
        List<Attribute> attributesList = attributes.getListOfNonSpecialAttributes();
        for (Attribute attribute : attributesList) {
            if (attribute.getName().compareTo("DEFAULTING")==0)
                continue;
            localHiddenVars.add(variables.newMultinomialVariable("LocalHidden_"+attribute.getName(),nstates));
        }

        // Create an empty DAG object with the defined variables.
        DAG dag = new DAG(variables);

        // Link the class as parent of all attributes
        //dag.getParentSets()
        //        .stream()
        //        .filter(w -> w.getMainVar() != classVar)
        //        .forEach(w -> w.addParent(classVar));

        // Link the local hidden as parent of all predictive attributes
        for (Attribute attribute : attributesList) {
            if (attribute.getName().compareTo("DEFAULTING")==0)
                continue;

            dag.getParentSet(variables.getVariableByName(attribute.getName())).addParent(variables.getVariableByName("LocalHidden_"+attribute.getName()));
        }

        // Show the new dynamic DAG structure
        System.out.println(dag.toString());

        return dag;
    }

    public static DAG getBCCFADAG(Attributes attributes, int nlocals) {
        // Create a Variables object from the attributes of the input data stream.
        Variables variables = new Variables(attributes);

        // Define the class variable.
        Variable classVar = variables.getVariableByName("DEFAULTING");

        // Define a local hidden variable.
        List<Variable> localHiddenVars = new ArrayList<>();
        for (int i = 0; i < nlocals; i++) {
            localHiddenVars.add(variables.newGaussianVariable("LocalHidden_"+i));
        }


        // Create an empty DAG object with the defined variables.
        DAG dag = new DAG(variables);


        for (int i = 0; i < localHiddenVars.size(); i++) {
            for (int j = i+1; j < localHiddenVars.size(); j++) {
                dag.getParentSet(localHiddenVars.get(i)).addParent(localHiddenVars.get(j));
            }
        }

        // Link the class as parent of all attributes
//        dag.getParentSets()
//                .stream()
//                .filter(w -> w.getMainVar() != classVar)
//                .forEach(w -> w.addParent(classVar));

        // Link the local hidden as parent of all predictive attributes
        for (Variable localHiddenVar : localHiddenVars) {
            dag.getParentSets()
                    .stream()
                    .filter(w -> w.getMainVar() != classVar)
                    .filter(w -> !w.getMainVar().getName().startsWith("Local"))
                    .forEach(w -> w.addParent(localHiddenVar));
        }


        // Show the new dynamic DAG structure
        System.out.println(dag.toString());

        return dag;
    }


    public static DAG getGPSMixtureDAG(Attributes attributes, int nstates) {
        // Create a Variables object from the attributes of the input data stream.
        Variables variables = new Variables(attributes);

        // Define the class variable.
        Variable classVar = variables.getVariableByName("DAY");

        // Define the global hidden variable.
        Variable globalHiddenVar = variables.newMultinomialVariable("GlobalHidden",nstates);

        // Create an empty DAG object with the defined variables.
        DAG dag = new DAG(variables);

        // Link the class as parent of all attributes
        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .filter(w -> w.getMainVar() != globalHiddenVar)
                .forEach(w -> w.addParent(classVar));

        // Link all the vars to the globalhidden
        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != globalHiddenVar)
                .forEach(w -> w.addParent(globalHiddenVar));

        // Show the new dynamic DAG structure
        System.out.println(dag.toString());

        return dag;
    }

    public static DAG getGPSMixtureDAGNoDay(Attributes attributes, int nstates) {
        // Create a Variables object from the attributes of the input data stream.
        Variables variables = new Variables(attributes);

        // Define the class variable.
        Variable classVar = variables.getVariableByName("DAY");

        // Define the global hidden variable.
        Variable globalHiddenVar = variables.newMultinomialVariable("GlobalHidden",nstates);

        // Create an empty DAG object with the defined variables.
        DAG dag = new DAG(variables);

        // Link all the vars to the globalhidden
        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .filter(w -> w.getMainVar() != globalHiddenVar)
                .forEach(w -> w.addParent(globalHiddenVar));

        // Show the new dynamic DAG structure
        System.out.println(dag.toString());

        return dag;
    }


    public static DAG getGPSFADAG(Attributes attributes, int nlocals) {
        // Create a Variables object from the attributes of the input data stream.
        Variables variables = new Variables(attributes);

        // Define a local hidden variable.
        List<Variable> localHiddenVars = new ArrayList<>();
        for (int i = 0; i < nlocals; i++) {
            localHiddenVars.add(variables.newGaussianVariable("LocalHidden_"+i));
        }


        // Create an empty DAG object with the defined variables.
        DAG dag = new DAG(variables);


        for (int i = 0; i < localHiddenVars.size(); i++) {
            for (int j = i+1; j < localHiddenVars.size(); j++) {
                dag.getParentSet(localHiddenVars.get(i)).addParent(localHiddenVars.get(j));
            }
        }

        // Link the local hidden as parent of all predictive attributes
        for (Variable localHiddenVar : localHiddenVars) {
            dag.getParentSets()
                    .stream()
                    .filter(w -> !w.getMainVar().getName().startsWith("Local"))
                    .forEach(w -> w.addParent(localHiddenVar));
        }


        // Show the new dynamic DAG structure
        System.out.println(dag.toString());

        return dag;
    }

    public static DAG getIDAMultiLocalGaussianDAG(int natts, int nlocals) {

        // Create a Variables object from the attributes of the input data stream.
        Variables variables = new Variables();

        // Define a local hidden variable.
        List<Variable> localHiddenVars = new ArrayList<>();
        for (int i = 0; i < nlocals; i++) {
            localHiddenVars.add(variables.newGaussianVariable("LocalHidden_"+i));
        }

        for (int i = 0; i < natts; i++) {
            variables.newGaussianVariable("G"+i);
        }



        // Create an empty DAG object with the defined variables.
        DAG dag = new DAG(variables);


        // Link the local hidden as parent of all predictive attributes
        for (Variable localHiddenVar : localHiddenVars) {
            dag.getParentSets()
                    .stream()
                    .filter(w -> !w.getMainVar().getName().startsWith("Local"))
                    .forEach(w -> w.addParent(localHiddenVar));
        }


        // Show the new dynamic DAG structure
        System.out.println(dag.toString());

        return dag;
    }

}