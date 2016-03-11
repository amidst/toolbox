package eu.amidst.standardmodels;

import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.utils.DataSetGenerator;
import junit.framework.TestCase;

/**
 * Created by ana@cs.aau.dk on 09/03/16.
 */
public class SwitchingKalmanFilterTest extends TestCase{
    private static DataStream<DynamicDataInstance> dataGaussians;
    private static boolean setUpIsDone = false;

    protected void setUp(){
        if (setUpIsDone) {
            return;
        }
        dataGaussians = DataSetGenerator.generate(1,1000,0,10);
        setUpIsDone = true;
    }

    public void test1(){
        System.out.println("------------------SKF (diagonal matrix) from streaming------------------");
        SwitchingKalmanFilter SKF = new SwitchingKalmanFilter(dataGaussians.getAttributes());
        System.out.println(SKF.getDynamicDAG());
        SKF.learnModel(dataGaussians);
        System.out.println(SKF.getModel());

    }
    public void test2(){
        System.out.println("------------------SKF (full cov. matrix) from streaming------------------");
        SwitchingKalmanFilter SKF = new SwitchingKalmanFilter(dataGaussians.getAttributes());
        SKF.setDiagonal(false);
        System.out.println(SKF.getDynamicDAG());
        SKF.learnModel(dataGaussians);
        System.out.println(SKF.getModel());
    }

    public void test3(){
        System.out.println("------------------SKF (diagonal matrix) from batches------------------");
        SwitchingKalmanFilter SKF = new SwitchingKalmanFilter(dataGaussians.getAttributes());
        System.out.println(SKF.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : dataGaussians.iterableOverBatches(100)) {
            SKF.updateModel(batch);
        }
        System.out.println(SKF.getModel());
    }
}