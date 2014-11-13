package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ana@cs.aau.dk on 12/11/14.
 */
public class DynamicDataOnMemoryFromFile implements DataOnMemory, DataOnDisk, DataOnStream {

    DataFileReader reader;
    /**
     * Only used in case the sequenceID is not in the datafile
     */
    int sequenceID = 0;
    /**
     * Only used in case the timeID is not in the datafile, time ID of the Present.
     */
    int timeIDcounter = 0;
    Attribute attSequenceID;
    Attribute attTimeID;
    DynamicDataInstance[] dataInstances;
    int pointer = 0;


    public DynamicDataOnMemoryFromFile(DataFileReader reader) {
        this.reader = reader;

        List<DynamicDataInstance> dataInstancesList = new ArrayList<>();

        DataRow present;
        DataRow past;
        present = this.reader.nextDataRow();
        attSequenceID = this.reader.getAttributes().getAttributeByName("SEQUENCE_ID");
        if (attSequenceID == null){
            /* This value should not be modified */
            this.sequenceID = 1;
        }

        attTimeID = this.reader.getAttributes().getAttributeByName("TIME_ID");
        if(attTimeID == null){
            this.timeIDcounter = 1;
        }

        while (reader.hasMoreDataRows()) {
            past = present;
            present = this.reader.nextDataRow();

         /* Not sequenceID nor TimeID are provided*/
            if(attSequenceID == null && attTimeID == null){
                dataInstancesList.add(new DynamicDataInstance(present, past, sequenceID, ++timeIDcounter));

         /* TimeID is provided*/
            }else if(attSequenceID == null){
                dataInstancesList.add(new DynamicDataInstance(present, past, sequenceID, (int)present.getValue(attTimeID)));

         /* SequenceID is provided*/
            }else if(attTimeID == null){
                double pastSequenceID = past.getValue(attSequenceID);
                double presentSequenceID = present.getValue(attSequenceID);
                if(pastSequenceID==presentSequenceID){
                    dataInstancesList.add(new DynamicDataInstance(present, past, (int)presentSequenceID, ++timeIDcounter));
                }
         /* SequenceID and TimeID are provided*/
            }else {

                double pastSequenceID = past.getValue(attSequenceID);
                double presentSequenceID = present.getValue(attSequenceID);
                double pastTimeID = past.getValue(attTimeID);
                double presentTimeID = present.getValue(attTimeID);
                dataInstancesList.add(new DynamicDataInstance(present, past, (int) presentSequenceID, (int) presentTimeID));
            }
        }
        reader.reset();

        dataInstances = new DynamicDataInstance[dataInstancesList.size()];
        int counter = 0;
        for (DynamicDataInstance inst : dataInstancesList) {
            dataInstances[counter] = inst;
            counter++;
        }

    }

    @Override
    public int getNumberOfDataInstances() {
        return dataInstances.length;
    }

    @Override
    public DataInstance getDataInstance(int i) {
        return dataInstances[i];
    }

    @Override
    public Attributes getAttributes() {
        return reader.getAttributes();
    }

    @Override
    public DataInstance nextDataInstance() {
        if (pointer >= getNumberOfDataInstances()) {
            throw new UnsupportedOperationException("Make sure to call hasMoreDataInstances() to know when the sequence " +
                    "has finished (restart() moves the reader pointer to the beginning");
        }
        return dataInstances[pointer++];
    }

    @Override
    public boolean hasMoreDataInstances() {
        return pointer < getNumberOfDataInstances();
    }

    @Override
    public void restart() {
        pointer = 0;
    }
}
