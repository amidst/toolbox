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
 *
 * The AMIDST Toolbox is specially designed to deal with data streams. The interface
 * eu.amidst.core.DataStream is the main point for dealing this kind of data. It is specially
 * designed to consume the data sequentially without loading it into main memory. In this way, the class
 * can handle very large data sets. A DataStream object is composed as a collection of
 * eu.amidst.core.DataInstance objects. <p>
 *
 * For an example of use see the class <br>
 *
 *  eu.amidst.core.examples.datastream.DataStreamExample <p>
 *
 * For details about the implementation of this class see Section 3 of the following paper, <br>
 *
 *  "Borchani et al. Probabilistic Graphical Models on Multi-Core CPUs using Java 8. IEEE-CIM (2015)"
 *
 */
public interface DataStream<E extends DataInstance> extends Iterable<E> {

    /**
     * Return an Attributes object containing the attributes of the loaded data set.
     * @return an Attributes object associated to the data stream
     */
    Attributes getAttributes();

    /**
     * This method should be invoked when the processing of the data stream is finished.
     */
    void close();

    /**
     * A data streams could be restarted. So, we could iterate over all the data samples again.
     * @return Whether the data stream can be restarted
     */
    boolean isRestartable();

    /**
     * For those data streams can be restarted, this method perform the restarting operation.
     */
    void restart();

    /**
     * This method returns a Stream of DataInstance objects. This stream must be consumed sequentially, i.e.
     * without invoking later on any parallel stream method.
     *
     * @return A valid Java stream of DataInstance objects.
     */
    Stream<E> stream();

    /**
     * This method returns a Stream of DataInstance objects. This stream is specially designed to be consumed
     * in parallel. Internally, data samples are grouped on batches and all the samples in the same batch are
     * processed with the same thread.
     *
     * @param batchSize, the size of the batches
     * @return A valid Java stream of DataInstance object to be consumed in parallel.
     */
    default Stream<E> parallelStream(int batchSize){
        return FixedBatchParallelSpliteratorWrapper.toFixedBatchStream(this.stream(), batchSize);
    }

    /**
     * Return an Itertor object to iterate over all the data instances of the data stream.
     * @return A Iterator over DataInstances
     */
    default Iterator<E> iterator(){
        return this.stream().iterator();
    }

    /**
     * This method returns a stream of DataOnMemory objects. Each DataOnMemory object contains a batch of data.
     * @param batchSize, the size of the data batches
     * @return A stream of DataOnMemory objects
     */
    default Stream<DataOnMemory<E>> streamOfBatches(int batchSize){
        return BatchesSpliterator.toFixedBatchStream(this,batchSize).sequential();
    }

    /**
     * This method returns a parallel stream of DataOnMemory objects. Each DataOnMemory object contains a batch of data.
     * @param batchSize, the size of the data batches
     * @return A stream of DataOnMemory objects
     */
    default Stream<DataOnMemory<E>> parallelStreamOfBatches(int batchSize){
        return FixedBatchParallelSpliteratorWrapper.toFixedBatchStream(this.streamOfBatches(batchSize), 1);
    }


    /**
     * This method returns a iterator over DataOnMemory objects. Each DataOnMemory object contains a batch of data.
     *
     * @param batchSize, the size of the data batches
     * @return A Iterator over DataOnMemory objects
     */
    default Iterable<DataOnMemory<E>> iterableOverBatches(int batchSize) {
        return BatchesSpliterator.toFixedBatchIterable(this,batchSize);
    }
}
