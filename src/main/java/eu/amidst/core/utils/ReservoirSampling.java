package eu.amidst.core.utils;

import eu.amidst.core.database.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by andresmasegosa on 10/12/14.
 */
public class ReservoirSampling {

    public static DataOnMemory samplingNumberOfSamples(int numberOfSamples, DataOnStream dataOnStream){
        Random random = new Random(0);
        DataOnMemoryList dataOnMemoryList = new DataOnMemoryList(dataOnStream.getAttributes());
        int count = 0;
        while (dataOnStream.hasNext()){
            DataInstance instance = dataOnStream.next();

            if (count<numberOfSamples)
                dataOnMemoryList.add(count,instance);
            else{
                int r = random.nextInt(count);
                if (r<numberOfSamples)
                    dataOnMemoryList.add(count,instance);
            }
            count++;
        }

        return dataOnMemoryList;
    }

    public static DataOnMemory samplingNumberOfGBs(double numberOfGB, DataOnStream dataOnStream) {
        double numberOfBytesPerSample = dataOnStream.getAttributes().getList().size()*8.0;

        //We assume an overhead of 10%.
        int numberOfSamples = (int) ((1-0.1)*numberOfGB*1073741824.0/numberOfBytesPerSample);

        return samplingNumberOfSamples(numberOfSamples,dataOnStream);
    }

    private static class DataOnMemoryList implements DataOnMemory{

        List<DataInstance> instanceList;
        Attributes attributes;

        DataOnMemoryList(Attributes attributes1){
            this.instanceList=new ArrayList();
            this.attributes=attributes1;
        }


        @Override
        public int getNumberOfDataInstances() {
            return this.instanceList.size();
        }

        @Override
        public DataInstance getDataInstance(int i) {
            return this.instanceList.get(i);
        }

        @Override
        public Attributes getAttributes() {
            return this.attributes;
        }

        public void add(int id, DataInstance data){
            this.instanceList.add(id,data);
        }
    }
}
