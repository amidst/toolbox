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
package eu.amidst.core.conceptdrift;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.learning.parametric.bayesian.BayesianParameterLearningAlgorithm;
import eu.amidst.core.conceptdrift.utils.Fading;
import eu.amidst.core.learning.parametric.bayesian.utils.DataPosterior;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.learning.parametric.bayesian.utils.PlateuStructure;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#svbfadingexample"> http://amidst.github.io/toolbox/CodeExamples.html#svbfadingexample </a>  </p>
 */
public class SVBFading implements BayesianParameterLearningAlgorithm, FadingLearner{
    SVB svb;

    public SVBFading(){
        svb = new SVB();
        svb.setTransitionMethod(new Fading(0.999));
    }

    public SVB getSVB(){
        return svb;
    }

    public double getFadingFactor() {
        return ((Fading)svb.getTransitionMethod()).getFadingFactor();
    }

    public void setFadingFactor(double fadingFactor) {
        ((Fading)svb.getTransitionMethod()).setFadingFactor(fadingFactor);
    }

    public int getWindowsSize() {
        return svb.getWindowsSize();
    }

    public void setWindowsSize(int windowsSize) {
        this.svb.setWindowsSize(windowsSize);
    }

    @Override
    public List<DataPosterior> computePosterior(DataOnMemory<DataInstance> batch) {
        throw new UnsupportedOperationException("Method not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataPosterior> computePosterior(DataOnMemory<DataInstance> batch, List<Variable> latentVariables) {
        throw new UnsupportedOperationException("Method not implemented");
    }

    @Override
    public void initLearning() {
        svb.initLearning();
    }

    @Override
    public double updateModel(DataOnMemory<DataInstance> batch) {
        return svb.updateModel(batch);
    }

    @Override
    public void setDataStream(DataStream<DataInstance> data) {
        svb.setDataStream(data);
    }

    @Override
    public double getLogMarginalProbability() {
        return 0;
    }

    @Override
    public void runLearning() {
        svb.runLearning();
    }

    @Override
    public void setDAG(DAG dag) {
        svb.setDAG(dag);
    }

    @Override
    public void setSeed(int seed) {
        svb.setSeed(seed);
    }

    @Override
    public BayesianNetwork getLearntBayesianNetwork() {
        return svb.getLearntBayesianNetwork();
    }

    @Override
    public void setParallelMode(boolean parallelMode) {
        throw new UnsupportedOperationException("Non Parallel Mode Supported.");
    }

    @Override
    public void setOutput(boolean activateOutput) {
        svb.setOutput(activateOutput);
    }

    @Override
    public double predictedLogLikelihood(DataOnMemory<DataInstance> batch) {
        return this.svb.predictedLogLikelihood(batch);
    }

    @Override
    public void setPlateuStructure(PlateuStructure plateuStructure) {
        this.svb.setPlateuStructure(plateuStructure);
    }

    @Override
    public void randomInitialize() {
        this.svb.randomInitialize();
    }
}
