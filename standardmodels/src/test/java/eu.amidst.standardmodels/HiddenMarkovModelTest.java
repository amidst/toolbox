package eu.amidst.standardmodels;

import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.utils.DataSetGenerator;
import junit.framework.TestCase;

/**
 * Created by ana@cs.aau.dk on 08/03/16.
 */
public class HiddenMarkovModelTest extends TestCase{

    private static DataStream<DynamicDataInstance> dataHybrid;
    private static DataStream<DynamicDataInstance> dataGaussians;
    private static boolean setUpIsDone = false;

    protected void setUp(){
        if (setUpIsDone) {
            return;
        }
        dataHybrid = DataSetGenerator.generate(1,1000,3,5);
        dataGaussians = DataSetGenerator.generate(1,1000,0,5);
        setUpIsDone = true;
    }

    public void test1(){
        System.out.println("------------------HMM (diagonal matrix) from streaming------------------");
        HiddenMarkovModel HMM = new HiddenMarkovModel(dataHybrid.getAttributes());
        System.out.println(HMM.getDynamicDAG());
        HMM.learnModel(dataHybrid);
        System.out.println(HMM.getModel());
    }
    public void test2(){
        System.out.println("------------------HMM (full cov. matrix) from streaming------------------");
        HiddenMarkovModel HMM = new HiddenMarkovModel(dataGaussians.getAttributes());
        HMM.setDiagonal(false);
        System.out.println(HMM.getDynamicDAG());
        HMM.learnModel(dataGaussians);
        System.out.println(HMM.getModel());
    }

    public void test3(){
        System.out.println("------------------HMM (diagonal matrix) from batches------------------");
        HiddenMarkovModel HMM = new HiddenMarkovModel(dataHybrid.getAttributes());
        System.out.println(HMM.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : dataHybrid.iterableOverBatches(100)) {
            HMM.updateModel(batch);
        }
        System.out.println(HMM.getModel());
    }

}
