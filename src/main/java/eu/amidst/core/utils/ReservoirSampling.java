package eu.amidst.core.utils;

import eu.amidst.core.database.*;
import eu.amidst.core.database.filereaders.DynamicDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.StaticDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffFileReader.ARFFDataReader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by andresmasegosa on 10/12/14.
 */
public class ReservoirSampling {

    //TODO Be careful with use of "add(int pos, element)" of List!!!!!!
    public static DataOnMemory samplingNumberOfSamples(int numberOfSamples, DataBase dataBase){

        Random random = new Random(0);
        DataOnMemoryListContainer dataOnMemoryList = new DataOnMemoryListContainer(dataBase.getAttributes());
        int count = 0;

        for (DataInstance instance : dataBase){
            if (count<numberOfSamples) {
                dataOnMemoryList.add(instance);
            }else{
                int r = random.nextInt(count+1);
                if (r < numberOfSamples)
                    dataOnMemoryList.set(r,instance);
            }
            count++;
        }
        return dataOnMemoryList;
    }

    public static DataOnMemory samplingNumberOfGBs(double numberOfGB, DataBase dataOnStream) {
        double numberOfBytesPerSample = dataOnStream.getAttributes().getList().size()*8.0;

        //We assume an overhead of 10%.
        int numberOfSamples = (int) ((1-0.1)*numberOfGB*1073741824.0/numberOfBytesPerSample);

        return samplingNumberOfSamples(numberOfSamples,dataOnStream);
    }

    public static void main(String[] args) throws Exception {
        DataOnDisk data = new StaticDataOnDiskFromFile(new ARFFDataReader("datasets/syntheticDataCajaMar.arff"));

        DataOnMemory dataOnMemory = ReservoirSampling.samplingNumberOfSamples(1000, data);



    }


    }
