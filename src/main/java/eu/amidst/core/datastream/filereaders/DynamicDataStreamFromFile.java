package eu.amidst.core.datastream.filereaders;

import eu.amidst.core.datastream.*;

import java.util.stream.Stream;

/**
 * Created by ana@cs.aau.dk on 12/11/14.
 */
public class DynamicDataStreamFromFile implements DataStream<DynamicDataInstance> {

    private DataFileReader reader;


    public DynamicDataStreamFromFile(DataFileReader reader1){
        this.reader=reader1;
    }

    @Override
    public Attributes getAttributes() {
        return reader.getAttributes();
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

    @Override
    public Stream<DynamicDataInstance> stream() {
        return DynamicDataInstanceSpliterator.toDynamicDataInstanceStream(reader);
    }
}
