package eu.amidst.ida2015;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.*;
import eu.amidst.core.inference.InferenceEngineForBN;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.Fading;
import eu.amidst.core.learning.PlateuIIDReplication;
import eu.amidst.core.learning.StreamingVariationalBayesVMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

import java.util.Random;

/**
 * Created by andresmasegosa on 13/4/15.
 */
public class GlobalHiddenConceptDrift {

    private static double getRealMeanRandomConceptDrift(BayesianNetwork bn) {

        Variable classVariable = bn.getStaticVariables().getVariableByName("ClassVar");
        Variable gaussianVar0 = bn.getStaticVariables().getVariableByName("GaussianVar0");

        Multinomial distClass = bn.getDistribution(classVariable);
        Normal_MultinomialNormalParents distGV0 = bn.getDistribution(gaussianVar0);

        double inter0 = distGV0.getNormal_NormalParentsDistribution(0).getIntercept();
        double inter1 = distGV0.getNormal_NormalParentsDistribution(1).getIntercept();

        double mean = distClass.getProbabilityOfState(0) * inter0;
        mean += distClass.getProbabilityOfState(1) * inter1;

        return mean;
    }


    private static double getRealMeanSmoothConceptDrift(BayesianNetwork bn) {

        Variable classVariable = bn.getStaticVariables().getVariableByName("ClassVar");
        Variable gaussianVar0 = bn.getStaticVariables().getVariableByName("GaussianVar0");
        Variable globalHidden = bn.getStaticVariables().getVariableByName("Global");

        Multinomial distClass = bn.getDistribution(classVariable);
        Normal_MultinomialNormalParents distGV0 = bn.getDistribution(gaussianVar0);
        Normal distGlobal = bn.getDistribution(globalHidden);

        double inter0 = distGV0.getNormal_NormalParentsDistribution(0).getIntercept();
        double inter1 = distGV0.getNormal_NormalParentsDistribution(1).getIntercept();

        double[] coef0 = distGV0.getNormal_NormalParentsDistribution(0).getCoeffParents();
        double[] coef1 = distGV0.getNormal_NormalParentsDistribution(1).getCoeffParents();

        double mean = distClass.getProbabilityOfState(0) * (inter0 + coef0[0] * distGlobal.getMean());
        mean += distClass.getProbabilityOfState(1) * (inter1 + coef1[0] * distGlobal.getMean());

        return mean;
    }


    private static double getLearntMean(BayesianNetwork bn, double globalMean) {

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

        double mean = distClass.getProbabilityOfState(0) * (inter0 + coef0[0] * globalMean);
        mean += distClass.getProbabilityOfState(1) * (inter1 + coef1[0] * globalMean);

        return mean;
    }

