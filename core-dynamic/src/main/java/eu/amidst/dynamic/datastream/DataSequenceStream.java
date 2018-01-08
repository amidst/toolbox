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

package eu.amidst.dynamic.datastream;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.utils.FixedBatchParallelSpliteratorWrapper;

import java.util.stream.Stream;

/**
 * The DataSequenceStream class defines a {@link Stream} of {@link DataSequence}.
 */
public final class DataSequenceStream {

    /**
     * Returns a {@link Stream} of {@link DataSequence}.
     * @param dataStream a DataStream object.
     * @return a Stream object.
     */
    public static Stream<DataSequence> streamOfDataSequences(DataStream<DynamicDataInstance> dataStream){
        return DataSequenceSpliterator.toDataSequenceStream(dataStream);
    }

    /**
     * Returns a parallel {@link Stream} of {@link DataSequence}.
     * @param dataStream a DataStream object.
     * @return a Stream object.
     */
    public static Stream<DataSequence> parallelStreamOfDataSequences(DataStream<DynamicDataInstance> dataStream){
        return FixedBatchParallelSpliteratorWrapper.toFixedBatchStream(DataSequenceStream.streamOfDataSequences(dataStream), 1);
    }

}
