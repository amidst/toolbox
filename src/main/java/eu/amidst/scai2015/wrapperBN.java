package eu.amidst.scai2015;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.InferenceEngineForBN;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;


/**
 * Created by Hanen on 18/05/15.
 */
public class wrapperBN {

    int seed = 0;
    Variable classVariable;
    static int DEFAULTER_VALUE_INDEX = 1;


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

    public static void main(String[] args) throws IOException {

        DataStream<DataInstance> data = DataStreamLoader.loadFromFile("datasets/randomdata.arff");

        //BayesianNetwork bn = wrapperBN.wrapperBNOneMonth(data);


    }

}