    public static void conceptDriftWithRandomChanges(String[] args) {

        BayesianNetworkGenerator.setNumberOfContinuousVars(1);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(0);
        BayesianNetwork naiveBayes = BayesianNetworkGenerator.generateNaiveBayesWithGlobalHiddenVar(2, "Global");

        naiveBayes.randomInitialization(new Random(0));

        Variable globalHidden = naiveBayes.getStaticVariables().getVariableByName("Global");
        Variable classVariable = naiveBayes.getStaticVariables().getVariableByName("ClassVar");
        Variable gaussianVar0 = naiveBayes.getStaticVariables().getVariableByName("GaussianVar0");

        for (ConditionalDistribution dist : naiveBayes.getConditionalDistributions()) {
            if (dist.getVariable().equals(classVariable) || dist.getVariable().equals(globalHidden))
                continue;
            Normal_MultinomialNormalParents newdist = naiveBayes.getDistribution(dist.getVariable());
            newdist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{0.0});
            newdist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{0.0});
        }

        System.out.println(naiveBayes.toString());

        int windowSize =100;
        int sampleSize = 5000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(naiveBayes);
        sampler.setHiddenVar(globalHidden);
        DataStream<DataInstance> data = sampler.sampleToDataBase(sampleSize);
        int count = windowSize;

        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setPlateuStructure(new PlateuGlobalHiddenConceptDrift(globalHidden,true));
        svb.setTransitionMethod(new GlobalHiddenTransitionMethod(globalHidden, 1, 5));
        svb.setWindowsSize(windowSize);
        svb.setDAG(naiveBayes.getDAG());
        svb.initLearning();


        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

            svb.updateModel(batch);
            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            //System.out.println("****************");
            //System.out.println(learntBN.toString());
            Normal normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();
            //System.out.println("Global Hidden: " + normal.getMean() +", " + normal.getVariance());
            //System.out.println("****************");

            Normal_MultinomialNormalParents distA = learntBN.getDistribution(gaussianVar0);

            Normal_MultinomialNormalParents distATrue = naiveBayes.getDistribution(gaussianVar0);


            System.out.print(count + "\t" + normal.getMean() + "\t" + getLearntMean(learntBN, normal.getMean())+"\t" + getRealMeanRandomConceptDrift(naiveBayes));
            //System.out.print("\t" + distA.getNormal_NormalParentsDistribution(0).getIntercept() +"\t" + distA.getNormal_NormalParentsDistribution(1).getIntercept());
            //System.out.print("\t" + distA.getNormal_NormalParentsDistribution(0).getCoeffParents()[0] + "\t" + distA.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);
            //System.out.print("\t" + distATrue.getNormal_NormalParentsDistribution(0).getIntercept() + "\t" + distATrue.getNormal_NormalParentsDistribution(1).getIntercept());
            System.out.println();

            count += windowSize;
        }


        for (int K = 1; K < 10; K++) {
            //System.out.println("******************************** CONCEPT DRIFT ********************************");
            naiveBayes.randomInitialization(new Random(K+1));

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
            data = sampler.sampleToDataBase(sampleSize);

            for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

                svb.updateModel(batch);
                BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

                //System.out.println("****************");
                //System.out.println(learntBN.toString());
                Normal normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();
                //System.out.println("Global Hidden: " + normal.getMean() + ", " + normal.getVariance());
                //System.out.println("****************");


                Normal_MultinomialNormalParents distA = learntBN.getDistribution(gaussianVar0);
                Normal_MultinomialNormalParents distATrue = naiveBayes.getDistribution(gaussianVar0);


                System.out.print(count + "\t" + normal.getMean() + "\t" + getLearntMean(learntBN, normal.getMean()) + "\t" + getRealMeanRandomConceptDrift(naiveBayes));
                //System.out.print("\t" + distA.getNormal_NormalParentsDistribution(0).getIntercept() +"\t" + distA.getNormal_NormalParentsDistribution(1).getIntercept());
                //System.out.print("\t" + distA.getNormal_NormalParentsDistribution(0).getCoeffParents()[0] + "\t" + distA.getNormal_NormalParentsDistribution(1).getCoeffParents()[0]);
                //System.out.print("\t" + distATrue.getNormal_NormalParentsDistribution(0).getIntercept() + "\t" + distATrue.getNormal_NormalParentsDistribution(1).getIntercept());
                System.out.println();

                count += windowSize;
            }
        }
    }

    public static void conceptDriftSmoothChanges(String[] args) {

        BayesianNetworkGenerator.setNumberOfContinuousVars(10);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(0);
        BayesianNetwork naiveBayes = BayesianNetworkGenerator.generateNaiveBayesWithGlobalHiddenVar(2, "Global");

        naiveBayes.randomInitialization(new Random(1));

        Variable globalHidden = naiveBayes.getStaticVariables().getVariableByName("Global");
        Variable classVariable = naiveBayes.getStaticVariables().getVariableByName("ClassVar");
        Variable gaussianVar0 = naiveBayes.getStaticVariables().getVariableByName("GaussianVar0");


        naiveBayes.getDistribution(globalHidden).randomInitialization(new Random(1));

        System.out.println(naiveBayes.toString());

        int windowSize = 100;
        int sampleSize = 5000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(naiveBayes);
        sampler.setHiddenVar(globalHidden);
        DataStream<DataInstance> data = sampler.sampleToDataBase(sampleSize);
        int count = windowSize;

        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        svb.setPlateuStructure(new PlateuGlobalHiddenConceptDrift(globalHidden, true));
        svb.setTransitionMethod(new GlobalHiddenTransitionMethod(globalHidden, 1, 5));
        svb.setWindowsSize(windowSize);
        svb.setDAG(naiveBayes.getDAG());
        svb.initLearning();


        double acumLL = 0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

            //BayesianNetwork learntBN = svb.getLearntBayesianNetwork();
            Normal normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();
            //System.out.println("H_prior:"+ normal.getMean() + "\t" + normal.getVariance());

            acumLL += svb.updateModel(batch);
            //System.out.println(count + "\t" +  acumLL/count);
            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            //System.out.println("****************");
            //System.out.println(learntBN.toString());
            normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();
            //System.out.println("Global Hidden: " + normal.getMean() +", " + normal.getVariance());
            //System.out.println("****************");

            Normal distGH = naiveBayes.getDistribution(globalHidden);
            //System.out.println(count + "\t" + normal.getMean() + "\t" + normal.getVariance() +  "\t" + getLearntMean(learntBN, normal.getMean()) + "\t" + getRealMeanSmoothConceptDrift(naiveBayes));
            System.out.print(count + "\t" + normal.getMean() + "\t" + getLearntMean(learntBN, normal.getMean()) + "\t" + getRealMeanSmoothConceptDrift(naiveBayes));
            //System.out.print("\t" + distGH.getMean());

            System.out.println();
            count += windowSize;
        }


        for (int K = 1; K < 10; K++) {
            //System.out.println("******************************** CONCEPT DRIFT ********************************");
            naiveBayes.getDistribution(globalHidden).randomInitialization(new Random(K+2));
            //((Normal)naiveBayes.getDistribution(globalHidden)).setVariance(0.1);

            //System.out.println(((Normal)naiveBayes.getDistribution(globalHidden)).getMean() +"\t" + ((Normal)naiveBayes.getDistribution(globalHidden)).getVariance());

            sampler = new BayesianNetworkSampler(naiveBayes);
            sampler.setHiddenVar(globalHidden);
            data = sampler.sampleToDataBase(sampleSize);

            for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

                acumLL += svb.updateModel(batch);
                //System.out.println(count + "\t" +  acumLL/count);
                BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

                //System.out.println("****************");
                //System.out.println(learntBN.toString());
                Normal normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();
                //System.out.println("Global Hidden: " + normal.getMean() + ", " + normal.getVariance());
                //System.out.println("****************");

                Normal distGH = naiveBayes.getDistribution(globalHidden);
                //System.out.println(count + "\t" + normal.getMean() + "\t" + normal.getVariance() +  "\t" + getLearntMean(learntBN, normal.getMean()) + "\t" + getRealMeanSmoothConceptDrift(naiveBayes));
                System.out.print(count + "\t" + normal.getMean() + "\t" + getLearntMean(learntBN, normal.getMean()) + "\t" + getRealMeanSmoothConceptDrift(naiveBayes));
                //System.out.print("\t" + distGH.getMean());
                System.out.println();
                count += windowSize;
            }
        }
    }


    public static void conceptDriftSeaLevel(String[] args) {

        DataStream<DataInstance> data = DataStreamLoader.loadFromFile("./IDA2015/DriftSets/sea.arff");

        StaticVariables variables = new StaticVariables(data.getAttributes());
        Variable globalHidden = variables.newGaussianVariable("Global");
        Variable classVariable = variables.getVariableByName("cl");
        Variable at1 = variables.getVariableByName("at1");
        Variable at2 = variables.getVariableByName("at2");
        Variable at3 = variables.getVariableByName("at3");

        DAG dag = new DAG(variables);
        dag.getParentSet(at1).addParent(classVariable);
        dag.getParentSet(at1).addParent(globalHidden);

        dag.getParentSet(at2).addParent(classVariable);
        dag.getParentSet(at2).addParent(globalHidden);

        dag.getParentSet(at3).addParent(classVariable);
        dag.getParentSet(at3).addParent(globalHidden);

        System.out.println(dag.toString());

        int windowSize = 10;
        int count = windowSize;


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        svb.setPlateuStructure(new PlateuGlobalHiddenConceptDrift(globalHidden, true));
        svb.setTransitionMethod(new GlobalHiddenTransitionMethod(globalHidden, 1, 5));
        svb.setWindowsSize(windowSize);
        svb.setDAG(dag);
        svb.initLearning();

        //System.out.println(svb.getLearntBayesianNetwork().toString());

        double acumLL = 0;
        double avACC = 0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

            double accuracy = computeAccuracy(svb.getLearntBayesianNetwork(), batch, classVariable);

            acumLL += svb.updateModel(batch);

            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            //System.out.println(learntBN.toString());
            Normal normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();
            normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();

            Normal_MultinomialNormalParents dist1 = learntBN.getDistribution(at1);
            Normal_MultinomialNormalParents dist2 = learntBN.getDistribution(at2);
            Normal_MultinomialNormalParents dist3 = learntBN.getDistribution(at3);
            System.out.print(count + "\t" + normal.getMean());
            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(0).getIntercept());
            System.out.print("\t" + dist1.getNormal_NormalParentsDistribution(1).getIntercept());
            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(0).getIntercept());
            System.out.print("\t" + dist2.getNormal_NormalParentsDistribution(1).getIntercept());
            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(0).getIntercept());
            System.out.print("\t" + dist3.getNormal_NormalParentsDistribution(1).getIntercept());
            System.out.print("\t" + accuracy);
            System.out.println();

            count += windowSize;
            avACC+= accuracy;

        }

        System.out.println(avACC/(count/windowSize));

    }

    public static double computeAccuracy(BayesianNetwork bn, DataOnMemory<DataInstance> data, Variable classVariable){

        double predictions = 0;
        InferenceEngineForBN.setModel(bn);
        for (DataInstance instance : data) {
            double realValue = instance.getValue(classVariable);
            instance.setValue(classVariable, Utils.missingValue());
            InferenceEngineForBN.setEvidence(instance);
            InferenceEngineForBN.runInference();
            Multinomial posterior = InferenceEngineForBN.getPosterior(classVariable);
            if (Utils.maxIndex(posterior.getProbabilities())==realValue)
                predictions++;

            instance.setValue(classVariable, realValue);
        }

        return predictions/data.getNumberOfDataInstances();
    }


    public static void conceptDriftHyperplane(String[] args) {

        DataStream<DataInstance> data = DataStreamLoader.loadFromFile("./IDA2015/DriftSets/hyperplane1.arff");

        StaticVariables variables = new StaticVariables(data.getAttributes());
        Variable globalHidden = variables.newGaussianVariable("Global");
        Variable classVariable = variables.getVariableByName("output");

        DAG dag = new DAG(variables);

        for (int i = 0; i < 10; i++) {
            Variable att = variables.getVariableByName("attr"+i);
            dag.getParentSet(att).addParent(classVariable);
            dag.getParentSet(att).addParent(globalHidden);

        }

        System.out.println(dag.toString());

        int windowSize = 100;
        int count = windowSize;


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        svb.setPlateuStructure(new PlateuGlobalHiddenConceptDrift(globalHidden, true));
        svb.setTransitionMethod(new GlobalHiddenTransitionMethod(globalHidden, 1, 5));
        svb.setWindowsSize(windowSize);
        svb.setDAG(dag);
        svb.initLearning();

        //System.out.println(svb.getLearntBayesianNetwork().toString());

        double acumLL = 0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

            double accuracy = computeAccuracy(svb.getLearntBayesianNetwork(), batch, classVariable);

            acumLL += svb.updateModel(batch);

            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();
            //System.out.println(learntBN.toString());
            Normal normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();
            normal = svb.getPlateuStructure().getEFVariablePosterior(globalHidden, 0).toUnivariateDistribution();

            System.out.println(count + "\t" + normal.getMean() +"\t" + accuracy);

            count += windowSize;
        }

    }

    public static void conceptDriftSeaLevelFading(String[] args) {

        DataStream<DataInstance> data = DataStreamLoader.loadFromFile("./IDA2015/DriftSets/sea.arff");

        StaticVariables variables = new StaticVariables(data.getAttributes());
        Variable classVariable = variables.getVariableByName("cl");
        Variable at1 = variables.getVariableByName("at1");
        Variable at2 = variables.getVariableByName("at2");
        Variable at3 = variables.getVariableByName("at3");

        DAG dag = new DAG(variables);
        dag.getParentSet(at1).addParent(classVariable);

        dag.getParentSet(at2).addParent(classVariable);

        dag.getParentSet(at3).addParent(classVariable);

        System.out.println(dag.toString());

        int windowSize = 1000;
        int count = windowSize;


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        svb.setPlateuStructure(new PlateuIIDReplication());
        svb.setTransitionMethod(new Fading(0.0001));
        svb.setWindowsSize(windowSize);
        svb.setDAG(dag);
        svb.initLearning();

        //System.out.println(svb.getLearntBayesianNetwork().toString());

        double acumLL = 0;
        double avACC = 0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)) {

            double accuracy = computeAccuracy(svb.getLearntBayesianNetwork(), batch, classVariable);

            acumLL += svb.updateModel(batch);

            BayesianNetwork learntBN = svb.getLearntBayesianNetwork();

            Normal_MultinomialParents dist1 = learntBN.getDistribution(at1);
            Normal_MultinomialParents dist2 = learntBN.getDistribution(at2);
            Normal_MultinomialParents dist3 = learntBN.getDistribution(at3);
            System.out.print(count);
            System.out.print("\t" + dist1.getNormal(0).getMean());
            System.out.print("\t" + dist1.getNormal(1).getMean());
            System.out.print("\t" + dist2.getNormal(0).getMean());
            System.out.print("\t" + dist2.getNormal(1).getMean());
            System.out.print("\t" + dist3.getNormal(0).getMean());
            System.out.print("\t" + dist3.getNormal(1).getMean());
            System.out.print("\t" + accuracy);
            System.out.println();

            count += windowSize;
            avACC+= accuracy;
        }

        System.out.println(svb.getLearntBayesianNetwork().toString());
        System.out.println(avACC/(count/windowSize));
    }

    public static void main(String[] args) {
        GlobalHiddenConceptDrift.conceptDriftSeaLevel(args);
    }
}