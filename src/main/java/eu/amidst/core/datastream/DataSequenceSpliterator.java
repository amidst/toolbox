package eu.amidst.core.datastream;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Spliterators.spliterator;
import static java.util.stream.StreamSupport.stream;

public class DataSequenceSpliterator implements Spliterator<DataSequence> {
    private final DataStream<DynamicDataInstance> dataStream;

    private final Spliterator<DynamicDataInstance> spliterator;
    private final int characteristics;
    private long est;
    private DynamicDataInstance tailInstance=null;
    private boolean advance = true;

    public DataSequenceSpliterator(DataStream<DynamicDataInstance> dataStream_, long est) {
        this.dataStream = dataStream_;
        this.spliterator = this.dataStream.stream().spliterator();
        final int c = spliterator.characteristics();
        this.characteristics = (c & SIZED) != 0 ? c | SUBSIZED : c;
        this.est = est;
    }
    public DataSequenceSpliterator(DataStream<DynamicDataInstance> dataStream_) {
        this(dataStream_, dataStream_.stream().spliterator().estimateSize());
    }

    public static Stream<DataSequence> toDataSequenceStream(DataStream<DynamicDataInstance> dataStream_) {
        return stream(new DataSequenceSpliterator(dataStream_), true);
    }

    @Override public Spliterator<DataSequence> trySplit() {
        if (!advance) return null;

        final HoldingConsumer<DynamicDataInstance> holder = new HoldingConsumer<>();


        final DataSequenceImpl container = new DataSequenceImpl(dataStream.getAttributes());
        final DataSequenceImpl[] a = new DataSequenceImpl[1];
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

        container.setSeqId(tailInstance.getSequenceID());

        while ((advance=spliterator.tryAdvance(holder)) && holder.value.getSequenceID()==tailInstance.getSequenceID()){
            tailInstance=holder.value;
            container.add(tailInstance);
        };

        tailInstance=holder.value;

        if (est != Long.MAX_VALUE) est -= container.getNumberOfDataInstances();

        if (container.getNumberOfDataInstances()>0) {
            return spliterator(a, 0, 1, characteristics());
        }else{
            return null;
        }
    }

    @Override
    public boolean tryAdvance(Consumer<? super DataSequence> action) {
        if (!advance) return false;

        final HoldingConsumer<DynamicDataInstance> holder = new HoldingConsumer<>();

        final DataSequenceImpl container = new DataSequenceImpl(dataStream.getAttributes());



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

        container.setSeqId(tailInstance.getSequenceID());


        while ((advance=spliterator.tryAdvance(holder)) && holder.value.getSequenceID()==tailInstance.getSequenceID()){
            tailInstance=holder.value;
            container.add(tailInstance);
        };

        tailInstance=holder.value;

        if (est != Long.MAX_VALUE) est -= container.getNumberOfDataInstances();

        if (container.getNumberOfDataInstances()>0) {
            action.accept(container);
            return true;
        }else{
            return false;
        }
    }

    @Override public Comparator<? super DataSequence> getComparator() {
        if (hasCharacteristics(SORTED)) return null;
        throw new IllegalStateException();
    }
    @Override public long estimateSize() { return est; }
    @Override public int characteristics() { return characteristics; }

    static final class HoldingConsumer<T> implements Consumer<T> {
        T value;
        @Override public void accept(T value) { this.value = value; }
    }

    static class DataSequenceImpl extends DataOnMemoryListContainer<DynamicDataInstance> implements DataSequence{

        int seqId=0;

        public DataSequenceImpl(Attributes attributes_) {
            super(attributes_);
        }

        public void setSeqId(int seqId) {
            this.seqId = seqId;
        }

        @Override
        public int getSequenceID() {
            return seqId;
        }
    }
}
