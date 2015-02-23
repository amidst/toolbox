package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.*;
import eu.amidst.core.utils.FixedBatchParallelSpliteratorWrapper;

import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public class StaticDataOnDiskFromFile implements DataOnDisk<StaticDataInstance> {

    DataFileReader reader;

    public StaticDataOnDiskFromFile(DataFileReader reader1) {
        this.reader = reader1;
    }

    @Override
    public Attributes getAttributes() {
        return reader.getAttributes();
    }

    @Override
    public Stream<StaticDataInstance> stream() {
        return this.reader.stream().map( dataRow -> new StaticDataInstanceImpl(dataRow));
    }

    @Override
    public Stream<StaticDataInstance> parallelStream(){
        return FixedBatchParallelSpliteratorWrapper.toFixedBatchStream(this.stream(), 128);
    }

    @Override
    public void close() {
        this.reader.close();
    }

    @Override
    public void restart() {
        this.reader.restart();
    }

}
