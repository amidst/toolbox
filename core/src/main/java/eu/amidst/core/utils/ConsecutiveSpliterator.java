/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.utils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by andresmasegosa on 16/12/14.
 */
public class ConsecutiveSpliterator<T> implements Spliterator<List<T>> {

    private final Spliterator<T> wrappedSpliterator;

    private final int n;

    private final Deque<T> deque;

    private final Consumer<T> dequeConsumer;

    public ConsecutiveSpliterator(Spliterator<T> wrappedSpliterator, int n) {
        this.wrappedSpliterator = wrappedSpliterator;
        this.n = n;
        this.deque = new LinkedList<>();
        this.dequeConsumer = new Consumer<T>() {
            @Override
            public void accept(T t) {
                deque.addLast(t);
            }
        };
    }

    public static <E> Stream<List<E>> consecutiveStream(Stream<E> stream, int n) {
        Spliterator<E> spliterator = stream.spliterator();
        Spliterator<List<E>> wrapper = new ConsecutiveSpliterator<>(spliterator, n);
        return StreamSupport.stream(wrapper, false);
    }

    @Override
    public boolean tryAdvance(Consumer<? super List<T>> action) {
        deque.pollFirst();
        fillDeque();
        if (deque.size() == n) {
            List<T> list = new ArrayList<>(deque);
            action.accept(list);
            return true;
        } else {
            return false;
        }
    }

    private void fillDeque() {
        while (deque.size() < n && wrappedSpliterator.tryAdvance(dequeConsumer))
            ;
    }

    @Override
    public Spliterator<List<T>> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return wrappedSpliterator.estimateSize();
    }

    @Override
    public int characteristics() {
        return wrappedSpliterator.characteristics();
    }
}