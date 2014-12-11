package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.Attributes;

import java.util.Iterator;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public interface DataFileReader extends Iterable<DataRow>, Iterator<DataRow> {

    Attributes getAttributes();

    DataRow next();

    /***
     * This method is expected to return true if at least two rows with the same sequence ID are left
     * @return
     */
    boolean hasNext();

    void reset();

    boolean doesItReadThisFileExtension(String fileExtension);

}
