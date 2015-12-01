package eu.amidst.cajamareval;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.filereaders.DataOnMemoryFromFile;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.learning.parametric.ParallelMLMissingData;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.MissingAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.Prediction;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.core.Instances;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ana@cs.aau.dk on 30/11/15.
 */
public class WrapperFS {

    String className;
    BayesianNetwork bn;
    Variable classVariable;
    Variables allVariables;

    public enum Model{
        NB, TAN
    }

    Model modelType;

    private DAG learnNBdag(List<Variable> SF){
        DAG dag = new DAG(allVariables);
        /* Add classVariable to all SF*/
        dag.getParentSets().stream()
                .filter(parent -> SF.contains(parent.getMainVar()))
                .filter(w -> w.getMainVar().getVarID() != classVariable.getVarID())
                .forEach(w -> w.addParent(classVariable));
        return dag;
    }

    private DAG learnTANdag(List<Variable> SF){
        return null;
    }

    private BayesianNetwork trainModel(DataOnMemory<DataInstance> dataTrain, List<Variable> SF){

        DAG dag;
        switch (modelType){
            case TAN:
                dag = learnTANdag(SF);
                break;
            default: //NB
                dag = learnNBdag(SF);
                break;
        }


        //We create a ParameterLearningAlgorithm object with the MaximumLikehood builder
        ParallelMLMissingData parameterLearningAlgorithm = new ParallelMLMissingData();

        //We set the options
        parameterLearningAlgorithm.setBatchSize(10000);
        parameterLearningAlgorithm.setParallelMode(true);
        parameterLearningAlgorithm.setDebug(false);

        //We fix the DAG structure
        parameterLearningAlgorithm.setDAG(dag);

        //We should invoke this method before processing any data
        parameterLearningAlgorithm.initLearning();

        parameterLearningAlgorithm.setDataStream(dataTrain);

        parameterLearningAlgorithm.runLearning();

        /*
        //We can perform parameter learnig by a sequential updating of data batches.
        for (DataOnMemory<DataInstance> batch : dataTrain.iterableOverBatches(1000)){
            parameterLearningAlgorithm.updateModel(batch);
        }
        */

        //And we get the model
        return parameterLearningAlgorithm.getLearntBayesianNetwork();
    }

    private double testModel(DataOnMemory<DataInstance> dataTest, BayesianNetwork bn){
        InferenceAlgorithm vmp = new VMP();
        ArrayList<Prediction> predictions = new ArrayList<>();

        for (DataInstance instance : dataTest) {
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

        return ThresholdCurve.getROCArea(tcurve);
    }

    private List<Variable> runWrapperFS(DataOnMemory<DataInstance> trainingData, DataOnMemory<DataInstance> testData){

        allVariables = new Variables(trainingData.getAttributes());
        this.classVariable = allVariables.getVariableByName(className);

        List<Variable> NSF = new ArrayList<>(allVariables.getListOfVariables()); // NSF: non selected features
        NSF.remove(classVariable);     //remove C
        int nbrNSF = NSF.size();

        List<Variable> SF = new ArrayList(); // SF:selected features
        Boolean stop = false;

        //Learn the initial BN with training data including only the class variable
        BayesianNetwork bNet = trainModel(trainingData, SF);

        //System.out.println(bNet.outputString());

        //Evaluate the initial BN with testing data including only the class variable, i.e., initial score or initial auc
        double score = testModel(testData, bNet);

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
                BayesianNetwork bNet_TMP = trainModel(trainingData, SF_TMP);
                //evaluate
                scores.put(V, testModel(testData, bNet_TMP));
                SF_TMP.remove(V);
            });

            //Scores for removing
            SF.parallelStream().forEach(V ->{
                List<Variable> SF_TMP = new ArrayList();

                SF_TMP.addAll(SF);

                SF_TMP.remove(V);

                //train
                BayesianNetwork bNet_TMP = trainModel(trainingData, SF_TMP);
                //evaluate
                scores.put(V, testModel(testData, bNet_TMP));
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

        //System.out.println(SF);

        return SF;
    }

    private DataOnMemory<DataInstance> loadDataOnMemoryFromFile(String path){
        ARFFDataReader reader = new ARFFDataReader();
        reader.loadFromFile(path);
        return new DataOnMemoryFromFile(reader);
    }

    private DataOnMemory<DataInstance> joinTrainData(DataOnMemory<DataInstance> data,
                                                     DataOnMemory<DataInstance> dataEval){
        DataOnMemoryListContainer<DataInstance> allTrain = new DataOnMemoryListContainer(data.getAttributes(),
                data.getList());
        for(DataInstance dataInstance: dataEval){
            allTrain.add(dataInstance);
        }
        return allTrain;
    }

    public void runExperiments(String[] args){

        DataOnMemory<DataInstance> dataTrain = loadDataOnMemoryFromFile(args[0]);
        DataOnMemory<DataInstance> dataTrainEval = loadDataOnMemoryFromFile(args[1]);
        DataOnMemory<DataInstance> dataTest = loadDataOnMemoryFromFile(args[2]);
        className = args[3];

        modelType = Model.NB;
        if(args.length>4){
            switch (args[4]){
                //To be implemented
                case "TAN":
                    modelType = Model.TAN;
                    break;
                //NB
                default:
                    modelType = Model.NB;
            }
        }

        List<Variable> selectedAtts = this.runWrapperFS(dataTrain, dataTrainEval);

        this.printSelectedAtts(selectedAtts);

        //join train data and train data Eval
        //Stream<DataInstance> allDataTrain = Stream.concat(dataTrain.stream(),dataTrainEval.stream());
        DataOnMemory<DataInstance> allDataTrain = joinTrainData(dataTrain, dataTrainEval);

        //Train the model with all training data and only the selected attributes
        BayesianNetwork bn = this.trainModel(allDataTrain,selectedAtts);

        System.out.printf("Area under the ROC curve = "+this.testModel(dataTest, bn));

    }

    private void printSelectedAtts(List<Variable> selectedAtts) {
        System.out.printf("Selected attributes:\n");
        Iterator<Variable> it = selectedAtts.iterator();
        if (it.hasNext()) {
            System.out.print(it.next().getName());
        }
        while (it.hasNext()) {
            System.out.print(", " + it.next().getName());
        }
        System.out.println("\n");
    }


    public static void main(String[] args) throws IOException {

        WrapperFS wrapperFS = new WrapperFS();
        wrapperFS.runExperiments(args);

    }
}
