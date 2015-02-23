package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.DataInstance;

import java.io.IOException;

/**
 * Created by andresmasegosa on 23/02/15.
 */
public interface DataFileWriter {

    String getFileExtension();

    void writeToFile(DataBase<? extends DataInstance> dataBase, String file) throws IOException;

}
