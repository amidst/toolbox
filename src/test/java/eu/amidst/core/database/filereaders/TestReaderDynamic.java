package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.header.DynamicModelHeader;
import eu.amidst.core.header.Variable;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by ana@cs.aau.dk on 18/11/14.
 */
public class TestReaderDynamic {

    private static final double DELTA = 1e-15;
    private static WekaDataFileReader reader;
    private static Attributes attributes;
    private static DynamicModelHeader dynamicModelHeader;
    private static DataRow datarow = null;
    private static DataInstance nextInstance = null;
    private static int index;
    private static Variable var;
    private static List<Variable> obsVars;
    private static List<Variable> temporalClones;
    private static DataOnDisk dataOnDisk;


    public static void loadFileAndInitialize(String s) {
        reader = new WekaDataFileReader(s);
        attributes = reader.getAttributes();
        dataOnDisk = new DynamicDataOnDiskFromFile(reader);
        dynamicModelHeader = new DynamicModelHeader(attributes);
        obsVars = dynamicModelHeader.getVariables();
        temporalClones = dynamicModelHeader.getTemporalClones();
    }

    /**********************************************************
     *                    NoTimeID & NoSeq
     **********************************************************/

    @Test
    public void nOfVars_NoTimeID_NoSeq(){
        loadFileAndInitialize("data/dataWeka/labor.arff");

        assertEquals(17, attributes.getList().size());
        assertEquals(17, obsVars.size());
        assertEquals(17, temporalClones.size());
        assertEquals(17, dynamicModelHeader.getNumberOfVars());
    }

    @Test
    public void numericAttributeValue_NoTimeID_NoSeq(){
        loadFileAndInitialize("data/dataWeka/labor.arff");

        if(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }
        assertEquals(Double.NaN,nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(40,(int)nextInstance.getValue(dynamicModelHeader.getVariableByName("WORKING-HOURS")));
    }

    @Test
    public void reachEOF_NoTimeID_NoSeq(){
        loadFileAndInitialize("data/dataWeka/labor.arff");

        while(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }

        /*Test values for the last instance*/
        assertEquals(88,(int)nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("WORKING-HOURS")));
        assertEquals(89,(int)nextInstance.getValue(dynamicModelHeader.getVariableByName("WORKING-HOURS")));
    }

    @Test
    public void checkAutomaticTimeID_NoTimeID_NoSeq(){
        loadFileAndInitialize("data/dataWeka/labor.arff");

        while(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }

        /*Test values for the last instance*/
        assertEquals(57,(int)nextInstance.getTimeID());
    }

    @Test
    public void checkAutomaticSeq_NoTimeID_NoSeq(){
        loadFileAndInitialize("data/dataWeka/labor.arff");

        while(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }

        /*Test values for the last instance*/
        assertEquals(1,(int)nextInstance.getSequenceID());
    }

    /**********************************************************
     *                       TimeID
     **********************************************************/

    @Test
    public void nOfVars_TimeID(){
        loadFileAndInitialize("data/dataWeka/laborTimeID.arff");

        assertEquals(18, attributes.getList().size());
        assertEquals(17, obsVars.size());
        assertEquals(17, temporalClones.size());
        assertEquals(17, dynamicModelHeader.getNumberOfVars());
    }



    @Test
    public void attributeValue_TimeID(){
        loadFileAndInitialize("data/dataWeka/laborTimeID.arff");

        if(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }

        //[?,1]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(40,(int)nextInstance.getValue(dynamicModelHeader.getVariableByName("WORKING-HOURS")));

        if(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }
        //[1,2]
        assertEquals(40,(int)nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("WORKING-HOURS")));
        assertEquals(35,(int)nextInstance.getValue(dynamicModelHeader.getVariableByName("WORKING-HOURS")));

