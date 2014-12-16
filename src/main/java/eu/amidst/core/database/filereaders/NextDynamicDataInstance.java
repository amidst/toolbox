/**
 ******************* ISSUE LIST **************************
 *
 * 1. We could eliminate the if(timeIDcounter == 1) in nextDataInstance_NoTimeID_NoSeq if
 * we maintain a future DataRow (we read an extra row in advance). Then we would need the
 * method public boolean isNull(){
 * return (present==null || past==null);
 * }
 *
 * ********************************************************
 */
package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.Attribute;

import java.util.Iterator;

/**
 * Created by ana@cs.aau.dk on 13/11/14.
 */
public final class NextDynamicDataInstance {

    private DataRow present;
    private DataRow past;

    /* Only used in case the sequenceID is not in the datafile */
    private int sequenceID;
    /* timeIDcounter is used to keep track of missing values*/
    private int timeIDcounter;

    public NextDynamicDataInstance(DataRow past, DataRow present, int sequenceID, int timeIDcounter){
        this.past = past;
        this.present = present;

        this.sequenceID = sequenceID;
        this.timeIDcounter = timeIDcounter;
    }
    public  DynamicDataInstance nextDataInstance_NoTimeID_NoSeq(Iterator<DataRow> reader){
        DynamicDataInstance dynDataInst = null;
        if(timeIDcounter == 0) {
            dynDataInst = new DynamicDataInstance(past, present, sequenceID, timeIDcounter++);
        }else {
            past = present;
            present = reader.next();
            dynDataInst = new DynamicDataInstance(past, present, sequenceID, timeIDcounter++);
        }
        return dynDataInst;
    }

    public DynamicDataInstance nextDataInstance_NoSeq(Iterator<DataRow> reader, Attribute attTimeID){
        double presentTimeID = present.getValue(attTimeID);

        /*Missing values of the form (X,?), where X can also be ?*/
        if(timeIDcounter < present.getValue(attTimeID)){
            timeIDcounter++;
            DynamicDataInstance dynDataInst = new DynamicDataInstance(past, new DataRowMissing(), (int) sequenceID,
                    (int) presentTimeID);
            past = new DataRowMissing(); //present is still the same instance, we need to fill in the missing instances
            return dynDataInst;

        /*Missing values of the form (X,Y), where X can also be ? and Y is an observed (already read) instance*/
        }else if(timeIDcounter == present.getValue(attTimeID)) {
            timeIDcounter++;
            DynamicDataInstance dynDataInst = new DynamicDataInstance(past, present, (int) sequenceID,
                    (int) presentTimeID);
            past = present; //present is still the same instance, we need to fill in the missing instances
            return dynDataInst;

        /*Read a new DataRow*/
        }else{
            present = reader.next();
            /*Recursive call to this method taking into account the past DataRow*/
            return nextDataInstance_NoSeq(reader, attTimeID);
        }

    }

    public DynamicDataInstance nextDataInstance_NoTimeID(Iterator<DataRow> reader, Attribute attSequenceID){
        DynamicDataInstance dynDataInst = null;
        if(timeIDcounter == 0) {
            dynDataInst =  new DynamicDataInstance(past, present, (int) present.getValue(attSequenceID), timeIDcounter);
            timeIDcounter++;
            return dynDataInst;
        }
        past = present;
        present = reader.next();
        double pastSequenceID = past.getValue(attSequenceID);
        double presentSequenceID = present.getValue(attSequenceID);
        if (Double.isNaN(pastSequenceID) || pastSequenceID == presentSequenceID) {
            dynDataInst =  new DynamicDataInstance(past, present, (int) presentSequenceID, timeIDcounter);
            timeIDcounter++;
            return dynDataInst;
        }
        else{
             past = new DataRowMissing();
             /* Recursive call */
             timeIDcounter = 0;
            dynDataInst =  new DynamicDataInstance(past, present, (int) presentSequenceID, timeIDcounter);
            timeIDcounter++;
            return dynDataInst;
        }
    }

    public DynamicDataInstance nextDataInstance(Iterator<DataRow> reader, Attribute attSequenceID, Attribute attTimeID){
        double pastSequenceID = past.getValue(attSequenceID);
        double presentTimeID = present.getValue(attTimeID);

        /*Missing values of the form (X,?), where X can also be ?*/
        if(timeIDcounter < present.getValue(attTimeID)){
            timeIDcounter++;
            DynamicDataInstance dynDataInst = new DynamicDataInstance(past, new DataRowMissing(), (int) pastSequenceID,
                    (int) presentTimeID);
            past = new DataRowMissing(); //present is still the same instance, we need to fill in the missing instances
            return dynDataInst;

        /*Missing values of the form (X,Y), where X can also be ? and Y is an observed (already read) instance*/
        }else if(timeIDcounter == present.getValue(attTimeID)) {
            timeIDcounter++;
            DynamicDataInstance dynDataInst = new DynamicDataInstance(past, present, (int) pastSequenceID,
                    (int) presentTimeID);
            past = present; //present is still the same instance, we need to fill in the missing instances
            return dynDataInst;

        /*Read a new DataRow*/
        }else{
            present = reader.next();
            double presentSequenceID = present.getValue(attSequenceID);
            if (pastSequenceID == presentSequenceID) {
                /*Recursive call to this method taking into account the past DataRow*/
                return nextDataInstance(reader, attSequenceID, attTimeID);
            }else{
                past = new DataRowMissing();
                /* It oculd be a recursive call discarding the past DataRow, but this is slightly more efficient*/
                DynamicDataInstance dynDataInst = new DynamicDataInstance(past, present, (int) presentSequenceID,
                        (int) present.getValue(attTimeID));
                past = present;
                present = reader.next();
                timeIDcounter = 1;
                return dynDataInst;
            }
        }
    }


}
