package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.Attribute;

/**
 * Created by ana@cs.aau.dk on 13/11/14.
 */
public final class NextDynamicDataInstance {

    private DataRow present;
    private DataRow past;

    /* Only used in case the sequenceID is not in the datafile */
    private int sequenceID = 0;
    /* timeIDcounter is used to keep track of missing values*/
    private int timeIDcounter = 0;

    public NextDynamicDataInstance(DataRow past, DataRow present){
        this.past = past;
        this.present = present;

        this.sequenceID = 1;
        this.timeIDcounter = 1;
    }
    public  DynamicDataInstance nextDataInstance_NoTimeID_NoSeq(DataFileReader reader){
        DynamicDataInstance dynDataInst = new DynamicDataInstance(present, past, sequenceID, ++timeIDcounter);
        past = present;
        present = reader.nextDataRow();
        return dynDataInst;
    }

    public DynamicDataInstance nextDataInstance_NoSeq(DataFileReader reader, Attribute attTimeID){
        double pastTimeID = past.getValue(attTimeID);

        /*Missing values of the form (X,?), where X can also be ?*/
        if(timeIDcounter < present.getValue(attTimeID)){
            timeIDcounter++;
            DynamicDataInstance dynDataInst = new DynamicDataInstance(past, new DataRowMissing(), (int) sequenceID,
                    (int) pastTimeID);
            past = new DataRowMissing(); //present is still the same instance, we need to fill in the missing instances
            return dynDataInst;

        /*Missing values of the form (X,Y), where X can also be ? and Y is an observed (already read) instance*/
        }else if(timeIDcounter == present.getValue(attTimeID)) {
            timeIDcounter++;
            DynamicDataInstance dynDataInst = new DynamicDataInstance(past, present, (int) sequenceID,
                    (int) pastTimeID);
            past = present; //present is still the same instance, we need to fill in the missing instances
            return dynDataInst;

        /*Read a new DataRow*/
        }else{
            present = reader.nextDataRow();
            /*Recursive call to this method taking into account the past DataRow*/
            return nextDataInstance_NoSeq(reader, attTimeID);
        }

    }

    public DynamicDataInstance nextDataInstance_NoTimeID(DataFileReader reader, Attribute attSequenceID){
        double pastSequenceID = past.getValue(attSequenceID);
        double presentSequenceID = present.getValue(attSequenceID);
        if (pastSequenceID == presentSequenceID) {
            timeIDcounter++;
            DynamicDataInstance dynDataInst = new DynamicDataInstance(present, past, (int) presentSequenceID, timeIDcounter);
            past = present;
            present = reader.nextDataRow();
            return dynDataInst;
        }
        else{
             past = present;
             present = reader.nextDataRow();
             /* Recursive call */
             timeIDcounter = 0;
             return nextDataInstance_NoTimeID(reader, attSequenceID);
        }
    }

    public DynamicDataInstance nextDataInstance(DataFileReader reader, Attribute attSequenceID, Attribute attTimeID){
        double pastSequenceID = past.getValue(attSequenceID);
        double pastTimeID = past.getValue(attTimeID);

        /*Missing values of the form (X,?), where X can also be ?*/
        if(timeIDcounter < present.getValue(attTimeID)){
            timeIDcounter++;
            DynamicDataInstance dynDataInst = new DynamicDataInstance(past, new DataRowMissing(), (int) pastSequenceID,
                    (int) pastTimeID);
            past = new DataRowMissing(); //present is still the same instance, we need to fill in the missing instances
            return dynDataInst;

        /*Missing values of the form (X,Y), where X can also be ? and Y is an observed (already read) instance*/
        }else if(timeIDcounter == present.getValue(attTimeID)) {
            timeIDcounter++;
            DynamicDataInstance dynDataInst = new DynamicDataInstance(past, present, (int) pastSequenceID,
                    (int) pastTimeID);
            past = present; //present is still the same instance, we need to fill in the missing instances
            return dynDataInst;

        /*Read a new DataRow*/
        }else{
            present = reader.nextDataRow();
            double presentSequenceID = present.getValue(attSequenceID);
            if (pastSequenceID == presentSequenceID) {
                /*Recursive call to this method taking into account the past DataRow*/
                return nextDataInstance(reader, attSequenceID, attTimeID);
            }else{
                past = present;
                present = reader.nextDataRow();
                /* Recursive call discarding the past DataRow*/
                timeIDcounter = 0;
                return nextDataInstance(reader, attSequenceID, attTimeID);
            }
        }
    }

    public boolean isNull(){
        return (present==null || past==null);
    }

}
