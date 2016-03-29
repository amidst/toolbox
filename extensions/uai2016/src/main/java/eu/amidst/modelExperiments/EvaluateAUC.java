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

package eu.amidst.modelExperiments;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.MissingAssignment;
import eu.amidst.core.variables.Variable;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.Prediction;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.core.Instances;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dario on 25/02/16.
 */
public class EvaluateAUC {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        String networkFolder = args[0];
        String dataFolder = args[1];

        Path networkPath = Paths.get(networkFolder);
        List<Path> networkFiles = listSourceFiles(networkPath,"*.bn");
        Path dataPath = Paths.get(dataFolder);
        List<Path> dataFiles = listSourceFiles(dataPath,"*.arff");

//        networkFiles.forEach(network -> System.out.println(network.toString()));
//        dataFiles.forEach(network -> System.out.println(network.toString()));

        networkFiles.forEach(networkFile -> {
            System.out.println("\nFILENAME: " + networkFile.getFileName().toString());
            BayesianNetwork model;
            try {
                model = BayesianNetworkLoader.loadFromFile(networkFile.toString());
                Variable classVariable = model.getVariables().getVariableByName("Default");

                dataFiles.forEach(dataFile -> {
                    System.out.println("DATA FILE: " + dataFile.getFileName().toString());
                    DataStream<DataInstance> data = DataStreamLoader.openFromFile(dataFile.toString());

                    InferenceAlgorithm inferenceVMP = new VMP();
                    InferenceAlgorithm inferenceIS = new ImportanceSampling();

                    ArrayList<Prediction> predictionsVMP = new ArrayList<>();
                    ArrayList<Prediction> predictionsIS = new ArrayList<>();

                    for (DataInstance instance : data) {
                        double classValue = instance.getValue(classVariable);

                        Prediction prediction;
                        Multinomial posterior;

                        inferenceVMP.setModel(model);
                        inferenceIS.setModel(model);

                        MissingAssignment assignment = new MissingAssignment(instance);
                        assignment.addMissingVariable(classVariable);

                        // Inference with VMP:
                        inferenceVMP.setEvidence(assignment);
                        inferenceVMP.runInference();
                        posterior = inferenceVMP.getPosterior(classVariable);
                        prediction = new NominalPrediction(classValue, posterior.getProbabilities());
                        predictionsVMP.add(prediction);

                        // Inference with IS:
                        inferenceIS.setEvidence(assignment);
                        inferenceIS.runInference();
                        posterior = inferenceIS.getPosterior(classVariable);
                        prediction = new NominalPrediction(classValue, posterior.getProbabilities());
                        predictionsIS.add(prediction);
                    }

                    ThresholdCurve thresholdCurveVMP = new ThresholdCurve();
                    Instances tcurveVMP = thresholdCurveVMP.getCurve(predictionsVMP);

                    ThresholdCurve thresholdCurveIS = new ThresholdCurve();
                    Instances tcurveIS = thresholdCurveIS.getCurve(predictionsIS);

                    System.out.println("AUROC VMP: " + ThresholdCurve.getROCArea(tcurveVMP) + ", AUROC IS: " +  ThresholdCurve.getROCArea(tcurveIS) );
                });
            }
            catch(Exception e) {
                System.out.println(e.getMessage());
            }
        });
    }

    public static List<Path> listSourceFiles(Path dir, String pattern) throws IOException {
        List<Path> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, pattern)) {
            for (Path entry : stream) {
                result.add(entry);
            }
        } catch (DirectoryIteratorException ex) {
            // I/O error encounted during the iteration, the cause is an IOException
            throw ex.getCause();
        }
        return result;
    }
}