        if(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }
        //[2,3]
        assertEquals(2,(int)nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("CONTRIBUTION-TO-HEALTH-PLAN")));
        assertEquals(1,(int)nextInstance.getValue(dynamicModelHeader.getVariableByName("CONTRIBUTION-TO-HEALTH-PLAN")));

        if(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }
        //[3,?]
        assertEquals(1,(int)nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("CONTRIBUTION-TO-HEALTH-PLAN")));
        assertEquals(Double.NaN,nextInstance.getValue(dynamicModelHeader.getVariableByName("CONTRIBUTION-TO-HEALTH-PLAN")), DELTA);

        if(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }
        //[?,?]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("CONTRIBUTION-TO-HEALTH-PLAN")), DELTA);
        assertEquals(Double.NaN,nextInstance.getValue(dynamicModelHeader.getVariableByName("CONTRIBUTION-TO-HEALTH-PLAN")), DELTA);

        if(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }
        //[?,5]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("CONTRIBUTION-TO-HEALTH-PLAN")), DELTA);
        assertEquals(4.5,nextInstance.getValue(dynamicModelHeader.getVariableByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);

        if(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }
        //[5,6]

    }

    @Test
    public void attributeValue_TimeID2(){
        loadFileAndInitialize("data/dataWeka/laborTimeID2.arff");

        if(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }

        //[?,1]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(40,(int)nextInstance.getValue(dynamicModelHeader.getVariableByName("WORKING-HOURS")));

        if(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }
        //[1,?]
        assertEquals(40,(int)nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("WORKING-HOURS")));
        assertEquals(Double.NaN,nextInstance.getValue(dynamicModelHeader.getVariableByName("WORKING-HOURS")), DELTA);

        if(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }

        //[?,?]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(Double.NaN,nextInstance.getValue(dynamicModelHeader.getVariableByName("WORKING-HOURS")), DELTA);

        if(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }

        //[?,?]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(Double.NaN,nextInstance.getValue(dynamicModelHeader.getVariableByName("WORKING-HOURS")), DELTA);

        if(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }

        //[?,5]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(35,nextInstance.getValue(dynamicModelHeader.getVariableByName("WORKING-HOURS")), DELTA);

        if(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }

        //[5,6]
        assertEquals(35,nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(38,nextInstance.getValue(dynamicModelHeader.getVariableByName("WORKING-HOURS")), DELTA);
    }



    @Test
    public void reachEOF_TimeID(){
        loadFileAndInitialize("data/dataWeka/laborTimeID.arff");

        while(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }

        /*Test values for the last instance*/
        assertEquals(88,(int)nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("WORKING-HOURS")));
        assertEquals(89,(int)nextInstance.getValue(dynamicModelHeader.getVariableByName("WORKING-HOURS")));
    }

    @Test
    public void checkAutomaticTimeID_TimeID(){
        loadFileAndInitialize("data/dataWeka/laborTimeID.arff");

        while(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }

        /*Test values for the last instance*/
        assertEquals(60,(int)nextInstance.getTimeID());
    }

    @Test
    public void checkAutomaticSeq_TimeID(){
        loadFileAndInitialize("data/dataWeka/laborTimeID.arff");

        while(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }

        /*Test values for the last instance*/
        assertEquals(1,(int)nextInstance.getSequenceID());
    }


    /**********************************************************
     *                        Seq
     **********************************************************/

    @Test
    public void nOfVars_seqID(){
        loadFileAndInitialize("data/dataWeka/laborSeqID.arff");

        assertEquals(18, attributes.getList().size());
        assertEquals(17, obsVars.size());
        assertEquals(17, temporalClones.size());
        assertEquals(17, dynamicModelHeader.getNumberOfVars());
    }

    @Test
    public void attributeValue_seqID() {
        loadFileAndInitialize("data/dataWeka/laborSeqID.arff");

        if (dataOnDisk.hasMoreDataInstances()) {
            nextInstance = dataOnDisk.nextDataInstance();
        }

        //Seq 1: Instances 1-4
        //[?,1]
        assertEquals(Double.NaN, nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(40, (int) nextInstance.getValue(dynamicModelHeader.getVariableByName("WORKING-HOURS")));

        if (dataOnDisk.hasMoreDataInstances()) {
            nextInstance = dataOnDisk.nextDataInstance();
        }

        //[1,2]
        assertEquals(40, nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(35, (int) nextInstance.getValue(dynamicModelHeader.getVariableByName("WORKING-HOURS")));

        if (dataOnDisk.hasMoreDataInstances()) {
            nextInstance = dataOnDisk.nextDataInstance();
        }

        //[2,3]
        assertEquals(4.5, nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);
        assertEquals(Double.NaN, nextInstance.getValue(dynamicModelHeader.getVariableByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);

        if (dataOnDisk.hasMoreDataInstances()) {
            nextInstance = dataOnDisk.nextDataInstance();
        }

        //[3,4]
        assertEquals(Double.NaN, nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);
        assertEquals(3.7, nextInstance.getValue(dynamicModelHeader.getVariableByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);

        if (dataOnDisk.hasMoreDataInstances()) {
            nextInstance = dataOnDisk.nextDataInstance();
        }

        //Seq 2: Instances 5-17
        //[5,6] (Every time we change sequence we add a missing row)
        assertEquals(Double.NaN, nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);
        assertEquals(4.5, nextInstance.getValue(dynamicModelHeader.getVariableByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);
    }

    @Test
    public void reachEOF_SeqID(){
        loadFileAndInitialize("data/dataWeka/laborSeqID.arff");

        while(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }

        /*Test values for the last instance*/
        assertEquals(88,(int)nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("WORKING-HOURS")));
        assertEquals(89,(int)nextInstance.getValue(dynamicModelHeader.getVariableByName("WORKING-HOURS")));
    }

    @Test
    public void checkAutomaticTimeID_SeqID(){
        loadFileAndInitialize("data/dataWeka/laborSeqID.arff");

        while(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }

        /*Test values for the last instance*/
        assertEquals(40,(int)nextInstance.getTimeID());
    }

    @Test
    public void checkAutomaticSeq_SeqID(){
        loadFileAndInitialize("data/dataWeka/laborSeqID.arff");

        while(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }

        /*Test values for the last instance*/
        assertEquals(3,(int)nextInstance.getSequenceID());
    }


    /**********************************************************
     *                    TimeID & Seq
     **********************************************************/

    @Test
    public void nOfVars_TimeID_SeqID(){
        loadFileAndInitialize("data/dataWeka/laborTimeIDSeqID.arff");

        assertEquals(19, attributes.getList().size());
        assertEquals(17, obsVars.size());
        assertEquals(17, temporalClones.size());
        assertEquals(17, dynamicModelHeader.getNumberOfVars());
    }


}
