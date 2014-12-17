package eu.amidst.core.database;

import java.util.Iterator;

/**
 * Created by andresmasegosa on 11/12/14.
 */
public interface DataBase extends Iterable<DataInstance> {

    Attributes getAttributes();

    void close();
}
