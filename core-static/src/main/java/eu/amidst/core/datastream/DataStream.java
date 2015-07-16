/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.datastream;

import eu.amidst.core.utils.FixedBatchParallelSpliteratorWrapper;

import java.util.Iterator;
import java.util.stream.Stream;

//TODO: Which the index of the variables TIME_ID and SEQ_ID

/**
 * The DataStream class is an interface for dealing with data streams.
 * <p> The whole AMIDST Toolbox is specially designed to process the data sequentially without loading it into main memory.
 * In this way, this class can handle very large data sets. A DataStream object is composed as a collection of
 * {@link DataInstance} objects. </p>
 *
 * For further details about the implementation of this class look at the following paper: <br>
 * <i> Masegosa et al. Probabilistic Graphical Models on Multi-Core CPUs using Java 8. IEEE-CIM (2015). </i>
 *
 * <p> For an example of use see the class {@code eu.amidst.core.examples.datastream.DataStreamExample}. </p>
 */
public interface DataStream<E extends DataInstance> extends Iterable<E> {

    /**
     * Returns an Attributes object containing the attributes of this DataStream.
     * @return an Attributes object associated with this DataStream.
     */
    Attributes getAttributes();

    /**
     * Closes this DataStream.
     * It should be invoked only when the processing of the data stream is finished.
     */
    void close();

    /**
     * Returns whether this DataStream can restart.
     * A DataStream can restart if is is possible to iterate over all the data samples once again.
     * @return true if this DataStream can restart.
     */
    boolean isRestartable();

    /**
     * Restarts this DataStream.
     */
    void restart();

    /**
     * Returns a Stream of DataInstance objects to be processed sequentially.
     * That is, without invoking later on any parallel stream method.
     * @return a valid Java stream of DataInstance objects to be processed sequentially.
     */
    Stream<E> stream();

    /**
     * Returns a Stream of DataInstance objects to be processed in parallel.
     * Internally, data samples are grouped into batches and all the samples
     * in the same batch are processed with the same thread.
     * @param batchSize the size of the batches.
     * @return a valid Java stream of DataInstance object to be processed in parallel.
     */
    default Stream<E> parallelStream(int batchSize){
        return FixedBatchParallelSpliteratorWrapper.toFixedBatchStream(this.stream(), batchSize);
    }

    /**
     * Returns an Iterator object that iterates over all the data instances of this DataStream.
     * @return an Iterator over DataInstances.
     */
    default Iterator<E> iterator(){
        return this.stream().iterator();
    }

    /**
     * Returns an iterator over DataOnMemory objects. Each DataOnMemory object contains a batch of data.
     * @param batchSize the size of the data batches.
     * @return an Iterator over DataOnMemory objects.
     */
    default Iterable<DataOnMemory<E>> iterableOverBatches(int batchSize) {
        return BatchesSpliterator.toFixedBatchIterable(this,batchSize);
    }

    /**
     * Returns a stream of DataOnMemory objects. Each DataOnMemory object contains a batch of data.
     * @param batchSize the size of the data batches.
     * @return a stream of DataOnMemory objects.
     */
    default Stream<DataOnMemory<E>> streamOfBatches(int batchSize){
        return BatchesSpliterator.toFixedBatchStream(this,batchSize).sequential();
    }

    /**
     * Returns a parallel stream of DataOnMemory objects. Each DataOnMemory object contains a batch of data.
     * @param batchSize the size of the data batches.
     * @return a parallel stream of DataOnMemory objects.
     */
    default Stream<DataOnMemory<E>> parallelStreamOfBatches(int batchSize){
        return FixedBatchParallelSpliteratorWrapper.toFixedBatchStream(this.streamOfBatches(batchSize), 1);
    }

}
