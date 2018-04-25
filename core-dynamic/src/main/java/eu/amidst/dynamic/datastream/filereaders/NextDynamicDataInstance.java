/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

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
package eu.amidst.dynamic.datastream.filereaders;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.core.datastream.filereaders.DataRow;
import eu.amidst.core.datastream.filereaders.DataRowMissing;

import java.util.Iterator;

/**
 * The NextDynamicDataInstance class is used to load the next dynamic data instance.
 */
public final class NextDynamicDataInstance {

    /** Represents a {@link DataRow} object of the present time. */
    private DataRow present;

    /** Represents a {@link DataRow} object of the past time. */
    private DataRow past;

    /** Represents the sequence ID.It is only used in case that the sequenceID is not in the datafile.*/
    private int sequenceID;

    /** Represents the timeIDcounter that is used to keep track of missing values. */
    private int timeIDcounter;

    /** Represents the start of a sequence*/
    private boolean startOfSequence = false;

    /** Represents the start of a sequence*/
    private DynamicDataInstanceImpl dynDataInstStart;

    /**
     * Creates a new NextDynamicDataInstance object.
     * @param past a {@link DataRow} object of the past time.
     * @param present a {@link DataRow} object of the present time.
     * @param sequenceID the sequence ID.
     * @param timeIDcounter the timeIDcounter.
     */
    public NextDynamicDataInstance(DataRow past, DataRow present, int sequenceID, int timeIDcounter){
        this.past = past;
        this.present = present;
        this.sequenceID = sequenceID;
        this.timeIDcounter = timeIDcounter;
    }

    /**
     * Returns a {@link DynamicDataInstance} object in case no SequenceID or TimeID are provided.
     * @param reader an Iterator object.
     * @return a {@link DynamicDataInstance} object.
     */
    public DynamicDataInstance nextDataInstance_NoTimeID_NoSeq(Iterator<DataRow> reader){
        DynamicDataInstanceImpl dynDataInst = null;
        if(timeIDcounter == 0) {
            dynDataInst = new DynamicDataInstanceImpl(past, present, sequenceID, timeIDcounter++);
        }else {
            past = present;
            present = reader.next();
            dynDataInst = new DynamicDataInstanceImpl(past, present, sequenceID, timeIDcounter++);
        }
        return dynDataInst;
    }

    /**
     * Returns a {@link DynamicDataInstance} object in case only a TimeID is provided.
     * @param reader an Iterator object.
     * @param attTimeID an {@link Attribute} object that represents the time ID.
     * @return a {@link DynamicDataInstance} object.
     */
    public DynamicDataInstance nextDataInstance_NoSeq(Iterator<DataRow> reader, Attribute attTimeID){
        double presentTimeID = present.getValue(attTimeID);

        /* Missing values of the form (X,?), where X can also be ?. */
        if(timeIDcounter < present.getValue(attTimeID)){
            timeIDcounter++;
            DynamicDataInstanceImpl dynDataInst = new DynamicDataInstanceImpl(past, new DataRowMissing(), (int) sequenceID,
                    (int) presentTimeID);
            past = new DataRowMissing(); //present is still the same instance, we need to fill in the missing instances
            return dynDataInst;

        /* Missing values of the form (X,Y), where X can also be ? and Y is an observed (already read) instance. */
        }else if(timeIDcounter == present.getValue(attTimeID)) {
            timeIDcounter++;
            DynamicDataInstanceImpl dynDataInst = new DynamicDataInstanceImpl(past, present, (int) sequenceID,
                    (int) presentTimeID);
            past = present; //present is still the same instance, we need to fill in the missing instances
            return dynDataInst;

        /* Read a new DataRow. */
        }else{
            present = reader.next();
            /*Recursive call to this method taking into account the past DataRow*/
            return nextDataInstance_NoSeq(reader, attTimeID);
        }

    }

    /**
     * Returns a {@link DynamicDataInstance} object in case only a SequenceID is provided.
     * @param reader an Iterator object.
     * @param attSequenceID an {@link Attribute} object that represents the sequence ID.
     * @return a {@link DynamicDataInstance} object.
     */
    public DynamicDataInstance nextDataInstance_NoTimeID(Iterator<DataRow> reader, Attribute attSequenceID){
        DynamicDataInstanceImpl dynDataInst = null;
        if(timeIDcounter == 0) {
            dynDataInst =  new DynamicDataInstanceImpl(past, present, (int) present.getValue(attSequenceID), timeIDcounter);
            timeIDcounter++;
            return dynDataInst;
        }
        past = present;
        present = reader.next();
        double pastSequenceID = past.getValue(attSequenceID);
        double presentSequenceID = present.getValue(attSequenceID);
        if (Double.isNaN(pastSequenceID) || pastSequenceID == presentSequenceID) {
            dynDataInst =  new DynamicDataInstanceImpl(past, present, (int) presentSequenceID, timeIDcounter);
            timeIDcounter++;
            return dynDataInst;
        }
        else{
             past = new DataRowMissing();
             /* Recursive call */
             timeIDcounter = 0;
            dynDataInst =  new DynamicDataInstanceImpl(past, present, (int) presentSequenceID, timeIDcounter);
            timeIDcounter++;
            return dynDataInst;
        }
    }

    /**
     * Returns a {@link DynamicDataInstance} object in case both a SequenceID  and a TimeID are provided.
     * @param reader an Iterator object.
     * @param attSequenceID an {@link Attribute} object that represents the sequence ID.
     * @param attTimeID an {@link Attribute} object that represents the time ID.
     * @return a {@link DynamicDataInstance} object.
     */
    public DynamicDataInstance nextDataInstance(Iterator<DataRow> reader, Attribute attSequenceID, Attribute attTimeID){
        double pastSequenceID = past.getValue(attSequenceID);
        double presentTimeID = present.getValue(attTimeID);

        /* Missing values of the form (X,?), where X can also be ?. */
        if(timeIDcounter < present.getValue(attTimeID)){
            timeIDcounter++;
            DynamicDataInstanceImpl dynDataInst = new DynamicDataInstanceImpl(past, new DataRowMissing(), (int) pastSequenceID,
                    (int) presentTimeID);
            past = new DataRowMissing(); //present is still the same instance, we need to fill in the missing instances
            return dynDataInst;

        /* Missing values of the form (X,Y), where X can also be ? and Y is an observed (already read) instance. */
        }else if(timeIDcounter == present.getValue(attTimeID)) {
            timeIDcounter++;
            DynamicDataInstanceImpl dynDataInst = new DynamicDataInstanceImpl(past, present, (int) pastSequenceID,
                    (int) presentTimeID);
            past = present; //present is still the same instance, we need to fill in the missing instances
            return dynDataInst;

        /* Read a new DataRow. */
        }else{

            if (startOfSequence){
                startOfSequence = false;
                return dynDataInstStart;
            }

            present = reader.next();
            double presentSequenceID = present.getValue(attSequenceID);
            if (pastSequenceID == presentSequenceID) {
                /*Recursive call to this method taking into account the past DataRow*/
                return nextDataInstance(reader, attSequenceID, attTimeID);
            }else{
                past = new DataRowMissing();
                /* It oculd be a recursive call discarding the past DataRow, but this is slightly more efficient*/
                dynDataInstStart = new DynamicDataInstanceImpl(past, present, (int) presentSequenceID,
                        (int) present.getValue(attTimeID));
                past = present;
                //present = reader.next();
                timeIDcounter = 1;
                //startOfSequence=true;
                return dynDataInstStart;
            }
        }
    }
}
