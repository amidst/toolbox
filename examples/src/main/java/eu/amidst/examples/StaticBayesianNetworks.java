/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.examples;

import eu.amidst.corestatic.datastream.DataInstance;
import eu.amidst.corestatic.io.DataStreamLoader;
import eu.amidst.corestatic.models.BayesianNetwork;
import eu.amidst.corestatic.io.BayesianNetworkWriter;
import eu.amidst.corestatic.models.DAG;
import eu.amidst.corestatic.datastream.DataStream;
import eu.amidst.corestatic.variables.Variables;
import eu.amidst.corestatic.variables.Variable;

import java.util.Arrays;

/**
 *  This class contains examples with the creation of different static BNs models. The are illustrative
 *  examples on how the AMIDST toolbox works when creating static BN Models.
 *
 * Created by andresmasegosa on 22/11/14.
 */
public final class StaticBayesianNetworks {

    private StaticBayesianNetworks(){
        //Not called
    }
    /**
     * In this example, we take a data set, create a BN and we compute the log-likelihood of all the samples
     * of this data set. The numbers defining the probability distributions of the BN are randomly fixed.
     * @throws Exception
     */
    public static void staticBNNoHidden() throws Exception {

        /**
         * 1. Our data is on disk and does not fit in memory. So, we use a DataOnDisk object.
         * 2. Our data is static and is on file, so we create the DataOnDisk using a StaticDataOnDiskFromFile object.
         * 3. Our data is in Weka format, so we use a WekaDataFileReader.
         */
        DataStream<DataInstance> data = DataStreamLoader.loadFromFile("datasets/syntheticData.arff");


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
         * 1. Once you have defined your StaticVariables object, the next step is to create
         * a DAG structure over this set of variables.
         *
         * 2. To add parents to each variable, we first recover the ParentSet object by the method
         * getParentSet(Variable var) and then call the method addParent().
         */
        DAG dag = new DAG(variables);

        dag.getParentSet(e).addParent(a);
        dag.getParentSet(e).addParent(b);

        dag.getParentSet(h).addParent(a);
        dag.getParentSet(h).addParent(b);

        dag.getParentSet(i).addParent(a);
        dag.getParentSet(i).addParent(b);
        dag.getParentSet(i).addParent(c);
        dag.getParentSet(i).addParent(d);

        dag.getParentSet(g).addParent(c);
        dag.getParentSet(g).addParent(d);

        /**
         * 1. We first check if the graph contains cycles.
         *
         * 2. We print out the created DAG. We can check that everything is as expected.
         */
        if (dag.containCycles()) {
            try {
            } catch (Exception ex) {
                throw new IllegalArgumentException(ex);
            }
        }

        System.out.println(dag.toString());


        /**
         * 1. We now create the Bayesian network from the previous DAG.
         *
         * 2. The BN object is created from the DAG. It automatically looks at the distribution tye
         * of each variable and their parents to initialize the Distributions objects that are stored
         * inside (i.e. Multinomial, Normal, CLG, etc). The parameters defining these distributions are
         * properly initialized.
         *
         * 3. The network is printed and we can have look at the kind of distributions stored in the BN object.
         */
        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);
        System.out.println(bn.toString());


        /**
         * 1. We iterate over the data set sample by sample.
         *
         * 2. For each sample or DataInstance object, we compute the log of the probability that the BN object
         * assigns to this observation.
         *
         * 3. We accumulate these log-probs and finally we print the log-prob of the data set.
         */
        double logProb = 0;
        for (DataInstance instance : data) {
            logProb += bn.getLogProbabiltyOf(instance);
        }
        System.out.println(logProb);

        BayesianNetworkWriter.saveToFile(bn,"networks/huginStaticBNExample.bn");
    }

    /**
     * In this example, we simply show how to create a BN model with hidden variables. We simply
     * create a BN for clustering, i.e.,  a naive-Bayes like structure with a single common hidden variable
     * acting as parant of all the observable variables.
     *
     * @throws Exception
     */
    public static void staticBNWithHidden() throws Exception {
        /**
         * 1. Our data is on disk and does not fit in memory. So, we use a DataOnDisk object.
         * 2. Our data is static and is on file, so we create the DataOnDisk using a StaticDataOnDiskFromFile object.
         * 3. Our data is in Weka format, so we use a WekaDataFileReader.
         */
        DataStream<DataInstance> data = DataStreamLoader.loadFromFile("datasets/syntheticData.arff");

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

        Variable hidden = variables.newMultionomialVariable("HiddenVar",Arrays.asList("TRUE", "FALSE"));

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

        BayesianNetworkWriter.saveToFile(bn,"networks/huginStaticBNHiddenExample.bn");

    }

    public static void main(String[] args) throws Exception {
        StaticBayesianNetworks.staticBNNoHidden();
        StaticBayesianNetworks.staticBNWithHidden();
    }
}
