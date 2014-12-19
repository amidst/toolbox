/*

package eu.amidst.core.database.statics.readers.impl;

import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.statics.readers.DataInstance;
import eu.amidst.core.database.DataStream;
//import junit.framework.TestCase;
import eu.amidst.core.database.filereaders.arffFileReader.ArffParserException;
import junit.framework.Assert;
import org.junit.Test;

public class ArffDataStreamTest {

    public void testRestart() throws Exception {

    }

    public void testGetStaticDataHeader() throws Exception {

    }

    @Test
    public void testConstructor1() throws Exception {
        Attributes attributes = new ForTesting1Attributes();
        DataStream dataStream = new ArffDataStream("data/arff/hayTrain.arff", attributes );
    }

    @Test
    public void testConstructor2() throws Exception {
        Attributes attributes = new ForTesting1Attributes();
        DataStream dataStream = new ArffDataStream("data/arff/testCapitals.arff", attributes );
    }

    @Test
    public void testConstructor3() throws Exception {
        Attributes attributes = new ForTesting1Attributes();
        DataStream dataStream = new ArffDataStream("data/arff/testSpacesAndTabs.arff", attributes );
    }

    @Test(expected=ArffParserException.class)
    public void testConstructor4() throws Exception {
        Attributes attributes = new ForTesting1Attributes();
        DataStream dataStream = new ArffDataStream("data/arff/testWrongRelationName.arff", attributes );
    }

    @Test (expected=ArffParserException.class)
    public void testConstructor5() throws Exception {
        Attributes attributes = new ForTesting1Attributes();
        DataStream dataStream = new ArffDataStream("data/arff/testWrongAttributeName.arff", attributes );
    }


    @Test (expected=ArffParserException.class)
    public void testConstructor6() throws Exception {
        Attributes attributes = new ForTesting1Attributes();
        DataStream dataStream = new ArffDataStream("data/arff/testEmptySpace.arff", attributes );
    }

    @Test()
    public void testLineArrayToLineEmpty() {
        Attributes attributes = new ForTesting1Attributes();
        Assert.assertEquals( null, ArffDataStream.lineArrayToLine( new String[]{}) );
    }

    @Test()
    public void testLineArrayToLineOneEmpty() {
        Attributes attributes = new ForTesting1Attributes();
        Assert.assertEquals( "", ArffDataStream.lineArrayToLine( new String[]{""}) );
    }

    @Test()
    public void testLineArrayToLineOneString() {
        Attributes attributes = new ForTesting1Attributes();
        Assert.assertEquals( "abc", ArffDataStream.lineArrayToLine( new String[]{"abc"}) );
    }


    @Test()
    public void testLineArrayToLineTwoStrings() {
        Attributes attributes = new ForTesting1Attributes();
        Assert.assertEquals( "abc,def", ArffDataStream.lineArrayToLine( new String[]{"abc", "def"}) );
    }

    @Test()
    public void testLineArrayToLineThreeStringsMiddleIsEmpty() {
        Attributes attributes = new ForTesting1Attributes();
        Assert.assertEquals( "abc,,def", ArffDataStream.lineArrayToLine( new String[]{"abc", "", "def"}) );
    }

    @Test
    public void testConstructorRelationInQuotes1() throws Exception {
        Attributes attributes = new ForTesting1Attributes();
        DataStream dataStream = new ArffDataStream("data/arff/testRelationInQuotes1.arff", attributes );
    }

    @Test
    public void testConstructorRelationInQuotes2() throws Exception {
        Attributes attributes = new ForTesting1Attributes();
        DataStream dataStream = new ArffDataStream("data/arff/testRelationInQuotes2.arff", attributes );
    }

    @Test
    public void testConstructorRelationInQuotes3() throws Exception {
        Attributes attributes = new ForTesting1Attributes();
        DataStream dataStream = new ArffDataStream("data/arff/testRelationInQuotes3.arff", attributes );
    }

    @Test
    public void testConstructorRelationInQuotes4() throws Exception {
        Attributes attributes = new ForTesting1Attributes();
        DataStream dataStream = new ArffDataStream("data/arff/testRelationInQuotes4.arff", attributes );
    }


    @Test (expected=ArffParserException.class)
    public void testConstructorAttributesInQuotes() throws Exception {
        Attributes attributes = new ForTesting1Attributes();
        DataStream dataStream = new ArffDataStream("data/arff/testAttributesInQuotes.arff", attributes );
    }

    @Test (expected=ArffParserException.class)
    public void testConstructorNoData() throws Exception {
        Attributes attributes = new ForTesting1Attributes();
        DataStream dataStream = new ArffDataStream("data/arff/testNoData.arff", attributes );
    }

    @Test
    public void testConstructorSubsetOfVariables() throws Exception {
        Attributes attributes = new ForTesting2Attributes();
        DataStream dataStream = new ArffDataStream("data/arff/testSubsetOfVariables.arff", attributes );
    }

    @Test
    public void testConstructorCapitalizingNames() throws Exception {
        Attributes attributes = new ForTesting2Attributes();
        DataStream dataStream = new ArffDataStream("data/arff/testCapitalizingNames.arff", attributes );
    }


    @Test  (expected=ArffParserException.class)
    public void testConstructorWrongAttributesClass() throws Exception {
        Attributes attributes = new ForTesting3Attributes();
        DataStream dataStream = new ArffDataStream("data/arff/testSubsetOfVariables.arff", attributes );
    }


    @Test (expected=ArffParserException.class)
    public void testConstructorWrongAttributesClass2()  throws Exception {
        Attributes attributes = new ForTesting4Attributes();
        DataStream dataStream = new ArffDataStream("data/arff/testSubsetOfVariables.arff", attributes );
    }


    @Test
    public void testGetDataInstance1() throws Exception {
        ForTesting1Attributes attributes = new ForTesting1Attributes();
        DataStream dataStream = new ArffDataStream("data/arff/hayTrain.arff", attributes );
        DataInstance dataInstance = dataStream.nextDataInstance();
        int tmp = dataInstance.getInteger(attributes.getCLASS());
        org.junit.Assert.assertEquals(1,tmp);

        dataInstance = dataStream.nextDataInstance();
        dataInstance = dataStream.nextDataInstance();
        double f1_2 = dataInstance.getReal(attributes.getF1());

        org.junit.Assert.assertTrue(153.9999 < f1_2 && f1_2 < 154.00001);

        while(dataStream.hasMoreDataInstances()){
            dataInstance = dataStream.nextDataInstance();
            tmp = dataInstance.getInteger(attributes.getCLASS());
          //  System.out.println("Class label;    "   + tmp );
        }

        dataStream.restart();
        dataInstance = dataStream.nextDataInstance();
        double f4_0 = dataInstance.getReal(attributes.getF4());
        org.junit.Assert.assertTrue(119.9999 < f4_0 && f4_0 < 120.00001);

        double f9 = 0;
        dataStream.restart();
        while(dataStream.hasMoreDataInstances()){
            dataInstance = dataStream.nextDataInstance();
            f9 = dataInstance.getReal(attributes.getF9());
            //System.out.println("f9;    "   + f9 );
        }
        org.junit.Assert.assertTrue(1.099999 < f9 && f9 < 1.1000001);


    }


    @Test
    public void testGetDataInstanceSubset() throws Exception {
        ForTesting2Attributes attributes = new ForTesting2Attributes();
        DataStream dataStream = new ArffDataStream("data/arff/testSubsetOfVariables.arff", attributes );
        DataInstance dataInstance = dataStream.nextDataInstance();
        int tmp = dataInstance.getInteger(attributes.getCLASS());
        org.junit.Assert.assertEquals(1,tmp);

        dataInstance = dataStream.nextDataInstance();
        dataInstance = dataStream.nextDataInstance();
        double two_names_2 = dataInstance.getReal(attributes.getTWO_NAMES());

        org.junit.Assert.assertTrue(7.9999 < two_names_2 && two_names_2 < 8.00001);

        while(dataStream.hasMoreDataInstances()){
            dataInstance = dataStream.nextDataInstance();
            tmp = dataInstance.getInteger(attributes.getCLASS());
            //  System.out.println("Class label;    "   + tmp );
        }

        dataStream.restart();
        dataInstance = dataStream.nextDataInstance();
        double three_names_here_0 = dataInstance.getReal(attributes.getTHREE_NAMES_HERE());
        org.junit.Assert.assertTrue(29.7999 < three_names_here_0 && three_names_here_0 < 29.80001);

        double three_names_here = 0;
        dataStream.restart();
        while(dataStream.hasMoreDataInstances()){
            dataInstance = dataStream.nextDataInstance();
            three_names_here = dataInstance.getReal(attributes.getTHREE_NAMES_HERE());
            //System.out.println("f9;    "   + f9 );
        }
        org.junit.Assert.assertTrue(0.399999 < three_names_here && three_names_here < 0.4000001);

    }



    //System.out.println(" dataStream has iterator: " );
}
*/
