package eu.amidst.standardmodels;

import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.utils.DataSetGenerator;
import junit.framework.TestCase;

/**
 * Created by ana@cs.aau.dk on 09/03/16.
 */
public class AutoRegressiveHMMTest  extends TestCase {
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
        System.out.println("------------------Auto-Regressive HMM (diagonal matrix) from streaming------------------");
        AutoRegressiveHMM autoRegressiveHMM = new AutoRegressiveHMM(dataHybrid.getAttributes());
        System.out.println(autoRegressiveHMM.getDynamicDAG());
        autoRegressiveHMM.learnModel(dataHybrid);
        System.out.println(autoRegressiveHMM.getModel());
    }
    public void test2(){
        System.out.println("------------------Auto-Regressive HMM (full cov. matrix) from streaming------------------");
        AutoRegressiveHMM autoRegressiveHMM = new AutoRegressiveHMM(dataGaussians.getAttributes());
        autoRegressiveHMM.setDiagonal(false);
        System.out.println(autoRegressiveHMM.getDynamicDAG());
        autoRegressiveHMM.learnModel(dataGaussians);
        System.out.println(autoRegressiveHMM.getModel());
    }

    public void test3(){
        System.out.println("------------------Auto-Regressive HMM (diagonal matrix) from batches------------------");
        AutoRegressiveHMM autoRegressiveHMM = new AutoRegressiveHMM(dataHybrid.getAttributes());
        System.out.println(autoRegressiveHMM.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : dataHybrid.iterableOverBatches(100)) {
            autoRegressiveHMM.updateModel(batch);
        }
        System.out.println(autoRegressiveHMM.getModel());
    }
}