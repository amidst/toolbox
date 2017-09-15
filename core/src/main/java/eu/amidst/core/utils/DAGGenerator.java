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

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

/**
 *
 * This class contains several utility methods for generating specific kind of DAGs.
 *
 */
public class DAGGenerator {

    /**
     * Returns the graphical structure for this NaiveBayesClassifier.
     * @param attributes, the attributes.
     * @param className, the class name.
     * @return a directed acyclic graph {@link DAG}.
     */
    public static DAG getNaiveBayesStructure(Attributes attributes, String className){
        Variables modelHeader = new Variables(attributes);
        Variable classVar = modelHeader.getVariableByName(className);
        DAG dag = new DAG(modelHeader);
        dag.getParentSets().stream().filter(w -> w.getMainVar().getVarID() != classVar.getVarID()).forEach(w -> w.addParent(classVar));

        dag.setName("DAGNB");

        return dag;
    }


    /**
     * This method creates a DAG object with a naive Bayes structure for the attributes of the passed data stream.
     * The main variable is defined as a latent binary variable which is a parent of all the observed variables.
     * @param attributes, the attributes.
     * @param varHiddenName, name of the created hidden var.
     * @param nstates, number of states of the hidden var.
     * @return a directed acyclic graph {@link DAG}.
     */
    public static DAG getHiddenNaiveBayesStructure(Attributes attributes, String varHiddenName, int nstates){
        //We create a Variables object from the attributes of the data stream
        Variables modelHeader = new Variables(attributes);

        //We define the global latent binary variable
        Variable globalHiddenVar = modelHeader.newMultinomialVariable(varHiddenName, nstates);

        //Then, we create a DAG object with the defined model header
        DAG dag = new DAG(modelHeader);

        //We set the linkds of the DAG.
        dag.getParentSets().stream().filter(w -> w.getMainVar() != globalHiddenVar).forEach(w -> w.addParent(globalHiddenVar));

        dag.setName("DAGHiddenNB");
        return dag;
    }


}
