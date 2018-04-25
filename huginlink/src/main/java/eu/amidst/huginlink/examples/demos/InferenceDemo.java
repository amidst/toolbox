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
package eu.amidst.huginlink.examples.demos;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.learning.parametric.DynamicNaiveBayesClassifier;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.huginlink.converters.DBNConverterToHugin;

import java.io.IOException;
import java.util.*;

/**
 * This class is a demo for making inference in a Dynamic Bayesian network model learnt from the Cajamar data set
 * using the Hugin inference engine.
 */
public class InferenceDemo {

    /**
     * Prints the belief of all the nodes in the Hugin model.
     * @param domainObject the expanded dynamic model.
     * @throws ExceptionHugin
     */
    public static void printBeliefs (Domain domainObject) throws ExceptionHugin {

        domainObject.getNodes().stream().forEach((node) -> {
            try {
                System.out.print("\n" + node.getName()+ ": ");
                int numStates = (int)((LabelledDCNode)node).getNumberOfStates();
                for (int j=0;j<numStates;j++){
                    System.out.print(((LabelledDCNode) node).getBelief(j) + " ");
                }

            } catch (ExceptionHugin exceptionHugin) {
                exceptionHugin.printStackTrace();
            }
        });
    }


    /**
     * The demo for the Cajamar data set.
     * @throws ExceptionHugin
     * @throws IOException
     */
    public static void demo() throws ExceptionHugin, IOException {

        //************************************************************
        //********************** LEARNING IN AMIDST ******************
        //************************************************************

        String file = "./datasets/simulated/bank_data_train.arff";
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(file);

        //System.out.println("ATTRIBUTES:");
        //data.getAttributes().getFullListOfAttributes().stream().forEach(a -> System.out.println(a.getName()));

        System.out.println("Learning a Dynamic Naive Bayes Classifier.");
        System.out.println("Traning Data: 4000 clients, 1000 days of records for each client, 10 profile variables.");

        DynamicNaiveBayesClassifier model = new DynamicNaiveBayesClassifier();
        //model.setClassVarID(data.getAttributes().getNumberOfAttributes()-3);//We set -3 to account for time id and seq_id
        model.setClassVarID(data.getAttributes().getAttributeByName("DEFAULT").getIndex() + 2);
        model.setParallelMode(true);
        model.learn(data);
        DynamicBayesianNetwork amidstDBN = model.getDynamicBNModel();

        //We randomly initialize the parensets Time 0 because parameters are wrongly learnt due
        //Random rand = new Random(0);
        //amidstDBN.getConditionalDistributionTime0().forEach(w -> w.randomInitialization(rand));
        System.out.println();
        System.out.println();
        System.out.println(amidstDBN.toString());

        Class huginDBN = DBNConverterToHugin.convertToHugin(amidstDBN);

         //************************************************************
         //********************** INFERENCE IN HUGIN ******************
         //************************************************************

         file = "./datasets/simulated/bank_data_predict.arff";
         data = DynamicDataStreamLoader.loadFromFile(file);

         // The value of the timeWindow must be sampleSize-1 at maximum
         int timeSlices = 9;


         System.out.println("Computing Probabilities of Defaulting for 10 clients using Hugin API:\n");

         Iterator<DynamicDataInstance> iterator = data.iterator();
         LabelledDCNode lastDefault =null;

        long currentSequenceID = 0;
        DynamicDataInstance dataInstance = iterator.next();

        Domain domainObject = huginDBN.createDBNDomain(timeSlices);


         while (iterator.hasNext()) {
             // Create a DBN runtime domain (from a Class object) with a time window of 'nSlices' .
             // The domain must be created using the method 'createDBNDomain'


             // ENTERING EVIDENCE IN ALL THE SLICES OF THE INITIAL EXPANDED DBN
             for (int i = 0; i <= timeSlices && iterator.hasNext(); i++) {
                 //System.out.println("\n-----> " + dataInstance.getTimeID() + ", " + dataInstance.getSequenceID());

                 for (Variable var: amidstDBN.getDynamicVariables().getListOfDynamicVariables()){
                     //Avoid entering evidence in class variable to have something to predict
                     if ((var.getVarID()!=model.getClassVarID())){
                         LabelledDCNode node = (LabelledDCNode)domainObject.getNodeByName("T"+i+"."+var.getName());
                         node.selectState((int)dataInstance.getValue(var));
                     }
                 }
                 dataInstance= iterator.next();
             }
             System.out.println("T"+timeSlices + ".DEFAULT");
             lastDefault =  (LabelledDCNode)domainObject.getNodeByName("T"+timeSlices + ".DEFAULT");
             domainObject.triangulateDBN(Domain.H_TM_TOTAL_WEIGHT);
             domainObject.compile();

//             while (currentSequenceID==dataInstance.getSequenceID() && iterator.hasNext()) {
//
//                 //System.out.println("TIME_ID: "+ dataInstance.getTimeID() + "  CUSTOMER ID:" +  dataInstance.getSequenceID());
//
//                 //Before moving the window
//                 lastDefault =  (LabelledDCNode)domainObject.getNodeByName("T"+timeSlices + ".DEFAULT");
//                 domainObject.moveDBNWindow(1);
//                 domainObject.uncompile();
//
//                 for (Variable var : amidstDBN.getDynamicVariables().getListOfDynamicVariables()) {
//                     //Avoid entering evidence in class variable to have something to predict
//                     if ((var.getVarID()!=model.getClassVarID())) {
//                         LabelledDCNode node = (LabelledDCNode) domainObject.getNodeByName("T" + timeSlices + "." + var.getName());
//                         node.selectState((long) dataInstance.getValue(var));
//                     }
//                 }
//
//                 domainObject.triangulateDBN(Domain.H_TM_TOTAL_WEIGHT);
//                 domainObject.compile();
//
//                 dataInstance= iterator.next();
//             }
             System.out.println("CLIENT ID: " + currentSequenceID + "   " + " Probability of defaulting: " +lastDefault.getBelief(1));
             domainObject.uncompile();

             domainObject.retractFindings();

             currentSequenceID = dataInstance.getSequenceID();
         }
     }

    public static void main(String[] args) throws ExceptionHugin, IOException {
        InferenceDemo.demo();
    }

}
