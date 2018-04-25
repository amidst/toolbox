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

package eu.amidst.lda.core;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.DataStream;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Spliterators.spliterator;
import static java.util.stream.StreamSupport.stream;

/**
 * The DataSequenceSpliterator class implements a {@link Spliterator} of {@link DataOnMemory}.
 */
public class BatchSpliteratorByID<T extends DataInstance> implements Spliterator<DataOnMemory<T>> {

    /** Represents a DataStream of DynamicDataInstance objects. */
    private final DataStream<T> dataStream;

    /** Represents a Spliterator of DynamicDataInstance objects. */
    private final Spliterator<T> spliterator;

    private final int characteristics;

    private long est;

    private T tailInstance=null;

    private boolean advance = true;

    /** Represents the batch size. */
    private final int batchSize;

    /**
     * Creates a new DataSequenceSpliterator.
     * @param dataStream_ a DataStream object.
     * @param est the estimated size
     * @param batchSize
     */
    public BatchSpliteratorByID(DataStream<T> dataStream_, long est, int batchSize) {
        this.dataStream = dataStream_;
        this.batchSize = batchSize;
        this.spliterator = this.dataStream.stream().spliterator();
        final int c = spliterator.characteristics();
        this.characteristics = (c & SIZED) != 0 ? c | SUBSIZED : c;
        this.est = est;
    }

    /**
     * Creates a new DataSequenceSpliterator.
     * @param dataStream_ a DataStream object.
     * @param batchSize
     */
    public BatchSpliteratorByID(DataStream<T> dataStream_, int batchSize) {
        this(dataStream_, dataStream_.stream().spliterator().estimateSize(), batchSize);
    }

    /**
     * Creates a new parallel {@code Stream} from a given data stream and batch size.
     * @param dataStream_ the data stream.
     * @param batchSize the batch size.
     * @param <T> the type of stream elements.
     * @return a new parallel {@code Stream}.
     */
    public static <T extends DataInstance> Stream<DataOnMemory<T>> streamOverDocuments(DataStream<T> dataStream_, int batchSize) {
        return stream(new BatchSpliteratorByID<>(dataStream_, batchSize), true);
    }


    public static <T extends DataInstance> Iterable<DataOnMemory<T>> iterableOverDocuments(DataStream<T> dataStream_, int batchSize) {
        return new Iterable<DataOnMemory<T>>() {
            @Override
            public Iterator<DataOnMemory<T>> iterator() {
                return BatchSpliteratorByID.streamOverDocuments(dataStream_,batchSize).iterator();
            }
        };
    }
    /**
     * {@inheritDoc}
     */
    @Override public Spliterator<DataOnMemory<T>> trySplit() {
        if (!advance) return null;

        final HoldingConsumer<T> holder = new HoldingConsumer<>();


        final DataOnMemoryListContainer container = new DataOnMemoryListContainer(dataStream.getAttributes());
        final DataOnMemoryListContainer[] a = new DataOnMemoryListContainer[1];
        a[0]=container;

        if (tailInstance==null) {
            if (spliterator.tryAdvance(holder)) {
                tailInstance = holder.value;
                container.add(tailInstance);
            }else{
                return null;
            }
        }else{
            container.add(tailInstance);
        }

        int count = 0;
        while (count<this.batchSize && advance){
            while ((advance=spliterator.tryAdvance(holder)) && getSequenceID(holder.value)==getSequenceID(tailInstance)){
                tailInstance=holder.value;
                container.add(tailInstance);
            };

            tailInstance=holder.value;
            count++;
            if (count<this.batchSize && advance)
                container.add(tailInstance);

        }


        if (est != Long.MAX_VALUE) est -= container.getNumberOfDataInstances();

        if (container.getNumberOfDataInstances()>0) {
            return spliterator(a, 0, 1, characteristics());
        }else{
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean tryAdvance(Consumer<? super DataOnMemory<T>> action) {
        if (!advance) return false;

        final HoldingConsumer<T> holder = new HoldingConsumer<>();

        final DataOnMemoryListContainer<T> container = new DataOnMemoryListContainer(dataStream.getAttributes());

        if (tailInstance==null) {
            if (spliterator.tryAdvance(holder)) {
                tailInstance = holder.value;
                container.add(tailInstance);
            }else{
                return false;
            }
        }else{
            container.add(tailInstance);
        }

        int count = 0;
        while (count<this.batchSize && advance){
            while ((advance=spliterator.tryAdvance(holder)) && getSequenceID(holder.value)==getSequenceID(tailInstance)){
                tailInstance=holder.value;
                container.add(tailInstance);
            };

            tailInstance=holder.value;
            count++;
            if (count<this.batchSize && advance)
                container.add(tailInstance);

        }

        if (est != Long.MAX_VALUE) est -= container.getNumberOfDataInstances();

        if (container.getNumberOfDataInstances()>0) {
            action.accept(container);
            return true;
        }else{
            return false;
        }
    }

    private static long getSequenceID(DataInstance tailInstance){
        return (long)tailInstance.getValue(tailInstance.getAttributes().getSeq_id());
    }

    /**
     * {@inheritDoc}
     */
    @Override public Comparator<? super DataOnMemory<T>> getComparator() {
        if (hasCharacteristics(SORTED)) return null;
        throw new IllegalStateException();
    }

    /**
     * {@inheritDoc}
     */
    @Override public long estimateSize() { return est; }

    /**
     * {@inheritDoc}
     */
    @Override public int characteristics() { return characteristics; }

    static final class HoldingConsumer<T> implements Consumer<T> {
        T value;
        @Override public void accept(T value) { this.value = value; }
    }


}
