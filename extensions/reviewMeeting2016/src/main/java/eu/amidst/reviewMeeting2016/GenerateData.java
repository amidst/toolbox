package eu.amidst.reviewMeeting2016;

import eu.amidst.core.distribution.Normal_MultinomialNormalParents;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import eu.amidst.flinklink.core.utils.DBNSampler;
import org.apache.flink.api.java.ExecutionEnvironment;

import java.util.Random;

/**
 * Created by ana@cs.aau.dk on 19/01/16.
 */
public class GenerateData {

    public static int BATCHSIZE = 500;
    public static boolean connectDBN = true;

    public static String path = "./datasets/dataFlink/conceptdrift/data";
    //String path = "hdfs:///tmp_conceptdrift_data";


    public static DynamicBayesianNetwork createDBN1(int numVars) throws Exception {

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

        //dag.getParentSetTimeT(classVar).addParent(classVar.getInterfaceVariable());
        dag.setName("dbn1");
        DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(dag);
        dbn.randomInitialization(new Random(1));

        return dbn;
    }

    public static void createDataSetsDBN(int numVars, int SAMPLESIZE,
                                         int NSETS) throws Exception {
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DynamicBayesianNetwork dbn = createDBN1(numVars);

        for (Variable variable : dbn.getDynamicVariables()) {
            if (!variable.getName().startsWith("A"))
                continue;


            Normal_MultinomialNormalParents dist = dbn.getConditionalDistributionTimeT(variable);
            dist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{1.0});
            dist.getNormal_NormalParentsDistribution(0).setIntercept(10);

            dist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{1.0});
            dist.getNormal_NormalParentsDistribution(1).setIntercept(10);
        }

        //System.out.println(dbn.toString());

        DBNSampler sampler = new DBNSampler(dbn);
        sampler.setNSamples(SAMPLESIZE);
        sampler.setBatchSize(BATCHSIZE);
        sampler.setSeed(0);

        DataFlink<DynamicDataInstance> data0 = sampler.cascadingSample(null);


        System.out.println("--------------- CREATING DATA 0 --------------------------");
        DataFlinkWriter.writeDataToARFFFolder(data0, path+"0.arff");
        data0 = DataFlinkLoader.loadDynamicDataFromFolder(env, path+"0.arff", false);


        DataFlink<DynamicDataInstance> dataPrev = data0;
        for (int i = 1; i < NSETS; i++) {
            System.out.println("--------------- CREATING DATA " + i + " --------------------------");
            if (i==4){
                for (Variable variable : dbn.getDynamicVariables()) {
                    if (!variable.getName().startsWith("A"))
                        continue;


                    Normal_MultinomialNormalParents dist = dbn.getConditionalDistributionTimeT(variable);
                    dist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{1.0});
                    dist.getNormal_NormalParentsDistribution(0).setIntercept(0);

                    dist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{1.0});
                    dist.getNormal_NormalParentsDistribution(1).setIntercept(0);
                }
                //System.out.println(dbn);
                sampler.setDBN(dbn);
            }
            if (i==7){
                for (Variable variable : dbn.getDynamicVariables()) {
                    if (!variable.getName().startsWith("A"))
                        continue;


                    Normal_MultinomialNormalParents dist = dbn.getConditionalDistributionTimeT(variable);
                    dist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{1.0});
                    dist.getNormal_NormalParentsDistribution(0).setIntercept(-10);

                    dist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{1.0});
                    dist.getNormal_NormalParentsDistribution(1).setIntercept(-10);
                }
                //System.out.println(dbn);
                sampler.setDBN(dbn);
            }
            DataFlink<DynamicDataInstance> dataNew = sampler.cascadingSample(dataPrev);//i%4==1);
            DataFlinkWriter.writeDataToARFFFolder(dataNew, path + i + ".arff");
            dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env, path + i + ".arff", false);
            dataPrev = dataNew;
        }
    }


    public static void main(String[] args) throws Exception {

        int numVars = Integer.parseInt(args[0]);
        int SAMPLESIZE = Integer.parseInt(args[1]);
        int NSETS = Integer.parseInt(args[2]);

        createDataSetsDBN(numVars, SAMPLESIZE, NSETS);

    }
}
