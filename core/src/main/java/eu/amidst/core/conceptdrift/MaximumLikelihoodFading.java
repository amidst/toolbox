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


import com.google.common.util.concurrent.AtomicDouble;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;

/**
 * This class extends the {@link ParallelMaximumLikelihood} class and implements the {@link FadingLearner} interface.
 * It defines the Maximum Likelihood Fading approach.
 *
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#mlfadingexample"> http://amidst.github.io/toolbox/CodeExamples.html#mlfadingexample </a>  </p>
 */
public class MaximumLikelihoodFading extends ParallelMaximumLikelihood implements FadingLearner {

    /** Represents the fading factor or rate. */
    double fadingFactor;

    /**
     * Returns the fading factor.
     * @return a {@code double} that represents the fading factor.
     */
    public double getFadingFactor() {
        return fadingFactor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFadingFactor(double fadingFactor) {
        this.fadingFactor = fadingFactor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double updateModel(DataOnMemory<DataInstance> batch) {

        SufficientStatistics batchSS = batch.stream()
                .map(efBayesianNetwork::getSufficientStatistics)
                .reduce(SufficientStatistics::sumVectorNonStateless).get();

        sumSS.multiplyBy(fadingFactor);
        sumSS.sum(batchSS);

        dataInstanceCount.set(dataInstanceCount.get() * fadingFactor + batch.getNumberOfDataInstances());

        return Double.NaN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runLearning() {

        efBayesianNetwork = new EF_BayesianNetwork(dag);

        dataInstanceCount = new AtomicDouble(0);
        sumSS = efBayesianNetwork.createZeroSufficientStatistics();
        for (DataOnMemory<DataInstance> batch : dataStream.iterableOverBatches(windowsSize)){
            SufficientStatistics batchSS = batch.stream()
                    .map(efBayesianNetwork::getSufficientStatistics)
                    .reduce(SufficientStatistics::sumVectorNonStateless).get();

            sumSS.multiplyBy(fadingFactor);
            sumSS.sum(batchSS);

            dataInstanceCount.set(dataInstanceCount.get()*fadingFactor + windowsSize);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParallelMode(boolean parallelMode) {
        throw new UnsupportedOperationException("Non Parallel Mode Supported.");
    }
}
