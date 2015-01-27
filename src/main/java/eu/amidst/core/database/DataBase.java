package eu.amidst.core.database;

import eu.amidst.core.database.filereaders.DataRow;
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

    default Stream<E> parallelStream(int batchSize){
        return FixedBatchParallelSpliteratorWrapper.toFixedBatchStream(this.stream(), batchSize);
    }

    default Stream<E> parallelStream(){
        return this.stream().parallel();
    }

    default Iterator<E> iterator(){
        return this.stream().iterator();
    }

    default void close(){
        //Only needed if iterator is not based on streams.
    }
}
