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
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;

import eu.amidst.core.header.StaticModelHeader;
import eu.amidst.core.header.Variable;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by ana@cs.aau.dk on 17/11/14.
 */
public class TestWekaReader {

    private static final double DELTA = 1e-15;

    @Test
    public void loadArffWekaFileStatic() {
        WekaDataFileReader reader = new WekaDataFileReader("data/dataWeka/labor.arff");

        Attributes attributes = reader.getAttributes();

        assertEquals(17,attributes.getSet().size());

        DataRow datarow = null;
        StaticDataInstance nextInstance = null;
        if(reader.hasMoreDataRows()) {
            datarow = reader.nextDataRow();
            nextInstance = new StaticDataInstance(datarow);
        }

        for(Attribute att: attributes.getSet()){
            System.out.println(att.getName());
        }

        /* Numeric attribute */
        assertEquals(5,(int)datarow.getValue(attributes.getAttributeByName("WAGE-INCREASE-FIRST-YEAR")));
        int index = attributes.getAttributeByName("WAGE-INCREASE-FIRST-YEAR").getIndex();
        StaticModelHeader modelHeader = new StaticModelHeader(attributes);
        //Variable var = modelHeader.getVariable(index);
        //assertEquals(5, (int)nextInstance.getValue(var));

        /* Discrete attribute */
        assertEquals(1,(int)datarow.getValue(attributes.getAttributeByName("VACATION")));

        /* Missing values */
        /* Get the 3rd instance */
        if(reader.hasMoreDataRows()) {
            datarow = reader.nextDataRow();
            datarow = reader.nextDataRow();
            nextInstance = new StaticDataInstance(datarow);
        }
        index = attributes.getAttributeByName("WAGE-INCREASE-FIRST-YEAR").getIndex();
        //var = modelHeader.getVariable(index);
        //assertEquals(Double.NaN, nextInstance.getValue(var), DELTA);

        /* Last instance in file*/

    }

    @Test
    public void loadArffWekaFileDynamic() {
        assertEquals(1,1);
    }

}
