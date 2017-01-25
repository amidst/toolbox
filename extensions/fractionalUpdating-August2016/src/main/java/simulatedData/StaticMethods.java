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

package simulatedData;

import eu.amidst.core.learning.parametric.bayesian.*;
import eu.amidst.lda.core.MultiDriftLDAv1;
import eu.amidst.lda.core.MultiDriftLDAv2;

/**
 * Created by andresmasegosa on 10/11/16.
 */
public class StaticMethods {


    static int sampleSize = 10000;
    static int totalITER = 10;

    public static BayesianParameterLearningAlgorithm initSVB(){
        SVB svb = new SVB();
        //MultiDriftSVB svb = new MultiDriftSVB();
        //StochasticVI svb = new StochasticVI();

        svb.getPlateuStructure().getVMP().setTestELBO(true);
        svb.getPlateuStructure().getVMP().setMaxIter(1000);
        svb.getPlateuStructure().getVMP().setOutput(true);
        svb.getPlateuStructure().getVMP().setThreshold(0.00001);

        svb.setWindowsSize(sampleSize);

        return svb;
    }

    public static BayesianParameterLearningAlgorithm initDrift(){
        DriftSVB svb = new DriftSVB();

        svb.getPlateuStructure().getVMP().setTestELBO(true);
        svb.getPlateuStructure().getVMP().setMaxIter(1000);
        svb.getPlateuStructure().getVMP().setOutput(true);
        svb.getPlateuStructure().getVMP().setThreshold(0.00001);

        svb.setWindowsSize(sampleSize);

        return svb;
    }

    public static BayesianParameterLearningAlgorithm initMultiDriftLDAv2(){
        MultiDriftLDAv2 svb = new MultiDriftLDAv2();

        svb.getPlateuStructure().getVMP().setTestELBO(true);
        svb.getPlateuStructure().getVMP().setMaxIter(1000);
        svb.getPlateuStructure().getVMP().setOutput(true);
        svb.getPlateuStructure().getVMP().setThreshold(0.00001);

        svb.setWindowsSize(sampleSize);
        return svb;
    }
    public static BayesianParameterLearningAlgorithm initMultiDriftLDAv1(){
        MultiDriftLDAv1 svb = new MultiDriftLDAv1();

        svb.getPlateuStructure().getVMP().setTestELBO(true);
        svb.getPlateuStructure().getVMP().setMaxIter(1000);
        svb.getPlateuStructure().getVMP().setOutput(true);
        svb.getPlateuStructure().getVMP().setThreshold(0.00001);

        svb.setWindowsSize(sampleSize);
        return svb;
    }

    public static BayesianParameterLearningAlgorithm initMultiDrift(){
        MultiDriftSVB svb = new MultiDriftSVB();

        svb.getPlateuStructure().getVMP().setTestELBO(true);
        svb.getPlateuStructure().getVMP().setMaxIter(1000);
        svb.getPlateuStructure().getVMP().setOutput(true);
        svb.getPlateuStructure().getVMP().setThreshold(0.00001);

        svb.setWindowsSize(sampleSize);
        return svb;
    }

    public static BayesianParameterLearningAlgorithm initSVI(){
        StochasticVI svb = new StochasticVI();

        svb.getSVB().getPlateuStructure().getVMP().setTestELBO(true);
        svb.getSVB().getPlateuStructure().getVMP().setMaxIter(1000);
        svb.getSVB().getPlateuStructure().getVMP().setOutput(true);
        svb.getSVB().getPlateuStructure().getVMP().setThreshold(0.00001);

        svb.setLocalThreshold(0.01);
        svb.setOutput(true);
        svb.setMaximumLocalIterations(100);
        svb.setBatchSize(sampleSize);
        svb.setDataSetSize(sampleSize*totalITER);
        svb.setLearningFactor(1.0);
        svb.setFixedStepSize(false);




        return svb;
    }


    public static BayesianParameterLearningAlgorithm initPopulation(){
        PopulationVI svb = new PopulationVI();

        svb.getSVB().getPlateuStructure().getVMP().setTestELBO(true);
        svb.getSVB().getPlateuStructure().getVMP().setMaxIter(1000);
        svb.getSVB().getPlateuStructure().getVMP().setOutput(true);
        svb.getSVB().getPlateuStructure().getVMP().setThreshold(0.00001);

        svb.setLocalThreshold(0.01);
        svb.setOutput(true);
        svb.setMaximumLocalIterations(100);
        svb.setBatchSize(sampleSize);
        svb.setDataSetSize(sampleSize);
        svb.setLearningFactor(0.9);
        svb.setFixedStepSize(true);




        return svb;
    }
}
