package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.*;

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
         * We read the two first rows now, to create the first couple in nextDataInstance
         */
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
    }

    @Override
    public DataInstance nextDataInstance() {

        /* 0 = false, false, i.e., Not sequenceID nor TimeID are provided */
        /* 1 = true,  false, i.e., TimeID is provided */
        /* 2 = false, true,  i.e., SequenceID is provided */
        /* 3 = true,  true,  i.e., SequenceID is provided*/
        int option = (attTimeID == null) ? 1 : 0 + 2 * ((attSequenceID == null) ? 1 : 0);

        switch (option) {

            /* Not sequenceID nor TimeID are provided*/
            case 0:
                nextDynamicDataInstance.nextDataInstance_NoTimeID_NoSeq(reader);

             /* Only TimeID is provided*/
            case 1:
                nextDynamicDataInstance.nextDataInstance_NoSeq(reader, attTimeID);

             /* Only SequenceID is provided*/
            case 2:
                nextDynamicDataInstance.nextDataInstance_NoTimeID(reader, attSequenceID);

             /* SequenceID and TimeID are provided*/
            case 3:
                nextDynamicDataInstance.nextDataInstance(reader, attSequenceID, attTimeID);
        }
        throw new IllegalArgumentException();
    }

    @Override
    public boolean hasMoreDataInstances() {
        return reader.hasMoreDataRows();
    }

    @Override
    public Attributes getAttributes() {
        return reader.getAttributes();
    }

    @Override
    public void restart() {
        this.reader.reset();
    }
}
