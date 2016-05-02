package eu.amidst.ecai2016;

import COM.hugin.HAPI.*;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.EF_Multinomial;
import eu.amidst.core.exponentialfamily.MomentParameters;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.inference.DynamicMAPInference;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.huginlink.converters.BNConverterToHugin;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by dario on 08/03/16.
 */
public class PosteriorDistributionsComparisonToHugin {

    final static int maxTimeStepsHugin=12;

    public static void main(String[] args) {

        //BasicConfigurator.configure();
        VMP dumb_vmp = new VMP();


        String outputDirectory = "/Users/dario/Desktop/dynMapNetworks/";

        int seedModel = 3634743;
        Random randomModel = new Random(seedModel);

        int seedEvidence = 935935;
        Random randomEvidence= new Random(seedEvidence);


        int nTimeSteps=6;
        int nSamplesForIS=20000;


        System.out.println("seedModel: " + seedModel);
        System.out.println("seedEvidence: " + seedEvidence);
        System.out.println("nSamplesIS: " + nSamplesForIS);



        HiddenLayerStronglyCorrelatedDynamicModel model = new HiddenLayerStronglyCorrelatedDynamicModel();

        model.setnStatesClassVar(2);
        model.setnStatesHidden(2);
        model.setnStates(2);

        model.setnHiddenContinuousVars(2);
        model.setnObservableDiscreteVars(2);
        model.setnObservableContinuousVars(2);

        model.generateModel();
        model.printDAG();



        model.randomInitialization(randomModel);
        double probKeepingClassState = 0.7;
        model.setProbabilityOfKeepingClass(probKeepingClassState);

        DynamicBayesianNetwork DBNmodel = model.getModel();
        System.out.println(DBNmodel.toString());



        model.setSeed(randomEvidence.nextInt());
        model.generateEvidence(nTimeSteps);

        List<DynamicAssignment> evidenceClass = model.getFullEvidence();
        System.out.println("FULL EVIDENCE:");
        evidenceClass.forEach(dynamicAssignment -> System.out.println(dynamicAssignment.outputString(DBNmodel.getDynamicVariables().getListOfDynamicVariables())));
        System.out.println("\n");

//        List<DynamicAssignment> evidence = null;
        List<DynamicAssignment> evidence = model.getEvidenceNoClass();
        System.out.println("EVIDENCE:");
        evidence.forEach(dynamicAssignment -> System.out.println(dynamicAssignment.outputString(DBNmodel.getDynamicVariables().getListOfDynamicVariables())));
        System.out.println("\n");


        DynamicMAPInference dynMAP = new DynamicMAPInference();

        Variable MAPVariable = model.getClassVariable();
        dynMAP.setModel(DBNmodel);
        dynMAP.setMAPvariable(MAPVariable);
        dynMAP.setNumberOfTimeSteps(nTimeSteps);

        dynMAP.setEvidence(evidence);

        BayesianNetwork staticModel = dynMAP.getUnfoldedStaticModel();
        Assignment staticEvidence = (evidence==null ? null : dynMAP.getUnfoldedEvidence());





        /////////////////////////////////////////////////
        // UNGROUPED VARIABLES WITH I.S.
        /////////////////////////////////////////////////
        System.out.println("UNGROUPED IS");
        final BayesianNetwork unfoldedStaticModel1 = dynMAP.getUnfoldedStaticModel();
        //unfoldedStaticModel1.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("ClassVar")).forEachOrdered(variable -> System.out.println(unfoldedStaticModel1.getConditionalDistribution(variable).toString()));


        try {
            BNConverterToHugin.convertToHugin(unfoldedStaticModel1).saveAsNet(outputDirectory + "model_ungrouped.net");
        }
        catch (ExceptionHugin e) {
            System.out.println(e.toString());
        }

        dynMAP = new DynamicMAPInference();
        dynMAP.setModel(DBNmodel);
        dynMAP.setMAPvariable(MAPVariable);
        dynMAP.setNumberOfTimeSteps(nTimeSteps);


        dynMAP.setSampleSize(nSamplesForIS);
        dynMAP.setEvidence(evidence);

        dynMAP.runInferenceUngroupedMAPVariable(DynamicMAPInference.SearchAlgorithm.IS);
        List<UnivariateDistribution> posteriors_ungroupedIS = dynMAP.getUngroupedPosteriorDistributions();

        System.out.println("Ungrouped IS finished");
        System.out.println("\n\n");


        /////////////////////////////////////////////////
        // UNGROUPED VARIABLES WITH VMP
        /////////////////////////////////////////////////
        System.out.println("UNGROUPED VMP");
        final BayesianNetwork unfoldedStaticModel2 = dynMAP.getUnfoldedStaticModel();
        //unfoldedStaticModel2.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("ClassVar")).forEachOrdered(variable -> System.out.println(unfoldedStaticModel2.getConditionalDistribution(variable).toString()));


        dynMAP = new DynamicMAPInference();
        dynMAP.setModel(DBNmodel);
        dynMAP.setMAPvariable(MAPVariable);
        dynMAP.setNumberOfTimeSteps(nTimeSteps);
        dynMAP.setEvidence(evidence);

        dynMAP.runInferenceUngroupedMAPVariable(DynamicMAPInference.SearchAlgorithm.VMP);
        List<UnivariateDistribution> posteriors_ungroupedVMP = dynMAP.getUngroupedPosteriorDistributions();

        System.out.println("Ungrouped VMP finished");
        System.out.println("\n\n");

        //System.out.println("UNGROUPED MODEL");
        //System.out.println(unfoldedStaticModel1);

        /////////////////////////////////////////////////
        // 2-GROUPED VARIABLES WITH I.S.
        /////////////////////////////////////////////////
        System.out.println("2-GROUPED IS");
        System.out.println("Grouped posteriors:");

        dynMAP = new DynamicMAPInference();
        dynMAP.setModel(DBNmodel);
        dynMAP.setMAPvariable(MAPVariable);
        dynMAP.setNumberOfTimeSteps(nTimeSteps);

        dynMAP.setSampleSize(nSamplesForIS);


        dynMAP.setNumberOfMergedClassVars(2);
        dynMAP.computeMergedClassVarModels();

        dynMAP.setEvidence(evidence);


        BayesianNetwork model_2grouped_0 = dynMAP.getMergedClassVarModels().get(0);
        BayesianNetwork model_2grouped_1 = dynMAP.getMergedClassVarModels().get(1);


        try {
            BNConverterToHugin.convertToHugin(model_2grouped_0).saveAsNet(outputDirectory + "model_2grouped_0.net");
            BNConverterToHugin.convertToHugin(model_2grouped_1).saveAsNet(outputDirectory + "model_2grouped_1.net");
        }
        catch (ExceptionHugin e) {
            System.out.println(e.toString());
        }

        dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.IS);
        List<List<UnivariateDistribution>> posteriors_2groupedIS = dynMAP.getGroupedPosteriorDistributions();

