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

package eu.amidst.huginlink.examples.learning;


import com.google.common.base.Stopwatch;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.huginlink.learning.ParallelPC;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class provides a link to the <a href="https://www.hugin.com">Hugin</a>'s functionality to learn in parallel
 * the structure of a Bayesian network model from data using the PC algorithm.
 *
 * An important remark is that Hugin only allows to apply the PC algorithm over a data set completely loaded into RAM
 * memory. The case where our data set does not fit into memory, it solved in AMIDST in the following way. We learn
 * the structure using a smaller data set produced by <a href="https://en.wikipedia.org/wiki/Reservoir_sampling">Reservoir sampling</a>
 * and, then, we use AMIDST's {@link ParallelMaximumLikelihood} to learn the parameters of the BN model over the
 * whole data set.
 *
 * <p> For further details about the implementation of the parallel PC algorithm look at the following paper: </p>
 *
 * <i> Madsen, A. L., Jensen, F., Salmerón, A., Langseth, H., Nielsen, T. D. (2015). Parallelization of the PC
 * Algorithm (2015). The XVI Conference of the Spanish Association for Artificial Intelligence (CAEPIA'15), pages 14-24 </i>
 */
public class ParallelPCExample {

    public static void main(String[] args) throws Exception {

        //We load a Bayesian network to generate a data stream
        //using BayesianNewtorkSampler class.
        int sampleSize = 100000;
        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("networks/dataWeka/Pigs.bn");
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);

        //We fix the number of samples in memory used for performing the structural learning.
        //They are randomly sub-sampled using Reservoir sampling.
        int samplesOnMemory = 5000;

        //We make different trials with different number of cores
        ArrayList<Integer> vNumCores = new ArrayList(Arrays.asList(1, 2, 3, 4));

        for (Integer numCores : vNumCores) {
            System.out.println("Learning PC: " + samplesOnMemory + " samples on memory, " + numCores + " core/s ...");
            DataStream<DataInstance> data = sampler.sampleToDataStream(sampleSize);

            //The class ParallelTAN is created
            ParallelPC parallelPC = new ParallelPC();

            //We activate the parallel mode.
            parallelPC.setParallelMode(true);

            //We set the number of cores to be used for the structural learning
            parallelPC.setNumCores(numCores);

            //We set the number of samples to be used for the learning the structure
            parallelPC.setNumSamplesOnMemory(samplesOnMemory);

            Stopwatch watch = Stopwatch.createStarted();

            //We just invoke this mode to learn a BN model for the data stream
            BayesianNetwork model = parallelPC.learn(data);

            System.out.println(watch.stop());
        }
    }
}