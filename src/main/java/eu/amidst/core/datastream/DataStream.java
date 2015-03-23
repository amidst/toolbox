package eu.amidst.core.datastream;

import eu.amidst.core.utils.FixedBatchParallelSpliteratorWrapper;

import java.util.Iterator;
import java.util.stream.Stream;
//TODO: Which the index of the variables TIME_ID and SEQ_ID
/**
 * Created by andresmasegosa on 11/12/14.
 */
public interface DataStream<E extends DataInstance> extends Iterable<E> {

    Attributes getAttributes();

    Stream<E> stream();

    void close();

    boolean isRestartable();

    void restart();

    default Stream<E> parallelStream(int batchSize){
        return FixedBatchParallelSpliteratorWrapper.toFixedBatchStream(this.stream(), batchSize);
    }

    default Stream<E> parallelStream(){
        return this.stream().parallel();
    }

    default Iterator<E> iterator(){
        return this.stream().iterator();
    }

    default Iterable<DataOnMemory<E>> iterableOverBatches(int batchSize) {
        return BatchesSpliterator.toFixedBatchIterable(this,batchSize);
    }

    default Stream<DataOnMemory<E>> streamOfBatches(int batchSize){
        return BatchesSpliterator.toFixedBatchStream(this,batchSize).sequential();
    }

    default Stream<DataOnMemory<E>> parallelStreamOfBatches(int batchSize){
        return FixedBatchParallelSpliteratorWrapper.toFixedBatchStream(this.streamOfBatches(batchSize), 1);
    }

}