        System.out.println("2-grouped IS finished");
        System.out.println("\n\n");


        /////////////////////////////////////////////////
        // 2-GROUPED VARIABLES WITH VMP
        /////////////////////////////////////////////////
        System.out.println("2-GROUPED VMP");
        System.out.println("Grouped posteriors:");

        dynMAP = new DynamicMAPInference();
        dynMAP.setModel(DBNmodel);
        dynMAP.setMAPvariable(MAPVariable);
        dynMAP.setNumberOfTimeSteps(nTimeSteps);

        dynMAP.setNumberOfMergedClassVars(2);
        dynMAP.computeMergedClassVarModels();

        dynMAP.setEvidence(evidence);

        dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.VMP);
        List<List<UnivariateDistribution>> posteriors_2groupedVMP = dynMAP.getGroupedPosteriorDistributions();


        System.out.println("2-grouped VMP finished");
        System.out.println("\n\n");


        //System.out.println("2-GROUPED MODEL 0");
        //System.out.println(model_2grouped_0);

        //System.out.println("2-GROUPED MODEL 1");
        //System.out.println(model_2grouped_1);

//        final BayesianNetwork unfoldedStaticModel5 = dynMAP.getMergedClassVarModels().get(0);
        //unfoldedStaticModel5.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("GROUPED")).forEachOrdered(variable -> System.out.println(unfoldedStaticModel5.getConditionalDistribution(variable).toString()));

//        final BayesianNetwork unfoldedStaticModel6 = dynMAP.getMergedClassVarModels().get(1);
//        unfoldedStaticModel6.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("GROUPED")).forEachOrdered(variable -> System.out.println(unfoldedStaticModel6.getConditionalDistribution(variable).toString()));




