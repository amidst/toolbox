/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.dynamic.learning.dynamic;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.exponentialfamily.EF_DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * This class defines the Dynamic Maximum Likelihood algorithm.
 */
public final class DynamicMaximumLikelihood {

    /** Represents the batch size used for learning the parameters, initialized to 1000. */
    private static int batchSize = 1000;

    /** Indicates the parallel processing mode, initialized to {@code true}. */
    private static boolean parallelMode = true;

    /**
     * Returns the batch size.
     * @return an {@int} that represents the the batch size.
     */
    public static int getBatchSize() {
        return batchSize;
    }

    /**
     * Sets the batch size.
     * @param batchSize an {@int} that represents the the batch size.
     */
    public static void setBatchSize(int batchSize) {
        DynamicMaximumLikelihood.batchSize = batchSize;
    }

    /**
     * Sets the parallel processing mode.
     * @param parallelMode {@code true} if the learning is performed in parallel, {@code false} otherwise.
     */
    public static void setParallelMode(boolean parallelMode) {
        DynamicMaximumLikelihood.parallelMode = parallelMode;
    }

    /**
     * Learns the parameters of the dynamic model from data stream.
     * @param dag a given {@link DynamicDAG} object.
     * @param dataStream a given {@link DataStream} of {@link DynamicDataInstance}s.
     * @return a {@link DynamicBayesianNetwork} object.
     */
    public static DynamicBayesianNetwork learnDynamic(DynamicDAG dag, DataStream<DynamicDataInstance> dataStream) {

        EF_DynamicBayesianNetwork efDynamicBayesianNetwork = new EF_DynamicBayesianNetwork(dag);

        Stream<DynamicDataInstance> stream = null;
        if (parallelMode){
            stream = dataStream.parallelStream(batchSize);
        }else{
            stream = dataStream.stream();
        }

        AtomicInteger dataInstanceCount = new AtomicInteger(0);

        SufficientStatistics sumSS = stream
                .peek(w -> {
                    if (w.getTimeID()==0)
                        dataInstanceCount.getAndIncrement();
                })
                .map(efDynamicBayesianNetwork::getSufficientStatistics)
                .reduce(SufficientStatistics::sumVectorNonStateless).get();

        //Normalize the sufficient statistics
        sumSS.divideBy(dataInstanceCount.get());

        efDynamicBayesianNetwork.setMomentParameters(sumSS);
        return efDynamicBayesianNetwork.toDynamicBayesianNetwork(dag);
    }
}
