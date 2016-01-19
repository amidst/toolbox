package eu.amidst.flinklink.examples;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.distribution.Normal_MultinomialNormalParents;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.flinklink.core.conceptdrift.IDAConceptDriftDetector;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import eu.amidst.flinklink.core.utils.BayesianNetworkSampler;
import eu.amidst.flinklink.core.utils.DBNSampler;
import org.apache.flink.api.java.ExecutionEnvironment;

import java.util.List;
import java.util.Random;

/**
 * Created by ana@cs.aau.dk on 18/01/16.
 */
public class ConceptDriftDetector {

    public static int NSETS = 15;
    public static int SAMPLESIZE = 1000;
    public static int BATCHSIZE = 500;
    public static int numVars = 10;
    public static boolean connectDBN = true;
    public static boolean dbn = true;


    public static BayesianNetwork createBN(int nVars) throws Exception {

        Variables dynamicVariables = new Variables();
        Variable classVar = dynamicVariables.newMultionomialVariable("C", 2);

        for (int i = 0; i < nVars; i++) {
            dynamicVariables.newGaussianVariable("A" + i);
        }
        DAG dag = new DAG(dynamicVariables);

        for (int i = 0; i < nVars; i++) {
            dag.getParentSet(dynamicVariables.getVariableByName("A" + i)).addParent(classVar);
        }

        dag.setName("dbn1");
        BayesianNetwork bn = new BayesianNetwork(dag);
        bn.randomInitialization(new Random(1));

        return bn;
    }

    public static void createDataSets(List<String> hiddenVars, List<String> noisyVars) throws Exception {

        BayesianNetwork bn = createBN(numVars);

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setBatchSize(BATCHSIZE);
        sampler.setSeed(1);

        if (hiddenVars!=null) {
            for (String hiddenVar : hiddenVars) {
                sampler.setHiddenVar(bn.getVariables().getVariableByName(hiddenVar));
            }
        }
        if (noisyVars!=null){
            for (String noisyVar : noisyVars) {
                sampler.setMARVar(bn.getVariables().getVariableByName(noisyVar), 0.1);
            }
        }
        for (int i = 0; i < NSETS; i++) {
            System.out.println("--------------- DATA " + i + " --------------------------");
            if (i%5==0){
                bn.randomInitialization(new Random((long)((i+10)%2)));
                sampler = new BayesianNetworkSampler(bn);
                sampler.setBatchSize(BATCHSIZE);
                sampler.setSeed(1);
            }
            DataFlink<DataInstance> data0 = sampler.sampleToDataFlink(SAMPLESIZE);
            DataFlinkWriter.writeDataToARFFFolder(data0, "hdfs:///tmp_conceptdrift_data" + i + ".arff");
        }
    }

    public static DynamicBayesianNetwork createDBN1() throws Exception {

        DynamicVariables dynamicVariables = new DynamicVariables();
        Variable classVar = dynamicVariables.newMultinomialDynamicVariable("C", 2);

        for (int i = 0; i < numVars; i++) {
            dynamicVariables.newGaussianDynamicVariable("A" + i);
        }
        DynamicDAG dag = new DynamicDAG(dynamicVariables);

        for (int i = 0; i < numVars; i++) {
            dag.getParentSetTimeT(dynamicVariables.getVariableByName("A" + i)).addParent(classVar);
            if (connectDBN) dag.getParentSetTimeT(dynamicVariables.getVariableByName("A" + i)).addParent(dynamicVariables.getVariableByName("A" + i).getInterfaceVariable());

        }

        dag.getParentSetTimeT(classVar).addParent(classVar.getInterfaceVariable());
        dag.setName("dbn1");
        DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(dag);
        dbn.randomInitialization(new Random(1));

        return dbn;
    }

