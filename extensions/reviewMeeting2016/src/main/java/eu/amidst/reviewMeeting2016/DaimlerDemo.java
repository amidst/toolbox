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

/**
 * Created by andresmasegosa on 19/1/16.
 */
public class DaimlerDemo {



    //static String fileARFF = "/Users/andresmasegosa/Desktop/DaimlerDataBinaryClass/2Labels/DaimlerFile.arff";

    //static String inputARFF ="/Users/andresmasegosa/Desktop/DaimlerDataBinaryClass/2Labels/DaimlerDataBinaryClass.arff";
    //static String networkFILE = "/Users/andresmasegosa/Desktop/DaimlerDataBinaryClass/AllLabels/dbn.bn";
    //static String inputARFF ="/Users/andresmasegosa/Desktop/DaimlerDataBinaryClass/AllLabels/DaimlerDataAllLabelsBinaryClass.arff";
    //static String fileARFF = "/Users/andresmasegosa/Desktop/DaimlerDataBinaryClass/AllLabels/DaimlerFile.arff";

    //public static void preprocess() throws Exception {
    //    DataStream<DataInstance> data = DataStreamLoader.openFromFile(inputARFF);
    //    DataStreamWriter.writeDataToFile(data,fileARFF);
    //}

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
                    //w.addParent(dynamicVariables.getInterfaceVariable(w.getMainVar()));
                });
        dag.getParentSetTimeT(classVar).addParent(dynamicVariables.getInterfaceVariable(classVar));

        return dag;
    }

    public static void main(String[] args) throws Exception {

        //--------------------- LEARNING PHASE --------------------------------------------------//
        String fileARFF = "/Users/andresmasegosa/Desktop/DaimlerDataBinaryClass/2Labels/DaimlerFile.arff";

        //Load the data in ARFF format
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(fileARFF);

        //Generate a dynamic naive Bayes structure
        DynamicDAG dynamicDAG = dynamicNaiveBayesStructure(data.getAttributes(),"MNVR_RuleLabeled");
        System.out.println(dynamicDAG);

        //Parameter Learning with Streaming variational Bayes VMP
        DynamicSVB svb = new DynamicSVB();

        //Set the desired options for the svb
        svb.setWindowsSize(1000);
        svb.setSeed(0);
        svb.setOutput(true);
        svb.setMaxIter(1000);
        svb.setThreshold(0.01);

        //We set the dynamicDAG, the data and start learning
        svb.setDynamicDAG(dynamicDAG);
        svb.setDataStream(data);
        svb.runLearning();

        //Get the learnt DBN
        DynamicBayesianNetwork dbnLearnt = svb.getLearntDBN();

        //Print the dynamic model
        System.out.println(dbnLearnt.toString());



        //--------------------- PREDICTION PHASE --------------------------------------------------//

        //We select VMP with the factored frontier algorithm as the Inference Algorithm
        FactoredFrontierForDBN FFalgorithm = new FactoredFrontierForDBN(new HuginInference());
        InferenceEngineForDBN.setInferenceAlgorithmForDBN(FFalgorithm);
        //Then, we set the DBN model
        InferenceEngineForDBN.setModel(dbnLearnt);


        //Get the class variable
        Variable classVar = dbnLearnt.getDynamicVariables().getVariableByName("MNVR_RuleLabeled");


        //We process the first 50 data sequences
        data.streamOfBatches(1000).limit(5).forEach( sequence -> {
            int time = 0 ;

            //For each instance of the data sequence
            for (DynamicDataInstance instance : sequence) {

                //The InferenceEngineForDBN must be reset at the begining of each sequence.
                if (instance.getTimeID() == 0){
                    InferenceEngineForDBN.reset();
                    time = 0;
                }

                //Remove the class label of the class variable
                instance.setValue(classVar,Double.NaN);

                //Set the evidence.
                InferenceEngineForDBN.addDynamicEvidence(instance);

                //Run inference
                InferenceEngineForDBN.runInference();

                //Query the posterior of the target variable
                Multinomial posterior = InferenceEngineForDBN.getFilteredPosterior(classVar);

                //Print the output
                System.out.println(instance.getSequenceID() +"\t"+posterior.getProbabilityOfState("LANECHANGE"));
            }
        });

    }
}