//
//        /////////////////////////////////////////////////
//        // 4-GROUPED VARIABLES WITH I.S.
//        /////////////////////////////////////////////////
//        dynMAP = new DynamicMAPInference();
//        dynMAP.setModel(DBNmodel);
//        dynMAP.setMAPvariable(MAPVariable);
//        dynMAP.setNumberOfTimeSteps(nTimeSteps);
//
//        dynMAP.setNumberOfStartingPoints(nSamplesForIS);
//
//
//        dynMAP.setNumberOfMergedClassVars(4);
//        dynMAP.computeMergedClassVarModels();
//
//        dynMAP.setEvidence(evidence);
//
////        final BayesianNetwork unfoldedStaticModel3 = dynMAP.getMergedClassVarModels().get(0);
////        unfoldedStaticModel3.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("GROUPED")).forEachOrdered(variable -> System.out.println(unfoldedStaticModel3.getConditionalDistribution(variable).toString()));
////        final BayesianNetwork unfoldedStaticModel4 = dynMAP.getMergedClassVarModels().get(1);
////        unfoldedStaticModel4.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("GROUPED")).forEachOrdered(variable -> System.out.println(unfoldedStaticModel4.getConditionalDistribution(variable).toString()));
//
//        BayesianNetwork model_4grouped = dynMAP.getMergedClassVarModels().get(0);
//
//        dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.IS);
//        List<List<UnivariateDistribution>> posteriors_4groupedIS = dynMAP.getGroupedPosteriorDistributions();
//
//        System.out.println("4-grouped IS finished");
//        System.out.println("\n\n");
//
//
//        /////////////////////////////////////////////////
//        // 4-GROUPED VARIABLES WITH VMP
//        /////////////////////////////////////////////////
//        dynMAP = new DynamicMAPInference();
//        dynMAP.setModel(DBNmodel);
//        dynMAP.setMAPvariable(MAPVariable);
//        dynMAP.setNumberOfTimeSteps(nTimeSteps);
//
//        dynMAP.setNumberOfMergedClassVars(4);
//        dynMAP.computeMergedClassVarModels();
//
//        dynMAP.setEvidence(evidence);
//
//        dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.VMP);
//        List<List<UnivariateDistribution>> posteriors_4groupedVMP = dynMAP.getGroupedPosteriorDistributions();
//
//
//        System.out.println("4-grouped VMP finished");
//        System.out.println("\n\n");
//
//
//
//
//        /////////////////////////////////////////////////
//        // 6-GROUPED VARIABLES WITH I.S.
//        /////////////////////////////////////////////////
//        dynMAP = new DynamicMAPInference();
//        dynMAP.setModel(DBNmodel);
//        dynMAP.setMAPvariable(MAPVariable);
//        dynMAP.setNumberOfTimeSteps(nTimeSteps);
//
//        dynMAP.setNumberOfStartingPoints(nSamplesForIS);
//
//
//        dynMAP.setNumberOfMergedClassVars(6);
//        dynMAP.computeMergedClassVarModels();
//
//        dynMAP.setEvidence(evidence);
//
////        final BayesianNetwork unfoldedStaticModel3 = dynMAP.getMergedClassVarModels().get(0);
////        unfoldedStaticModel3.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("GROUPED")).forEachOrdered(variable -> System.out.println(unfoldedStaticModel3.getConditionalDistribution(variable).toString()));
////        final BayesianNetwork unfoldedStaticModel4 = dynMAP.getMergedClassVarModels().get(1);
////        unfoldedStaticModel4.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("GROUPED")).forEachOrdered(variable -> System.out.println(unfoldedStaticModel4.getConditionalDistribution(variable).toString()));
//
//        BayesianNetwork model_6grouped = dynMAP.getMergedClassVarModels().get(0);
//
//        dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.IS);
//        List<List<UnivariateDistribution>> posteriors_6groupedIS = dynMAP.getGroupedPosteriorDistributions();
//
//        System.out.println("6-grouped IS finished");
//        System.out.println("\n\n");
//
//
//        /////////////////////////////////////////////////
//        // 6-GROUPED VARIABLES WITH VMP
//        /////////////////////////////////////////////////
//        dynMAP = new DynamicMAPInference();
//        dynMAP.setModel(DBNmodel);
//        dynMAP.setMAPvariable(MAPVariable);
//        dynMAP.setNumberOfTimeSteps(nTimeSteps);
//
//        dynMAP.setNumberOfMergedClassVars(6);
//        dynMAP.computeMergedClassVarModels();
//
//        dynMAP.setEvidence(evidence);
//
//        dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.VMP);
//        List<List<UnivariateDistribution>> posteriors_6groupedVMP = dynMAP.getGroupedPosteriorDistributions();
//
//
//        System.out.println("6-grouped VMP finished");
//        System.out.println("\n\n");
//
//

