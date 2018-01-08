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

package eu.amidst.dynamic.datastream.filereaders;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.filereaders.DataFileReader;
import eu.amidst.core.datastream.filereaders.DataRow;
import eu.amidst.core.datastream.filereaders.DataRowMissing;
import eu.amidst.dynamic.datastream.DynamicDataInstance;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.stream.StreamSupport.stream;

/**
 * The DynamicDataInstanceSpliterator class defines a {@link Spliterator} over {@link DynamicDataInstance} elements.
 */
public class DynamicDataInstanceSpliterator implements Spliterator<DynamicDataInstance> {

    /** Represents a {@link DataFileReader} object. */
    private DataFileReader reader;

    /** Represents a {@link Iterator} over {@link DataRow}. */
    private Iterator<DataRow> dataRowIterator;

    /** Represents the {@link Attribute} object defining the sequence ID. */
    private Attribute attSequenceID;

    /** Represents the {@link Attribute} object defining the time ID. */
    private Attribute attTimeID;

    /** Represents a {@link NextDynamicDataInstance} object. */
    private NextDynamicDataInstance nextDynamicDataInstance;

    /** Represents a {@link Spliterator} over {@link DataRow} elemenets. */
    private final Spliterator<DataRow> spliterator;

    /** Represents the characteristics. */
    private final int characteristics;

    /** Represents the estimated size. */
    private long est;

    /** Represents the option whether SequenceID and TimeID are provided or not. */
    private int option;

    /**
     * Creates a new DynamicDataInstanceSpliterator given a valid {@link DataFileReader} object.
     * @param reader_ a valid {@link DataFileReader} object.
     */
    public DynamicDataInstanceSpliterator(DataFileReader reader_) {
        this.reader=reader_;
        dataRowIterator = this.reader.iterator();
        this.spliterator = this.reader.spliterator();
        final int c = spliterator.characteristics();
        this.characteristics = (c & SIZED) != 0 ? c | SUBSIZED : c;
        this.est = spliterator.estimateSize();

        /** We read the two first rows now, to create the first couple in next. */
        DataRow present;
        DataRow past = new DataRowMissing();

        int timeID = 0;
        int sequenceID = 0;

        if (dataRowIterator.hasNext()) {
            present = this.dataRowIterator.next();
        }else {
            throw new UnsupportedOperationException("There are insufficient instances to learn a model.");
        }

        attSequenceID = this.reader.getAttributes().getSeq_id();
        if (attSequenceID!=null)
            sequenceID = (int)present.getValue(attSequenceID);

        attTimeID = this.reader.getAttributes().getTime_id();
        if (attTimeID!=null)
            timeID = (int)present.getValue(attTimeID);

        nextDynamicDataInstance = new NextDynamicDataInstance(past, present, sequenceID, timeID);

        /* 0 = false, false, i.e., No sequenceID or TimeID are provided. */
        /* 1 = true,  false, i.e., TimeID is provided. */
        /* 2 = false, true,  i.e., SequenceID is provided. */
        /* 3 = true,  true,  i.e., Both SequenceID and TimeID are provided. */
        option = ((attTimeID == null) ? 0 : 1) + 2 * ((attSequenceID == null) ? 0 : 1);
    }

    /**
     * Returns a {@link Stream} of {@link DynamicDataInstance} given a valid {@link DataFileReader} object.
     * @param reader a valid {@link DataFileReader} object.
     * @return a Stream object.
     */
    public static Stream<DynamicDataInstance> toDynamicDataInstanceStream(DataFileReader reader) {
        return stream(new DynamicDataInstanceSpliterator(reader), false);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Spliterator<DynamicDataInstance> trySplit() {
        //this.reader.spliterator().trySplit()
        return null;
        /*
        final HoldingConsumer<DataRow> holder = new HoldingConsumer<>();
        if (!spliterator.tryAdvance(holder)) return null;
        final Object[] a = new Object[windowsSize];
        int j = 0;
        do a[j] = holder.value; while (++j < windowsSize && tryAdvance(holder));
        if (est != Long.MAX_VALUE) est -= j;
        return spliterator(a, 0, j, characteristics());
        */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean tryAdvance(Consumer<? super DynamicDataInstance> action) {

        if (!dataRowIterator.hasNext())
            return false;

        switch (option) {

            /* No sequenceID or TimeID are provided. */
            case 0:
                action.accept(nextDynamicDataInstance.nextDataInstance_NoTimeID_NoSeq(dataRowIterator));
                return true;

             /* Only a TimeID is provided. */
            case 1:
                action.accept(nextDynamicDataInstance.nextDataInstance_NoSeq(dataRowIterator, attTimeID));
                return true;

             /* Only a SequenceID is provided. */
            case 2:
                action.accept(nextDynamicDataInstance.nextDataInstance_NoTimeID(dataRowIterator, attSequenceID));
                return true;

             /* Both SequenceID and TimeID are provided. */
            case 3:
                DynamicDataInstance dynamicDataInstance = nextDynamicDataInstance.nextDataInstance(dataRowIterator, attSequenceID, attTimeID);
                if (dynamicDataInstance!=null) {
                    action.accept(dynamicDataInstance);
                    return true;
                }else{
                    return false;
                }

            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void forEachRemaining(Consumer<? super DynamicDataInstance> action) {
        while(this.dataRowIterator.hasNext()){
            this.tryAdvance(action);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Comparator<DynamicDataInstance> getComparator() {
        if (hasCharacteristics(SORTED)) return null;
        throw new IllegalStateException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long estimateSize() { return est; }

    /**
     * {@inheritDoc}
     */
    @Override
    public int characteristics() { return characteristics; }

    static final class HoldingConsumer<T> implements Consumer<T> {
        Object value;
        @Override public void accept(T value) { this.value = value; }
    }
}
