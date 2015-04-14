package eu.amidst.ida2015;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.distribution.Normal_MultinomialNormalParents;
import eu.amidst.core.learning.StreamingVariationalBayesVMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;

import java.util.Random;

/**
 * Created by andresmasegosa on 13/4/15.
 */
public class GlobalHiddenConceptDrift {

    private static double getRealMean(BayesianNetwork bn){

        Variable classVariable = bn.getStaticVariables().getVariableByName("ClassVar");
        Variable gaussianVar0 = bn.getStaticVariables().getVariableByName("GaussianVar0");

        Multinomial distClass = bn.getDistribution(classVariable);
        Normal_MultinomialNormalParents distGV0 = bn.getDistribution(gaussianVar0);

        double inter0 = distGV0.getNormal_NormalParentsDistribution(0).getIntercept();
        double inter1 = distGV0.getNormal_NormalParentsDistribution(1).getIntercept();

        double mean = distClass.getProbabilityOfState(0)*inter0;
        mean+= distClass.getProbabilityOfState(1)*inter1;

        return mean;
    }

    private static double getLearntMean(BayesianNetwork bn, double globalMean){

        Variable globalHidden = bn.getStaticVariables().getVariableByName("Global");
        Variable classVariable = bn.getStaticVariables().getVariableByName("ClassVar");
        Variable gaussianVar0 = bn.getStaticVariables().getVariableByName("GaussianVar0");


        Normal distGlobal = bn.getDistribution(globalHidden);
        Multinomial distClass = bn.getDistribution(classVariable);
        Normal_MultinomialNormalParents distGV0 = bn.getDistribution(gaussianVar0);

        double inter0 = distGV0.getNormal_NormalParentsDistribution(0).getIntercept();
        double inter1 = distGV0.getNormal_NormalParentsDistribution(1).getIntercept();

        double[] coef0 = distGV0.getNormal_NormalParentsDistribution(0).getCoeffParents();
        double[] coef1 = distGV0.getNormal_NormalParentsDistribution(1).getCoeffParents();

        double mean = distClass.getProbabilityOfState(0)*(inter0 + coef0[0]*globalMean);
        mean+= distClass.getProbabilityOfState(1)*(inter1 + coef1[0]*globalMean);

        return mean;
    }

    public static void main(String[] args) {

        BayesianNetworkGenerator.setNumberOfContinuousVars(1);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(0);
        BayesianNetwork naiveBayes = BayesianNetworkGenerator.generateNaiveBayesWithGlobalHiddenVar(2, "Global");

        naiveBayes.randomInitialization(new Random(0));

        Variable globalHidden = naiveBayes.getStaticVariables().getVariableByName("Global");
        Variable classVariable = naiveBayes.getStaticVariables().getVariableByName("ClassVar");
        Variable gaussianVar0 = naiveBayes.getStaticVariables().getVariableByName("GaussianVar0");

        for (ConditionalDistribution dist : naiveBayes.getConditionalDistributions()){
            if (dist.getVariable().equals(classVariable) || dist.getVariable().equals(globalHidden))
                continue;
            Normal_MultinomialNormalParents newdist = naiveBayes.getDistribution(dist.getVariable());
            newdist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{0.0});
            newdist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{0.0});
        }

        System.out.println(naiveBayes.toString());

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(naiveBayes);
        sampler.setHiddenVar(globalHidden);
        DataStream<DataInstance> data = sampler.sampleToDataBase(1000);
        int windowSize = 100;
        int count = windowSize;

        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setPlateuStructure(new PlateuGlobalHiddenConceptDrift(globalHidden));
        svb.setTransitionMethod(new GlobalHiddenTransitionMethod(globalHidden, 0.1));
        svb.setWindowsSize(windowSize);
        svb.setDAG(naiveBayes.getDAG());
        svb.initLearning();


        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

            svb.updateModel(batch);
            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            //System.out.println("****************");
            //System.out.println(learntBN.toString());
            Normal normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden,0).toUnivariateDistribution();
            //System.out.println("Global Hidden: " + normal.getMean() +", " + normal.getVariance());
            //System.out.println("****************");

            System.out.println(count + "\t" + normal.getMean() +"\t" + getLearntMean(learntBN, normal.getMean()) + "\t" + getRealMean(naiveBayes));

            count+=windowSize;
        }


        for (int K=1; K<10; K++) {
            //System.out.println("******************************** CONCEPT DRIFT ********************************");
            naiveBayes.randomInitialization(new Random(K));

            for (ConditionalDistribution dist : naiveBayes.getConditionalDistributions()) {
                if (dist.getVariable().equals(classVariable) || dist.getVariable().equals(globalHidden))
                    continue;
                Normal_MultinomialNormalParents newdist = naiveBayes.getDistribution(dist.getVariable());
                newdist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{0.0});
                newdist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{0.0});
            }

            //System.out.println(naiveBayes.toString());

            sampler = new BayesianNetworkSampler(naiveBayes);
            sampler.setHiddenVar(globalHidden);
            data = sampler.sampleToDataBase(1000);

            for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

                svb.updateModel(batch);
                BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

                //System.out.println("****************");
                //System.out.println(learntBN.toString());
                Normal normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();
                //System.out.println("Global Hidden: " + normal.getMean() + ", " + normal.getVariance());
                //System.out.println("****************");

                System.out.println(count + "\t" + normal.getMean() +"\t" + getLearntMean(learntBN, normal.getMean()) + "\t" + getRealMean(naiveBayes));

                count+=windowSize;
            }
        }
    }
}