//
//        /////////////////////////////////////////////////
//        // 10-GROUPED VARIABLES WITH I.S.
//        /////////////////////////////////////////////////
//        dynMAP = new DynamicMAPInference();
//        dynMAP.setModel(DBNmodel);
//        dynMAP.setMAPvariable(MAPVariable);
//        dynMAP.setNumberOfTimeSteps(nTimeSteps);
//
//        dynMAP.setNumberOfStartingPoints(nSamplesForIS);
//
//
//        dynMAP.setNumberOfMergedClassVars(10);
//        dynMAP.computeMergedClassVarModels();
//
//        dynMAP.setEvidence(evidence);
//
////        final BayesianNetwork unfoldedStaticModel3 = dynMAP.getMergedClassVarModels().get(0);
////        unfoldedStaticModel3.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("GROUPED")).forEachOrdered(variable -> System.out.println(unfoldedStaticModel3.getConditionalDistribution(variable).toString()));
////        final BayesianNetwork unfoldedStaticModel4 = dynMAP.getMergedClassVarModels().get(1);
////        unfoldedStaticModel4.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("GROUPED")).forEachOrdered(variable -> System.out.println(unfoldedStaticModel4.getConditionalDistribution(variable).toString()));
//
//        BayesianNetwork model_10grouped = dynMAP.getMergedClassVarModels().get(0);
//
//        dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.IS);
//        List<List<UnivariateDistribution>> posteriors_10groupedIS = dynMAP.getGroupedPosteriorDistributions();
//
//        System.out.println("10-grouped IS finished");
//        System.out.println("\n\n");
//
//
//        /////////////////////////////////////////////////
//        // 10-GROUPED VARIABLES WITH VMP
//        /////////////////////////////////////////////////
//        dynMAP = new DynamicMAPInference();
//        dynMAP.setModel(DBNmodel);
//        dynMAP.setMAPvariable(MAPVariable);
//        dynMAP.setNumberOfTimeSteps(nTimeSteps);
//
//        dynMAP.setNumberOfMergedClassVars(10);
//        dynMAP.computeMergedClassVarModels();
//
//        dynMAP.setEvidence(evidence);
//
//        dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.VMP);
//        List<List<UnivariateDistribution>> posteriors_10groupedVMP = dynMAP.getGroupedPosteriorDistributions();
//
//
//        System.out.println("10-grouped VMP finished");
//        System.out.println("\n\n");




        System.out.println("\n\n");

        if (nTimeSteps<=maxTimeStepsHugin) {

            try {
                Domain huginBN = BNConverterToHugin.convertToHugin(staticModel);
                huginBN.compile();
//                System.out.println("HUGIN Domain compiled");

                if(staticEvidence!=null) {
                    staticEvidence.getVariables().forEach(variable -> {
                        if (variable.isMultinomial()) {
                            try {
                                ((DiscreteNode) huginBN.getNodeByName(variable.getName())).selectState((int) staticEvidence.getValue(variable));
                            } catch (ExceptionHugin e) {
                                System.out.println(e.getMessage());
                            }
                        } else if (variable.isNormal()) {
                            try {
                                ((ContinuousChanceNode) huginBN.getNodeByName(variable.getName())).enterValue(staticEvidence.getValue(variable));
                            } catch (ExceptionHugin e) {
                                System.out.println(e.getMessage());
                            }
                        } else {
                            throw new IllegalArgumentException("Variable type not allowed.");
                        }
                    });
                }

//                System.out.println("HUGIN Evidence set");

                huginBN.propagate(Domain.H_EQUILIBRIUM_SUM, Domain.H_EVIDENCE_MODE_NORMAL);


                System.out.println("POSTERIOR DISTRIBUTIONS FOR UNGROUPED CLASS VARIABLES");
                double totalDivergenceIS = 0;
                double totalDivergenceVMP = 0;
                for (int i = 0; i < nTimeSteps; i++) {

                    Variable ungroupedClassVar = staticModel.getVariables().getVariableById(i);
                    System.out.println("Posterior Marginal for " + ungroupedClassVar.getName());

                    NodeList thisClassVarNodeList = new NodeList();
                    thisClassVarNodeList.add(huginBN.getNodeByName(ungroupedClassVar.getName()));

                    System.out.println("HUGIN: " + Arrays.toString(huginBN.getMarginal(thisClassVarNodeList).getData()));

                    System.out.println("IS:    " + Arrays.toString(posteriors_ungroupedIS.get(i).getParameters()));
                    System.out.println("VMP:   " + Arrays.toString(posteriors_ungroupedVMP.get(i).getParameters()));



                    double [] huginPosteriorParameters = huginBN.getMarginal(thisClassVarNodeList).getData();
                    EF_Multinomial huginPosterior = new EF_Multinomial(ungroupedClassVar);
                    huginPosterior.setMomentParameters((MomentParameters)new ArrayVector(huginPosteriorParameters));


                    EF_Multinomial ISPosterior = posteriors_ungroupedIS.get(i).toEFUnivariateDistribution();
                    EF_Multinomial VMPPosterior = posteriors_ungroupedVMP.get(i).toEFUnivariateDistribution();

                    totalDivergenceIS = totalDivergenceIS + huginPosterior.kl(ISPosterior.getNaturalParameters(),ISPosterior.computeLogNormalizer());
                    totalDivergenceVMP = totalDivergenceVMP + huginPosterior.kl(VMPPosterior.getNaturalParameters(),VMPPosterior.computeLogNormalizer());
                    System.out.println("K-L Divergence HUGIN vs IS:  " + huginPosterior.kl(ISPosterior.getNaturalParameters(),ISPosterior.computeLogNormalizer()));
                    System.out.println("K-L Divergence HUGIN vs VMP: " + huginPosterior.kl(VMPPosterior.getNaturalParameters(),VMPPosterior.computeLogNormalizer()));
                }

                System.out.println("\nTotal K-L Divergence HUGIN vs IS:  " + totalDivergenceIS);
                System.out.println("Total K-L Divergence HUGIN vs VMP: " + totalDivergenceVMP + "\n");






                System.out.println("\n\n");

                Domain huginBN_2grouped = BNConverterToHugin.convertToHugin(model_2grouped_0);
                huginBN_2grouped.compile();
//                System.out.println("HUGIN Domain compiled");

                if (staticEvidence!=null) {
                    staticEvidence.getVariables().forEach(variable -> {
                        if (variable.isMultinomial()) {
                            try {
                                ((DiscreteNode) huginBN_2grouped.getNodeByName(variable.getName())).selectState((int) staticEvidence.getValue(variable));
                            } catch (ExceptionHugin e) {
                                System.out.println(e.getMessage());
                            }
                        } else if (variable.isNormal()) {
                            try {
                                ((ContinuousChanceNode) huginBN_2grouped.getNodeByName(variable.getName())).enterValue(staticEvidence.getValue(variable));
                            } catch (ExceptionHugin e) {
                                System.out.println(e.getMessage());
                            }
                        } else {
                            throw new IllegalArgumentException("Variable type not allowed.");
                        }
                    });
                }

//                System.out.println("HUGIN Evidence set");

                huginBN_2grouped.propagate(Domain.H_EQUILIBRIUM_SUM, Domain.H_EVIDENCE_MODE_NORMAL);


                System.out.println("POSTERIOR DISTRIBUTIONS FOR 2-GROUPED CLASS VARIABLES, MODEL 0");
                totalDivergenceIS = 0;
                totalDivergenceVMP = 0;
                for (int i = 0; i < model_2grouped_0.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("GROUPED")).count(); i++) {

                    Variable ungroupedClassVar = model_2grouped_0.getVariables().getVariableById(i);
                    System.out.println("Posterior Marginal for " + ungroupedClassVar.getName());

                    NodeList thisClassVarNodeList = new NodeList();
                    thisClassVarNodeList.add(huginBN_2grouped.getNodeByName(ungroupedClassVar.getName()));

                    System.out.println("HUGIN: " + Arrays.toString(huginBN_2grouped.getMarginal(thisClassVarNodeList).getData()));

                    System.out.println("IS:    " + Arrays.toString(posteriors_2groupedIS.get(0).get(i).getParameters()));
                    System.out.println("VMP:   " + Arrays.toString(posteriors_2groupedVMP.get(0).get(i).getParameters()));

                    double [] huginPosteriorParameters = huginBN_2grouped.getMarginal(thisClassVarNodeList).getData();
                    EF_Multinomial huginPosterior = new EF_Multinomial(ungroupedClassVar);
                    huginPosterior.setMomentParameters((MomentParameters)new ArrayVector(huginPosteriorParameters));


                    EF_Multinomial ISPosterior = posteriors_2groupedIS.get(0).get(i).toEFUnivariateDistribution();
                    EF_Multinomial VMPPosterior = posteriors_2groupedVMP.get(0).get(i).toEFUnivariateDistribution();

                    totalDivergenceIS = totalDivergenceIS + huginPosterior.kl(ISPosterior.getNaturalParameters(),ISPosterior.computeLogNormalizer());
                    totalDivergenceVMP = totalDivergenceVMP + huginPosterior.kl(VMPPosterior.getNaturalParameters(),VMPPosterior.computeLogNormalizer());
                    System.out.println("K-L Divergence HUGIN vs IS:  " + huginPosterior.kl(ISPosterior.getNaturalParameters(),ISPosterior.computeLogNormalizer()));
                    System.out.println("K-L Divergence HUGIN vs VMP: " + huginPosterior.kl(VMPPosterior.getNaturalParameters(),VMPPosterior.computeLogNormalizer()));
                }

                System.out.println("\nTotal K-L Divergence HUGIN vs IS:  " + totalDivergenceIS);
                System.out.println("Total K-L Divergence HUGIN vs VMP: " + totalDivergenceVMP + "\n");




                System.out.println("\n\n");

                Domain huginBN_2grouped_1 = BNConverterToHugin.convertToHugin(model_2grouped_1);
                huginBN_2grouped_1.compile();
//                System.out.println("HUGIN Domain compiled");

                if (staticEvidence!=null) {
                    staticEvidence.getVariables().forEach(variable -> {
                        if (variable.isMultinomial()) {
                            try {
                                ((DiscreteNode) huginBN_2grouped_1.getNodeByName(variable.getName())).selectState((int) staticEvidence.getValue(variable));
                            } catch (ExceptionHugin e) {
                                System.out.println(e.getMessage());
                            }
                        } else if (variable.isNormal()) {
                            try {
                                ((ContinuousChanceNode) huginBN_2grouped_1.getNodeByName(variable.getName())).enterValue(staticEvidence.getValue(variable));
                            } catch (ExceptionHugin e) {
                                System.out.println(e.getMessage());
                            }
                        } else {
                            throw new IllegalArgumentException("Variable type not allowed.");
                        }
                    });
                }

//                System.out.println("HUGIN Evidence set");

                huginBN_2grouped_1.propagate(Domain.H_EQUILIBRIUM_SUM, Domain.H_EVIDENCE_MODE_NORMAL);


                System.out.println("POSTERIOR DISTRIBUTIONS FOR 2-GROUPED CLASS VARIABLES, MODEL 1");
                totalDivergenceIS = 0;
                totalDivergenceVMP = 0;
                for (int i = 0; i < model_2grouped_1.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("GROUPED")).count(); i++) {

                    Variable ungroupedClassVar = model_2grouped_1.getVariables().getVariableById(i);
                    System.out.println("Posterior Marginal for " + ungroupedClassVar.getName());

                    NodeList thisClassVarNodeList = new NodeList();
                    thisClassVarNodeList.add(huginBN_2grouped_1.getNodeByName(ungroupedClassVar.getName()));

                    System.out.println("HUGIN: " + Arrays.toString(huginBN_2grouped_1.getMarginal(thisClassVarNodeList).getData()));

                    System.out.println("IS:    " + Arrays.toString(posteriors_2groupedIS.get(1).get(i).getParameters()));
                    System.out.println("VMP:   " + Arrays.toString(posteriors_2groupedVMP.get(1).get(i).getParameters()));

                    double [] huginPosteriorParameters = huginBN_2grouped_1.getMarginal(thisClassVarNodeList).getData();
                    EF_Multinomial huginPosterior = new EF_Multinomial(ungroupedClassVar);
                    huginPosterior.setMomentParameters((MomentParameters)new ArrayVector(huginPosteriorParameters));


                    EF_Multinomial ISPosterior = posteriors_2groupedIS.get(1).get(i).toEFUnivariateDistribution();
                    EF_Multinomial VMPPosterior = posteriors_2groupedVMP.get(1).get(i).toEFUnivariateDistribution();

                    totalDivergenceIS = totalDivergenceIS + huginPosterior.kl(ISPosterior.getNaturalParameters(),ISPosterior.computeLogNormalizer());
                    totalDivergenceVMP = totalDivergenceVMP + huginPosterior.kl(VMPPosterior.getNaturalParameters(),VMPPosterior.computeLogNormalizer());
                    System.out.println("K-L Divergence HUGIN vs IS:  " + huginPosterior.kl(ISPosterior.getNaturalParameters(),ISPosterior.computeLogNormalizer()));
                    System.out.println("K-L Divergence HUGIN vs VMP: " + huginPosterior.kl(VMPPosterior.getNaturalParameters(),VMPPosterior.computeLogNormalizer()));
                }

                System.out.println("\nTotal K-L Divergence HUGIN vs IS:  " + totalDivergenceIS);
                System.out.println("Total K-L Divergence HUGIN vs VMP: " + totalDivergenceVMP + "\n");







