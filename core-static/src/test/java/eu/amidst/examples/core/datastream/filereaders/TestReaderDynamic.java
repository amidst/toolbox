package eu.amidst.examples.core.datastream.filereaders;

import eu.amidst.examples.core.datastream.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.examples.core.variables.DynamicVariables;
import eu.amidst.examples.core.variables.Variable;
import eu.amidst.examples.core.datastream.Attributes;
import eu.amidst.examples.core.datastream.DataStream;
import eu.amidst.examples.core.datastream.DynamicDataInstance;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by ana@cs.aau.dk on 18/11/14.
 */

//TODO Test DataOnMemory
public class TestReaderDynamic {

    private static final double DELTA = 1e-15;
    private static ARFFDataReader reader;
    private static Attributes attributes;
    private static DynamicVariables dynamicVariables;

    private static DynamicDataInstance nextInstance = null;
    private static int index;
    private static Variable var;
    private static List<Variable> obsVars;
    private static List<Variable> temporalClones;
    private static DataStream<DynamicDataInstance> dataOnDisk;
    private static Iterator<DynamicDataInstance> dataOnDiskIterator;


    public static void loadFileAndInitialize(String s) {
        reader = new ARFFDataReader();
        reader.loadFromFile(s);
        attributes = reader.getAttributes();
        dataOnDisk = new DynamicDataStreamFromFile(reader);
        dataOnDiskIterator = dataOnDisk.iterator();
        dynamicVariables = new DynamicVariables(attributes);
        obsVars = dynamicVariables.getListOfDynamicVariables();
        //temporalClones = dynamicVariables.getListOfTemporalClones();
    }

    /**********************************************************
     *                    NoTimeID & NoSeq
     **********************************************************/

    @Test
    public void nOfVars_NoTimeID_NoSeq(){
        loadFileAndInitialize("data/dataWeka/labor.arff");

        assertEquals(17, attributes.getList().size());
        assertEquals(17, obsVars.size());
        //assertEquals(17, temporalClones.size());
        assertEquals(17, dynamicVariables.getNumberOfVars());
    }

