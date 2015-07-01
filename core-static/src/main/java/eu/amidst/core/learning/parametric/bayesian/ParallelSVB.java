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

package eu.amidst.core.learning.parametric.bayesian;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.CompoundVector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Created by andresmasegosa on 30/06/15.
 */
public class ParallelSVB implements BayesianParameterLearningAlgorithm{

    DataStream<DataInstance> data;
    SVB[] svbEngines;
    DAG dag;
    int nCores = -1;
    SVB SVBEngine = new SVB();
    double logLikelihood;
    int seed = 0;
    boolean activateOutput=false;

    public void setSeed(int seed_){
        seed = seed_;
    }

    public void setNCores(int nCores) {
        this.nCores = nCores;
    }

    public SVB getSVBEngine() {
        return SVBEngine;
    }

    public void setSVBEngine(SVB SVBEngine) {
        this.SVBEngine = SVBEngine;
    }

    @Override
    public void initLearning() {
        if (this.nCores==-1)
            this.nCores=Runtime.getRuntime().availableProcessors();

        svbEngines = new SVB[nCores];

        for (int i = 0; i < nCores; i++) {
            svbEngines[i] = new SVB();
            svbEngines[i].setSeed(this.seed);
            svbEngines[i].setDAG(this.dag);
            //svbEngines[i].setPlateuStructure(this.SVBEngine.getPlateuStructure());
            //svbEngines[i].setTransitionMethod(this.SVBEngine.getTransitionMethod());
            svbEngines[i].setWindowsSize(this.SVBEngine.getWindowsSize());
            svbEngines[i].getPlateuStructure().getVMP().setOutput(activateOutput);
            svbEngines[i].initLearning();
        }


    }

    @Override
    public double updateModel(DataOnMemory<DataInstance> batch) {
        throw new UnsupportedOperationException("Use standard StreamingSVB for sequential updating");
    }

    @Override
    public void setDataStream(DataStream<DataInstance> data_) {
        this.data=data_;
    }

    @Override
    public double getLogMarginalProbability() {
        return this.logLikelihood;
    }

    @Override
    public void runLearning() {
        this.initLearning();


        Iterator<DataOnMemory<DataInstance>> iterator = this.data.iterableOverBatches(this.SVBEngine.getWindowsSize()).iterator();

        CompoundVector posterior =  this.svbEngines[0].getNaturalParameterPrior();
        logLikelihood = 0;
        while(iterator.hasNext()){

            //Load Data
            List<DataOnMemory<DataInstance>> dataBatches = new ArrayList();
            int cont=0;
            while (iterator.hasNext() && cont<nCores){
                dataBatches.add(iterator.next());
                cont++;
            }

            //Run Inference
            SVB.BatchOutput out=
                    IntStream.range(0, dataBatches.size())
                        .parallel()
                        .mapToObj(i -> this.svbEngines[i].updateModelOnBatchParallel(dataBatches.get(i)))
                        .reduce(SVB.BatchOutput::sum)
                        .get();

            //Update logLikelihood
            this.logLikelihood+=out.getElbo();

            //Combine the output
            posterior.sum(out.getVector());
            for (int i = 0; i < nCores; i++) {
                this.svbEngines[i].updateNaturalParameterPrior(posterior);
            }
        }

    }

    @Override
    public void setDAG(DAG dag_) {
        this.dag = dag_;
    }

    @Override
    public BayesianNetwork getLearntBayesianNetwork() {
        return this.svbEngines[0].getLearntBayesianNetwork();
    }

    @Override
    public void setParallelMode(boolean parallelMode) {

    }

    @Override
    public void setOutput(boolean activateOutput_) {
        activateOutput = activateOutput_;
    }
}