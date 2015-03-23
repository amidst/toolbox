package eu.amidst.core.datastream;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Spliterators.spliterator;
import static java.util.stream.StreamSupport.stream;

public class BatchesSpliterator<T extends DataInstance> implements Spliterator<DataOnMemory<T>> {
    private final DataStream<T> dataStream;

    private final Spliterator<T> spliterator;
    private final int batchSize;
    private final int characteristics;
    private long est;

    public BatchesSpliterator(DataStream<T> dataStream_, long est, int batchSize) {
        this.dataStream = dataStream_;
        this.spliterator = this.dataStream.stream().spliterator();
        final int c = spliterator.characteristics();
        this.characteristics = (c & SIZED) != 0 ? c | SUBSIZED : c;
        this.est = est;
        this.batchSize = batchSize;
    }
    public BatchesSpliterator(DataStream<T> dataStream_, int batchSize) {
        this(dataStream_, dataStream_.stream().spliterator().estimateSize()/batchSize, batchSize);
    }

    public static <T extends DataInstance> Stream<DataOnMemory<T>> toFixedBatchStream(DataStream<T> dataStream_, int batchSize) {
        return stream(new BatchesSpliterator<>(dataStream_, batchSize), true);
    }

    public static <T extends DataInstance> Iterable<DataOnMemory<T>> toFixedBatchIterable(DataStream<T> dataStream_, int batchSize) {
        return new BatchIterator<T>(toFixedBatchStream(dataStream_, batchSize));
    }

    @Override public Spliterator<DataOnMemory<T>> trySplit() {
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

    @Override public Comparator<? super DataOnMemory<T>> getComparator() {
        if (hasCharacteristics(SORTED)) return null;
        throw new IllegalStateException();
    }
    @Override public long estimateSize() { return est; }
    @Override public int characteristics() { return characteristics; }

    static final class HoldingConsumer<T> implements Consumer<T> {
        T value;
        @Override public void accept(T value) { this.value = value; }
    }

    static class BatchIterator <T extends DataInstance> implements Iterable<DataOnMemory<T>>{

        Stream<DataOnMemory<T>> stream;

        BatchIterator(Stream<DataOnMemory<T>> stream_){
            stream=stream_;
        }

        @Override
        public Iterator<DataOnMemory<T>> iterator() {
            return this.stream.iterator();
        }
    }
}
