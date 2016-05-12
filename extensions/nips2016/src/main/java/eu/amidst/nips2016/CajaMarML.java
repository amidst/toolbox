package eu.amidst.nips2016;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.distribution.Normal_MultinomialParents;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.io.PrintWriter;

/**
 * Created by ana@cs.aau.dk on 09/05/16.
 */
public class CajaMarML {

    public static PrintWriter writerMean;

    public static DAG dag;

    public static int numIter = 84;
    public static int batchSize = 1000;

    public static ParallelMaximumLikelihood ml;
    public static ParallelMaximumLikelihood mlPerBatch;

    public static boolean includeClassVar = true;

    private static void maximumLikelihoodInit(DAG dagML){
        ml = new ParallelMaximumLikelihood();
        ml.setParallelMode(false);
        ml.setDAG(dagML);
        ml.initLearning();

        mlPerBatch = new ParallelMaximumLikelihood();
        mlPerBatch.setParallelMode(true);
        mlPerBatch.setDAG(dagML);
        mlPerBatch.initLearning();
    }

    public static void printOutput(int currentMonth) throws Exception{

        BayesianNetwork bnML = ml.getLearntBayesianNetwork();
        BayesianNetwork bnMLPerBatch = mlPerBatch.getLearntBayesianNetwork();


        double[] meanML=new double[2], realMean=new double[2];
        double[] sdML=new double[2], realSd=new double[2];

        Variable var = dag.getVariables().getVariableByName("VAR10");
        if(!includeClassVar) {
            Normal normalML = ((Normal) bnML.getConditionalDistribution(var));
            meanML[0] = normalML.getMean();
            sdML[0] = normalML.getSd();
            Normal normalMLPerBatch = ((Normal) bnMLPerBatch.getConditionalDistribution(var));
            realMean[0] = normalMLPerBatch.getMean();
            realSd[0] = normalMLPerBatch.getSd();
        }else{
            for (int i = 0; i < 2; i++) {
                Normal normalML = ((Normal_MultinomialParents) bnML.getConditionalDistribution(var)).getNormal(i);
                meanML[i] = normalML.getMean();
                sdML[i] = normalML.getSd();
                Normal normalMLPerBatch = ((Normal_MultinomialParents)bnMLPerBatch.getConditionalDistribution(var)).getNormal(i);
                realMean[i] = normalMLPerBatch.getMean();
                realSd[i] = normalMLPerBatch.getSd();
            }
        }

        String means = ""+currentMonth;
        for (int i = 0; i < 2; i++) {
            means += "\t"+realMean[i]+"\t"+meanML[i];
        }
        for (int i = 0; i < 2; i++) {
            means += "\t"+realSd[i]+"\t"+sdML[i];
        }


        writerMean.println(means);

    }

    public static DAG createDAGforML(Attributes attributes){

        // Create a Variables object from the attributes of the input data stream.
        Variables variables = new Variables(attributes);

        // Define the class variable.
        Variable classVar = variables.getVariableByName("DEFAULTING");


        // Create an empty DAG object with the defined variables.
        DAG dag = new DAG(variables);

        // Link the class as parent of all attributes
        if(includeClassVar) {
            dag.getParentSets()
                    .stream()
                    .filter(w -> w.getMainVar() != classVar)
                    .filter(w -> !w.getMainVar().getName().startsWith("Hidden"))
                    .forEach(w -> w.addParent(classVar));
        }

        // Show the new dynamic DAG structure
        System.out.println(dag.toString());

        return dag;
    }


    public static void main(String[] args) throws Exception{

        String path="/Users/ana/Documents/Amidst-MyFiles/CajaMar/dataWeka/dataWeka";
        String outputPath = "extensions/nips2016/doc-Experiments/preliminaryExperiments/";


        DataStream<DataInstance> dataMonthi = DataStreamLoader.openFromFile(path + 0 + ".arff");

        dag = createDAGforML(dataMonthi.getAttributes());

        maximumLikelihoodInit(dag);

        writerMean = new PrintWriter(outputPath + "CajaMar/MLCajaMar_mean_" + "bs" + batchSize  + "_"+ includeClassVar + "_"+numIter+
                ".txt", "UTF-8");

        for (int m = 0; m < numIter; m++) {

            int currentMonth = m;

            //if (IntStream.of(peakMonths).anyMatch(x -> x == currentMonth))
            //    continue;

            dataMonthi = DataStreamLoader.openFromFile(path + currentMonth + ".arff");

            /**
             * Update with all different learning techniques
             */
            int batchCount = 0;
            for (DataOnMemory<DataInstance> batch : dataMonthi.iterableOverBatches(batchSize)) {

                System.out.println("--------------------------------- MONTH "+currentMonth+"/"+batchCount+ " --------------------------");

                /* Learn maximum likelihood to get the real means*/
                ml.updateModel(batch);
                mlPerBatch.updateModel(batch);

                batchCount++;

            }
            printOutput(currentMonth);
            System.out.println(ml.getLearntBayesianNetwork());
            System.out.println(mlPerBatch.getLearntBayesianNetwork());
            mlPerBatch.initLearning();
        }

        writerMean.close();

    }
}
