package eu.amidst.core.database;

import eu.amidst.core.database.filereaders.DataRow;
import eu.amidst.core.utils.FixedBatchParallelSpliteratorWrapper;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 11/12/14.
 */
public interface DataBase extends Iterable<DataInstance> {

    Attributes getAttributes();

    Stream<DataInstance> stream();

    default Stream<DataInstance> parallelStream(int batchSize){
        return FixedBatchParallelSpliteratorWrapper.toFixedBatchStream(this.stream(), batchSize);
    }

    default Stream<DataInstance> parallelStream(){
        return this.stream().parallel();
    }

    default Iterator<DataInstance> iterator(){
        return this.stream().iterator();
    }

    default void close(){
        //Only needed if iterator is not based on streams.
    }
}
