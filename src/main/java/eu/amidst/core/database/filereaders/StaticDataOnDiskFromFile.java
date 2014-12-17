package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.DataOnStream;
import eu.amidst.core.utils.FixedBatchParallelSpliteratorWrapper;

import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public class StaticDataOnDiskFromFile implements DataOnDisk, DataOnStream {

    DataFileReader reader;

    public StaticDataOnDiskFromFile(DataFileReader reader1) {
        this.reader = reader1;
    }

    @Override
    public Attributes getAttributes() {
        return reader.getAttributes();
    }

    @Override
    public Stream<DataInstance> stream() {
        return this.reader.stream().map( dataRow -> new StaticDataInstance(dataRow));
    }

    @Override
    public Stream<DataInstance> parallelStream(){
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
