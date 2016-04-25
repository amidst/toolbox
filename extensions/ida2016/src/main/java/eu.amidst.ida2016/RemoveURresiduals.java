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


package eu.amidst.ida2016;

import eu.amidst.core.datastream.*;
import eu.amidst.core.distribution.Normal_MultinomialNormalParents;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

/**
 * This class removes the contribution of the UR variable to the IDA model
 *
 */
public class RemoveURresiduals {

    private static NaiveBayesVirtualConceptDriftDetector virtualDriftDetector;
    private static Variable unemploymentRateVar;

    static String path="/Users/ana/Documents/Amidst-MyFiles/CajaMar/dataWekaUnemploymentRate/dataWekaUnemploymentRate";
    static String outputPath = "/Users/ana/Documents/Amidst-MyFiles/CajaMar/dataNoResidualsNoUR/dataNoResidualsNoUR";

    private static void printOutput(double [] meanHiddenVars, int currentMonth){
        for (int j = 0; j < meanHiddenVars.length; j++) {
            System.out.print(currentMonth + "\t" + meanHiddenVars[j] + "\t");
            meanHiddenVars[j] = 0;
        }
        if (unemploymentRateVar != null) {
            System.out.print(virtualDriftDetector.getSvb().getPlateuStructure().
                    getNodeOfNonReplicatedVar(unemploymentRateVar).getAssignment().getValue(unemploymentRateVar) + "\t");
        }
        System.out.println();
    }

    public static void main(String[] args) throws IOException{

        int NSETS = 84;

        //We can open the data stream using the static class DataStreamLoader

        DataStream<DataInstance> dataMonth = DataStreamLoader.openFromFile(path+"0.arff");


        //We create a eu.amidst.ida2016.NaiveBayesVirtualConceptDriftDetector object
        virtualDriftDetector = new NaiveBayesVirtualConceptDriftDetector();

        //We set class variable as the last attribute
        virtualDriftDetector.setClassIndex(-1);

        virtualDriftDetector.setSeed(154);

        //We set the data which is going to be used
        virtualDriftDetector.setData(dataMonth);

        //We fix the size of the window
        int windowSize = 100;
        virtualDriftDetector.setWindowsSize(windowSize);

        //We fix the number of global latent variables
        virtualDriftDetector.setNumberOfGlobalVars(0);

        //We should invoke this method before processing any data
        virtualDriftDetector.initLearningWithUR();

        int[] peakMonths = {2, 8, 14, 20, 26, 32, 38, 44, 47, 50, 53, 56, 59, 62, 65, 68, 71, 74, 77, 80, 83};

        //virtualDriftDetector.deactivateTransitionMethod();

        //Some prints
        System.out.print("Month");
        for (Variable hiddenVar : virtualDriftDetector.getHiddenVars()) {
            System.out.print("\t" + hiddenVar.getName());
        }

        String unemploymentRateAttName = "UNEMPLOYMENT_RATE_ALMERIA";
        try {
            unemploymentRateVar = virtualDriftDetector.getSvb().getDAG().getVariables().getVariableByName(unemploymentRateAttName);
            System.out.print("\t UnempRate");
        } catch (UnsupportedOperationException e) {
        }

        //New dataset in which we remove the UR att
        List<Attribute> atts = dataMonth.getAttributes().getFullListOfAttributes();
        atts.remove(dataMonth.getAttributes().getAttributeByName(unemploymentRateAttName));
        Attributes attsNoUR = new Attributes(atts);
        DataOnMemoryListContainer<DataInstance> newData = new DataOnMemoryListContainer<>(attsNoUR);

        System.out.println();

        virtualDriftDetector.setTransitionVariance(0);

        double[] meanHiddenVars;;

        //System.out.println(virtualDriftDetector.getLearntBayesianNetwork());

        for (int i = 0; i < NSETS; i++) {

            int currentMonth = i;

            if (IntStream.of(peakMonths).anyMatch(x -> x == currentMonth))
                continue;

            dataMonth = DataStreamLoader.openFromFile(path+currentMonth+".arff");

            virtualDriftDetector.setTransitionVariance(0);

            meanHiddenVars = virtualDriftDetector.updateModel(dataMonth);

            virtualDriftDetector.setTransitionVariance(0.1);
            virtualDriftDetector.getSvb().applyTransition();

            //We print the output
            printOutput(meanHiddenVars, currentMonth);

            //Remove residuals

            BayesianNetwork learntBN = virtualDriftDetector.getLearntBayesianNetwork();
            HashMap<Variable, double[]> parameters = new HashMap<>();
            Variables vars = virtualDriftDetector.getLearntBayesianNetwork().getDAG().getVariables();
            for (Variable var : vars) {
                parameters.put(var,learntBN.getConditionalDistribution(var).getParameters());
            }

            System.out.println(learntBN);
            Variable classVar = vars.getVariableByName("DEFAULTING");
            dataMonth.restart();
            dataMonth = dataMonth.map(instance -> {
                vars.getListOfVariables().stream()
                        .filter(var -> !var.equals(unemploymentRateVar))
                        .filter(var -> !var.equals(classVar))
                        .forEach(var->{
                            int classVal = (int)instance.getValue(classVar);

                            Normal_MultinomialNormalParents dist = learntBN.getConditionalDistribution(var);
                            double b0=dist.getNormal_NormalParentsDistribution(classVal).getIntercept();
                            double b1=dist.getNormal_NormalParentsDistribution(classVal).getCoeffForParent(unemploymentRateVar);
                            double UR = instance.getValue(unemploymentRateVar);

                            instance.setValue(var, instance.getValue(var) - b0 - b1*UR);
                        });
                //instance.setValue(unemploymentRateVar, 0);
                //System.out.println(instance);
                return instance;
            });

            dataMonth.restart();

            for (DataInstance dataInstance : dataMonth) {
                newData.add(dataInstance);
            }

            //Print new dataset
            DataStreamWriter.writeDataToFile(newData,outputPath+currentMonth+".arff");

        }
    }
}
