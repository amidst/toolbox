package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.DataInstance;

/**
 * Created by ana@cs.aau.dk on 13/11/14.
 */
public class Utils {

    public static DynamicDataInstance nextDataInstance_NoTimeID_NoSeq(DataFileReader reader, DataRow present,
                                                                      DataRow past, int sequenceID, int timeIDcounter){
        DynamicDataInstance dynDataInst = new DynamicDataInstance(present, past, sequenceID, ++timeIDcounter);
        past = present;
        present = reader.nextDataRow();
        return dynDataInst;
    }

    public static DynamicDataInstance nextDataInstance_NoSeq(DataFileReader reader, DataRow present, DataRow past,
                                                      int sequenceID, int timeIDcounter, Attribute attTimeID){
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
            return nextDataInstance_NoSeq(reader, present, past, sequenceID, timeIDcounter, attTimeID);
        }

    }

    public static DynamicDataInstance nextDataInstance_NoTimeID(DataFileReader reader, DataRow present, DataRow past,
                                                         int sequenceID, int timeIDcounter, Attribute attSequenceID){
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
             return nextDataInstance_NoTimeID(reader, present, past, sequenceID, timeIDcounter, attSequenceID);
        }
    }

    public static DynamicDataInstance nextDataInstance(DataFileReader reader, DataRow present, DataRow past, int sequenceID,
                                                int timeIDcounter, Attribute attSequenceID, Attribute attTimeID){
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
                return nextDataInstance(reader, present, past, sequenceID, timeIDcounter, attSequenceID, attTimeID);
            }else{
                past = present;
                present = reader.nextDataRow();
                /* Recursive call discarding the past DataRow*/
                return nextDataInstance(reader, present, past, sequenceID, timeIDcounter,  attSequenceID, attTimeID);
            }
        }
    }

}
