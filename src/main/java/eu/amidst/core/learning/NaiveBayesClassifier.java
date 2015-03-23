package eu.amidst.core.learning;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 06/01/15.
 */
public class NaiveBayesClassifier {

    int classVarID;
    BayesianNetwork bnModel;
    boolean parallelMode = true;


    public boolean isParallelMode() {
        return parallelMode;
    }

    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
    }

    public int getClassVarID() {
        return classVarID;
    }

    public void setClassVarID(int classVarID) {
        this.classVarID = classVarID;
    }

    public BayesianNetwork getBNModel() {
        return bnModel;
    }

    private DAG staticNaiveBayesStructure(DataStream<DataInstance> dataStream){
        StaticVariables modelHeader = new StaticVariables(dataStream.getAttributes());
        Variable classVar = modelHeader.getVariableById(this.getClassVarID());
        DAG dag = new DAG(modelHeader);
        if (parallelMode)
            dag.getParentSets().parallelStream().filter(w -> w.getMainVar().getVarID() != classVar.getVarID()).forEach(w -> w.addParent(classVar));
        else
            dag.getParentSets().stream().filter(w -> w.getMainVar().getVarID() != classVar.getVarID()).forEach(w -> w.addParent(classVar));

        return dag;
    }

    public void learn(DataStream<DataInstance> dataStream){
        LearningEngineForBN.setStaticStructuralLearningAlgorithm(this::staticNaiveBayesStructure);
        LearningEngineForBN.setStaticParameterLearningAlgorithm(MaximumLikelihoodForBN::learnParametersStaticModel);
        MaximumLikelihoodForBN.setParallelMode(this.isParallelMode());
        bnModel = LearningEngineForBN.learnStaticModel(dataStream);
        //DAG dag = this.staticNaiveBayesStructure(dataStream);
        //bnModel = MaximumLikelihood.learnParametersStaticModel(dag,dataStream);
    }

    public static void main(String[] args){

        BayesianNetworkGenerator.setNumberOfContinuousVars(0);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(50000);
        BayesianNetworkGenerator.setNumberOfStates(10);
        BayesianNetworkGenerator.setSeed(0);

        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);

        int sampleSize = 100;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        DataStream<DataInstance> data =  sampler.sampleToDataBase(sampleSize);

        for (int i = 1; i <= 10; i++) {
            NaiveBayesClassifier model = new NaiveBayesClassifier();
            model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 1);
            model.learn(data);
            BayesianNetwork nbClassifier = model.getBNModel();
        }

    }
}
