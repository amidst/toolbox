package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.*;

/**
 * Created by ana@cs.aau.dk on 12/11/14.
 */
public class DynamicDataOnDiskFromFile  implements DataOnDisk, DataOnStream {

    DataFileReader reader;
    /**
     * Only used in case the sequenceID is not in the datafile
     */
    int sequenceID = 0;
    /**
     * Only used in case the timeID is not in the datafile, time ID of the Present.
     */
    int timeIDcounter = 0;
    DataRow present;
    DataRow past;
    Attribute attSequenceID;
    Attribute attTimeID;

    public DynamicDataOnDiskFromFile(DataFileReader reader){
        this.reader=reader;
        /**
         * We read the first row now, to create the first couple in nextDataInstance
         */
        this.present = this.reader.nextDataRow();

        attSequenceID = this.reader.getAttributes().getAttributeByName("SEQUENCE_ID");
        if (attSequenceID == null){
            /* This value should not be modified */
            this.sequenceID = 1;
        }

        attTimeID = this.reader.getAttributes().getAttributeByName("TIME_ID");
        if(attTimeID == null){
            this.timeIDcounter = 1;
        }
    }

    @Override
    public DataInstance nextDataInstance() {

        past = present;
        present = this.reader.nextDataRow();

        /* Not sequenceID nor TimeID are provided*/
        if(attSequenceID == null && attTimeID == null){
            return new DynamicDataInstance(present, past, sequenceID, ++timeIDcounter);

         /* TimeID is provided*/
        }else if(attSequenceID == null){
            return new DynamicDataInstance(present, past, sequenceID, (int)present.getValue(attTimeID));

         /* SequenceID is provided*/
        }else if(attTimeID == null){
            double pastSequenceID = past.getValue(attSequenceID);
            double presentSequenceID = present.getValue(attSequenceID);
            if(pastSequenceID==presentSequenceID){
                return new DynamicDataInstance(present, past, (int)presentSequenceID, ++timeIDcounter);
            }
        }
         /* SequenceID and TimeID are provided*/
        double pastSequenceID = past.getValue(attSequenceID);
        double presentSequenceID = present.getValue(attSequenceID);
        double pastTimeID = past.getValue(attTimeID);
        double presentTimeID = present.getValue(attTimeID);
        return new DynamicDataInstance(present, past, (int) presentSequenceID, (int) presentTimeID);

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
