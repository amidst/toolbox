/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.corestatic.datastream.filereaders;

import eu.amidst.corestatic.datastream.Attribute;
import eu.amidst.corestatic.datastream.Attributes;
import eu.amidst.corestatic.datastream.DynamicDataInstance;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Spliterators.spliterator;
import static java.util.stream.StreamSupport.stream;

public class DynamicDataInstanceSpliterator implements Spliterator<DynamicDataInstance> {

    private DataFileReader reader;
    private Iterator<DataRow> dataRowIterator;
    private Attribute attSequenceID;
    private Attribute attTimeID;
    private NextDynamicDataInstance nextDynamicDataInstance;

    private final Spliterator<DataRow> spliterator;
    //private final int batchSize;
    private final int characteristics;
    private long est;
    private int option;

    //public DynamicDataInstanceFixedBatchParallelSpliteratorWrapper(DataFileReader reader1, long est, int batchSize) {
    public DynamicDataInstanceSpliterator(DataFileReader reader_) {
        this.reader=reader_;
        dataRowIterator = this.reader.iterator();
        this.spliterator = this.reader.spliterator();

        final int c = spliterator.characteristics();
        this.characteristics = (c & SIZED) != 0 ? c | SUBSIZED : c;
        this.est = spliterator.estimateSize();
        //this.batchSize = batchSize;


        /**
         * We read the two first rows now, to create the first couple in next
         */
        DataRow present;
        DataRow past = new DataRowMissing();

        int timeID = 0;
        int sequenceID = 0;

        if (dataRowIterator.hasNext()) {
            present = this.dataRowIterator.next();
        }else {
            throw new UnsupportedOperationException("There are insufficient instances to learn a model.");
        }

        try {
            attSequenceID = this.reader.getAttributes().getAttributeByName(Attributes.SEQUENCE_ID_ATT_NAME);
            sequenceID = (int)present.getValue(attSequenceID);
        }catch (UnsupportedOperationException e){
            attSequenceID = null;
        }
        try {
            attTimeID = this.reader.getAttributes().getAttributeByName(Attributes.TIME_ID_ATT_NAME);
            timeID = (int)present.getValue(attTimeID);
        }catch (UnsupportedOperationException e){
            attTimeID = null;
        }

        nextDynamicDataInstance = new NextDynamicDataInstance(past, present, sequenceID, timeID);


        /* 0 = false, false, i.e., Not sequenceID nor TimeID are provided */
        /* 1 = true,  false, i.e., TimeID is provided */
        /* 2 = false, true,  i.e., SequenceID is provided */
        /* 3 = true,  true,  i.e., SequenceID is provided*/
        option = ((attTimeID == null) ? 0 : 1) + 2 * ((attSequenceID == null) ? 0 : 1);

    }


    //public DynamicDataInstanceFixedBatchParallelSpliteratorWrapper(Spliterator<DataRow> toWrap, int batchSize) {
    //    this(toWrap, toWrap.estimateSize(), batchSize);
    //}

    //public static Stream<DynamicDataInstance> toFixedBatchStream(Stream<DataRow> in, int batchSize) {
    //    return stream(new DynamicDataInstanceFixedBatchParallelSpliteratorWrapper(in.spliterator(), batchSize), true);
    //}

    public static Stream<DynamicDataInstance> toDynamicDataInstanceStream(DataFileReader reader) {
        return stream(new DynamicDataInstanceSpliterator(reader), false);
    }

    @Override public Spliterator<DynamicDataInstance> trySplit() {
        //this.reader.spliterator().trySplit()
        return null;
        /*
        final HoldingConsumer<DataRow> holder = new HoldingConsumer<>();
        if (!spliterator.tryAdvance(holder)) return null;
        final Object[] a = new Object[batchSize];
        int j = 0;
        do a[j] = holder.value; while (++j < batchSize && tryAdvance(holder));
        if (est != Long.MAX_VALUE) est -= j;
        return spliterator(a, 0, j, characteristics());
        */
    }

    @Override
    public boolean tryAdvance(Consumer<? super DynamicDataInstance> action) {

        if (!dataRowIterator.hasNext())
            return false;

        switch (option) {

            /* Not sequenceID nor TimeID are provided*/
            case 0:
                action.accept(nextDynamicDataInstance.nextDataInstance_NoTimeID_NoSeq(dataRowIterator));
                return true;

             /* Only TimeID is provided*/
            case 1:
                action.accept(nextDynamicDataInstance.nextDataInstance_NoSeq(dataRowIterator, attTimeID));
                return true;

             /* Only SequenceID is provided*/
            case 2:
                action.accept(nextDynamicDataInstance.nextDataInstance_NoTimeID(dataRowIterator, attSequenceID));
                return true;

             /* SequenceID and TimeID are provided*/
            case 3:
                action.accept(nextDynamicDataInstance.nextDataInstance(dataRowIterator, attSequenceID, attTimeID));
                return true;

            default:
                throw new IllegalArgumentException();

        }
    }

    @Override
    public void forEachRemaining(Consumer<? super DynamicDataInstance> action) {
        while(this.dataRowIterator.hasNext()){
            this.tryAdvance(action);
        }
    }
    @Override
    public Comparator<DynamicDataInstance> getComparator() {
        if (hasCharacteristics(SORTED)) return null;
        throw new IllegalStateException();
    }

    @Override
    public long estimateSize() { return est; }

    @Override
    public int characteristics() { return characteristics; }

    static final class HoldingConsumer<T> implements Consumer<T> {
        Object value;
        @Override public void accept(T value) { this.value = value; }
    }
}
