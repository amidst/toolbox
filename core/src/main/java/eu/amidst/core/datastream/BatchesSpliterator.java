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

package eu.amidst.core.datastream;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Spliterators.spliterator;
import static java.util.stream.StreamSupport.stream;

/**
 * The BatchesSpliterator class implements a {@link Spliterator} for iterating over data batches of a {@link DataStream}.
 * The data batches are explicitly stored in {@link DataOnMemory} objects.
 * This class is used by the class {@link DataStream}.
 *
 * <p> For further details about the implementation of this class using Java 8 functional-style programming look at the following paper: </p>
 *
 * <i> Masegosa et al. Probabilistic Graphical Models on Multi-Core CPUs using Java 8. IEEE-CIM (2015). </i>
 *
 * @param <T> A generic Type that extends the interface {@link DataInstance}.
 */
public class BatchesSpliterator<T extends DataInstance> implements Spliterator<DataOnMemory<T>> {

    /** Represents the data stream. */
    private final DataStream<T> dataStream;

    /** Represents the Spliterator. */
    private final Spliterator<T> spliterator;

    /** Represents the batch size. */
    private final int batchSize;

    /** Represents the characteristics. */
    private final int characteristics;

    /** Represents est. */
    private long est;

    /**
     * Creates a new BatchesSpliterator.
     * @param dataStream_ the data stream.
     * @param est est.
     * @param batchSize the batch size.
     */
    public BatchesSpliterator(DataStream<T> dataStream_, long est, int batchSize) {
        this.dataStream = dataStream_;
        this.spliterator = this.dataStream.stream().spliterator();
        final int c = spliterator.characteristics();
        this.characteristics = (c & SIZED) != 0 ? c | SUBSIZED : c;
        this.est = est;
        this.batchSize = batchSize;
    }

    /**
     * Creates a new BatchesSpliterator.
     * @param dataStream_ the data stream.
     * @param batchSize the batch size.
     */
    public BatchesSpliterator(DataStream<T> dataStream_, int batchSize) {
        this(dataStream_, dataStream_.stream().spliterator().estimateSize()/batchSize, batchSize);
    }

    /**
     * Creates a new parallel {@code Stream} from a given data stream and batch size.
     * @param dataStream_ the data stream.
     * @param batchSize the batch size.
     * @param <T> the type of stream elements.
     * @return a new parallel {@code Stream}.
     */
    public static <T extends DataInstance> Stream<DataOnMemory<T>> toFixedBatchStream(DataStream<T> dataStream_, int batchSize) {
        return stream(new BatchesSpliterator<>(dataStream_, batchSize), true);
    }

    /**
     * This class defines a batch iterator over the stream.
     * @param <T> the type of stream elements.
     */
    static class BatchIterator <T extends DataInstance> implements Iterable<DataOnMemory<T>>{

        Stream<DataOnMemory<T>> stream;

        BatchIterator(Stream<DataOnMemory<T>> stream_){
            stream=stream_;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterator<DataOnMemory<T>> iterator() {
            return this.stream.iterator();
        }
    }

    /**
     * Creates a new BatchIterator from a given data stream and batch size.
     * @param dataStream_ the data stream.
     * @param batchSize the batch size.
     * @param <T> the type of stream elements.
     * @return a new BatchIterator.
     */
    public static <T extends DataInstance> Iterable<DataOnMemory<T>> toFixedBatchIterable(DataStream<T> dataStream_, int batchSize) {
        return new BatchIterator<T>(toFixedBatchStream(dataStream_, batchSize));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Spliterator<DataOnMemory<T>> trySplit() {
        final HoldingConsumer<T> holder = new HoldingConsumer<>();
        if (!spliterator.tryAdvance(holder))
            return null;

        final DataOnMemoryListContainer<T> container = new DataOnMemoryListContainer<>(dataStream.getAttributes());
        final Object[] a = new Object[1];
        a[0]=container;
        int j = 0;
        do{
            container.add(holder.value);
        }while (++j < batchSize && spliterator.tryAdvance(holder));
        if (est != Long.MAX_VALUE) est -= 1;
        return spliterator(a, 0, 1, characteristics());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean tryAdvance(Consumer<? super DataOnMemory<T>> action) {
        final HoldingConsumer<T> holder = new HoldingConsumer<>();
        if (!spliterator.tryAdvance(holder))
            return false;

        final DataOnMemoryListContainer<T> container = new DataOnMemoryListContainer<>(dataStream.getAttributes());
        int j = 0;
        do{
            container.add(holder.value);
        }while (++j < batchSize && spliterator.tryAdvance(holder));

        if (j>0 && est != Long.MAX_VALUE) est -= 1;

        if (j>0) {
            action.accept(container);
            return true;
        }else{
            return false;
        }
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

        /**
         * {@inheritDoc}
         */
        @Override public void accept(T value) { this.value = value; }
    }
}
