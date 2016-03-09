package eu.amidst.standardmodels;

import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.utils.DataSetGenerator;
import junit.framework.TestCase;

/**
 * Created by ana@cs.aau.dk on 09/03/16.
 */
public class FactorialHMMTest extends TestCase{
    private static DataStream<DynamicDataInstance> dataHybrid;
    private static DataStream<DynamicDataInstance> dataGaussians;
    private static boolean setUpIsDone = false;

    protected void setUp(){
        if (setUpIsDone) {
            return;
        }
        dataHybrid = DataSetGenerator.generate(1,1000,3,10);
        dataGaussians = DataSetGenerator.generate(1,1000,0,10);
        setUpIsDone = true;
    }

    public void test1(){
        System.out.println("------------------Factorial HMM (diagonal matrix) from streaming------------------");
        FactorialHMM factorialHMM = new FactorialHMM(dataHybrid.getAttributes());
        System.out.println(factorialHMM.getDynamicDAG());
        factorialHMM.learnModel(dataHybrid);
        System.out.println(factorialHMM.getModel());
    }
    public void test2(){
        System.out.println("------------------Factorial HMM (full cov. matrix) from streaming------------------");
        FactorialHMM factorialHMM = new FactorialHMM(dataGaussians.getAttributes());
        factorialHMM.setDiagonal(false);
        System.out.println(factorialHMM.getDynamicDAG());
        factorialHMM.learnModel(dataGaussians);
        System.out.println(factorialHMM.getModel());
    }

    public void test3(){
        System.out.println("------------------Factorial HMM (diagonal matrix) from batches------------------");
        FactorialHMM factorialHMM = new FactorialHMM(dataHybrid.getAttributes());
        System.out.println(factorialHMM.getDynamicDAG());
        for (DataOnMemory<DynamicDataInstance> batch : dataHybrid.iterableOverBatches(100)) {
            factorialHMM.updateModel(batch);
        }
        System.out.println(factorialHMM.getModel());
    }
}