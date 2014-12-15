package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.DataOnStream;

import java.util.Iterator;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public class StaticDataOnDiskFromFile implements DataOnDisk, DataOnStream, Iterator<DataInstance>{

    private Iterator<DataRow> dataRowIterator;
    DataFileReader reader;


    public StaticDataOnDiskFromFile(DataFileReader reader1) {
        this.reader = reader1;
        this.dataRowIterator = reader.iterator();
    }

    @Override
    public DataInstance next() {
        return new StaticDataInstance(this.dataRowIterator.next());
    }

    @Override
    public boolean hasNext() {
        return dataRowIterator.hasNext();
    }

    @Override
    public Attributes getAttributes() {
        return reader.getAttributes();
    }

    @Override
    public void restart() {
        this.reader.restart();
        this.dataRowIterator = reader.iterator();
    }

    @Override
    public Iterator<DataInstance> iterator() {
        return this;
    }
}
