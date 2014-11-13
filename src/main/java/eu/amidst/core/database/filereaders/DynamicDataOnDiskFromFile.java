package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.*;

import eu.amidst.core.database.filereaders.Utils;

/**
 * Created by ana@cs.aau.dk on 12/11/14.
 */
public class DynamicDataOnDiskFromFile  implements DataOnDisk, DataOnStream {

    DataFileReader reader;
    /* Only used in case the sequenceID is not in the datafile */
    int sequenceID = 0;
    /* timeIDcounter is used to keep track of missing values*/
    int timeIDcounter = 0;
    DataRow present;
    DataRow past;
    Attribute attSequenceID;
    Attribute attTimeID;


    public DynamicDataOnDiskFromFile(DataFileReader reader){
        this.reader=reader;
        /**
         * We read the two first rows now, to create the first couple in nextDataInstance
         */
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
        this.timeIDcounter = 1;
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
                Utils.nextDataInstance_NoTimeID_NoSeq(reader, present, past, sequenceID, timeIDcounter);

             /* Only TimeID is provided*/
            case 1:
                Utils.nextDataInstance_NoSeq(reader, present, past, sequenceID, timeIDcounter, attTimeID);

             /* Only SequenceID is provided*/
            case 2:
                Utils.nextDataInstance_NoTimeID(reader, present, past, sequenceID, timeIDcounter, attSequenceID);

             /* SequenceID and TimeID are provided*/
            case 3:
                Utils.nextDataInstance(reader, present, past, sequenceID, timeIDcounter, attSequenceID, attTimeID);
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
