/**
 ******************* ISSUE LIST **************************
 *
 * 1. Do we need a getVariableByName(String s) in StaticModelHeader?

 *
 * ********************************************************
 */

package eu.amidst.core.database.filereaders;


import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;

import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by ana@cs.aau.dk on 17/11/14.
 */
public class TestReaderStatic {

    private static final double DELTA = 1e-15;
    private static WekaDataFileReader reader;
    private static Attributes attributes;
    private static StaticVariables staticVariables;
    private static DataRow datarow = null;
    private static DataInstance nextInstance = null;
    private static int index;
    private static Variable var;
    private static DataOnDisk dataOnDiskReader;

    public static void loadFileAndInitializeStatic(){
        reader = new WekaDataFileReader("data/dataWeka/labor.arff");
        dataOnDiskReader = new StaticDataOnDiskFromFile(reader);
        attributes = dataOnDiskReader.getAttributes();
        staticVariables = new StaticVariables(attributes);
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

        if (reader.hasNext()) {
            datarow = reader.next();
            nextInstance = new StaticDataInstance(datarow);
        }

        for (Attribute att : attributes.getList()) {
            System.out.println(att.getName() + ", " + att.getIndex());
        }

        /* Numeric attribute */
        assertEquals(5, (int) datarow.getValue(attributes.getAttributeByName("WAGE-INCREASE-FIRST-YEAR")));
        index = attributes.getAttributeByName("WAGE-INCREASE-FIRST-YEAR").getIndex();
        var = staticVariables.getVariableById(index);
        System.out.println(var.getName());
        assertEquals(5, (int) nextInstance.getValue(var));
    }

    @Test
    public void numericAttributeValue_DataOnDisk() {

        loadFileAndInitializeStatic();

        if (dataOnDiskReader.hasNext()) {
            nextInstance = dataOnDiskReader.next();
        }

        for (Attribute att : attributes.getList()) {
            System.out.println(att.getName() + ", " + att.getIndex());
        }

        /* Numeric attribute */
        assertEquals(5, (int) nextInstance.getValue(staticVariables.getVariableByName("WAGE-INCREASE-FIRST-YEAR")));
    }

    @Test
    public void discreteAttributeValue() {
        loadFileAndInitializeStatic();
        if (reader.hasNext()) {
            datarow = reader.next();
            nextInstance = new StaticDataInstance(datarow);
        }

        /* Discrete attribute */
        assertEquals(1, (int) datarow.getValue(attributes.getAttributeByName("VACATION")));
        /* Number of states */
        assertEquals(3, staticVariables.getVariableByName("PENSION").getNumberOfStates());
    }

    @Test
    public void missingValues() {

        loadFileAndInitializeStatic();

        /* Missing values (Get the 3rd instance) */
        if (reader.hasNext()) {
            reader.next();
            reader.next();
            datarow = reader.next();
            nextInstance = new StaticDataInstance(datarow);
        }
        index = attributes.getAttributeByName("WAGE-INCREASE-FIRST-YEAR").getIndex();
        var = staticVariables.getVariableByName("WAGE-INCREASE-FIRST-YEAR");
        assertEquals(Double.NaN, nextInstance.getValue(var), DELTA);
    }

    @Test
    public void numberOfInstances_DataOnDisk() {

        loadFileAndInitializeStatic();

        /* Number of instances */
        int instanceCounter = 0;
        for (DataInstance dataInstance: dataOnDiskReader){
            instanceCounter++;
        }
        assertEquals(57, instanceCounter);
    }

    @Test
    public void numberOfInstances() {

        loadFileAndInitializeStatic();

        /* Number of instances */
        int instanceCounter = 0;
        for (DataRow row: reader){
            instanceCounter++;
        }
        assertEquals(57, instanceCounter);
    }


    @Test
    public void hasMoreDataRowsNotUsed() {

        loadFileAndInitializeStatic();

        int instanceCounter = 57;
        /* nexDataRow without calling hasNext */
        while(instanceCounter>=0){
            instanceCounter--;
            datarow = reader.next();
        }
        //Actually, datarow is not null, but the weka Instance inside datarow. I am not sure what should be the
        //expected behavour here.
        //assertNull(datarow);
    }

    @Test
    public void hasMoreDataRowsNotUsed_DataOnDisk() {

        loadFileAndInitializeStatic();

        int instanceCounter = 57;
        /* nexDataRow without calling hasNext */
        while(instanceCounter>=0){
            instanceCounter--;
            nextInstance = dataOnDiskReader.next();
        }
        //I am not sure what should be the expected behavour here.
        //assertNull(nextInstance);
    }

}
