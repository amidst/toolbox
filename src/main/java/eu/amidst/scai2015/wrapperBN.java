package eu.amidst.scai2015;

import eu.amidst.core.datastream.*;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.inference.InferenceEngineForBN;
import eu.amidst.core.inference.VMP;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.Prediction;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.core.Instances;

import java.io.IOException;
import java.util.*;


/**
 * Created by Hanen on 18/05/15.
 */
public class wrapperBN {

    int seed = 0;
    Variable classVariable;
    Variable classVariable_PM;

    Attribute SEQUENCE_ID;
    Attribute TIME_ID;
    static int DEFAULTER_VALUE_INDEX = 1;
    static int NON_DEFAULTER_VALUE_INDEX = 0;

    static boolean usePRCArea = false; //By default ROCArea is used

    public static boolean isUsePRCArea() {
        return usePRCArea;
    }

    public static void setUsePRCArea(boolean usePRCArea) {
        wrapperBN.usePRCArea = usePRCArea;
    }

    public Attribute getSEQUENCE_ID() {
        return SEQUENCE_ID;
    }

    public void setSEQUENCE_ID(Attribute SEQUENCE_ID) {
        this.SEQUENCE_ID = SEQUENCE_ID;
    }

    public Attribute getTIME_ID() {
        return TIME_ID;
    }

    public void setTIME_ID(Attribute TIME_ID) {
        this.TIME_ID = TIME_ID;
    }

    public Variable getClassVariable_PM() {
        return classVariable_PM;
    }