    public static void createDataSetsDBN(List<String> hiddenVars, List<String> noisyVars) throws Exception {
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DynamicBayesianNetwork dbn = createDBN1();

        for (Variable variable : dbn.getDynamicVariables()) {
            if (!variable.getName().startsWith("A"))
                continue;


            Normal_MultinomialNormalParents dist = dbn.getConditionalDistributionTimeT(variable);
            dist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{1.0});
            dist.getNormal_NormalParentsDistribution(0).setIntercept(1);

            dist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{1.0});
            dist.getNormal_NormalParentsDistribution(1).setIntercept(1);
        }

        System.out.println(dbn.toString());

        DBNSampler sampler = new DBNSampler(dbn);
        sampler.setNSamples(SAMPLESIZE);
        sampler.setBatchSize(BATCHSIZE);
        sampler.setSeed(0);

        if (hiddenVars!=null) {
            for (String hiddenVar : hiddenVars) {
                sampler.setHiddenVar(dbn.getDynamicVariables().getVariableByName(hiddenVar));
            }
        }
        if (noisyVars!=null){
            for (String noisyVar : noisyVars) {
                sampler.setMARVar(dbn.getDynamicVariables().getVariableByName(noisyVar), 0.1);
            }
        }

        DataFlink<DynamicDataInstance> data0 = sampler.cascadingSample(null);


        DataFlinkWriter.writeDataToARFFFolder(data0, "hdfs:///tmp_conceptdrift_data0.arff");
        data0 = DataFlinkLoader.loadDynamicDataFromFolder(env, "hdfs:///tmp_conceptdrift_data0.arff", false);

        List<Long> list = data0.getDataSet().map(d -> d.getSequenceID()).collect();
        System.out.println(list);


        DataFlink<DynamicDataInstance> dataPrev = data0;
        for (int i = 1; i < NSETS; i++) {
            System.out.println("--------------- DATA " + i + " --------------------------");
            if (i==5){
                for (Variable variable : dbn.getDynamicVariables()) {
                    if (!variable.getName().startsWith("A"))
                        continue;


                    Normal_MultinomialNormalParents dist = dbn.getConditionalDistributionTimeT(variable);
                    dist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{1.0});
                    dist.getNormal_NormalParentsDistribution(0).setIntercept(0);

                    dist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{1.0});
                    dist.getNormal_NormalParentsDistribution(1).setIntercept(0);
                }
                System.out.println(dbn);
                sampler.setDBN(dbn);
            }
            if (i==10){
                for (Variable variable : dbn.getDynamicVariables()) {
                    if (!variable.getName().startsWith("A"))
                        continue;


                    Normal_MultinomialNormalParents dist = dbn.getConditionalDistributionTimeT(variable);
                    dist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{1.0});
                    dist.getNormal_NormalParentsDistribution(0).setIntercept(-1);

                    dist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{1.0});
                    dist.getNormal_NormalParentsDistribution(1).setIntercept(-1);
                }
                System.out.println(dbn);
                sampler.setDBN(dbn);
            }
            DataFlink<DynamicDataInstance> dataNew = sampler.cascadingSample(dataPrev);//i%4==1);
            DataFlinkWriter.writeDataToARFFFolder(dataNew, "hdfs:///tmp_conceptdrift_data" + i + ".arff");
            dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env, "hdfs:///tmp_conceptdrift_data" + i + ".arff", false);
            dataPrev = dataNew;
        }
    }

    public static void testUpdateN() throws Exception {
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DataFlink<DataInstance> data0 = DataFlinkLoader.loadDataFromFolder(env,
                "hdfs:///tmp_conceptdrift_data0.arff", false);


        long start = System.nanoTime();
        IDAConceptDriftDetector learn = new IDAConceptDriftDetector();
        learn.setBatchSize(1000);
        learn.setClassIndex(0);
        learn.setAttributes(data0.getAttributes());
        learn.setNumberOfGlobalVars(1);
        learn.setTransitionVariance(0.1);
        learn.setSeed(0);

        learn.initLearning();
        double[] output = new double[NSETS];

        System.out.println("--------------- DATA " + 0 + " --------------------------");
        double[] out = learn.updateModelWithNewTimeSlice(data0);
        //System.out.println(learn.getLearntDynamicBayesianNetwork());
        output[0] = out[0];

        for (int i = 1; i < NSETS; i++) {
            System.out.println("--------------- DATA " + i + " --------------------------");
            DataFlink<DataInstance> dataNew = DataFlinkLoader.loadDataFromFolder(env,
                    "hdfs:///tmp_conceptdrift_data" + i + ".arff", false);
            out = learn.updateModelWithNewTimeSlice(dataNew);
            //System.out.println(learn.getLearntDynamicBayesianNetwork());
            output[i] = out[0];

        }
        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;

        System.out.println("Running time" + seconds + " seconds");

        System.out.println(learn.getLearntDynamicBayesianNetwork());

        for (int i = 0; i < NSETS; i++) {
            System.out.println("E(H_"+i+") =\t" + output[i]);
        }

    }

    public static void main(String[] args) throws Exception {

        numVars = Integer.parseInt(args[0]);
        SAMPLESIZE = Integer.parseInt(args[1]);
        NSETS = Integer.parseInt(args[2]);
        boolean createDataSet = Boolean.parseBoolean(args[3]);
        dbn = Boolean.parseBoolean(args[4]);


        if(createDataSet) {
            if(dbn)
                createDataSetsDBN(null, null);
            else
                createDataSets(null,null);
        }
        testUpdateN();
    }

}
