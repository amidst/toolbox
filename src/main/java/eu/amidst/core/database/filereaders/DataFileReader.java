package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.Attributes;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public interface DataFileReader {

    public Attributes getAttributes();

    public DataRow nextDataRow();

    /***
     * This method is expected to return true if at least two rows with the same sequence ID are left
     * @return
     */
    public boolean hasMoreDataRows();

    public void reset();

    public boolean doesItReadThisFileExtension(String fileExtension);

}
