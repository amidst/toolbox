package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.Attributes;
import eu.amidst.core.utils.FixedBatchParallelSpliteratorWrapper;
import scala.collection.Parallel;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public interface DataFileReader extends Iterable<DataRow> {

    void loadFromFile(String path);

    Attributes getAttributes();

    boolean doesItReadThisFileExtension(String fileExtension);

    Stream<DataRow> stream();

    default void restart(){
        //Only needed if iterator is not based on streams.
    }

    default void close(){
        //Only needed if iterator is not based on streams.
    }

    default Stream<DataRow> parallelStream(int batchSize) {
        return FixedBatchParallelSpliteratorWrapper.toFixedBatchStream(this.stream(), batchSize);
    }

    default Stream<DataRow> parallelStream(){
        return this.stream().parallel();
    }

    default Iterator<DataRow> iterator() {
        return this.stream().iterator();
    }
}
