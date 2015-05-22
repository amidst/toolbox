package eu.amidst.scai2015;

import eu.amidst.core.datastream.*;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.InferenceAlgorithmForBN;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.StreamingVariationalBayesVMP;
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
    boolean parallelMode = true;
    int NbrClients= 50000;
    HashMap<Integer, Multinomial> posteriorsGlobal = new HashMap<>();

    static boolean usePRCArea = false; //By default ROCArea is used
    static boolean nonDeterministic = false; //By default, if a client is DEFAULTER one month, then it is predicted as
                                             //defaulter until evidence shows otherwise.
    static boolean NB = false;

    public static boolean isNB() {
        return NB;
    }

    public static void setNB(boolean NB) {
        wrapperBN.NB = NB;
    }

    public static boolean isNonDeterministic() {
        return nonDeterministic;
    }

    public static void setNonDeterministic(boolean nonDeterministic) {
        wrapperBN.nonDeterministic = nonDeterministic;
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

    public BayesianNetwork wrapperBNOneMonthNB(DataOnMemory<DataInstance> data){

        StaticVariables Vars = new StaticVariables(data.getAttributes());

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

        System.out.println(bNet.toString());

        //Evaluate the initial BN with testing data including only the class variable, i.e., initial score or initial auc
        double score = testFS(testData, bNet);

        int cont=0;
        //iterate until there is no improvement in score
        while (nbrNSF > 0 && stop == false ){

            System.out.print("Iteration: " + cont + ", Score: "+score +", Number of selected variables: "+ SF.size() + ", ");
            SF.stream().forEach(v -> System.out.print(v.getName() + ", "));
            System.out.println();
            Map<Variable, Double> scores = new HashMap<>(); //scores for each considered feature

            for(Variable V:NSF) {

                //if (V.getVarID()>5)
                //    break;
                System.out.println("Testing "+V.getName());
                SF.add(V);
                //train
                bNet = train(trainingData, Vars, SF, false);
                //evaluate
                scores.put(V, testFS(testData, bNet));
                SF.remove(V);
            }
                //determine the Variable V with max score
            double maxScore = (Collections.max(scores.values()));  //returns max value in the Hashmap

            if (maxScore - score > 0.001){
                score = maxScore;
                //Variable with best score
                for (Map.Entry<Variable, Double> entry : scores.entrySet()) {
                    if (entry.getValue()== maxScore){
                        Variable SelectedV = entry.getKey();
                        SF.add(SelectedV);
                        NSF.remove(SelectedV);
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

        System.out.println(bNet.getDAG().toString());

        return bNet;
    }

    public BayesianNetwork wrapperBNOneMonth(DataOnMemory<DataInstance> data){

        StaticVariables Vars = new StaticVariables(data.getAttributes());

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
        BayesianNetwork bNet = train(trainingData, Vars, SF);

        System.out.println(bNet.toString());

        //Evaluate the initial BN with testing data including only the class variable, i.e., initial score or initial auc
        double score = test(testData, bNet, posteriorsGlobal, false);

        int cont=0;
        //iterate until there is no improvement in score
        while (nbrNSF > 0 && stop == false ){

            System.out.print("Iteration: " + cont + ", Score: "+score +", Number of selected variables: "+ SF.size() + ", ");
            SF.stream().forEach(v -> System.out.print(v.getName() + ", "));
            System.out.println();
            Map<Variable, Double> scores = new HashMap<>(); //scores for each considered feature

            for(Variable V:NSF) {

                //if (V.getVarID()>5)
                //    break;
                System.out.println("Testing variable: "+V.getName());
                SF.add(V);
                //train
                bNet = train(trainingData, Vars, SF);
                //evaluate
                scores.put(V, test(testData, bNet, posteriorsGlobal, false));
                SF.remove(V);
            }
            //determine the Variable V with max score
            double maxScore = (Collections.max(scores.values()));  //returns max value in the Hashmap

            if (maxScore - score > 0.001){
                score = maxScore;
                //Variable with best score
                for (Map.Entry<Variable, Double> entry : scores.entrySet()) {
                    if (entry.getValue()== maxScore){
                        Variable SelectedV = entry.getKey();
                        SF.add(SelectedV);
                        NSF.remove(SelectedV);
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
        bNet = train(data, Vars, SF);
        test(data, bNet, posteriorsGlobal, true);

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


    public BayesianNetwork train(DataOnMemory<DataInstance> data, StaticVariables allVars, List<Variable> SF, boolean includeClassVariablePM){

        DAG dag = new DAG(allVars);
        if(includeClassVariablePM)
            dag.getParentSet(classVariable).addParent(classVariable_PM);
        /* Add classVariable to all SF*/
        dag.getParentSets().stream()
                .filter(parent -> SF.contains(parent.getMainVar()))
                .filter(w -> w.getMainVar().getVarID() != classVariable.getVarID())
                .forEach(w -> w.addParent(classVariable));

        StreamingVariationalBayesVMP vmp = new StreamingVariationalBayesVMP();

        vmp.setDAG(dag);
        vmp.setDataStream(data);
        vmp.setWindowsSize(100);
        vmp.runLearning();

        return vmp.getLearntBayesianNetwork();
    }

    public BayesianNetwork train(DataOnMemory<DataInstance> data, StaticVariables allVars, List<Variable> SF){

        DAG dag = new DAG(allVars);
        if(data.getDataInstance(0).getValue(TIME_ID)!=0)
            dag.getParentSet(classVariable).addParent(classVariable_PM);
        /* Add classVariable to all SF*/
        dag.getParentSets().stream()
                .filter(parent -> SF.contains(parent.getMainVar()))
                .filter(w -> w.getMainVar().getVarID() != classVariable.getVarID())
                .forEach(w -> w.addParent(classVariable));

        StreamingVariationalBayesVMP vmp = new StreamingVariationalBayesVMP();

        vmp.setDAG(dag);
        vmp.setDataStream(data);
        vmp.setWindowsSize(100);
        vmp.runLearning();

        return vmp.getLearntBayesianNetwork();
    }

    public double testFS(DataOnMemory<DataInstance> data, BayesianNetwork bn){
        InferenceAlgorithmForBN vmp = new VMP();
        ArrayList<Prediction> predictions = new ArrayList<>();
        int currentMonthIndex = (int)data.getDataInstance(0).getValue(TIME_ID);

        for (DataInstance instance : data) {
            int clientID = (int) instance.getValue(SEQUENCE_ID);
            double classValue = instance.getValue(classVariable);
            Prediction prediction;
            Multinomial posterior;

            vmp.setModel(bn);
            instance.setValue(classVariable, Utils.missingValue());
            vmp.setEvidence(instance);
            vmp.runInference();
            posterior = vmp.getPosterior(classVariable);
            instance.setValue(classVariable, classValue);
            prediction = new NominalPrediction(classValue, posterior.getProbabilities());
            predictions.add(prediction);
        }

        ThresholdCurve thresholdCurve = new ThresholdCurve();
        Instances tcurve = thresholdCurve.getCurve(predictions);

        if(usePRCArea)
            return ThresholdCurve.getPRCArea(tcurve);
        else
            return ThresholdCurve.getROCArea(tcurve);
    }

    public double test(DataOnMemory<DataInstance> data, BayesianNetwork bn, HashMap<Integer, Multinomial> posteriors, boolean updatePosteriors){


        InferenceAlgorithmForBN vmp = new VMP();
        ArrayList<Prediction> predictions = new ArrayList<>();

        int currentMonthIndex = (int)data.getDataInstance(0).getValue(TIME_ID);

        for (DataInstance instance : data) {
            int clientID = (int) instance.getValue(SEQUENCE_ID);

            double classValue = instance.getValue(classVariable);
            Prediction prediction;
            Multinomial posterior;

            if(!nonDeterministic
                    && (defaultingClients.get(clientID) != null)
                    && (defaultingClients.get(clientID) - currentMonthIndex >= 12)) {
                prediction = new NominalPrediction(classValue, new double[]{0.0, 1.0});
                posterior = new Multinomial(classVariable);
                /* This is for the sake of "correctness", this will never be used*/
                posterior.setProbabilityOfState(DEFAULTER_VALUE_INDEX, 1.0);
                posterior.setProbabilityOfState(NON_DEFAULTER_VALUE_INDEX, 0.0);
            }else{
                /*Propagates*/
                bn.setConditionalDistribution(classVariable_PM, posteriors.get(clientID));

                /*
                Multinomial_MultinomialParents distClass = bn.getConditionalDistribution(classVariable);
                Multinomial deterministic = new Multinomial(classVariable);
                deterministic.setProbabilityOfState(DEFAULTER_VALUE_INDEX, 1.0);
                deterministic.setProbabilityOfState(NON_DEFAULTER_VALUE_INDEX, 0.0);
                distClass.setMultinomial(DEFAULTER_VALUE_INDEX, deterministic);
                */

                vmp.setModel(bn);

                double classValue_PM = instance.getValue(classVariable_PM);
                instance.setValue(classVariable, Utils.missingValue());
                instance.setValue(classVariable_PM, Utils.missingValue());
                vmp.setEvidence(instance);
                vmp.runInference();
                posterior = vmp.getPosterior(classVariable);

                instance.setValue(classVariable, classValue);
                instance.setValue(classVariable_PM, classValue_PM);
                prediction = new NominalPrediction(classValue, posterior.getProbabilities());
            }

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
        InferenceAlgorithmForBN vmp = new VMP();
        ArrayList<Prediction> predictions = new ArrayList<>();

        /*
        for (int i = 0; i < NbrClients ; i++){
            Multinomial uniform = new Multinomial(classVariable_PM);
            uniform.setProbabilityOfState(DEFAULTER_VALUE_INDEX, 0.5);
            uniform.setProbabilityOfState(NON_DEFAULTER_VALUE_INDEX, 0.5);
            posteriors.put(i, uniform);
        }
        */

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

        StaticVariables Vars = new StaticVariables(data.getAttributes());
        classVariable = Vars.getVariableById(Vars.getNumberOfVars()-1);
        classVariable_PM = Vars.getVariableById(Vars.getNumberOfVars()-2);

        TIME_ID = data.getAttributes().getAttributeByName("TIME_ID");
        SEQUENCE_ID = data.getAttributes().getAttributeByName("SEQUENCE_ID");

        int count = 0;
        double averageAUC = 0;

        /*
        for (int i = 0; i < NbrClients ; i++){
            Multinomial uniform = new Multinomial(classVariable_PM);
            uniform.setProbabilityOfState(DEFAULTER_VALUE_INDEX, 0.5);
            uniform.setProbabilityOfState(NON_DEFAULTER_VALUE_INDEX, 0.5);
            posteriorsGlobal.put(i, uniform);
        }
        */


        Iterable<DataOnMemory<DataInstance>> iteratable = data.iterableOverBatches(NbrClients);
        Iterator<DataOnMemory<DataInstance>> iterator =  iteratable.iterator();
        Queue<DataOnMemory<DataInstance>> monthsMinus12to0 = new LinkedList<>();

        iterator.next(); //First month is discarded

        //Take 13 batches at a time - 1 for training and 12 for testing
        //for (int i = 0; i < 13; i++) {
        for (int i = 0; i < 3; i++) {
            monthsMinus12to0.add(iterator.next());
        }

        while(iterator.hasNext()){

            int idMonthMinus12 = (int)monthsMinus12to0.peek().getDataInstance(0).getValue(TIME_ID);
            BayesianNetwork bn = null;
            if(isNB()){
                DataOnMemory<DataInstance> batch = monthsMinus12to0.poll();
                StaticVariables vars = new StaticVariables(batch.getAttributes());
                train(batch, vars, vars.getListOfVariables());
            }
            else
                bn = wrapperBNOneMonthNB(monthsMinus12to0.poll());

            double auc = propagateAndTest(monthsMinus12to0, bn);

            System.out.println( idMonthMinus12 + "\t" + auc);
            averageAUC += auc;

            count += NbrClients;

            DataOnMemory<DataInstance> currentMonth = iterator.next();
            monthsMinus12to0.add(currentMonth);
        }


        System.out.println("Average Accuracy: " + averageAUC / (count / NbrClients));
    }

    public static void main(String[] args) throws IOException {

        //DataStream<DataInstance> data = DataStreamLoader.loadFromFile("datasets/BankArtificialDataSCAI2015_DEFAULTING_PM.arff");
        DataStream<DataInstance> data = DataStreamLoader.loadFromFile(args[0]);

        for (int i = 1; i < args.length ; i++) {
            if(args[i].equalsIgnoreCase("PRCArea"))
                setUsePRCArea(true);
            if(args[i].equalsIgnoreCase("nonDeterministic"))
                setNonDeterministic(true);
            if(args[i].equalsIgnoreCase("NB"))
                setNB(true);
        }

       wrapperBN wbnet = new wrapperBN();

       wbnet.learnCajamarModel(data);

    }

}
