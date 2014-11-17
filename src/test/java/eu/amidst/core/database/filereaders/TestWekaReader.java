package eu.amidst.core.database.filereaders;


import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by ana@cs.aau.dk on 17/11/14.
 */
public class TestWekaReader {

    @Test
    public void loadArffWekaFile() {
        WekaDataFileReader reader = new WekaDataFileReader("dataWeka/breast-cancer.arff");

        Attributes attributes = reader.getAttributes();

        //assertEquals("onetwo", result);

    }
}
