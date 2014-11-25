package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.*;

import java.util.Iterator;

/**
 * Created by ana@cs.aau.dk on 12/11/14.
 */
public class DynamicDataOnDiskFromFile  implements DataOnDisk, DataOnStream {

    DataFileReader reader;

    Attribute attSequenceID;
    Attribute attTimeID;

    NextDynamicDataInstance nextDynamicDataInstance;


    public DynamicDataOnDiskFromFile(DataFileReader reader){
        this.reader=reader;

        /**
         * We read the two first rows now, to create the first couple in next
         */
        DataRow present;
        DataRow past = new DataRowMissing();

        int timeID = 0;
        int sequenceID = 0;

        if (reader.hasNext()) {
            present = this.reader.next();
        }else {
            throw new UnsupportedOperationException("There are insufficient instances to learn a model.");
        }

        try {
            attSequenceID = this.reader.getAttributes().getAttributeByName("SEQUENCE_ID");
            sequenceID = (int)present.getValue(attSequenceID);
        }catch (UnsupportedOperationException e){
            attSequenceID = null;
        }
        try {
            attTimeID = this.reader.getAttributes().getAttributeByName("TIME_ID");
            timeID = (int)present.getValue(attTimeID);
        }catch (UnsupportedOperationException e){
            attTimeID = null;
        }

        nextDynamicDataInstance = new NextDynamicDataInstance(past, present, sequenceID, timeID);
    }

    @Override
    public DataInstance next() {

        /* 0 = false, false, i.e., Not sequenceID nor TimeID are provided */
        /* 1 = true,  false, i.e., TimeID is provided */
        /* 2 = false, true,  i.e., SequenceID is provided */
        /* 3 = true,  true,  i.e., SequenceID is provided*/
        int option = ((attTimeID == null) ? 0 : 1) + 2 * ((attSequenceID == null) ? 0 : 1);

        switch (option) {

            /* Not sequenceID nor TimeID are provided*/
            case 0:
                return nextDynamicDataInstance.nextDataInstance_NoTimeID_NoSeq(reader);

             /* Only TimeID is provided*/
            case 1:
                return nextDynamicDataInstance.nextDataInstance_NoSeq(reader, attTimeID);

             /* Only SequenceID is provided*/
            case 2:
                return nextDynamicDataInstance.nextDataInstance_NoTimeID(reader, attSequenceID);

             /* SequenceID and TimeID are provided*/
            case 3:
                return nextDynamicDataInstance.nextDataInstance(reader, attSequenceID, attTimeID);
        }
        throw new IllegalArgumentException();
    }

    @Override
    public boolean hasNext() {
        return reader.hasNext();
    }

    @Override
    public Attributes getAttributes() {
        return reader.getAttributes();
    }

    @Override
    public void restart() {
        this.reader.reset();
    }

    @Override
    public Iterator<DataInstance> iterator() {
        return this;
    }
}
