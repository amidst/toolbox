package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ana@cs.aau.dk on 12/11/14.
 */
public class DynamicDataOnMemoryFromFile implements DataOnMemory, DataOnDisk, DataOnStream {

    DataFileReader reader;

    Attribute attSequenceID;
    Attribute attTimeID;

    NextDynamicDataInstance nextDynamicDataInstance;

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
        attTimeID = this.reader.getAttributes().getAttributeByName("TIME_ID");

        nextDynamicDataInstance = new NextDynamicDataInstance(past, present);

        while (reader.hasMoreDataRows()) {

            /* 0 = false, false, i.e., Not sequenceID nor TimeID are provided */
            /* 1 = true,  false, i.e., TimeID is provided */
            /* 2 = false, true,  i.e., SequenceID is provided */
            /* 3 = true,  true,  i.e., SequenceID is provided*/
            int option = (attTimeID == null) ? 1 : 0 + 2 * ((attSequenceID == null) ? 1 : 0);

            switch (option) {

            /* Not sequenceID nor TimeID are provided*/
                case 0:
                    dataInstancesList.add(nextDynamicDataInstance.nextDataInstance_NoTimeID_NoSeq(reader));

             /* Only TimeID is provided*/
                case 1:
                    dataInstancesList.add(nextDynamicDataInstance.nextDataInstance_NoSeq(reader, attTimeID));

             /* Only SequenceID is provided*/
                case 2:
                    dataInstancesList.add(nextDynamicDataInstance.nextDataInstance_NoTimeID(reader, attSequenceID));

             /* SequenceID and TimeID are provided*/
                case 3:
                    dataInstancesList.add(nextDynamicDataInstance.nextDataInstance(reader, attSequenceID, attTimeID));
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
