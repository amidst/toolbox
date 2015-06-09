package eu.amidst.dynamic.datastream.filereaders;

import eu.amidst.corestatic.datastream.Attributes;
import eu.amidst.corestatic.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.corestatic.datastream.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.corestatic.variables.Variable;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

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
        loadFileAndInitialize("datasets/dataWeka/labor.arff");

        Assert.assertEquals(17, attributes.getList().size());
        Assert.assertEquals(17, obsVars.size());
        //assertEquals(17, temporalClones.size());
        Assert.assertEquals(17, dynamicVariables.getNumberOfVars());
    }

    @Test
    public void numericAttributeValue_NoTimeID_NoSeq(){
        loadFileAndInitialize("datasets/dataWeka/labor.arff");

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        Assert.assertEquals(40, (int) nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));
    }

    @Test
    public void reachEOF_NoTimeID_NoSeq(){
        loadFileAndInitialize("datasets/dataWeka/labor.arff");

        while(dataOnDiskIterator.hasNext())
        {
            nextInstance = dataOnDiskIterator.next();
        }

        /*Test values for the last instance*/
        Assert.assertEquals(88, (int) nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")));
        Assert.assertEquals(89, (int) nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));
    }

    @Test
    public void checkAutomaticTimeID_NoTimeID_NoSeq(){
        loadFileAndInitialize("datasets/dataWeka/labor.arff");

        while(dataOnDiskIterator.hasNext())
        {
            nextInstance = dataOnDiskIterator.next();
        }

        /*Test values for the last instance*/
        Assert.assertEquals(56, (int) nextInstance.getTimeID());
    }

    @Test
    public void checkAutomaticSeq_NoTimeID_NoSeq(){
        loadFileAndInitialize("datasets/dataWeka/labor.arff");

        while(dataOnDiskIterator.hasNext())
        {
            nextInstance = dataOnDiskIterator.next();
        }

        /*Test values for the last instance*/
        Assert.assertEquals(0, (int) nextInstance.getSequenceID());
    }

    /**********************************************************
     *                       TimeID
     **********************************************************/

    @Test
    public void nOfVars_TimeID(){
        loadFileAndInitialize("datasets/dataWeka/laborTimeID.arff");

        Assert.assertEquals(18, attributes.getList().size());
        Assert.assertEquals(17, obsVars.size());
        //assertEquals(17, temporalClones.size());
        Assert.assertEquals(17, dynamicVariables.getNumberOfVars());
    }



    @Test
    public void attributeValue_TimeID(){
        loadFileAndInitialize("datasets/dataWeka/laborTimeID.arff");

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }

        //[?,1]
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        Assert.assertEquals(40, (int) nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[1,2]
        Assert.assertEquals(40, (int) nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")));
        Assert.assertEquals(35, (int) nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[2,3]
        Assert.assertEquals(2, (int) nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("contribution-to-health-plan")));
        Assert.assertEquals(1, (int) nextInstance.getValue(dynamicVariables.getVariableByName("contribution-to-health-plan")));

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[3,?]
        Assert.assertEquals(1, (int) nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("contribution-to-health-plan")));
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getVariableByName("contribution-to-health-plan")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[?,?]
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("contribution-to-health-plan")), DELTA);
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getVariableByName("contribution-to-health-plan")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[?,5]
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("contribution-to-health-plan")), DELTA);
        Assert.assertEquals(4.5, nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[5,6]

    }

    @Test
    public void attributeValue_TimeID2(){
        loadFileAndInitialize("datasets/dataWeka/laborTimeID2.arff");

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }

        //[?,1]
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        Assert.assertEquals(40, (int) nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[1,?]
        Assert.assertEquals(40, (int) nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")));
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }

        //[?,?]
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }

        //[?,?]
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }

        //[?,5]
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        Assert.assertEquals(35, nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }

        //[5,6]
        Assert.assertEquals(35, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        Assert.assertEquals(38, nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")), DELTA);
    }



    @Test
    public void reachEOF_TimeID(){
        loadFileAndInitialize("datasets/dataWeka/laborTimeID.arff");

        for(DynamicDataInstance instance: dataOnDisk)
        {
            nextInstance = instance;
        }

        /*Test values for the last instance*/
        Assert.assertEquals(88, (int) nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")));
        Assert.assertEquals(89, (int) nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));
    }

    @Test
    public void checkAutomaticTimeID_TimeID(){
        loadFileAndInitialize("datasets/dataWeka/laborTimeID.arff");

        for(DynamicDataInstance instance: dataOnDisk)
        {
            nextInstance = instance;
        }

        /*Test values for the last instance*/
        Assert.assertEquals(59, (int) nextInstance.getTimeID());
    }

    @Test
    public void checkAutomaticSeq_TimeID(){
        loadFileAndInitialize("datasets/dataWeka/laborTimeID.arff");

        for(DynamicDataInstance instance: dataOnDisk)
        {
            nextInstance = instance;
        }

        /*Test values for the last instance*/
        Assert.assertEquals(0, (int) nextInstance.getSequenceID());
    }


    /**********************************************************
     *                        Seq
     **********************************************************/

    @Test
    public void nOfVars_seqID(){
        loadFileAndInitialize("datasets/dataWeka/laborSeqID.arff");

        Assert.assertEquals(18, attributes.getList().size());
        Assert.assertEquals(17, obsVars.size());
        //assertEquals(17, temporalClones.size());
        Assert.assertEquals(17, dynamicVariables.getNumberOfVars());
    }

    @Test
    public void attributeValue_seqID() {
        loadFileAndInitialize("datasets/dataWeka/laborSeqID.arff");

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //Seq 1: Instances 1-4
        //[?,1]
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        Assert.assertEquals(40, (int) nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[1,2]
        Assert.assertEquals(40, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        Assert.assertEquals(35, (int) nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[2,3]
        Assert.assertEquals(4.5, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("wage-increase-first-year")), DELTA);
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[3,4]
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("wage-increase-first-year")), DELTA);
        Assert.assertEquals(3.7, nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //Seq 2: Instances 5-17
        //[5,6] (Every time we change sequence we add a missing row)
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("wage-increase-first-year")), DELTA);
        Assert.assertEquals(4.5, nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[7,8]
        Assert.assertEquals(4.5, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("wage-increase-first-year")), DELTA);
        Assert.assertEquals(2, nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);
    }

    @Test
    public void reachEOF_SeqID(){
        loadFileAndInitialize("datasets/dataWeka/laborSeqID.arff");

        for(DynamicDataInstance instance: dataOnDisk){}

        /*Test values for the last instance*/
        Assert.assertEquals(88, (int) nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")));
        Assert.assertEquals(89, (int) nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));
    }

    @Test
    public void checkAutomaticTimeID_SeqID(){
        loadFileAndInitialize("datasets/dataWeka/laborSeqID.arff");

        for(DynamicDataInstance instance: dataOnDisk){}

        /*Test values for the last instance*/
        Assert.assertEquals(39, (int) nextInstance.getTimeID());
    }

    @Test
    public void checkAutomaticSeq_SeqID(){
        loadFileAndInitialize("datasets/dataWeka/laborSeqID.arff");

        for(DynamicDataInstance instance: dataOnDisk)
        {
            nextInstance = instance;
        }

        /*Test values for the last instance*/
        Assert.assertEquals(2, (int) nextInstance.getSequenceID());
    }


    /**********************************************************
     *                    TimeID & Seq
     **********************************************************/

    @Test
    public void nOfVars_TimeID_SeqID(){
        loadFileAndInitialize("datasets/dataWeka/laborTimeIDSeqID.arff");

        Assert.assertEquals(19, attributes.getList().size());
        Assert.assertEquals(17, obsVars.size());
        //assertEquals(17, temporalClones.size());
        Assert.assertEquals(17, dynamicVariables.getNumberOfVars());
    }

    @Test
    public void attributeValue_TimeID_SeqID() {
        loadFileAndInitialize("datasets/dataWeka/laborTimeIDSeqID.arff");

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //Seq 1: Instances 1-5
        //[?,1]
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        Assert.assertEquals(40, (int) nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[1,2]
        Assert.assertEquals(40, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")), DELTA);
        Assert.assertEquals(35, (int) nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[2,3]
        Assert.assertEquals(4.5, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("wage-increase-first-year")), DELTA);
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[3,?]
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("wage-increase-first-year")), DELTA);
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[?,5]
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("wage-increase-first-year")), DELTA);
        Assert.assertEquals(3.7, nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //Seq 2: Instances 1-13 (5-17)
        //[6,7] (Every time we change sequence we add a missing row)
        Assert.assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("wage-increase-first-year")), DELTA);
        Assert.assertEquals(4.5, nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[7,8] (Every time we change sequence we add a missing row)
        Assert.assertEquals(4.5, nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("wage-increase-first-year")), DELTA);
        Assert.assertEquals(2, nextInstance.getValue(dynamicVariables.getVariableByName("wage-increase-first-year")), DELTA);
    }

    @Test
    public void reachEOF_TimeID_SeqID(){
        loadFileAndInitialize("datasets/dataWeka/laborTimeIDSeqID.arff");

        for(DynamicDataInstance instance: dataOnDisk)
        {
            nextInstance = instance;
        }

        /*Test values for the last instance*/
        Assert.assertEquals(88, (int) nextInstance.getValue(dynamicVariables.getInterfaceVariableByName("working-hours")));
        Assert.assertEquals(89, (int) nextInstance.getValue(dynamicVariables.getVariableByName("working-hours")));
    }

    @Test
    public void checkAutomaticTimeID_TimeID_SeqID(){
        loadFileAndInitialize("datasets/dataWeka/laborTimeIDSeqID.arff");

        for(DynamicDataInstance instance: dataOnDisk)
        {
            nextInstance = instance;
        }

        /*Test values for the last instance*/
        Assert.assertEquals(39, (int) nextInstance.getTimeID());
    }

    @Test
    public void checkAutomaticSeq_TimeID_SeqID(){
        loadFileAndInitialize("datasets/dataWeka/laborTimeIDSeqID.arff");

        for(DynamicDataInstance instance: dataOnDisk)
        {
            nextInstance = instance;
        }

        /*Test values for the last instance*/
        Assert.assertEquals(2, (int) nextInstance.getSequenceID());
    }


}
