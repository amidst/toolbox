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

package eu.amidst.icdm2016;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.distribution.Normal_MultinomialNormalParents;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Variable;

import java.io.IOException;

/**
 * Created by andresmasegosa on 7/6/16.
 */
public class RemoveGlobalHiddenResiduals {

    static String outputPath="/Users/andresmasegosa/Documents/tmp/R3_";

    public static void remove(int currentMonth, NaiveBayesVirtualConceptDriftDetector virtualDriftDetector, DataStream<DataInstance> dataMonthi) throws IOException {
        //WRITE RESIDUALS
        Variable classVar = virtualDriftDetector.getClassVariable();
        Variable globalHidden = virtualDriftDetector.getHiddenVars().get(0);
        BayesianNetwork learntBN = virtualDriftDetector.getLearntBayesianNetwork();
        double globalHiddenMean = ((Normal) learntBN.getConditionalDistribution(globalHidden)).getMean();
        dataMonthi.restart();
        dataMonthi = dataMonthi.map(instance -> {
            learntBN.getVariables().getListOfVariables().stream()
                    .filter(var -> !var.equals(classVar))
                    .filter(var -> !var.equals(globalHidden))
                    .forEach(var -> {
                        int classVal = (int) instance.getValue(classVar);

                        Normal_MultinomialNormalParents dist = learntBN.getConditionalDistribution(var);
                        double b0 = dist.getNormal_NormalParentsDistribution(classVal).getIntercept();
                        double b1 = dist.getNormal_NormalParentsDistribution(classVal).getCoeffForParent(globalHidden);

                        //if (instance.getValue(variable) != 0)
                        instance.setValue(var, instance.getValue(var) - b0 - b1 * globalHiddenMean);
                    });
            return instance;
        });

        dataMonthi.restart();

        DataOnMemoryListContainer<DataInstance> newData = new DataOnMemoryListContainer<>(dataMonthi.getAttributes());

        for (DataInstance dataInstance : dataMonthi) {
            newData.add(dataInstance);
        }

        //Print new dataset
        DataStreamWriter.writeDataToFile(newData, outputPath + currentMonth + ".arff");
    }

}