//
//                System.out.println("\n\n");
//
//                Domain huginBN_4grouped = BNConverterToHugin.convertToHugin(model_4grouped);
//                huginBN_4grouped.compile();
////                System.out.println("HUGIN Domain compiled");
//
//                staticEvidence.getVariables().forEach(variable -> {
//                    if (variable.isMultinomial()) {
//                        try {
//                            ((DiscreteNode) huginBN_4grouped.getNodeByName(variable.getName())).selectState((int) staticEvidence.getValue(variable));
//                        } catch (ExceptionHugin e) {
//                            System.out.println(e.getMessage());
//                        }
//                    } else if (variable.isNormal()) {
//                        try {
//                            ((ContinuousChanceNode) huginBN_4grouped.getNodeByName(variable.getName())).enterValue(staticEvidence.getValue(variable));
//                        } catch (ExceptionHugin e) {
//                            System.out.println(e.getMessage());
//                        }
//                    } else {
//                        throw new IllegalArgumentException("Variable type not allowed.");
//                    }
//                });
//
////                System.out.println("HUGIN Evidence set");
//
//                huginBN_4grouped.propagate(Domain.H_EQUILIBRIUM_SUM, Domain.H_EVIDENCE_MODE_NORMAL);
//
//
//                System.out.println("POSTERIOR DISTRIBUTIONS FOR 4-GROUPED CLASS VARIABLES");
//                totalDivergenceIS = 0;
//                totalDivergenceVMP = 0;
//                for (int i = 0; i < model_4grouped.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("GROUPED")).count(); i++) {
//
//                    Variable ungroupedClassVar = model_4grouped.getVariables().getVariableById(i);
//                    System.out.println("Posterior Marginal for " + ungroupedClassVar.getName());
//
//                    NodeList thisClassVarNodeList = new NodeList();
//                    thisClassVarNodeList.add(huginBN_4grouped.getNodeByName(ungroupedClassVar.getName()));
//
//                    System.out.println("HUGIN: " + Arrays.toString(huginBN_4grouped.getMarginal(thisClassVarNodeList).getData()));
//
//                    System.out.println("IS:    " + Arrays.toString(posteriors_4groupedIS.get(0).get(i).getParameters()));
//                    System.out.println("VMP:   " + Arrays.toString(posteriors_4groupedVMP.get(0).get(i).getParameters()));
//
//                    double [] huginPosteriorParameters = huginBN_4grouped.getMarginal(thisClassVarNodeList).getData();
//                    EF_Multinomial huginPosterior = new EF_Multinomial(ungroupedClassVar);
//                    huginPosterior.setMomentParameters((MomentParameters)new ArrayVector(huginPosteriorParameters));
//
//
//                    EF_Multinomial ISPosterior = posteriors_4groupedIS.get(0).get(i).toEFUnivariateDistribution();
//                    EF_Multinomial VMPPosterior = posteriors_4groupedVMP.get(0).get(i).toEFUnivariateDistribution();
//
//                    totalDivergenceIS = totalDivergenceIS + huginPosterior.kl(ISPosterior.getNaturalParameters(),ISPosterior.computeLogNormalizer());
//                    totalDivergenceVMP = totalDivergenceVMP + huginPosterior.kl(VMPPosterior.getNaturalParameters(),VMPPosterior.computeLogNormalizer());
//                    System.out.println("K-L Divergence HUGIN vs IS:  " + huginPosterior.kl(ISPosterior.getNaturalParameters(),ISPosterior.computeLogNormalizer()));
//                    System.out.println("K-L Divergence HUGIN vs VMP: " + huginPosterior.kl(VMPPosterior.getNaturalParameters(),VMPPosterior.computeLogNormalizer()));
//                }
//
//                System.out.println("\nTotal K-L Divergence HUGIN vs IS:  " + totalDivergenceIS);
//                System.out.println("Total K-L Divergence HUGIN vs VMP: " + totalDivergenceVMP + "\n");
//
//
//
//
//
//                System.out.println("\n\n");
//
//                Domain huginBN_6grouped = BNConverterToHugin.convertToHugin(model_6grouped);
//                huginBN_6grouped.compile();
////                System.out.println("HUGIN Domain compiled");
//
//                staticEvidence.getVariables().forEach(variable -> {
//                    if (variable.isMultinomial()) {
//                        try {
//                            ((DiscreteNode) huginBN_6grouped.getNodeByName(variable.getName())).selectState((int) staticEvidence.getValue(variable));
//                        } catch (ExceptionHugin e) {
//                            System.out.println(e.getMessage());
//                        }
//                    } else if (variable.isNormal()) {
//                        try {
//                            ((ContinuousChanceNode) huginBN_6grouped.getNodeByName(variable.getName())).enterValue(staticEvidence.getValue(variable));
//                        } catch (ExceptionHugin e) {
//                            System.out.println(e.getMessage());
//                        }
//                    } else {
//                        throw new IllegalArgumentException("Variable type not allowed.");
//                    }
//                });
//
////                System.out.println("HUGIN Evidence set");
//
//                huginBN_6grouped.propagate(Domain.H_EQUILIBRIUM_SUM, Domain.H_EVIDENCE_MODE_NORMAL);
//
//
//                System.out.println("POSTERIOR DISTRIBUTIONS FOR 6-GROUPED CLASS VARIABLES");
//                totalDivergenceIS = 0;
//                totalDivergenceVMP = 0;
//                for (int i = 0; i < model_6grouped.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("GROUPED")).count(); i++) {
//
//                    Variable ungroupedClassVar = model_6grouped.getVariables().getVariableById(i);
//                    System.out.println("Posterior Marginal for " + ungroupedClassVar.getName());
//
//                    NodeList thisClassVarNodeList = new NodeList();
//                    thisClassVarNodeList.add(huginBN_6grouped.getNodeByName(ungroupedClassVar.getName()));
//
//                    System.out.println("HUGIN: " + Arrays.toString(huginBN_6grouped.getMarginal(thisClassVarNodeList).getData()));
//
//                    System.out.println("IS:    " + Arrays.toString(posteriors_6groupedIS.get(0).get(i).getParameters()));
//                    System.out.println("VMP:   " + Arrays.toString(posteriors_6groupedVMP.get(0).get(i).getParameters()));
//
//                    double [] huginPosteriorParameters = huginBN_6grouped.getMarginal(thisClassVarNodeList).getData();
//                    EF_Multinomial huginPosterior = new EF_Multinomial(ungroupedClassVar);
//                    huginPosterior.setMomentParameters((MomentParameters)new ArrayVector(huginPosteriorParameters));
//
//
//                    EF_Multinomial ISPosterior = posteriors_6groupedIS.get(0).get(i).toEFUnivariateDistribution();
//                    EF_Multinomial VMPPosterior = posteriors_6groupedVMP.get(0).get(i).toEFUnivariateDistribution();
//
//                    totalDivergenceIS = totalDivergenceIS + huginPosterior.kl(ISPosterior.getNaturalParameters(),ISPosterior.computeLogNormalizer());
//                    totalDivergenceVMP = totalDivergenceVMP + huginPosterior.kl(VMPPosterior.getNaturalParameters(),VMPPosterior.computeLogNormalizer());
//                    System.out.println("K-L Divergence HUGIN vs IS:  " + huginPosterior.kl(ISPosterior.getNaturalParameters(),ISPosterior.computeLogNormalizer()));
//                    System.out.println("K-L Divergence HUGIN vs VMP: " + huginPosterior.kl(VMPPosterior.getNaturalParameters(),VMPPosterior.computeLogNormalizer()));
//
//                }
//
//                System.out.println("\nTotal K-L Divergence HUGIN vs IS:  " + totalDivergenceIS);
//                System.out.println("Total K-L Divergence HUGIN vs VMP: " + totalDivergenceVMP + "\n");
//
//

