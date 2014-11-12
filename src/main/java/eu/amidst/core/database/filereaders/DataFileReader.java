package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.Attributes;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public interface DataFileReader {

    public Attributes getAttributes();

    public DataRow nextDataRow();

    public boolean hasMoreDataRows();

    public void reset();

    public boolean doesItReadThisFileExtension(String fileExtension);

}
