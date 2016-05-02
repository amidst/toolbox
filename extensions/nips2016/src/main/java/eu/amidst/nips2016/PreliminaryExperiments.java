package eu.amidst.nips2016;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.learning.parametric.bayesian.DriftSVB;
import eu.amidst.core.learning.parametric.bayesian.PopulationVI;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.learning.parametric.bayesian.StochasticVI;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;

import java.io.PrintWriter;

/**
 * Created by ana@cs.aau.dk on 02/05/16.
 */
public class PreliminaryExperiments {

    public static BayesianNetwork oneNormalVarBN;

    public static int[] batchSize = {50,100,1000};
    public static int[] memoryPopulationVI = {50,100,1000};
    public static int sampleSize = 1000000;

    public static DriftSVB driftSVB;
    public static SVB svb;
    public static PopulationVI populationVI;
    public static StochasticVI stochasticVI;

    public static void driftSVBinit(){

        driftSVB = new DriftSVB();
        driftSVB.setWindowsSize(batchSize[0]);
        driftSVB.setSeed(0);
        VMP vmp = driftSVB.getPlateuStructure().getVMP();
        vmp.setOutput(false);
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        driftSVB.setDAG(oneNormalVarBN.getDAG());

        driftSVB.initLearning();
    }

    public static void SVBinit(){

        svb = new SVB();
        svb.setWindowsSize(batchSize[0]);
        svb.setSeed(0);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setOutput(false);
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        svb.setDAG(oneNormalVarBN.getDAG());

        svb.initLearning();
    }

    public static void populationVIint(){
        populationVI = new PopulationVI();
        populationVI.setWindowSize(memoryPopulationVI[0]);
        populationVI.setSeed(0);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setOutput(false);
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        populationVI.setDAG(oneNormalVarBN.getDAG());

        populationVI.initLearning();
    }

    public static void stochasticVIint(){
        stochasticVI = new StochasticVI(sampleSize);
        stochasticVI.setBatchSize(batchSize[0]);
        stochasticVI.setSeed(0);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setOutput(false);
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        stochasticVI.setDAG(oneNormalVarBN.getDAG());

        stochasticVI.initLearning();
    }

    public static void EscenarioMixedConceptDrift(){

    }

    public static void printOutput(int it) throws Exception{


    }

    public static void main(String[] args) throws Exception{

        oneNormalVarBN = BayesianNetworkLoader.loadFromFile("./networks/simulated/Normal.bn");

        System.out.println(oneNormalVarBN);

        /**
         * Define Learning VI techniques
         */
        driftSVBinit();
        SVBinit();
        populationVIint();
        stochasticVIint();

        /**
         * Output files for predLL, lambda, mean, population size
         */
        PrintWriter writerPredLL = new PrintWriter("predLL.txt", "UTF-8");
        PrintWriter writerLambda = new PrintWriter("lamda.txt", "UTF-8");
        PrintWriter writerMean = new PrintWriter("mean.txt", "UTF-8");
        PrintWriter writerPopulationSize = new PrintWriter("populationSize.txt", "UTF-8");


        double pred = 0;

        for (int i = 0; i < 100; i++) {

            if (false) {
                Normal normal = oneNormalVarBN.getConditionalDistribution(oneNormalVarBN.getVariables().getVariableByName("A"));
                normal.setMean(normal.getMean()+5);
                normal.setVariance(normal.getVariance()+0.5);

                //System.out.println(oneNormalVarBN);
            }

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(oneNormalVarBN);
            sampler.setSeed(i);

            DataOnMemory<DataInstance> batch = sampler.sampleToDataStream(batchSize[0]).toDataOnMemory();

            if (i>0)
                pred+=driftSVB.predictedLogLikelihood(batch);


            /**
             * Update with all different learning techniques
             */
            driftSVB.updateModelWithConceptDrift(batch);
            svb.updateModel(batch);
            populationVI.updateModel(batch);
            stochasticVI.updateModel(batch);


            //System.out.println(svb.getLearntBayesianNetwork());

            /**
             * Outputs: predLL, lambda, mean, population size
             */

            writerPredLL.println(svb.predictedLogLikelihood(batch)+"\t"+svb.predictedLogLikelihood(batch)+"\t"+
                    "\t"+stochasticVI.predictedLogLikelihood(batch)+"\t"+populationVI.predictedLogLikelihood(batch));

            /* Learn maximum likelihood to get the mean*/
            writerMean.println(svb+"\t"+svb.predictedLogLikelihood(batch)+"\t"+
                    "\t"+stochasticVI.predictedLogLikelihood(batch)+"\t"+populationVI.predictedLogLikelihood(batch));

            writerPredLL.println(svb.predictedLogLikelihood(batch)+"\t"+svb.predictedLogLikelihood(batch)+"\t"+
                    "\t"+stochasticVI.predictedLogLikelihood(batch)+"\t"+populationVI.predictedLogLikelihood(batch));

            writerPredLL.println(svb.predictedLogLikelihood(batch)+"\t"+svb.predictedLogLikelihood(batch)+"\t"+
                    "\t"+stochasticVI.predictedLogLikelihood(batch)+"\t"+populationVI.predictedLogLikelihood(batch));


            //System.out.println("N Iter: " + i + ", " + driftSVB.getLambdaValue());



            /*if (i > 0 && i % 3 == 0) {
                assertEquals(0.0, svb.getLambdaValue(), 0.1);
            } else if (i > 0 && i % 3 != 0) {
                assertEquals(1.0, svb.getLambdaValue(), 0.1);
            }*/
        }

        /**
         * Close all output files
         */
        writerPredLL.close();
        writerLambda.close();
        writerMean.close();
        writerPopulationSize.close();

        //System.out.println(driftSVB.getLearntBayesianNetwork());


        System.out.println(pred);
    }
}
