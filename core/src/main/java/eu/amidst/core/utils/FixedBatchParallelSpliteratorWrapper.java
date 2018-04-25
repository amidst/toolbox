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

package eu.amidst.core.utils;

import eu.amidst.core.datastream.DataStream;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.stream.StreamSupport.stream;

/**
 * This class implements the {@link Spliterator} interface for providing streams
 * which can be transversed using a batches of a fixed size. It is mainly used by the
 * {@link DataStream} class to generate streams of batches of data instances, which
 * are quite suitable for parallelizing data processing algorithms.
 *
 * <p> For further details about the implementation of this class using Java 8
 * functional-style programming look at the following paper:
 * <i> Masegosa et al. Probabilistic Graphical Models on Multi-Core CPUs using Java 8. IEEE-CIM (2015). </i></p>
 *
 * @param <T> the type of the elements of the underlying transversed stream.
 */
public class FixedBatchParallelSpliteratorWrapper<T> implements Spliterator<T> {
    private final Spliterator<T> spliterator;
    private final int batchSize;
    private final int characteristics;
    private long est;

    /**
     * Creates a new FixedBatchParallelSpliteratorWrapper object.
     * @param toWrap a {@link Spliterator} object to be wrapped and transformed into
     *               a fixed batch size spliterator.
     * @param est the estimated length of the underlying stream.
     * @param batchSize the desired size of the batches.
     */
    public FixedBatchParallelSpliteratorWrapper(Spliterator<T> toWrap, long est, int batchSize) {
        final int c = toWrap.characteristics();
        this.characteristics = (c & SIZED) != 0 ? c | SUBSIZED : c;
        this.spliterator = toWrap;
        this.est = est;
        this.batchSize = batchSize;
    }

    /**
     * Creates a new FixedBatchParallelSpliteratorWrapper object.
     * @param toWrap a {@link Spliterator} object to be wrapped and transformed into
     *               a fixed batch size spliterator.
     * @param batchSize the desired size of the batches.
     */
    public FixedBatchParallelSpliteratorWrapper(Spliterator<T> toWrap, int batchSize) {
        this(toWrap, toWrap.estimateSize(), batchSize);
    }

    /**
     * Static method for creating the spliterator.
     * @param in, a {@link Spliterator} object to be wrapped and transformed in a
     *                fixed batch size spliterator.
     * @param batchSize the desired size of the batches.
     * @param <T> the type of the elements of the underlying transversed stream.
     * @return a {@link Stream} object.
     */
    public static <T> Stream<T> toFixedBatchStream(Stream<T> in, int batchSize) {
        return stream(new FixedBatchParallelSpliteratorWrapper<>(in.spliterator(), batchSize), true);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Spliterator<T> trySplit() {
        final HoldingConsumer<T> holder = new HoldingConsumer<>();
        if (!spliterator.tryAdvance(holder)) return null;
        final Object[] a = new Object[batchSize];
        int j = 0;
        do a[j] = holder.value; while (++j < batchSize && tryAdvance(holder));
        if (est != Long.MAX_VALUE) est -= j;
        return Spliterators.spliterator(a, 0, j, characteristics());
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean tryAdvance(Consumer<? super T> action) {
        return spliterator.tryAdvance(action);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void forEachRemaining(Consumer<? super T> action) {
        spliterator.forEachRemaining(action);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Comparator<? super T> getComparator() {
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
        Object value;
        @Override public void accept(T value) { this.value = value; }
    }
}
