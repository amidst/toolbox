package eu.amidst.core.datastream.filereaders;

import eu.amidst.core.datastream.*;
import eu.amidst.core.utils.FixedBatchParallelSpliteratorWrapper;

import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public class DataStreamFromFile implements DataStream<DataInstance> {

    DataFileReader reader;

    public DataStreamFromFile(DataFileReader reader1) {
        this.reader = reader1;
    }

    @Override
    public Attributes getAttributes() {
        return reader.getAttributes();
    }

    @Override
    public Stream<DataInstance> stream() {
        return this.reader.stream().map( dataRow -> new DataInstanceImpl(dataRow));
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
    public boolean isRestartable() {
        return true;
    }

    @Override
    public void restart() {
        this.reader.restart();
    }

}
