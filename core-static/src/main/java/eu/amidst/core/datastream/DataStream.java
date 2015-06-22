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

/**
 *
 *
 *
 * //TODO: Which the index of the variables TIME_ID and SEQ_ID
 * Created by andresmasegosa on 11/12/14.
 */
public interface DataStream<E extends DataInstance> extends Iterable<E> {

    Attributes getAttributes();

    Stream<E> stream();

    void close();

    boolean isRestartable();

    void restart();

    default Stream<E> parallelStream(int batchSize){
        return FixedBatchParallelSpliteratorWrapper.toFixedBatchStream(this.stream(), batchSize);
    }

    default Stream<E> parallelStream(){
        return this.stream().parallel();
    }

    default Iterator<E> iterator(){
        return this.stream().iterator();
    }

    default Iterable<DataOnMemory<E>> iterableOverBatches(int batchSize) {
        return BatchesSpliterator.toFixedBatchIterable(this,batchSize);
    }

    default Stream<DataOnMemory<E>> streamOfBatches(int batchSize){
        return BatchesSpliterator.toFixedBatchStream(this,batchSize).sequential();
    }

    default Stream<DataOnMemory<E>> parallelStreamOfBatches(int batchSize){
        return FixedBatchParallelSpliteratorWrapper.toFixedBatchStream(this.streamOfBatches(batchSize), 1);
    }

}
