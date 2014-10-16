package eu.amidst.core.database.statics.readers.Impl;

import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.statics.DataStream;
//import junit.framework.TestCase;
import eu.amidst.core.database.statics.readers.ArffParserException;
import junit.framework.Assert;
import org.junit.Test;

public class ArffDataStreamTest {

    public void testRestart() throws Exception {

    }

    public void testGetStaticDataHeader() throws Exception {

    }

    @Test
    public void testConstructor1() throws Exception {
        Attributes attributes = new DefaultTestAttributes();
        DataStream dataStream = new ArffDataStream("data/arff/hayTrain.arff", attributes );
    }

    @Test
    public void testConstructor2() throws Exception {
        Attributes attributes = new DefaultTestAttributes();
        DataStream dataStream = new ArffDataStream("data/arff/testCapitals.arff", attributes );
    }

    @Test
    public void testConstructor3() throws Exception {
        Attributes attributes = new DefaultTestAttributes();
        DataStream dataStream = new ArffDataStream("data/arff/testSpacesAndTabs.arff", attributes );
    }

    @Test(expected=ArffParserException.class)
    public void testConstructor4() throws Exception {
        Attributes attributes = new DefaultTestAttributes();
        DataStream dataStream = new ArffDataStream("data/arff/testWrongRelationName.arff", attributes );
    }

    @Test (expected=ArffParserException.class)
    public void testConstructor5() throws Exception {
        Attributes attributes = new DefaultTestAttributes();
        DataStream dataStream = new ArffDataStream("data/arff/testWrongAttributeName.arff", attributes );
    }


    @Test (expected=ArffParserException.class)
    public void testConstructor6() throws Exception {
        Attributes attributes = new DefaultTestAttributes();
        DataStream dataStream = new ArffDataStream("data/arff/testEmptySpace.arff", attributes );
    }

    @Test()
    public void testLineArrayToLineEmpty() {
        Attributes attributes = new DefaultTestAttributes();
        Assert.assertEquals( null, ArffDataStream.lineArrayToLine( new String[]{}) );
    }

    @Test()
    public void testLineArrayToLineOneEmpty() {
        Attributes attributes = new DefaultTestAttributes();
        Assert.assertEquals( "", ArffDataStream.lineArrayToLine( new String[]{""}) );
    }

    @Test()
    public void testLineArrayToLineOneString() {
        Attributes attributes = new DefaultTestAttributes();
        Assert.assertEquals( "abc", ArffDataStream.lineArrayToLine( new String[]{"abc"}) );
    }


    @Test()
    public void testLineArrayToLineTwoStrings() {
        Attributes attributes = new DefaultTestAttributes();
        Assert.assertEquals( "abc,def", ArffDataStream.lineArrayToLine( new String[]{"abc", "def"}) );
    }

    @Test()
    public void testLineArrayToLineThreeStringsMiddleIsEmpty() {
        Attributes attributes = new DefaultTestAttributes();
        Assert.assertEquals( "abc,,def", ArffDataStream.lineArrayToLine( new String[]{"abc", "", "def"}) );
    }

    @Test
    public void testConstructorRelationInQuotes1() throws Exception {
        Attributes attributes = new DefaultTestAttributes();
        DataStream dataStream = new ArffDataStream("data/arff/testRelationInQuotes1.arff", attributes );
    }

    @Test
    public void testConstructorRelationInQuotes2() throws Exception {
        Attributes attributes = new DefaultTestAttributes();
        DataStream dataStream = new ArffDataStream("data/arff/testRelationInQuotes2.arff", attributes );
    }

    @Test
    public void testConstructorRelationInQuotes3() throws Exception {
        Attributes attributes = new DefaultTestAttributes();
        DataStream dataStream = new ArffDataStream("data/arff/testRelationInQuotes3.arff", attributes );
    }

    @Test
    public void testConstructorRelationInQuotes4() throws Exception {
        Attributes attributes = new DefaultTestAttributes();
        DataStream dataStream = new ArffDataStream("data/arff/testRelationInQuotes4.arff", attributes );
    }


    @Test
    public void testConstructorAttributesInQuotes() throws Exception {
        Attributes attributes = new DefaultTestAttributes();
        DataStream dataStream = new ArffDataStream("data/arff/testAttributesInQuotes.arff", attributes );
    }

    //System.out.println(" dataStream has iterator: " );
}