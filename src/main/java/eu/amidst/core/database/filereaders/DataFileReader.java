package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.Attributes;

import java.util.Iterator;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public interface DataFileReader extends Iterable<DataRow> {

    Attributes getAttributes();

    void restart();

    boolean doesItReadThisFileExtension(String fileExtension);

    void close();
}
