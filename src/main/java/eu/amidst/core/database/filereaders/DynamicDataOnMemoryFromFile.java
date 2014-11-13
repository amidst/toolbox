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

        DataRow present = null;
        DataRow past = null;
        try {
            if (reader.hasMoreDataRows())
                past = this.reader.nextDataRow();
            if (reader.hasMoreDataRows())
                present = this.reader.nextDataRow();
        }catch (UnsupportedOperationException e){System.err.println("There are insufficient instances to learn a model.");}

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

            /* 0 = false, false, i.e., Not sequenceID nor TimeID are provided */
            /* 1 = true,  false, i.e., TimeID is provided */
            /* 2 = false, true,  i.e., SequenceID is provided */
            /* 3 = true,  true,  i.e., SequenceID is provided*/
            int option = (attTimeID == null) ? 1 : 0 + 2 * ((attSequenceID == null) ? 1 : 0);

            switch (option) {

            /* Not sequenceID nor TimeID are provided*/
                case 0:
                    dataInstancesList.add(Utils.nextDataInstance_NoTimeID_NoSeq(reader, present, past, sequenceID, timeIDcounter));

             /* Only TimeID is provided*/
                case 1:
                    dataInstancesList.add(Utils.nextDataInstance_NoSeq(reader, present, past, sequenceID, timeIDcounter, attTimeID));

             /* Only SequenceID is provided*/
                case 2:
                    dataInstancesList.add(Utils.nextDataInstance_NoTimeID(reader, present, past, sequenceID, timeIDcounter, attSequenceID));

             /* SequenceID and TimeID are provided*/
                case 3:
                    dataInstancesList.add(Utils.nextDataInstance(reader, present, past, sequenceID, timeIDcounter, attSequenceID, attTimeID));
            }
            throw new IllegalArgumentException();
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
