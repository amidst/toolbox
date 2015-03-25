package eu.amidst.core.learning.dynamic;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataWriter;
import eu.amidst.core.io.DynamicDataStreamLoader;
import eu.amidst.core.learning.MaximumLikelihoodForBN;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.DynamicVariables;
import eu.amidst.core.variables.Variable;

import java.io.IOException;

/**
 * Created by andresmasegosa on 06/01/15.
 */
public class DynamicNaiveBayesClassifier {

    int classVarID;
    DynamicBayesianNetwork bnModel;
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

    //TODO: Consider the case where the dynamic data base have TIME_ID and SEQ_ID
    public void setClassVarID(int classVarID) {
        this.classVarID = classVarID;
    }

    public DynamicBayesianNetwork getDynamicBNModel() {
        return bnModel;
    }

    private DynamicDAG dynamicNaiveBayesStructure(DataStream<DynamicDataInstance> dataStream){

        DynamicVariables modelHeader = new DynamicVariables(dataStream.getAttributes());
        Variable classVar = modelHeader.getVariableById(this.getClassVarID());
        DynamicDAG dag = new DynamicDAG(modelHeader);

        // TODO Remove this commented part. Done for efficiency in the inference demo.

        dag.getParentSetsTimeT().stream()
                .filter(w -> w.getMainVar().getVarID() != classVar.getVarID())
                .forEach(w -> {
                    w.addParent(classVar);
                    //w.addParent(modelHeader.getInterfaceVariable(w.getMainVar()));
                });


        dag.getParentSetTimeT(classVar).addParent(modelHeader.getInterfaceVariable(classVar));

        return dag;
    }

    public void learn(DataStream<DynamicDataInstance> dataStream){
        LearningEngineForDBN.setDynamicStructuralLearningAlgorithm(this::dynamicNaiveBayesStructure);
        MaximumLikelihoodForBN.setParallelMode(this.isParallelMode());
        LearningEngineForDBN.setDynamicParameterLearningAlgorithm(MaximumLikelihoodForDBN::learnDynamic);
        bnModel = LearningEngineForDBN.learnDynamicModel(dataStream);
    }

    public static void main(String[] args) throws IOException {

        BayesianNetworkGenerator.setNumberOfContinuousVars(0);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(5);
        BayesianNetworkGenerator.setNumberOfStates(2);
        BayesianNetworkGenerator.setSeed(0);
        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);

        int sampleSize = 1000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        String file = "./datasets/randomdata.arff";
        DataStream<DataInstance> dataStream = sampler.sampleToDataBase(sampleSize);
        ARFFDataWriter.writeToARFFFile(dataStream, file);

        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(file);

        for (int i = 1; i <= 1; i++) {
            DynamicNaiveBayesClassifier model = new DynamicNaiveBayesClassifier();
            model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 1);
            model.setParallelMode(true);
            model.learn(data);
            DynamicBayesianNetwork nbClassifier = model.getDynamicBNModel();
            System.out.println(nbClassifier.toString());
        }

    }
}
