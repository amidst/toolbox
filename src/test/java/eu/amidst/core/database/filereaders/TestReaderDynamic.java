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


    public static void loadFileAndInitialize_NoTimeID_NoSeq() {
        reader = new WekaDataFileReader("data/dataWeka/labor.arff");
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
        loadFileAndInitialize_NoTimeID_NoSeq();

        assertEquals(17, attributes.getList().size());
        assertEquals(17, obsVars.size());
        assertEquals(17, temporalClones.size());
        assertEquals(17, dynamicModelHeader.getNumberOfVars());
    }

    @Test
    public void numericAttributeValue_NoTimeID_NoSeq(){
        loadFileAndInitialize_NoTimeID_NoSeq();

        if(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }
        assertEquals(40,(int)nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("WORKING-HOURS")));
        assertEquals(35,(int)nextInstance.getValue(dynamicModelHeader.getVariableByName("WORKING-HOURS")));
    }

    @Test
    public void reachEOF_NoTimeID_NoSeq(){
        loadFileAndInitialize_NoTimeID_NoSeq();

        while(dataOnDisk.hasMoreDataInstances()){
            nextInstance = dataOnDisk.nextDataInstance();
        }

        /*Test values for the last instance*/
        assertEquals(88,(int)nextInstance.getValue(dynamicModelHeader.getTemporalCloneByName("WORKING-HOURS")));
        assertEquals(89,(int)nextInstance.getValue(dynamicModelHeader.getVariableByName("WORKING-HOURS")));
    }

    /**********************************************************
     *                       TimeID
     **********************************************************/


    /**********************************************************
     *                        Seq
     **********************************************************/


    /**********************************************************
     *                    TimeID & Seq
     **********************************************************/


}
