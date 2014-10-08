package eu.amidst.core.database.statics.readers.Impl;

import eu.amidst.core.database.statics.DataStream;
import junit.framework.TestCase;

public class ArffDataStreamTest extends TestCase {

    public void testRestart() throws Exception {

    }

    public void testGetStaticDataHeader() throws Exception {

    }

    public void testConstructor1() throws Exception {
        DataStream dataStream = new ArffDataStream("data/arff/hayTrain.arff");
    }


    public void testConstructor2() throws Exception {
        DataStream dataStream = new ArffDataStream("data/arff/testCapitals.arff");
    }

    public void testConstructor3() throws Exception {

        DataStream dataStream = new ArffDataStream("data/arff/testSpacesAndTabs.arff");
    }


    public void testConstructor4() throws Exception {

        DataStream dataStream = new ArffDataStream("data/arff/testWrongRelationName.arff");
    }

    public void testConstructor5() throws Exception {

        DataStream dataStream = new ArffDataStream("data/arff/testWrongAttributeName.arff");
    }



    //System.out.println(" dataStream has iterator: " );
}