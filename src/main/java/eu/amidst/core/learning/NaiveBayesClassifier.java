package eu.amidst.core.learning;

import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.filereaders.StaticDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

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
        LearningEngine.setStaticParameterLearningAlgorithm(MaximumLikelihood::parallelLearnStatic);
        bnModel = LearningEngine.learnStaticModel(dataBase);
    }

    public static void main(String[] args){

        String dataFile = new String("./datasets/Pigs.arff");
        DataBase data = new StaticDataOnDiskFromFile(new ARFFDataReader(dataFile));

        NaiveBayesClassifier model = new NaiveBayesClassifier();
        model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 1);

        model.learn(data);

        BayesianNetwork nbClassifier = model.getBNModel();

    }
}