//
//
//
//                System.out.println("\n\n");
//
//                Domain huginBN_10grouped = BNConverterToHugin.convertToHugin(model_10grouped);
//                huginBN_10grouped.compile();
////                System.out.println("HUGIN Domain compiled");
//
//                staticEvidence.getVariables().forEach(variable -> {
//                    if (variable.isMultinomial()) {
//                        try {
//                            ((DiscreteNode) huginBN_10grouped.getNodeByName(variable.getName())).selectState((int) staticEvidence.getValue(variable));
//                        } catch (ExceptionHugin e) {
//                            System.out.println(e.getMessage());
//                        }
//                    } else if (variable.isNormal()) {
//                        try {
//                            ((ContinuousChanceNode) huginBN_10grouped.getNodeByName(variable.getName())).enterValue(staticEvidence.getValue(variable));
//                        } catch (ExceptionHugin e) {
//                            System.out.println(e.getMessage());
//                        }
//                    } else {
//                        throw new IllegalArgumentException("Variable type not allowed.");
//                    }
//                });
//
////                System.out.println("HUGIN Evidence set");
//
//                huginBN_10grouped.propagate(Domain.H_EQUILIBRIUM_SUM, Domain.H_EVIDENCE_MODE_NORMAL);
//
//
//                System.out.println("POSTERIOR DISTRIBUTIONS FOR 10-GROUPED CLASS VARIABLES");
//                totalDivergenceIS = 0;
//                totalDivergenceVMP = 0;
//                for (int i = 0; i < model_10grouped.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("GROUPED")).count(); i++) {
//
//                    Variable ungroupedClassVar = model_10grouped.getVariables().getVariableById(i);
//                    System.out.println("Posterior Marginal for " + ungroupedClassVar.getName());
//
//                    NodeList thisClassVarNodeList = new NodeList();
//                    thisClassVarNodeList.add(huginBN_10grouped.getNodeByName(ungroupedClassVar.getName()));
//
//                    System.out.println("HUGIN: " + Arrays.toString(huginBN_10grouped.getMarginal(thisClassVarNodeList).getData()));
//
//                    System.out.println("IS:    " + Arrays.toString(posteriors_10groupedIS.get(0).get(i).getParameters()));
//                    System.out.println("VMP:   " + Arrays.toString(posteriors_10groupedVMP.get(0).get(i).getParameters()));
//
//                    double [] huginPosteriorParameters = huginBN_10grouped.getMarginal(thisClassVarNodeList).getData();
//                    EF_Multinomial huginPosterior = new EF_Multinomial(ungroupedClassVar);
//                    huginPosterior.setMomentParameters((MomentParameters)new ArrayVector(huginPosteriorParameters));
//
//
//                    EF_Multinomial ISPosterior = posteriors_10groupedIS.get(0).get(i).toEFUnivariateDistribution();
//                    EF_Multinomial VMPPosterior = posteriors_10groupedVMP.get(0).get(i).toEFUnivariateDistribution();
//
//                    totalDivergenceIS = totalDivergenceIS + huginPosterior.kl(ISPosterior.getNaturalParameters(),ISPosterior.computeLogNormalizer());
//                    totalDivergenceVMP = totalDivergenceVMP + huginPosterior.kl(VMPPosterior.getNaturalParameters(),VMPPosterior.computeLogNormalizer());
//                    System.out.println("K-L Divergence HUGIN vs IS:  " + huginPosterior.kl(ISPosterior.getNaturalParameters(),ISPosterior.computeLogNormalizer()));
//                    System.out.println("K-L Divergence HUGIN vs VMP: " + huginPosterior.kl(VMPPosterior.getNaturalParameters(),VMPPosterior.computeLogNormalizer()));
//
//                }
//
//                System.out.println("\nTotal K-L Divergence HUGIN vs IS:  " + totalDivergenceIS);
//                System.out.println("Total K-L Divergence HUGIN vs VMP: " + totalDivergenceVMP + "\n");



            }
            catch (ExceptionHugin e) {
                System.out.println("\nHUGIN EXCEPTION:");
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }


    }

}
