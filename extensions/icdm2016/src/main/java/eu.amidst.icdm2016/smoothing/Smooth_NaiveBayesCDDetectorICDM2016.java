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

package eu.amidst.icdm2016.smoothing;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ana@cs.aau.dk on 27/04/16.
 */
public class Smooth_NaiveBayesCDDetectorICDM2016 {

    private static Smooth_NaiveBayesVirtualConceptDriftDetector virtualDriftDetector;
    private static Variable unemploymentRateVar;
    private static boolean includeUR = false;
    private static boolean includeIndicators = false;

    //static String[] varNames = {"VAR01","VAR02","VAR03","VAR04","VAR07","VAR08"};


    static int windowSize = 39000;

    static String path = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWeka/dataWeka";

    //static String path="/Users/ana/Documents/Amidst-MyFiles/CajaMar/dataWeka/dataWeka";
    //static String path="/Users/ana/Documents/Amidst-MyFiles/CajaMar/dataWekaUnemploymentRateShifted/dataWekaUnemploymentRateShifted";
    //static String path="/Users/ana/Documents/Amidst-MyFiles/CajaMar/dataNoResidualsNoUR/dataNoResidualsNoUR";
    private static void printOutput(double [] meanHiddenVars, int currentMonth){
        for (int j = 0; j < meanHiddenVars.length; j++) {
            System.out.print(currentMonth + "\t" + meanHiddenVars[j]);
            meanHiddenVars[j] = 0;
        }

        if (unemploymentRateVar != null) {
            System.out.print("\t" +virtualDriftDetector.getSvb().getPlateuStructure().
                    getNodeOfNonReplicatedVar(unemploymentRateVar).getAssignment().getValue(unemploymentRateVar));
        }
        System.out.println();
    }


    public static void main(String[] args) {

        int NSETS = 84;

        String[] varNames = {"VAR07"};

        int numVars = varNames.length;

        //We can open the data stream using the static class DataStreamLoader

/*
        args = new String[3];
        args[0] = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWeka/joinMonthsMinor.arff";
        args[1] = "2";
        args[2] = "100";
*/

        if (Integer.parseInt(args[1])==0){
            varNames=new String[]{"VAR04"};
        }else if (Integer.parseInt(args[1])==1){
            varNames=new String[]{"VAR07"};
        }else{
            varNames = new String[]{"VAR01","VAR02","VAR03","VAR04","VAR07","VAR08"};
        }

        double transitionVariance = Double.parseDouble(args[2]);

        String outputFile = args[0];


        DataStream<DataInstance> dataMonth0 = DataStreamLoader.openFromFile(outputFile);

        List<Attribute> attsSubSetList = new ArrayList<>();

        for (Attribute attribute : dataMonth0.getAttributes()) {
            if (attribute.getName().startsWith("DEFAULTING"))
                attsSubSetList.add(attribute);
            for (String varName : varNames) {
                if (attribute.getName().startsWith(varName))
                    attsSubSetList.add(attribute);
            }
        }

        String unemploymentRateAttName = "UNEMPLOYMENT_RATE_ALMERIA";
        if(includeUR){
            attsSubSetList.add(dataMonth0.getAttributes().getAttributeByName(unemploymentRateAttName));
        }
        Attributes attsSubset = new Attributes(attsSubSetList);

        //We create a eu.amidst.eu.amidst.icdm2016.NaiveBayesVirtualConceptDriftDetector object
        virtualDriftDetector = new Smooth_NaiveBayesVirtualConceptDriftDetector();

        //We set class variable as the last attribute
        virtualDriftDetector.setClassIndex(-1);

        virtualDriftDetector.setSeed(1);

        //We set the data which is going to be used
        //virtualDriftDetector.setData(dataMonth0);
        virtualDriftDetector.setAttributes(attsSubset);

        //We fix the size of the window
        virtualDriftDetector.setWindowsSize(windowSize);

        virtualDriftDetector.setOutput(false);

        //We fix the number of global latent variables
        virtualDriftDetector.setNumberOfGlobalVars(1);

        virtualDriftDetector.setIncludeUR(includeUR);

        virtualDriftDetector.setIncludeIndicators(includeIndicators);


        virtualDriftDetector.setTransitionVariance(transitionVariance);

        //We should invoke this method before processing any data
        virtualDriftDetector.initLearning();

        System.out.println(virtualDriftDetector.getSvb().getDAG());

        //If UR is to be included
        //virtualDriftDetector.initLearningWithUR();

        //Some prints
        System.out.print("Month");
        for (Variable hiddenVar : virtualDriftDetector.getHiddenVars()) {
            System.out.print("\t" + hiddenVar.getName());
        }

        try {
            unemploymentRateVar = virtualDriftDetector.getSvb().getDAG().getVariables().getVariableByName(unemploymentRateAttName);
            System.out.print("\t UnempRate");
        } catch (UnsupportedOperationException e) {
        }


        System.out.println();

        double[] meanHiddenVars;





            meanHiddenVars = virtualDriftDetector.updateModel(dataMonth0);

        System.out.println("-----------");
        for (int i = 0; i < meanHiddenVars.length; i++) {
            System.out.println(meanHiddenVars[i]);
        }
        System.out.println("-----------");


            List<Variable> param = virtualDriftDetector.getSvb().getPlateuStructure().getNonReplicatedVariables();

            for (Variable variable : param) {
                if (!variable.isNormal() && !variable.isNormalParameter())
                    continue;
                    Normal dist = virtualDriftDetector.getSvb().getParameterPosterior(variable);
                    System.out.print(variable.getName() + "\t" + dist.getMean() + "\t" + dist.getVariance() + "\t");
            }

            System.out.println();

            virtualDriftDetector.setTransitionVariance(transitionVariance);
            virtualDriftDetector.getSvb().applyTransition();

            //System.out.println(virtualDriftDetector.getLearntBayesianNetwork());

            //We print the output
            //printOutput(meanHiddenVars, currentMonth);


    }
}
