package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.Attributes;

import java.util.Iterator;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public interface DataFileReader extends Iterable<DataRow>, Iterator<DataRow> {

    public Attributes getAttributes();

    public DataRow next();

    /***
     * This method is expected to return true if at least two rows with the same sequence ID are left
     * @return
     */
    public boolean hasNext();

    public void reset();

    public boolean doesItReadThisFileExtension(String fileExtension);

}
