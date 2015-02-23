package eu.amidst.core.database;

import eu.amidst.core.utils.FixedBatchParallelSpliteratorWrapper;

import java.util.Iterator;
import java.util.stream.Stream;
//TODO: Which the index of the variables TIME_ID and SEQ_ID
/**
 * Created by andresmasegosa on 11/12/14.
 */
public interface DataBase <E extends DataInstance> extends Iterable<E> {

    Attributes getAttributes();

    Stream<E> stream();


    void close();

    default Stream<E> parallelStream(int batchSize){
        return FixedBatchParallelSpliteratorWrapper.toFixedBatchStream(this.stream(), batchSize);
    }

    default Stream<E> parallelStream(){
        return this.stream().parallel();
    }

    default Iterator<E> iterator(){
        return this.stream().iterator();
    }

    default Stream<DataOnMemory<E>> streamOfBatches(int batchSize){
        return BatchesSpliterator.toFixedBatchStream(this,batchSize);
    }

    default Stream<DataOnMemory<E>> parallelStreamOfBatches(int batchSize){
        return FixedBatchParallelSpliteratorWrapper.toFixedBatchStream(this.streamOfBatches(batchSize), 1);
    }

}
