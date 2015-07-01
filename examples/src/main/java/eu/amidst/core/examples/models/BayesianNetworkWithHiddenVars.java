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
package eu.amidst.core.examples.models;



import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.util.Arrays;


/**
 * In this example, we simply show how to create a BN model with hidden variables. We simply
 * create a BN for clustering, i.e.,  a naive-Bayes like structure with a single common hidden variable
 * acting as parant of all the observable variables.
 *
 * Created by andresmasegosa on 18/6/15.
 */
public class BayesianNetworkWithHiddenVars {

    public static void main(String[] args) throws Exception {

        //We can open the data stream using the static class DataStreamLoader
        DataStream<DataInstance> data = DataStreamLoader.openFromFile("datasets/syntheticData.arff");

        /**
         * 1. Once the data is loaded, we create a random variable for each of the attributes (i.e. data columns)
         * in our data.
         *
         * 2. StaticVariables is the class for doing that. It takes a list of Attributes and internally creates
         * all the variables. We create the variables using StaticVariables class to guarantee that each variable
         * has a different ID number and make it transparent for the user.
         *
         * 3. We can extract the Variable objects by using the method getVariableByName();
         */
        Variables variables = new Variables(data.getAttributes());

        Variable a = variables.getVariableByName("A");
        Variable b = variables.getVariableByName("B");
        Variable c = variables.getVariableByName("C");
        Variable d = variables.getVariableByName("D");
        Variable e = variables.getVariableByName("E");
        Variable g = variables.getVariableByName("G");
        Variable h = variables.getVariableByName("H");
        Variable i = variables.getVariableByName("I");

        /**
         * 1. We create the hidden variable. For doing that we make use of the class VariableBuilder. When
         * a variable is created from an Attribute object, it contains all the information we need (e.g.
         * the name, the type, etc). But hidden variables does not have an associated attribute
         * and, for this reason, we use now this VariableBuilder to provide this information to
         * StaticVariables object.
         *
         * 2. Using VariableBuilder, we define a variable called HiddenVar, which is not observable (i.e. hidden), its state
         * space is a finite set with two elements, and its distribution type is multinomial.
         *
         * 3. We finally create the hidden variable using the method "newVariable".
         */

        Variable hidden = variables.newMultionomialVariable("HiddenVar", Arrays.asList("TRUE", "FALSE"));

        /**
         * 1. Once we have defined your StaticVariables object, including the hidden variable,
         * the next step is to create a DAG structure over this set of variables.
         *
         * 2. To add parents to each variable, we first recover the ParentSet object by the method
         * getParentSet(Variable var) and then call the method addParent(Variable var).
         *
         * 3. We just put the hidden variable as parent of all the other variables. Following a naive-Bayes
         * like structure.
         */
        DAG dag = new DAG(variables);

        dag.getParentSet(a).addParent(hidden);
        dag.getParentSet(b).addParent(hidden);
        dag.getParentSet(c).addParent(hidden);
        dag.getParentSet(d).addParent(hidden);
        dag.getParentSet(e).addParent(hidden);
        dag.getParentSet(g).addParent(hidden);
        dag.getParentSet(h).addParent(hidden);
        dag.getParentSet(i).addParent(hidden);

        /**
         * We print the graph to see if is properly created.
         */
        System.out.println(dag.toString());

        /**
         * 1. We now create the Bayesian network from the previous DAG.
         *
         * 2. The BN object is created from the DAG. It automatically looks at the distribution type
         * of each variable and their parents to initialize the Distributions objects that are stored
         * inside (i.e. Multinomial, Normal, CLG, etc). The parameters defining these distributions are
         * properly initialized.
         *
         * 3. The network is printed and we can have look at the kind of distributions stored in the BN object.
         */
        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);
        System.out.println(bn.toString());

        /**
         * Finally teh Bayesian network is saved to a file.
         */
        BayesianNetworkWriter.saveToFile(bn, "networks/huginStaticBNHiddenExample.bn");

    }
}
