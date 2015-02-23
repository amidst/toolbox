package eu.amidst.core.io;

import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.DynamicDataInstance;
import eu.amidst.core.database.StaticDataInstance;
import eu.amidst.core.database.filereaders.DataFileReader;
import eu.amidst.core.database.filereaders.DynamicDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.StaticDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffFileReader.ARFFDataReader;

/**
 * Created by andresmasegosa on 23/02/15.
 */
public class DynamicDataStreamLoader {

    private static DataFileReader dataFileReader = new ARFFDataReader();


    public static void setDataFileReader(DataFileReader dataFileReader) {
        dataFileReader = dataFileReader;
    }

    public static DataBase<DynamicDataInstance> loadFromFile(String path){
        dataFileReader.loadFromFile(path);
        return new DynamicDataOnDiskFromFile(dataFileReader);
    }

}
