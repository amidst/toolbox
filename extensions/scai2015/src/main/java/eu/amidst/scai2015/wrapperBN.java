/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.scai2015;

import eu.amidst.core.datastream.*;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.MissingAssignment;
import eu.amidst.core.variables.Variables;
import eu.amidst.core.variables.Variable;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.Prediction;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.core.Instances;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


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
    static int NbrClients= 50000;

    HashMap<Integer, Multinomial> posteriorsGlobal = new HashMap<>();


    static boolean usePRCArea = false; //By default ROCArea is used

    static boolean dynamicNB = false;

    static boolean onlyPrediction = false;

    public static boolean isDynamicNB() {
        return dynamicNB;
    }

    public static void setDynamicNB(boolean dynamicNB) {
        wrapperBN.dynamicNB = dynamicNB;
    }

    public static boolean isOnlyPrediction() {
        return onlyPrediction;
    }

    public static void setOnlyPrediction(boolean onlyPrediction) {
        wrapperBN.onlyPrediction = onlyPrediction;
    }

    HashMap<Integer, Integer> defaultingClients = new HashMap<>();

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

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public static int getNbrClients() {
        return NbrClients;
    }

    public static void setNbrClients(int nbrClients) {
        NbrClients = nbrClients;
    }

    public BayesianNetwork wrapperBNOneMonthNB(DataOnMemory<DataInstance> data){

        Variables Vars = new Variables(data.getAttributes());

        //Split the whole data into training and testing
        List<DataOnMemory<DataInstance>> splitData = this.splitTrainAndTest(data,66.0);
        DataOnMemory<DataInstance> trainingData = splitData.get(0);
        DataOnMemory<DataInstance> testData = splitData.get(1);

        List<Variable> NSF = new ArrayList<>(Vars.getListOfVariables()); // NSF: non selected features
        NSF.remove(classVariable);     //remove C
        NSF.remove(classVariable_PM); // remove C'
        int nbrNSF = NSF.size();

        List<Variable> SF = new ArrayList(); // SF:selected features
        Boolean stop = false;

        //Learn the initial BN with training data including only the class variable
        BayesianNetwork bNet = train(trainingData, Vars, SF,false);

        //System.out.println(bNet.outputString());

        //Evaluate the initial BN with testing data including only the class variable, i.e., initial score or initial auc
        double score = testFS(testData, bNet);

        int cont=0;
        //iterate until there is no improvement in score
        while (nbrNSF > 0 && stop == false ){

            //System.out.print("Iteration: " + cont + ", Score: "+score +", Number of selected variables: "+ SF.size() + ", ");
            //SF.stream().forEach(v -> System.out.print(v.getName() + ", "));
            //System.out.println();
            Map<Variable, Double> scores = new ConcurrentHashMap<>(); //scores for each considered feature

            //Scores for adding
            NSF.parallelStream().forEach(V -> {
                List<Variable> SF_TMP = new ArrayList();

                SF_TMP.addAll(SF);

                SF_TMP.add(V);

              //train
                BayesianNetwork bNet_TMP = train(trainingData, Vars, SF_TMP, false);
                //evaluate
                scores.put(V, testFS(testData, bNet_TMP));
                SF_TMP.remove(V);
            });

            //Scores for removing
            SF.parallelStream().forEach(V ->{
                List<Variable> SF_TMP = new ArrayList();

                SF_TMP.addAll(SF);

                SF_TMP.remove(V);

                //train
                BayesianNetwork bNet_TMP = train(trainingData, Vars, SF_TMP, false);
                //evaluate
                scores.put(V, testFS(testData, bNet_TMP));
                SF_TMP.add(V);
            });

            //determine the Variable V with max score
            double maxScore = (Collections.max(scores.values()));  //returns max value in the Hashmap

            if (maxScore - score > 0.001){
                score = maxScore;
                //Variable with best score
                for (Map.Entry<Variable, Double> entry : scores.entrySet()) {
                    if (entry.getValue()== maxScore){
                        Variable SelectedV = entry.getKey();
                        if (SF.contains(SelectedV)) {
                            SF.remove(SelectedV);
                            NSF.add(SelectedV);
                        }else {
                            SF.add(SelectedV);
                            NSF.remove(SelectedV);
                        }
                        break;
                    }
                }
                nbrNSF = nbrNSF - 1;
                }
            else{
                stop = true;
            }
            cont++;
        }

        //Final training with the winning SF and the full initial data
        bNet = train(data, Vars, SF, true);

        //System.out.println(bNet.getDAG().outputString());

        return bNet;
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
            else
                test.add(dataInstance);
        }

        Collections.shuffle(train.getList(), random);
        Collections.shuffle(test.getList(), random);

        return Arrays.asList(train, test);
    }


    public BayesianNetwork train(DataOnMemory<DataInstance> data, Variables allVars, List<Variable> SF, boolean includeClassVariablePM){

        DAG dag = new DAG(allVars);
        if(includeClassVariablePM)
            dag.getParentSet(classVariable).addParent(classVariable_PM);
        /* Add classVariable to all SF*/
        dag.getParentSets().stream()
                .filter(parent -> SF.contains(parent.getMainVar()))
                .filter(w -> w.getMainVar().getVarID() != classVariable.getVarID())
                .forEach(w -> w.addParent(classVariable));

        SVB vmp = new SVB();

        vmp.setDAG(dag);
        vmp.setDataStream(data);
        vmp.setWindowsSize(100);
        vmp.runLearning();

        return vmp.getLearntBayesianNetwork();
    }

    public double testFS(DataOnMemory<DataInstance> data, BayesianNetwork bn){
        InferenceAlgorithm vmp = new VMP();
        ArrayList<Prediction> predictions = new ArrayList<>();

        for (DataInstance instance : data) {
            double classValue = instance.getValue(classVariable);
            Prediction prediction;
            Multinomial posterior;

            vmp.setModel(bn);

            MissingAssignment assignment = new MissingAssignment(instance);
            assignment.addMissingVariable(classVariable);

            vmp.setEvidence(assignment);
            vmp.runInference();
            posterior = vmp.getPosterior(classVariable);
            prediction = new NominalPrediction(classValue, posterior.getProbabilities());
            predictions.add(prediction);
        }

        ThresholdCurve thresholdCurve = new ThresholdCurve();
        Instances tcurve = thresholdCurve.getCurve(predictions);

        if(isUsePRCArea())
            return ThresholdCurve.getPRCArea(tcurve);
        else
            return ThresholdCurve.getROCArea(tcurve);
    }

    public double test(DataOnMemory<DataInstance> data, BayesianNetwork bn, HashMap<Integer, Multinomial> posteriors, boolean updatePosteriors){


        InferenceAlgorithm vmp = new VMP();
        ArrayList<Prediction> predictions = new ArrayList<>();

        int currentMonthIndex = (int)data.getDataInstance(0).getValue(TIME_ID);

        for (DataInstance instance : data) {
            int clientID = (int) instance.getValue(SEQUENCE_ID);

            double classValue = instance.getValue(classVariable);
            Prediction prediction;
            Multinomial posterior;


            /*Propagates*/
            bn.setConditionalDistribution(classVariable_PM, posteriors.get(clientID));

            vmp.setModel(bn);

            MissingAssignment assignment = new MissingAssignment(instance);
            assignment.addMissingVariable(classVariable);
            assignment.addMissingVariable(classVariable_PM);

            vmp.setEvidence(assignment);
            vmp.runInference();
            posterior = vmp.getPosterior(classVariable);

            prediction = new NominalPrediction(classValue, posterior.getProbabilities());


            predictions.add(prediction);

            if (classValue == DEFAULTER_VALUE_INDEX) {
                defaultingClients.putIfAbsent(clientID, currentMonthIndex);
            }

            if(updatePosteriors) {
                Multinomial multi_PM = posterior.toEFUnivariateDistribution().deepCopy(classVariable_PM).toUnivariateDistribution();
                if (classValue == DEFAULTER_VALUE_INDEX) {
                    multi_PM.setProbabilityOfState(DEFAULTER_VALUE_INDEX, 1.0);
                    multi_PM.setProbabilityOfState(NON_DEFAULTER_VALUE_INDEX, 0);
                }
                posteriors.put(clientID, multi_PM);
            }
        }

        ThresholdCurve thresholdCurve = new ThresholdCurve();
        Instances tcurve = thresholdCurve.getCurve(predictions);

        if(usePRCArea)
            return ThresholdCurve.getPRCArea(tcurve);
        else
            return ThresholdCurve.getROCArea(tcurve);
    }

    public double propagateAndTest(Queue<DataOnMemory<DataInstance>> data, BayesianNetwork bn){

        HashMap<Integer, Multinomial> posteriors = new HashMap<>();
        InferenceAlgorithm vmp = new VMP();
        ArrayList<Prediction> predictions = new ArrayList<>();

        boolean firstMonth = true;

        Iterator<DataOnMemory<DataInstance>> iterator = data.iterator();
        while(iterator.hasNext()){

            Prediction prediction = null;
            Multinomial posterior = null;
            DataOnMemory<DataInstance> batch = iterator.next();
            int currentMonthIndex = (int)batch.getDataInstance(0).getValue(TIME_ID);

            for (DataInstance instance : batch) {

                int clientID = (int) instance.getValue(SEQUENCE_ID);
                double classValue = instance.getValue(classVariable);

                /*Propagates*/
                double classValue_PM = -1;
                if(!firstMonth){
                    bn.setConditionalDistribution(classVariable_PM, posteriors.get(clientID));
                    classValue_PM = instance.getValue(classVariable_PM);
                    instance.setValue(classVariable_PM, Utils.missingValue());
                }
                vmp.setModel(bn);
                instance.setValue(classVariable, Utils.missingValue());
                vmp.setEvidence(instance);
                vmp.runInference();
                posterior = vmp.getPosterior(classVariable);

                instance.setValue(classVariable, classValue);
                if(!firstMonth) {
                    instance.setValue(classVariable_PM, classValue_PM);
                }
                if(!iterator.hasNext()) { //Last month or present
                    prediction = new NominalPrediction(classValue, posterior.getProbabilities());
                    predictions.add(prediction);
                }


                Multinomial multi_PM = posterior.toEFUnivariateDistribution().deepCopy(classVariable_PM).toUnivariateDistribution();

                posteriors.put(clientID, multi_PM);
            }

            firstMonth = false;

            if(!iterator.hasNext()) {//Last month or present time
                ThresholdCurve thresholdCurve = new ThresholdCurve();
                Instances tcurve = thresholdCurve.getCurve(predictions);

                if(usePRCArea)
                    return ThresholdCurve.getPRCArea(tcurve);
                else
                    return ThresholdCurve.getROCArea(tcurve);
            }
        }
        throw new UnsupportedOperationException("Something went wrong: The method should have stopped at some point in the loop.");
    }

    void learnCajamarModel(DataStream<DataInstance> data) {

        Variables Vars = new Variables(data.getAttributes());
        classVariable = Vars.getVariableById(Vars.getNumberOfVars()-1);
        classVariable_PM = Vars.getVariableById(Vars.getNumberOfVars()-2);

        TIME_ID = data.getAttributes().getAttributeByName("TIME_ID");
        SEQUENCE_ID = data.getAttributes().getAttributeByName("SEQUENCE_ID");

        int count = 0;
        double averageAUC = 0;


        Iterable<DataOnMemory<DataInstance>> iteratable = data.iterableOverBatches(getNbrClients());
        Iterator<DataOnMemory<DataInstance>> iterator =  iteratable.iterator();
        Queue<DataOnMemory<DataInstance>> monthsMinus12to0 = new LinkedList<>();

        iterator.next(); //First month is discarded

        //Take 13 batches at a time - 1 for training and 12 for testing
        for (int i = 0; i < 12; i++) {
            monthsMinus12to0.add(iterator.next());
        }

        while(iterator.hasNext()){

            DataOnMemory<DataInstance> currentMonth = iterator.next();
            monthsMinus12to0.add(currentMonth);

            int idMonthMinus12 = (int)monthsMinus12to0.peek().getDataInstance(0).getValue(TIME_ID);
            BayesianNetwork bn = null;
            if(isOnlyPrediction()){
                DataOnMemory<DataInstance> batch = monthsMinus12to0.poll();
                Variables vars = new Variables(batch.getAttributes());
                bn = train(batch, vars, vars.getListOfVariables(),this.isDynamicNB());
                double auc = propagateAndTest(monthsMinus12to0, bn);

                System.out.println( idMonthMinus12 + "\t" + auc);
                averageAUC += auc;
            }
            else {
                bn = wrapperBNOneMonthNB(monthsMinus12to0.poll());
                double auc = propagateAndTest(monthsMinus12to0, bn);

                System.out.print( idMonthMinus12 + "\t" + auc);
                bn.getDAG().getParentSets().stream().filter(p -> p.getNumberOfParents()>0).forEach(p-> System.out.print("\t" + p.getMainVar().getName()));
                System.out.println();
                averageAUC += auc;
            }

            count += getNbrClients();


        }


        System.out.println("Average Accuracy: " + averageAUC / (count / getNbrClients()));
    }

    public static void main(String[] args) throws IOException {

        //DataStream<DataInstance> data = DataStreamLoader.open("datasets/BankArtificialDataSCAI2015_DEFAULTING_PM.arff");
        DataStream<DataInstance> data = DataStreamLoader.open(args[0]);

        for (int i = 1; i < args.length ; i++) {
            if(args[i].equalsIgnoreCase("PRCArea"))
                setUsePRCArea(true);
            if(args[i].equalsIgnoreCase("onlyPrediction"))
                setOnlyPrediction(true);
            if(args[i].equalsIgnoreCase("dynamic"))
                setDynamicNB(true);
            if(args[i].equalsIgnoreCase("-c"))
                setNbrClients(Integer.parseInt(args[++i]));

        }

       wrapperBN wbnet = new wrapperBN();

       wbnet.learnCajamarModel(data);

    }

}
