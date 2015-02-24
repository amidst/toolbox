package eu.amidst.core.learning;

import com.google.common.base.Stopwatch;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

/**
 *
 * TODO Add a method for updating a model with one data instance:
 *
 * public BayesianNetwork updateModel(BayesianNetwork model, DataInstance instance);
 *
 * Created by andresmasegosa on 06/01/15.
 */
public final class LearningEngineForBN {


    private static StaticParameterLearningAlgorithm staticParameterLearningAlgorithm = MaximumLikelihoodForBN::learnParametersStaticModel;

    private static StaticStructuralLearningAlgorithm staticStructuralLearningAlgorithm = LearningEngineForBN::staticNaiveBayesStructure;


    private static DAG staticNaiveBayesStructure(DataStream<DataInstance> dataStream){
        StaticVariables modelHeader = new StaticVariables(dataStream.getAttributes());
        DAG dag = new DAG(modelHeader);
        Variable classVar = modelHeader.getVariableById(modelHeader.getNumberOfVars()-1);
        dag.getParentSets().stream().filter(w -> w.getMainVar().getVarID() != classVar.getVarID()).forEach(w -> w.addParent(classVar));

        return dag;
    }

    public static void setStaticParameterLearningAlgorithm(StaticParameterLearningAlgorithm staticParameterLearningAlgorithm) {
        LearningEngineForBN.staticParameterLearningAlgorithm = staticParameterLearningAlgorithm;
    }


    public static void setStaticStructuralLearningAlgorithm(StaticStructuralLearningAlgorithm staticStructuralLearningAlgorithm) {
        LearningEngineForBN.staticStructuralLearningAlgorithm = staticStructuralLearningAlgorithm;
    }

    public static BayesianNetwork learnParameters(DAG dag, DataStream<DataInstance> dataStream){
        return staticParameterLearningAlgorithm.learn(dag,dataStream);
    }


    public static DAG learnDAG(DataStream<DataInstance> dataStream){
        return staticStructuralLearningAlgorithm.learn(dataStream);
    }


    public static BayesianNetwork learnStaticModel(DataStream<DataInstance> database){

        Stopwatch watch = Stopwatch.createStarted();
        DAG dag = staticStructuralLearningAlgorithm.learn(database);
        System.out.println("Structural Learning : " + watch.stop());

        watch = Stopwatch.createStarted();
        BayesianNetwork network = staticParameterLearningAlgorithm.learn(dag,database);
        System.out.println("Parameter Learning: " + watch.stop());

        return network;
    }


    public static void main(String[] args) throws Exception{

//        String dataFile = new String("./datasets/Pigs.arff");
//        DataStream<StaticDataInstance> data = new StaticDataOnDiskFromFile(new ARFFDataReader(dataFile));
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
