package eu.amidst.core.learning;

import eu.amidst.core.database.DataBase;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

import java.util.Random;

/**
 * Created by andresmasegosa on 06/01/15.
 */
public class NaiveBayesClassifier {

    int classVarID;
    BayesianNetwork bnModel;

    public int getClassVarID() {
        return classVarID;
    }

    public void setClassVarID(int classVarID) {
        this.classVarID = classVarID;
    }

    public BayesianNetwork getBNModel() {
        return bnModel;
    }

    private DAG staticNaiveBayesStructure(DataBase dataBase){
        StaticVariables modelHeader = new StaticVariables(dataBase.getAttributes());
        Variable classVar = modelHeader.getVariableById(this.getClassVarID());
        DAG dag = new DAG(modelHeader);
        dag.getParentSets().parallelStream().filter(w -> w.getMainVar().getVarID() != classVar.getVarID()).forEach(w -> w.addParent(classVar));
        return dag;
    }

    public void learn(DataBase dataBase){
        LearningEngine.setStaticStructuralLearningAlgorithm(this::staticNaiveBayesStructure);
        LearningEngine.setStaticParameterLearningAlgorithm(MaximumLikelihood::learnParametersStaticModel);
        bnModel = LearningEngine.learnStaticModel(dataBase);

        //DAG dag = this.staticNaiveBayesStructure(dataBase);
        //bnModel = MaximumLikelihood.learnParametersStaticModel(dag,dataBase);
    }

    public static void main(String[] args){

        BayesianNetworkGenerator.setNumberOfContinuousVars(0);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(50000);
        BayesianNetworkGenerator.setNumberOfStates(10);
        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(new Random(0));

        int sampleSize = 100;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setParallelMode(false);
        DataBase data =  sampler.sampleToDataBase(sampleSize);

        for (int i = 1; i <= 10; i++) {
            NaiveBayesClassifier model = new NaiveBayesClassifier();
            model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 1);
            model.learn(data);
            BayesianNetwork nbClassifier = model.getBNModel();
        }

    }
}
