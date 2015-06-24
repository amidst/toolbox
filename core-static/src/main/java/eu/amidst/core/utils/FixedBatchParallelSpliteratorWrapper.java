/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.utils;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.stream.StreamSupport.stream;

public class FixedBatchParallelSpliteratorWrapper<T> implements Spliterator<T> {
    private final Spliterator<T> spliterator;
    private final int batchSize;
    private final int characteristics;
    private long est;

    public FixedBatchParallelSpliteratorWrapper(Spliterator<T> toWrap, long est, int batchSize) {
        final int c = toWrap.characteristics();
        this.characteristics = (c & SIZED) != 0 ? c | SUBSIZED : c;
        this.spliterator = toWrap;
        this.est = est;
        this.batchSize = batchSize;
    }
    public FixedBatchParallelSpliteratorWrapper(Spliterator<T> toWrap, int batchSize) {
        this(toWrap, toWrap.estimateSize(), batchSize);
    }

    public static <T> Stream<T> toFixedBatchStream(Stream<T> in, int batchSize) {
        return stream(new FixedBatchParallelSpliteratorWrapper<>(in.spliterator(), batchSize), true);
    }

    @Override public Spliterator<T> trySplit() {
        final HoldingConsumer<T> holder = new HoldingConsumer<>();
        if (!spliterator.tryAdvance(holder)) return null;
        final Object[] a = new Object[batchSize];
        int j = 0;
        do a[j] = holder.value; while (++j < batchSize && tryAdvance(holder));
        if (est != Long.MAX_VALUE) est -= j;
        return Spliterators.spliterator(a, 0, j, characteristics());
    }
    @Override public boolean tryAdvance(Consumer<? super T> action) {
        return spliterator.tryAdvance(action);
    }
    @Override public void forEachRemaining(Consumer<? super T> action) {
        spliterator.forEachRemaining(action);
    }
    @Override public Comparator<? super T> getComparator() {
        if (hasCharacteristics(SORTED)) return null;
        throw new IllegalStateException();
    }
    @Override public long estimateSize() { return est; }
    @Override public int characteristics() { return characteristics; }

    static final class HoldingConsumer<T> implements Consumer<T> {
        Object value;
        @Override public void accept(T value) { this.value = value; }
    }
}