    @Test
    public void numericAttributeValue_NoTimeID_NoSeq(){
        loadFileAndInitialize("data/dataWeka/labor.arff");

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        assertEquals(40,(int)nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));
    }

    @Test
    public void reachEOF_NoTimeID_NoSeq(){
        loadFileAndInitialize("data/dataWeka/labor.arff");

        while(dataOnDiskIterator.hasNext())
        {
            nextInstance = dataOnDiskIterator.next();
        }

        /*Test values for the last instance*/
        assertEquals(88,(int)nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")));
        assertEquals(89,(int)nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));
    }

    @Test
    public void checkAutomaticTimeID_NoTimeID_NoSeq(){
        loadFileAndInitialize("data/dataWeka/labor.arff");

        while(dataOnDiskIterator.hasNext())
        {
            nextInstance = dataOnDiskIterator.next();
        }

        /*Test values for the last instance*/
        assertEquals(56,(int)nextInstance.getTimeID());
    }

    @Test
    public void checkAutomaticSeq_NoTimeID_NoSeq(){
        loadFileAndInitialize("data/dataWeka/labor.arff");

        while(dataOnDiskIterator.hasNext())
        {
            nextInstance = dataOnDiskIterator.next();
        }

        /*Test values for the last instance*/
        assertEquals(0,(int)nextInstance.getSequenceID());
    }

    /**********************************************************
     *                       TimeID
     **********************************************************/

    @Test
    public void nOfVars_TimeID(){
        loadFileAndInitialize("data/dataWeka/laborTimeID.arff");

        assertEquals(18, attributes.getList().size());
        assertEquals(17, obsVars.size());
        //assertEquals(17, temporalClones.size());
        assertEquals(17, dynamicVariables.getNumberOfVars());
    }



    @Test
    public void attributeValue_TimeID(){
        loadFileAndInitialize("data/dataWeka/laborTimeID.arff");

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }

        //[?,1]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        assertEquals(40,(int)nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[1,2]
        assertEquals(40,(int)nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")));
        assertEquals(35,(int)nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[2,3]
        assertEquals(2,(int)nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("contribution-to-health-plan")));
        assertEquals(1,(int)nextInstance.getValue(dynamicVariables.getVariableByName("contribution-to-health-plan")));

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[3,?]
        assertEquals(1,(int)nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("contribution-to-health-plan")));
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getVariableByName("contribution-to-health-plan")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[?,?]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("contribution-to-health-plan")), DELTA);
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getVariableByName("contribution-to-health-plan")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[?,5]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("contribution-to-health-plan")), DELTA);
        assertEquals(4.5,nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[5,6]

    }

    @Test
    public void attributeValue_TimeID2(){
        loadFileAndInitialize("data/dataWeka/laborTimeID2.arff");

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }

        //[?,1]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        assertEquals(40,(int)nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[1,?]
        assertEquals(40,(int)nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")));
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }

        //[?,?]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }

        //[?,?]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }

        //[?,5]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        assertEquals(35,nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }

        //[5,6]
        assertEquals(35,nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        assertEquals(38,nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")), DELTA);
    }



    @Test
    public void reachEOF_TimeID(){
        loadFileAndInitialize("data/dataWeka/laborTimeID.arff");

        for(DynamicDataInstance instance: dataOnDisk)
        {
            nextInstance = instance;
        }

        /*Test values for the last instance*/
        assertEquals(88,(int)nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")));
        assertEquals(89,(int)nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));
    }

    @Test
    public void checkAutomaticTimeID_TimeID(){
        loadFileAndInitialize("data/dataWeka/laborTimeID.arff");

        for(DynamicDataInstance instance: dataOnDisk)
        {
            nextInstance = instance;
        }

        /*Test values for the last instance*/
        assertEquals(59,(int)nextInstance.getTimeID());
    }

    @Test
    public void checkAutomaticSeq_TimeID(){
        loadFileAndInitialize("data/dataWeka/laborTimeID.arff");

        for(DynamicDataInstance instance: dataOnDisk)
        {
            nextInstance = instance;
        }

        /*Test values for the last instance*/
        assertEquals(0,(int)nextInstance.getSequenceID());
    }


    /**********************************************************
     *                        Seq
     **********************************************************/

    @Test
    public void nOfVars_seqID(){
        loadFileAndInitialize("data/dataWeka/laborSeqID.arff");

        assertEquals(18, attributes.getList().size());
        assertEquals(17, obsVars.size());
        //assertEquals(17, temporalClones.size());
        assertEquals(17, dynamicVariables.getNumberOfVars());
    }

    @Test
    public void attributeValue_seqID() {
        loadFileAndInitialize("data/dataWeka/laborSeqID.arff");

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //Seq 1: Instances 1-4
        //[?,1]
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        assertEquals(40, (int) nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[1,2]
        assertEquals(40, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        assertEquals(35, (int) nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[2,3]
        assertEquals(4.5, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("wage-increase-first-year")), DELTA);
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[3,4]
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("wage-increase-first-year")), DELTA);
        assertEquals(3.7, nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //Seq 2: Instances 5-17
        //[5,6] (Every time we change sequence we add a missing row)
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("wage-increase-first-year")), DELTA);
        assertEquals(4.5, nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[7,8]
        assertEquals(4.5, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("wage-increase-first-year")), DELTA);
        assertEquals(2, nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);
    }

    @Test
    public void reachEOF_SeqID(){
        loadFileAndInitialize("data/dataWeka/laborSeqID.arff");

        for(DynamicDataInstance instance: dataOnDisk){}

        /*Test values for the last instance*/
        assertEquals(88,(int)nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")));
        assertEquals(89,(int)nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));
    }

    @Test
    public void checkAutomaticTimeID_SeqID(){
        loadFileAndInitialize("data/dataWeka/laborSeqID.arff");

        for(DynamicDataInstance instance: dataOnDisk){}

        /*Test values for the last instance*/
        assertEquals(39,(int)nextInstance.getTimeID());
    }

    @Test
    public void checkAutomaticSeq_SeqID(){
        loadFileAndInitialize("data/dataWeka/laborSeqID.arff");

        for(DynamicDataInstance instance: dataOnDisk)
        {
            nextInstance = instance;
        }

        /*Test values for the last instance*/
        assertEquals(2,(int)nextInstance.getSequenceID());
    }


    /**********************************************************
     *                    TimeID & Seq
     **********************************************************/

    @Test
    public void nOfVars_TimeID_SeqID(){
        loadFileAndInitialize("data/dataWeka/laborTimeIDSeqID.arff");

        assertEquals(19, attributes.getList().size());
        assertEquals(17, obsVars.size());
        //assertEquals(17, temporalClones.size());
        assertEquals(17, dynamicVariables.getNumberOfVars());
    }

    @Test
    public void attributeValue_TimeID_SeqID() {
        loadFileAndInitialize("data/dataWeka/laborTimeIDSeqID.arff");

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //Seq 1: Instances 1-5
        //[?,1]
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        assertEquals(40, (int) nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[1,2]
        assertEquals(40, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        assertEquals(35, (int) nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[2,3]
        assertEquals(4.5, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("wage-increase-first-year")), DELTA);
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[3,?]
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("wage-increase-first-year")), DELTA);
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[?,5]
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("wage-increase-first-year")), DELTA);
        assertEquals(3.7, nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //Seq 2: Instances 1-13 (5-17)
        //[6,7] (Every time we change sequence we add a missing row)
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("wage-increase-first-year")), DELTA);
        assertEquals(4.5, nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[7,8] (Every time we change sequence we add a missing row)
        assertEquals(4.5, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("wage-increase-first-year")), DELTA);
        assertEquals(2, nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);
    }

    @Test
    public void reachEOF_TimeID_SeqID(){
        loadFileAndInitialize("data/dataWeka/laborTimeIDSeqID.arff");

        for(DynamicDataInstance instance: dataOnDisk)
        {
            nextInstance = instance;
        }

        /*Test values for the last instance*/
        assertEquals(88,(int)nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")));
        assertEquals(89,(int)nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));
    }

    @Test
    public void checkAutomaticTimeID_TimeID_SeqID(){
        loadFileAndInitialize("data/dataWeka/laborTimeIDSeqID.arff");

        for(DynamicDataInstance instance: dataOnDisk)
        {
            nextInstance = instance;
        }

        /*Test values for the last instance*/
        assertEquals(39,(int)nextInstance.getTimeID());
    }

    @Test
    public void checkAutomaticSeq_TimeID_SeqID(){
        loadFileAndInitialize("data/dataWeka/laborTimeIDSeqID.arff");

        for(DynamicDataInstance instance: dataOnDisk)
        {
            nextInstance = instance;
        }

        /*Test values for the last instance*/
        assertEquals(2,(int)nextInstance.getSequenceID());
    }


}
