package moa.classifiers.bayes;

import eu.amidst.core.datastream.*;
import eu.amidst.core.datastream.filereaders.arffWekaReader.DataRowWeka;
import eu.amidst.core.datastream.filereaders.DataInstanceImpl;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.InferenceEngineForBN;
import eu.amidst.core.learning.StreamingVariationalBayesVMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.StateSpaceType;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;
import eu.amidst.ida2015.GlobalHiddenTransitionMethod;
import eu.amidst.ida2015.PlateuGlobalHiddenConceptDrift;
import moa.classifiers.AbstractClassifier;
import moa.core.InstancesHeader;
import moa.core.Measurement;
import weka.core.Instance;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by ana@cs.aau.dk on 20/04/15.
 */
public class amidstModels extends AbstractClassifier {

    private static final long serialVersionUID = 1L;

    public enum Model{
        GLOBAL_, LOCAL, GLOBAL_LOCAL
    }


    /**
     * Parameters of the amidst model
     */
    public Model modelType_ = Model.GLOBAL_;

    public int windowSize_ = 100;

    public int count_;

    public double meanStart_ = 0;

    public double noise_ = 0.1;

    public boolean globalDynamic_ = true;

    /**
     * Private fields
     */
    private StreamingVariationalBayesVMP svb_ = null;

    private DAG dag_ = null;

    private Variable classVar_ = null;

    private Variable globalHiddenVar_ = null;

    private Attributes attributes_ = null;

    //private BayesianNetwork network_ = null;

    private BayesianNetwork learntBN_ = null;


    DataOnMemoryListContainer<DataInstance> batch_ = null;

    @Override
    public String getPurposeString() {
        return "Amidst concept-drift classifier: performs concept-drift detection with a BN model plus one or several hidden variables.";
    }

    @Override
    public void resetLearningImpl() {

    }

    @Override
    public void setModelContext(InstancesHeader ih) {
        super.setModelContext(ih);

        createDAG();

        switch (modelType_){
            case GLOBAL_:
                initializeGLOBALmodel();
                break;
            case LOCAL:
                initializeLOCALmodel();
                break;
            case GLOBAL_LOCAL:
                initializeGLOBAL_LOCALmodel();
                break;
            default:
                System.out.println("The options for modelType are GLOBAL_, LOCAL and GLOBAL_LOCAL");
                break;
        }

        //learntBN_= BayesianNetwork.newBayesianNetwork(dag_);
        //learntBN_.randomInitialization(classifierRandom);

        System.out.println(dag_.toString());

        count_ = windowSize_;
        batch_ = new DataOnMemoryListContainer<>(attributes_);

        svb_.setSeed(randomSeed);  //Note that the default value is 1
        svb_.setWindowsSize(windowSize_);
        svb_.setDAG(dag_);
        svb_.initLearning();
    }

    private void createDAG(){
        weka.core.Attribute attrWeka;
        Enumeration attributesWeka = modelContext.enumerateAttributes();
        List<Attribute> attrList = new ArrayList<>();
        /* Predictive attributes */
        while (attributesWeka.hasMoreElements()) {
            attrWeka = (weka.core.Attribute) attributesWeka.nextElement();
            convertAttribute(attrWeka,attrList);
        }
        /* Class attribute */
        convertAttribute(modelContext.classAttribute(), attrList);
        attributes_ = new Attributes(attrList);
        StaticVariables variables = new StaticVariables(attributes_);
        String className = modelContext.classAttribute().name();
        classVar_ = variables.getVariableByName(className);
        globalHiddenVar_ =  variables.newGaussianVariable("Global");

        /* Create NB structure */
        dag_ = new DAG(variables);
    }

    private void convertAttribute(weka.core.Attribute attrWeka, List<Attribute> attrs){
        StateSpaceType stateSpaceTypeAtt;
        if(attrWeka.isNominal()){
            String[] vals = new String[attrWeka.numValues()];
            for (int i=0; i<attrWeka.numValues(); i++) {
                vals[i] = attrWeka.value(i);
            }
            stateSpaceTypeAtt = new FiniteStateSpace(attrWeka.numValues());
        }else{
            stateSpaceTypeAtt = new RealStateSpace();
        }
        Attribute att = new Attribute(attrWeka.index(),attrWeka.name(), stateSpaceTypeAtt);
        attrs.add(att);
    }

    private void initializeGLOBALmodel(){
        dag_.getParentSets().stream()
                .filter(parentSet -> !parentSet.getMainVar().equals(classVar_) && !parentSet.getMainVar().equals(globalHiddenVar_))
                .forEach(w -> {
                    w.addParent(classVar_);
                    w.addParent(globalHiddenVar_);
                });

        svb_ = new StreamingVariationalBayesVMP();
        svb_.setPlateuStructure(new PlateuGlobalHiddenConceptDrift(globalHiddenVar_, globalDynamic_));
        svb_.setTransitionMethod(new GlobalHiddenTransitionMethod(globalHiddenVar_, meanStart_, noise_));
    }

    private void initializeLOCALmodel(){return;}

    private void initializeGLOBAL_LOCALmodel(){return;}



    @Override
    public void trainOnInstanceImpl(Instance inst) {

        DataInstance dataInstance = new DataInstanceImpl(new DataRowWeka(inst));
        if(count_ < windowSize_) {
            batch_.add(dataInstance);
            count_++;
        }else{
            count_ = 0;
            svb_.updateModel(batch_);
            batch_ = new DataOnMemoryListContainer(attributes_);
            learntBN_ = svb_.getLearntBayesianNetwork();
        }
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {

        if(learntBN_ == null) {
            double[] votes = new double[classVar_.getNumberOfStates()];
            for (int i = 0; i < votes.length; i++) {
                votes[i] = 1.0/votes.length;
            }
            return votes;
        }

        InferenceEngineForBN.setModel(learntBN_);

        DataInstance dataInstance = new DataInstanceImpl(new DataRowWeka(inst));

        double realValue = dataInstance.getValue(classVar_);
        dataInstance.setValue(classVar_, Utils.missingValue());
        InferenceEngineForBN.setEvidence(dataInstance);
        InferenceEngineForBN.runInference();
        Multinomial posterior = InferenceEngineForBN.getPosterior(classVar_);
        dataInstance.setValue(classVar_, realValue);

        return posterior.getProbabilities();
    }


    @Override
    public void getModelDescription(StringBuilder out, int indent) {

    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    @Override
    public boolean isRandomizable() {
        return true;
    }


}
