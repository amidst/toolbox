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

        //Load the data in Weka's ARFF format
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile
                ("/Users/helgel/Desktop/DaimlerFile-AllLabelsRecoded-100000-train.arff");

        //Generate a dynamic naive Bayes structure
        DynamicDAG dynamicDAG = dynamicNaiveBayesStructure(data.getAttributes(),"MNVR_RuleLabeled");

        //Parameter Learning with Streaming variational Bayes VMP
        //Set the desired options
        DynamicSVB svb = new DynamicSVB();
        svb.setWindowsSize(1000);   // Samples handled together
        svb.setSeed(0);             // For the random initialization
        svb.setOutput(true);        // Dump learning progress
        svb.setMaxIter(100);        // Max no. iterations by the variational Bayes message passing
        svb.setThreshold(0.0001);   // Cnvergence criteria: Monitor lower-bound increase

        //We give the dynamicDAG and the data to the learner
        // and then we run the learning
        svb.setDynamicDAG(dynamicDAG);
        svb.setDataStream(data);
        System.out.println("Start learning: Report no.iter and Expecected lower-bound per sequence");
        svb.runLearning();

        //Get the learned DBN for the inference engine,
        // and modify to include expert knowledge:
        // ** LC is cannot be followed by LF **
        DynamicBayesianNetwork dbnLearned = svb.getLearntDBN();
        Variable classVar = dbnLearned.getDynamicVariables().getVariableByName("MNVR_RuleLabeled");
        Multinomial_MultinomialParents dist = dbnLearned.getConditionalDistributionTimeT(classVar);
        dist.getMultinomial(0).setProbabilities(new double[]{1.0,0.0});

        //Print the dynamic model to screen
        System.out.println("\nThe learned network:\n============================");
        System.out.println(dbnLearned.toString());

        //--------------------- PREDICTION PHASE --------------------------------------------------//

        //We select HUGIN as the Inference Algorithm.
        // It is accessible through the HuginLink (imported)
        InferenceEngineForDBN.setInferenceAlgorithmForDBN(new FactoredFrontierForDBN(new HuginInference()));

        //Then, we give the DBN model to the inference engine
        // and then load the test-set
        InferenceEngineForDBN.setModel(dbnLearned);
        DataStream<DynamicDataInstance> dataTest = DynamicDataStreamLoader.loadFromFile
                ("/Users/helgel/Desktop/DaimlerFile-AllLabelsRecoded-100000-test.arff");

        //We process the first few data sequences and show results
        System.out.println("Report P(LC) per Sequence-ID & Time-step:");
        data.streamOfBatches(1000).limit(1).forEach( sequence -> {

            //For each instance of the data sequence
            for (DynamicDataInstance instance : sequence) {

                //The InferenceEngineForDBN must be reset at the beginning of each sequence.
                if (instance.getTimeID() == 0){
                    InferenceEngineForDBN.reset();
                }

                // Hide the class label from the test-set
                instance.setValue(classVar, Double.NaN);

                //Set the evidence, and then calculate posterior
                InferenceEngineForDBN.addDynamicEvidence(instance);
                InferenceEngineForDBN.runInference();

                //Query the posterior of the target variable and print output to screen
                Multinomial posterior = InferenceEngineForDBN.getFilteredPosterior(classVar);
                System.out.println(instance.getSequenceID() + "\t" +
                        posterior.getProbabilityOfState("LANECHANGE"));
            }
        });

    }
}