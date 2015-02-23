package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.*;
import eu.amidst.core.utils.FixedBatchParallelSpliteratorWrapper;

import java.util.stream.Stream;

/**
 * Created by ana@cs.aau.dk on 12/11/14.
 */
public class DynamicDataOnDiskFromFile implements DataOnDisk<DynamicDataInstance>{

    private DataFileReader reader;


    public DynamicDataOnDiskFromFile(DataFileReader reader1){
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
    public void restart() {
        this.reader.restart();
    }
    @Override
    public Stream<DynamicDataInstance> stream() {
        return DynamicDataInstanceSpliterator.toDynamicDataInstanceStream(reader);
    }
}
