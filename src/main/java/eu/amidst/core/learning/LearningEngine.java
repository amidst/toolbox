package eu.amidst.core.learning;

import com.google.common.base.Stopwatch;
import eu.amidst.core.database.DataBase;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.variables.DynamicVariables;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 06/01/15.
 */
public final class LearningEngine {

    private static StaticParameterLearningAlgorithm staticParameterLearningAlgorithm = MaximumLikelihood::learnParametersStaticModel;

    private static DynamicParameterLearningAlgorithm dynamicParameterLearningAlgorithm = MaximumLikelihood::learnDynamic;

    private static StaticStructuralLearningAlgorithm staticStructuralLearningAlgorithm = LearningEngine::staticNaiveBayesStructure;

    private static DynamicStructuralLearningAlgorithm dynamicStructuralLearningAlgorithm = LearningEngine::dynamicNaiveBayesStructure;


    private static DAG staticNaiveBayesStructure(DataBase dataBase){
        StaticVariables modelHeader = new StaticVariables(dataBase.getAttributes());
        DAG dag = new DAG(modelHeader);
        Variable classVar = modelHeader.getVariableById(modelHeader.getNumberOfVars()-1);
        dag.getParentSets().stream().filter(w -> w.getMainVar().getVarID() != classVar.getVarID()).forEach(w -> w.addParent(classVar));

        return dag;
    }

    private static DynamicDAG dynamicNaiveBayesStructure(DataBase dataBase){
        DynamicVariables modelHeader = new DynamicVariables(dataBase.getAttributes());
        DynamicDAG dag = new DynamicDAG(modelHeader);
        Variable classVar = modelHeader.getVariableById(modelHeader.getNumberOfVars()-1);
        dag.getParentSetsTimeT()
                .stream()
                .filter(w-> w.getMainVar()
                .getVarID()!=classVar.getVarID())
                .forEach(w -> {
                    w.addParent(classVar);
                    w.addParent(modelHeader.getTemporalClone(w.getMainVar()));
                });

        return dag;
    }

    public static void setStaticParameterLearningAlgorithm(StaticParameterLearningAlgorithm staticParameterLearningAlgorithm) {
        LearningEngine.staticParameterLearningAlgorithm = staticParameterLearningAlgorithm;
    }

    public static void setDynamicParameterLearningAlgorithm(DynamicParameterLearningAlgorithm dynamicParameterLearningAlgorithm) {
        LearningEngine.dynamicParameterLearningAlgorithm = dynamicParameterLearningAlgorithm;
    }

    public static void setStaticStructuralLearningAlgorithm(StaticStructuralLearningAlgorithm staticStructuralLearningAlgorithm) {
        LearningEngine.staticStructuralLearningAlgorithm = staticStructuralLearningAlgorithm;
    }

    public static void setDynamicStructuralLearningAlgorithm(DynamicStructuralLearningAlgorithm dynamicStructuralLearningAlgorithm) {
        LearningEngine.dynamicStructuralLearningAlgorithm = dynamicStructuralLearningAlgorithm;
    }

    public static BayesianNetwork learnParameters(DAG dag, DataBase database){
        return staticParameterLearningAlgorithm.learn(dag,database);
    }

    public static DynamicBayesianNetwork learnParameters(DynamicDAG dag, DataBase dataBase){
        return dynamicParameterLearningAlgorithm.learn(dag,dataBase);
    }

    public static DAG learnDAG(DataBase dataBase){
        return staticStructuralLearningAlgorithm.learn(dataBase);
    }

    public static DynamicDAG learnDynamicDAG(DataBase dataBase){
        return dynamicStructuralLearningAlgorithm.learn(dataBase);
    }

    public static BayesianNetwork learnStaticModel(DataBase database){

        Stopwatch watch = Stopwatch.createStarted();
        DAG dag = staticStructuralLearningAlgorithm.learn(database);
        System.out.println("Structural Learning : " + watch.stop());

        watch = Stopwatch.createStarted();
        BayesianNetwork network = staticParameterLearningAlgorithm.learn(dag,database);
        System.out.println("Parameter Learning: " + watch.stop());

        return network;
    }

    public static DynamicBayesianNetwork learnDynamicModel(DataBase database){
        Stopwatch watch = Stopwatch.createStarted();
        DynamicDAG dag = dynamicStructuralLearningAlgorithm.learn(database);
        System.out.println("Structural Learning : " + watch.stop());

        watch = Stopwatch.createStarted();
        DynamicBayesianNetwork network = dynamicParameterLearningAlgorithm.learn(dag,database);
        System.out.println("Parameter Learning: " + watch.stop());

        return network;
    }

    public static void main(String[] args) throws Exception{

//        String dataFile = new String("./datasets/Pigs.arff");
//        DataBase<StaticDataInstance> data = new StaticDataOnDiskFromFile(new ARFFDataReader(dataFile));
//
//        ParallelTAN tan= new ParallelTAN();
//        tan.setNumCores(4);
//        tan.setNumSamplesOnMemory(1000);
//        tan.setNameRoot("p630400490");
//        tan.setNameTarget("p48124091");
//        LearningEngine.setStaticStructuralLearningAlgorithm(tan::learnDAG);
//
//        MaximumLikelihood.setBatchSize(1000);
//        LearningEngine.setStaticParameterLearningAlgorithm(MaximumLikelihood::learnParametersStaticModel);
//
//        BayesianNetwork tanModel = LearningEngine.learnStaticModel(data);

    }

}
