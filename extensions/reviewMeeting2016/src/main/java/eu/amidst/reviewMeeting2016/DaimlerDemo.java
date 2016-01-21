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

package eu.amidst.reviewMeeting2016;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.inference.FactoredFrontierForDBN;
import eu.amidst.dynamic.inference.InferenceEngineForDBN;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.learning.dynamic.DynamicSVB;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.huginlink.inference.HuginInference;

// Created by andresmasegosa on 19/1/16.

public class DaimlerDemo {

    /**
     * This static method builds a dynamic Naive Bayes classifier for the given set of Attributes and the
     * given class name.
     * @param attributes, a set of attributes
     * @param className, the name of the attribute which contains the class.
     * @return A valid DynamicDAG object.
     */
    private static DynamicDAG dynamicNaiveBayesStructure(Attributes attributes, String  className){

        //Create the dynamic variables from the given attributes
        DynamicVariables dynamicVariables = new DynamicVariables(attributes);

        //Get the class variable
        Variable classVar = dynamicVariables.getVariableByName(className);

        //Create an empty dynamic DAG
        DynamicDAG dag = new DynamicDAG(dynamicVariables);

        //Add the parents sets
        dag.getParentSetsTimeT().stream()
                .filter(w -> w.getMainVar().getVarID() != classVar.getVarID())
                .forEach(w -> {
                    w.addParent(classVar);
                });
        dag.getParentSetTimeT(classVar).addParent(dynamicVariables.getInterfaceVariable(classVar));

        return dag;
    }

    public static void main(String[] args) throws Exception {

        //--------------------- LEARNING PHASE --------------------------------------------------//

        //Load the data in ARFF format
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile
                ("/Users/helgel/Desktop/DaimlerFileAllLabelsRecoded-100000-selection-train.arff");

        //Generate a dynamic naive Bayes structure
        DynamicDAG dynamicDAG = dynamicNaiveBayesStructure(data.getAttributes(),"MNVR_RuleLabeled");

        //Parameter Learning with Streaming variational Bayes VMP
        DynamicSVB svb = new DynamicSVB();

        //Set the desired options for the svb
        svb.setWindowsSize(1000);
        svb.setSeed(0);
        svb.setOutput(true);
        svb.setMaxIter(100);
        svb.setThreshold(0.001);

        //We set the dynamicDAG, the data and start learning
        svb.setDynamicDAG(dynamicDAG);
        svb.setDataStream(data);
        svb.runLearning();

        //Get the learnt DBN, and modify to include expert knowledge: LC is not followed by LF
        DynamicBayesianNetwork dbnLearnt = svb.getLearntDBN();
        Variable classVar = dbnLearnt.getDynamicVariables().getVariableByName("MNVR_RuleLabeled");
        Multinomial_MultinomialParents dist = dbnLearnt.getConditionalDistributionTimeT(classVar);
        dist.getMultinomial(0).setProbabilities(new double[]{1.0,0.0});

        //Print the dynamic model to screen
        System.out.println(dbnLearnt.toString());


        //--------------------- PREDICTION PHASE --------------------------------------------------//

        //We select HUGIN as the Inference Algorithm. It is accessible through the HuginLink
        InferenceEngineForDBN.setInferenceAlgorithmForDBN(new FactoredFrontierForDBN(new HuginInference()));

        //Then, we set the DBN model and load the test-set
        InferenceEngineForDBN.setModel(dbnLearnt);
        DataStream<DynamicDataInstance> dataTest = DynamicDataStreamLoader.loadFromFile
                ("/Users/helgel/Desktop/DaimlerFileAllLabelsRecoded-100000-selection-test.arff");

        //We process the first few data sequences and show results
        data.streamOfBatches(1000).limit(2).forEach( sequence -> {

            //For each instance of the data sequence
            for (DynamicDataInstance instance : sequence) {

                //The InferenceEngineForDBN must be reset at the beginning of each sequence.
                if (instance.getTimeID() == 0){
                    InferenceEngineForDBN.reset();
                }

                // Remove the class label
                instance.setValue(classVar, Double.NaN);

                //Set the evidence.
                InferenceEngineForDBN.addDynamicEvidence(instance);

                //Run inference
                InferenceEngineForDBN.runInference();

                //Query the posterior of the target variable and print output
                Multinomial posterior = InferenceEngineForDBN.getFilteredPosterior(classVar);
                System.out.println(instance.getSequenceID() + "\t" + posterior.getProbabilityOfState("LANECHANGE"));
            }
        });

    }
}