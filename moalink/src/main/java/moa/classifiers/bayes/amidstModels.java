package moa.classifiers.bayes;

import eu.amidst.core.datastream.*;
import eu.amidst.core.datastream.filereaders.arffWekaReader.DataRowWeka;
import eu.amidst.core.datastream.filereaders.DataInstanceImpl;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.InferenceEngineForBN;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.StateSpaceType;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;
import eu.amidst.ida2015.NaiveBayesConceptDrift;
import moa.classifiers.AbstractClassifier;
import moa.core.InstancesHeader;
import moa.core.Measurement;
import moa.options.FloatOption;
import moa.options.IntOption;
import moa.options.MultiChoiceOption;
import weka.core.Instance;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by ana@cs.aau.dk on 20/04/15.
 */
public class amidstModels extends AbstractClassifier {

    private static final long serialVersionUID = 1L;


    /**
     * Parameters of the amidst model
     */

    String[] driftModes = new String[]{
            "GLOBAL",
            "LOCAL",
            "GLOBAL_LOCAL"};

    public MultiChoiceOption driftDetectorOption = new MultiChoiceOption(
            "driftDetector", 'd', "Drift detector type.", new String[]{
            "GLOBAL", "LOCAL", "GLOBAL_LOCAL"}, new String[]{
            "GLOBAL",
            "LOCAL",
            "GLOBAL_LOCAL"}, 0);

    public IntOption windowSizeOption = new IntOption("windowSize",
            'w', "Size of the window in which to apply variational Bayes",
            100);
    protected  int windowSize_ = 100;

    public FloatOption transitionVarianceOption = new FloatOption("transitionVariance",
            'v', "Transition variance for the global hidden variable.",
            0.1);
    protected double transitionVariance_ = 0.1;


    /**
     * Private fields
     */
    //private StreamingVariationalBayesVMP svb_ = null;

    NaiveBayesConceptDrift nb_ = null;

    private Variable classVar_ = null;

    private Attributes attributes_ = null;


    private BayesianNetwork learntBN_ = null;


    DataOnMemoryListContainer<DataInstance> batch_ = null;

    private int count_ = 0;

    public int getWindowSize_() {
        return windowSize_;
    }

    public void setWindowSize_(int windowSize_) {
        this.windowSize_ = windowSize_;
    }

    public double getTransitionVariance_() {
        return transitionVariance_;
    }

    public void setTransitionVariance_(double transitionVariance_) {
        this.transitionVariance_ = transitionVariance_;
    }

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

        nb_ = new NaiveBayesConceptDrift();

        convertAttributes();
        batch_ = new DataOnMemoryListContainer(attributes_);

        nb_.setData(batch_);
        nb_.setSeed(randomSeed);//Note that the default value is 1
        nb_.setClassIndex(-1);
        nb_.setWindowsSize(windowSize_);
        nb_.setTransitionVariance(transitionVariance_);
        nb_.setConceptDriftDetector(NaiveBayesConceptDrift.DriftDetector.valueOf(this.driftModes[driftDetectorOption.getChosenIndex()]));

        nb_.learnDAG();

        System.out.println(nb_.getLearntBayesianNetwork().getDAG().toString());

    }

    private void convertAttributes(){
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


    @Override
    public void trainOnInstanceImpl(Instance inst) {

        DataInstance dataInstance = new DataInstanceImpl(new DataRowWeka(inst));
        if(count_ < windowSize_) {
            batch_.add(dataInstance);
            count_++;
        }else{
            count_ = 0;
            nb_.updateModel(batch_);
            batch_ = new DataOnMemoryListContainer(attributes_);
            learntBN_ = nb_.getLearntBayesianNetwork();
            System.out.println(learntBN_.toString());
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

    public static void main(String[] args){
    }

}
