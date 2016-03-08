package eu.amidst.ecml2016;

import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import COM.hugin.HAPI.NodeList;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.inference.DynamicMAPInference;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.huginlink.inference.HuginInference;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by dario on 08/03/16.
 */
public class ComparisonToHugin {

    public static void main(String[] args) {

        int seed = 8648462;

        int nTimeSteps=7;



        Random random = new Random(seed);

        HiddenLayerDynamicModel model = new HiddenLayerDynamicModel();

        model.setnHiddenContinuousVars(1);


        model.setnStatesClassVar(2);
        model.setnStatesHidden(2);
        model.setnStates(2);

        model.setnHiddenContinuousVars(1);
        model.setnObservableDiscreteVars(1);
        model.setnObservableContinuousVars(1);

        model.generateModel();


        model.printDAG();

        model.setSeed(random.nextInt());
        model.randomInitialization(random);
        model.setProbabilityOfKeepingClass(0.8);

        model.printHiddenLayerModel();


        DynamicBayesianNetwork DBNmodel = model.getModel();
        model.generateEvidence(nTimeSteps);

        List<DynamicAssignment> evidence = model.getEvidenceNoClass();
        evidence.forEach(dynamicAssignment -> System.out.println(dynamicAssignment.outputString(DBNmodel.getDynamicVariables().getListOfDynamicVariables())));

        System.out.println("\n\n");

        DynamicMAPInference dynMAP = new DynamicMAPInference();

        Variable MAPVariable = model.getClassVariable();
        dynMAP.setModel(DBNmodel);
        dynMAP.setMAPvariable(MAPVariable);
        dynMAP.setNumberOfTimeSteps(nTimeSteps);

        //dynMAP.setEvidence(evidence);


        BayesianNetwork staticModel = dynMAP.getUnfoldedStaticModel();
        Assignment staticEvidence = dynMAP.getUnfoldedEvidence();


        //System.out.println(staticModel.toString());

        //System.out.println("STATIC VARIABLES");
        //staticModel.getVariables().getListOfVariables().forEach(variable -> System.out.println(variable.getName()));

        System.out.println("STATIC EVIDENCE:");
        //System.out.println(staticEvidence.outputString(staticModel.getVariables().getListOfVariables())+"\n");

        System.out.println("A");

        HuginInference huginInference = new HuginInference();
        huginInference.setModel(staticModel);

        System.out.println("B");

        huginInference.setEvidence(staticEvidence);

        Domain huginBN = huginInference.getHuginBN();

        NodeList classVarReplications = new NodeList();

        System.out.println("C");

        try {
            huginBN.initialize();

            //System.out.println(huginBN.getNodes().toString());
            huginBN.getNodes().stream().filter(node -> {
                try {
                    return node.getName().contains(MAPVariable.getName());
                }
                catch (ExceptionHugin e) {
                    System.out.println(e.getMessage());
                    return false;
                }
            }).forEach(classVarReplications::add);

            System.out.println(classVarReplications.toString());

            huginBN.findMAPConfigurations(classVarReplications, 0.0001);

            System.out.println("HUGIN MAP Sequence:");
            System.out.println(Arrays.toString(huginBN.getMAPConfiguration(0)) + " with probability " + huginBN.getProbabilityOfMAPConfiguration(0));
        }
        catch (ExceptionHugin e) {
            System.out.println("\nHUGIN EXCEPTION:");
            System.out.println(e.getMessage());
        }

        dynMAP.setNumberOfMergedClassVars(3);
        dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.IS);
        System.out.println("DynMAP (Grouped-IS) Sequence:");
        System.out.println(Arrays.toString(dynMAP.getMAPsequence()));

        dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.VMP);
        System.out.println("DynMAP (Grouped-VMP) Sequence:");
        System.out.println(Arrays.toString(dynMAP.getMAPsequence()));

        dynMAP.runInferenceUngroupedMAPVariable(DynamicMAPInference.SearchAlgorithm.IS);
        System.out.println("DynMAP (Ungrouped-IS) Sequence:");
        System.out.println(Arrays.toString(dynMAP.getMAPsequence()));

        dynMAP.runInferenceUngroupedMAPVariable(DynamicMAPInference.SearchAlgorithm.VMP);
        System.out.println("DynMAP (Ungrouped-VMP) Sequence:");
        System.out.println(Arrays.toString(dynMAP.getMAPsequence()));
    }

}
