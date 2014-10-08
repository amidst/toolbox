package eu.amidst.core.database.statics.readers.Impl;

import eu.amidst.core.database.statics.DataStream;
//import junit.framework.TestCase;
import eu.amidst.core.database.statics.readers.ArffParserException;
import junit.framework.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class ArffDataStreamTest {

    public void testRestart() throws Exception {

    }

    public void testGetStaticDataHeader() throws Exception {

    }

    @Test
    public void testConstructor1() throws Exception {
        DataStream dataStream = new ArffDataStream("data/arff/hayTrain.arff");
    }

    @Test
    public void testConstructor2() throws Exception {
        DataStream dataStream = new ArffDataStream("data/arff/testCapitals.arff");
    }

    @Test
    public void testConstructor3() throws Exception {

        DataStream dataStream = new ArffDataStream("data/arff/testSpacesAndTabs.arff");
    }

    @Test(expected=ArffParserException.class)
    public void testConstructor4() throws Exception {

        DataStream dataStream = new ArffDataStream("data/arff/testWrongRelationName.arff");
    }

    @Test(expected=ArffParserException.class)
    public void testConstructor5() throws Exception {

        DataStream dataStream = new ArffDataStream("data/arff/testWrongAttributeName.arff");
    }


    @Test(expected=ArffParserException.class)
    public void testConstructor6() throws Exception {

        DataStream dataStream = new ArffDataStream("data/arff/testEmptySpace.arff");
    }

    @Test()
    public void testLineArrayToLineEmpty() {
        Assert.assertEquals( null, ArffDataStream.lineArrayToLine( new String[]{}) );
    }

    @Test()
    public void testLineArrayToLineOneEmpty() {
        Assert.assertEquals( "", ArffDataStream.lineArrayToLine( new String[]{""}) );
    }

    @Test()
    public void testLineArrayToLineOneString() {
        Assert.assertEquals( "abc", ArffDataStream.lineArrayToLine( new String[]{"abc"}) );
    }


    @Test()
    public void testLineArrayToLineTwoStrings() {
        Assert.assertEquals( "abc,def", ArffDataStream.lineArrayToLine( new String[]{"abc", "def"}) );
    }

    @Test()
    public void testLineArrayToLineThreeStringsMiddleIsEmpty() {
        Assert.assertEquals( "abc,,def", ArffDataStream.lineArrayToLine( new String[]{"abc", "", "def"}) );
    }

    @Test
    public void testConstructorRelationInQuotes() throws Exception {
        DataStream dataStream = new ArffDataStream("data/arff/testRelationInQuotes.arff");
    }


    //System.out.println(" dataStream has iterator: " );
}