    public void setClassVariable_PM(Variable classVariable_PM) {
        this.classVariable_PM = classVariable_PM;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public Variable getClassVariable() {
        return classVariable;
    }

    public void setClassVariable(Variable classVariable) {
        this.classVariable = classVariable;
    }

    public BayesianNetwork wrapperBNOneMonth(DataOnMemory<DataInstance> data) throws IOException {

        StaticVariables Vars = new StaticVariables(data.getAttributes());
        int nbrVars = Vars.getNumberOfVars();
        Variable classVar = Vars.getVariableById(-1);

        //Split the whole data into training and testing
        List<DataOnMemory<DataInstance>> splitData = this.splitTrainAndTest(data,66.0);
        DataOnMemory<DataInstance> trainingData = splitData.get(0);
        DataOnMemory<DataInstance> testData = splitData.get(1);


        StaticVariables NonSelectedVars = Vars; // All the features variables minus the C and C'
        int nbrNonSelected = nbrVars-2; //nbr of all features except the 2 classes C and C'?
        StaticVariables SelectedVars = new StaticVariables();
        Boolean stop = false;

        DAG dag = new DAG(Vars);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        //Learn the initial BN with training data including only the class variable
        // bn = trainModel(trainingData,SelectedVars)

        //Evaluate the initial BN with testing data including only the class variable, i.e., initial score or initial auc
        double score = computeAccuracy (bn, testData, classVar); //add HashMap


        //iterate until there is no improvement in score
        while (nbrNonSelected > 0 && stop == false ) {

            double[] scores = new double[nbrVars]; //save the score for each potential considered variable

            for(Variable V:NonSelectedVars){

                //learn a naive bayes including the class and the new selected set of variables including the variable V

                //bn = trainModel(trainingData,NewSelectedVars)

                //evaluate using the testing data
                //auc = testModel(testData, bn, HashMap)
                scores[V.getVarID()] = computeAccuracy (bn, testData, classVar);

                //determine the max score and the index of the Variable

                int maxScore = max(scores);
                int index = maxIndex(scores);

                if (score < maxScore){
                    score = maxScore;
                    //add the variable with index to the list of SelectedVars
                    //SelectedVars = SelectedVars + V
                    nbrNonSelected = nbrNonSelected -1;
                }
                else{

                    stop = true;
                }
            }
        }


        //Update HashMap considering the winning subset of the selected features and all the data (i.e., training +testing data)
        //HashMap = updateHM (data, SelectedVars)

        return bn;
    }


    public static double computeAccuracy(BayesianNetwork bn, DataOnMemory<DataInstance> data, Variable classVar){

        double predictions = 0;

        InferenceEngineForBN.setModel(bn);

        for (DataInstance instance : data) {
            double realValue = instance.getValue(classVar);
            instance.setValue(classVar, Utils.missingValue());
            InferenceEngineForBN.setEvidence(instance);
            InferenceEngineForBN.runInference();
            Multinomial posterior = InferenceEngineForBN.getPosterior(classVar);
            if (Utils.maxIndex(posterior.getProbabilities())==realValue)
                predictions++;
            instance.setValue(classVar, realValue);
        }

        return predictions/data.getNumberOfDataInstances();
    }


    List<DataOnMemory<DataInstance>> splitTrainAndTest(DataOnMemory<DataInstance> data, double trainPercentage) {
        Random random = new Random(this.seed);

        DataOnMemoryListContainer<DataInstance> train = new DataOnMemoryListContainer(data.getAttributes());
        DataOnMemoryListContainer<DataInstance> test = new DataOnMemoryListContainer(data.getAttributes());

        for (DataInstance dataInstance : data) {
            if (dataInstance.getValue(classVariable) == DEFAULTER_VALUE_INDEX)
                continue;

            if (random.nextDouble()<trainPercentage/100.0)
                train.add(dataInstance);
            else
                test.add(dataInstance);
        }

        for (DataInstance dataInstance : data) {
            if (dataInstance.getValue(classVariable) != DEFAULTER_VALUE_INDEX)
                continue;

            if (random.nextDouble()<trainPercentage/100.0)
                train.add(dataInstance);
        }

        Collections.shuffle(train.getList(), random);
        Collections.shuffle(test.getList(), random);

        return Arrays.asList(train, test);
    }


    public double test(DataOnMemory<DataInstance> data, BayesianNetwork bn, HashMap<Integer, Multinomial> posteriors, boolean updatePosteriors){


        VMP vmp = new VMP();
        ArrayList<Prediction> predictions = new ArrayList<>();

        for (DataInstance instance : data) {
            int clientID = (int) instance.getValue(SEQUENCE_ID);

            //Multinomial distClass_PM = bn.getConditionalDistribution(classVariable_PM);
            //distClass_PM = posteriors.get(clientID);

            ((Multinomial)bn.getConditionalDistribution(classVariable_PM)).setProbabilities(posteriors.get(clientID).getProbabilities());

            Multinomial_MultinomialParents distClass = bn.getConditionalDistribution(classVariable);
            Multinomial deterministic = new Multinomial(classVariable);
            deterministic.setProbabilityOfState(DEFAULTER_VALUE_INDEX, 1.0);
            deterministic.setProbabilityOfState(NON_DEFAULTER_VALUE_INDEX, 0.0);
            distClass.setMultinomial(DEFAULTER_VALUE_INDEX, deterministic);

            vmp.setModel(bn);

            instance.setValue(classVariable, Utils.missingValue());
            instance.setValue(classVariable_PM, Utils.missingValue());
            vmp.setEvidence(instance);
            vmp.runInference();
            Multinomial posterior = vmp.getPosterior(classVariable);

            double realValue = instance.getValue(classVariable);
            Prediction prediction = new NominalPrediction(realValue, posterior.getProbabilities());

            predictions.add(prediction);

            if(realValue == DEFAULTER_VALUE_INDEX) {
                posterior.setProbabilityOfState(DEFAULTER_VALUE_INDEX, 1.0);
                posterior.setProbabilityOfState(NON_DEFAULTER_VALUE_INDEX, 0);
            }

            if(updatePosteriors)
                posteriors.put(clientID, posterior);
        }

        ThresholdCurve thresholdCurve = new ThresholdCurve();
        Instances tcurve = thresholdCurve.getCurve(predictions);

        if(usePRCArea)
            return ThresholdCurve.getPRCArea(tcurve);
        else
            return ThresholdCurve.getROCArea(tcurve);
    }


    public static void main(String[] args) throws IOException {

        DataStream<DataInstance> data = DataStreamLoader.loadFromFile("datasets/randomdata.arff");

        String arg;
        for (int i = 0; i < args.length ; i++) {
            arg = args[i];
            if(args[i].equalsIgnoreCase("PRCArea"))
                setUsePRCArea(true);
        }
        //BayesianNetwork bn = wrapperBN.wrapperBNOneMonth(data);


    }

}
