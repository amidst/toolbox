/**
 ******************* ISSUE LIST **************************
 *
 * 1. Do we need a getVariable(String s) in StaticModelHeader?

 *
 * ********************************************************
 */

package eu.amidst.core.database.filereaders;


import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;

import eu.amidst.core.header.StaticModelHeader;
import eu.amidst.core.header.Variable;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by ana@cs.aau.dk on 17/11/14.
 */
public class TestReaderStatic {

    private static final double DELTA = 1e-15;
    private static WekaDataFileReader reader;
    private static Attributes attributes;
    private static StaticModelHeader modelHeader;
    private static DataRow datarow = null;
    private static DataInstance nextInstance = null;
    private static int index;
    private static Variable var;
    private static DataOnDisk dataOnDiskReader;

    public static void loadFileAndInitializeStatic(){
        reader = new WekaDataFileReader("data/dataWeka/labor.arff");
        dataOnDiskReader = new StaticDataOnDiskFromFile(reader);
        attributes = dataOnDiskReader.getAttributes();
        modelHeader = new StaticModelHeader(attributes);
    }

    @Test
    public void loadArffWekaFileStatic() {
        reader = new WekaDataFileReader("data/dataWeka/labor.arff");

        attributes = reader.getAttributes();

        assertEquals(17, attributes.getList().size());
    }

    @Test
    public void numericAttributeValue() {

        loadFileAndInitializeStatic();

        if (reader.hasMoreDataRows()) {
            datarow = reader.nextDataRow();
            nextInstance = new StaticDataInstance(datarow);
        }

        for (Attribute att : attributes.getList()) {
            System.out.println(att.getName() + ", " + att.getIndex());
        }

        /* Numeric attribute */
        assertEquals(5, (int) datarow.getValue(attributes.getAttributeByName("WAGE-INCREASE-FIRST-YEAR")));
        index = attributes.getAttributeByName("WAGE-INCREASE-FIRST-YEAR").getIndex();
        var = modelHeader.getVariable(index);
        System.out.println(var.getName());
        assertEquals(5, (int) nextInstance.getValue(var));
    }

    @Test
    public void numericAttributeValue_DataOnDisk() {

        loadFileAndInitializeStatic();

        if (dataOnDiskReader.hasMoreDataInstances()) {
            nextInstance = dataOnDiskReader.nextDataInstance();
        }

        for (Attribute att : attributes.getList()) {
            System.out.println(att.getName() + ", " + att.getIndex());
        }

        /* Numeric attribute */
        assertEquals(5, (int) nextInstance.getValue(modelHeader.getVariable("WAGE-INCREASE-FIRST-YEAR")));
    }

    @Test
    public void discreteAttributeValue() {
        loadFileAndInitializeStatic();
        if (reader.hasMoreDataRows()) {
            datarow = reader.nextDataRow();
            nextInstance = new StaticDataInstance(datarow);
        }

        /* Discrete attribute */
        assertEquals(1, (int) datarow.getValue(attributes.getAttributeByName("VACATION")));
        /* Number of states */
        assertEquals(3, modelHeader.getVariable("PENSION").getNumberOfStates());
    }

    @Test
    public void missingValues() {

        loadFileAndInitializeStatic();

        /* Missing values (Get the 3rd instance) */
        if (reader.hasMoreDataRows()) {
            reader.nextDataRow();
            reader.nextDataRow();
            datarow = reader.nextDataRow();
            nextInstance = new StaticDataInstance(datarow);
        }
        index = attributes.getAttributeByName("WAGE-INCREASE-FIRST-YEAR").getIndex();
        var = modelHeader.getVariable("WAGE-INCREASE-FIRST-YEAR");
        assertEquals(Double.NaN, nextInstance.getValue(var), DELTA);
    }

    @Test
    public void numberOfInstances_DataOnDisk() {

        loadFileAndInitializeStatic();

        /* Number of instances */
        int instanceCounter = 0;
        while (dataOnDiskReader.hasMoreDataInstances()) {
            instanceCounter++;
            dataOnDiskReader.nextDataInstance();
        }
        assertEquals(57, instanceCounter);
    }

    @Test
    public void numberOfInstances() {

        loadFileAndInitializeStatic();

        /* Number of instances */
        int instanceCounter = 0;
        while (reader.hasMoreDataRows()) {
            instanceCounter++;
            reader.nextDataRow();
        }
        assertEquals(57, instanceCounter);
    }


    @Test
    public void hasMoreDataRowsNotUsed() {

        loadFileAndInitializeStatic();

        int instanceCounter = 57;
        /* nexDataRow without calling hasMoreDataRows */
        while(instanceCounter>=0){
            instanceCounter--;
            datarow = reader.nextDataRow();
        }
        //Actually, datarow is not null, but the weka Instance inside datarow. I am not sure what should be the
        //expected behavour here.
        //assertNull(datarow);
    }

    @Test
    public void hasMoreDataRowsNotUsed_DataOnDisk() {

        loadFileAndInitializeStatic();

        int instanceCounter = 57;
        /* nexDataRow without calling hasMoreDataRows */
        while(instanceCounter>=0){
            instanceCounter--;
            nextInstance = dataOnDiskReader.nextDataInstance();
        }
        //I am not sure what should be the expected behavour here.
        //assertNull(nextInstance);
    }

}
