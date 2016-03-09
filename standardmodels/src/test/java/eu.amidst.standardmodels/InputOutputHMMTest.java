package eu.amidst.standardmodels;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.utils.DataSetGenerator;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ana@cs.aau.dk on 09/03/16.
 */
public class InputOutputHMMTest extends TestCase {
    private static DataStream<DynamicDataInstance> data;
    private static Attributes dataAttributes;
    private static boolean setUpIsDone = false;
    private static List<Attribute> inputAtts;
    private static List<Attribute> outputAtts;

    protected void setUp(){
        if (setUpIsDone) {
            return;
        }
        data = DataSetGenerator.generate(1,1000,3,3);
        dataAttributes = data.getAttributes();
        inputAtts = new ArrayList<>();
        inputAtts.add(dataAttributes.getAttributeByName("DiscreteVar0"));
        inputAtts.add(dataAttributes.getAttributeByName("DiscreteVar1"));
        inputAtts.add(dataAttributes.getAttributeByName("DiscreteVar2"));
        outputAtts = new ArrayList<>();
        outputAtts.add(dataAttributes.getAttributeByName("GaussianVar0"));
        outputAtts.add(dataAttributes.getAttributeByName("GaussianVar1"));
        outputAtts.add(dataAttributes.getAttributeByName("GaussianVar2"));
        setUpIsDone = true;
    }

    public void test1(){
        System.out.println("------------------Input-Output HMM (diagonal matrix) from streaming------------------");
        InputOutputHMM IOHMM = new InputOutputHMM(dataAttributes,inputAtts,outputAtts);
        IOHMM.setNumStates(2);
        System.out.println(IOHMM.getDynamicDAG());
        IOHMM.learnModel(data);
        System.out.println(IOHMM.getModel());

    }
    public void test2(){
        System.out.println("------------------Input-Output HMM (full cov. matrix) from streaming------------------");
        InputOutputHMM IOHMM = new InputOutputHMM(dataAttributes,inputAtts,outputAtts);
        IOHMM.setDiagonal(false);
        System.out.println(IOHMM.getDynamicDAG());
        IOHMM.learnModel(data);
        System.out.println(IOHMM.getModel());
    }

    public void test3(){
        System.out.println("------------------Input-Output HMM (diagonal matrix) from batches------------------");
        InputOutputHMM IOHMM = new InputOutputHMM(dataAttributes,inputAtts,outputAtts);
        System.out.println(IOHMM.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : data.iterableOverBatches(100)) {
            IOHMM.updateModel(batch);
        }
    }


}