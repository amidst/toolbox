package eu.amidst.standardmodels;

import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.utils.DataSetGenerator;
import junit.framework.TestCase;

/**
 * Created by ana@cs.aau.dk on 08/03/16.
 */
public class KalmanFilterTest extends TestCase{
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
        System.out.println("------------------KF (diagonal matrix) from streaming------------------");
        KalmanFilter KF = new KalmanFilter(dataGaussians.getAttributes());
        KF.setNumHidden(2);
        System.out.println(KF.getDynamicDAG());
        KF.learnModel(dataGaussians);
        System.out.println(KF.getModel());
    }
    public void test2(){
        System.out.println("------------------KF (full cov. matrix) from streaming------------------");
        KalmanFilter KF = new KalmanFilter(dataGaussians.getAttributes());
        KF.setDiagonal(false);
        System.out.println(KF.getDynamicDAG());
        KF.learnModel(dataGaussians);
        System.out.println(KF.getModel());
    }

    public void test3(){
        System.out.println("------------------KF (diagonal matrix) from batches------------------");
        KalmanFilter KF = new KalmanFilter(dataGaussians.getAttributes());
        System.out.println(KF.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : dataGaussians.iterableOverBatches(100)) {
            KF.updateModel(batch);
        }
        System.out.println(KF.getModel());
    }

}