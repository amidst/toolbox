package eu.amidst.ecai2016;

import COM.hugin.HAPI.*;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.inference.DynamicMAPInference;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.huginlink.converters.BNConverterToHugin;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Created by dario on 08/03/16.
 */
public class HiddenMarkovModelComparisonToHuginExperimentsSimple {

    final static int maxTimeStepsHugin=30;

    public static void main(String[] args) {

        // First seeds used for numberOfModelsToTest = 5, numberOfEvidencesPerModel = 20, nTimeSteps=10, 10000 samples for IS
        //int seedModel = 186248462;
        //int seedEvidence = 1336;

        // Seeds used for numberOfModelsToTest = 10, numberOfEvidencesPerModel = 50, nTimeSteps=50, 20000 samples for IS
        //int seedModel = 68534;
        //int seedEvidence = 2159632;


        // Seeds used for numberOfModelsToTest = 10, numberOfEvidencesPerModel = 30, nTimeSteps=10, 10000 samples for IS
        //int seedModel = 12345;
        //int seedEvidence = 54321;


        //NEW EXPERIMENTS

        // Seeds used for numberOfModelsToTest = 10, numberOfEvidencesPerModel = 10, nTimeSteps=20, 10000 samples for IS, 6 obs.vars
        //int seedModel = 3634;
        //int seedEvidence = 35235;


        //NEW EXPERIMENTS

        // Seeds used for numberOfModelsToTest = 10, numberOfEvidencesPerModel = 10, nTimeSteps=20, 10000 samples for IS, 6 obs.vars
        //int seedModel = 3634743;
        //int seedEvidence = 935;

        int seedModel = 21945;
        Random randomModel = new Random(seedModel);

        int seedEvidence = 4634;
        Random randomEvidence= new Random(seedEvidence);



        int numberOfModelsToTest = 1;

        int numberOfEvidencesPerModel = 1;


        //BasicConfigurator.configure();
        VMP dumb_vmp = new VMP();



        int nTimeSteps=20;

        int nSamplesForIS=5000;

        System.out.println("seedModel: " + seedModel);
        System.out.println("seedEvidence: " + seedEvidence);
        System.out.println("nSamplesIS: " + nSamplesForIS);


        int [] sequenceAllZeros = new int[nTimeSteps];
        int [] sequenceAllOnes = Arrays.stream(sequenceAllZeros).map(k-> k+1).toArray();

        double[] precision_UngroupedIS = new double[numberOfModelsToTest*numberOfEvidencesPerModel];
        double[] precision_2GroupedIS = new double[numberOfModelsToTest*numberOfEvidencesPerModel];
        double[] precision_3GroupedIS = new double[numberOfModelsToTest*numberOfEvidencesPerModel];
        double[] precision_4GroupedIS = new double[numberOfModelsToTest*numberOfEvidencesPerModel];

        double[] precision_UngroupedVMP = new double[numberOfModelsToTest*numberOfEvidencesPerModel];
        double[] precision_2GroupedVMP = new double[numberOfModelsToTest*numberOfEvidencesPerModel];
        double[] precision_3GroupedVMP = new double[numberOfModelsToTest*numberOfEvidencesPerModel];
        double[] precision_4GroupedVMP = new double[numberOfModelsToTest*numberOfEvidencesPerModel];

        double[] precision_allZeros = new double[numberOfModelsToTest*numberOfEvidencesPerModel];
        double[] precision_allOnes = new double[numberOfModelsToTest*numberOfEvidencesPerModel];

        double[] precision_Hugin = new double[numberOfModelsToTest*numberOfEvidencesPerModel];

        DynMAPHiddenMarkovModel model = new DynMAPHiddenMarkovModel();


        model.setnStatesClassVar(2);
        model.setnStates(2);

        model.setnObservableContinuousVars(1);

        model.generateModel();
        model.printDAG();


        int experimentNumber = 0;
        for (int i = 0; i < numberOfModelsToTest; i++) {

            System.out.println("\nMODEL NUMBER "+ i+"\n");
            //model.setSeed(randomModel.nextInt());
            model.randomInitialization(randomModel);
            model.setProbabilityOfKeepingClass(0.6);


            DynamicBayesianNetwork DBNmodel = model.getModel();
            System.out.println(DBNmodel);


            for (int j = 0; j < numberOfEvidencesPerModel; j++) {

                System.out.println("\nEVIDENCE NUMBER "+ j + "\n");
                model.setSeed(randomEvidence.nextInt());
                model.generateEvidence(nTimeSteps);

                List<DynamicAssignment> evidence = model.getEvidenceNoClass();
//                evidence.forEach(dynamicAssignment -> System.out.println(dynamicAssignment.outputString(DBNmodel.getDynamicVariables().getListOfDynamicVariables())));
//                System.out.println("\n");



                DynamicMAPInference dynMAP = new DynamicMAPInference();

                Variable MAPVariable = model.getClassVariable();
                dynMAP.setModel(DBNmodel);
                dynMAP.setMAPvariable(MAPVariable);
                dynMAP.setNumberOfTimeSteps(nTimeSteps);

                dynMAP.setEvidence(evidence);

                BayesianNetwork staticModel = dynMAP.getUnfoldedStaticModel();
                Assignment staticEvidence = dynMAP.getUnfoldedEvidence();

                int[] sequence_Hugin = new int[nTimeSteps];
                if (nTimeSteps<=maxTimeStepsHugin) {
                    try {
                        Domain huginBN = BNConverterToHugin.convertToHugin(staticModel);
                        huginBN.compile();
//                System.out.println("HUGIN Domain compiled");

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

//                System.out.println("HUGIN Evidence set");

                        huginBN.propagate(Domain.H_EQUILIBRIUM_SUM, Domain.H_EVIDENCE_MODE_NORMAL);

//                System.out.println("HUGIN Propagation done");
                        NodeList classVarReplications = new NodeList();

                        //System.out.println(huginBN.getNodes().toString());
                        huginBN.getNodes().stream().filter(node -> {
                            try {
                                return node.getName().contains(MAPVariable.getName());
                            } catch (ExceptionHugin e) {
                                System.out.println(e.getMessage());
                                return false;
                            }
                        }).forEach(classVarReplications::add);

//                System.out.println("HUGIN Prob. evidence: " + huginBN.getLogLikelihood());


//                System.out.println("HUGIN MAP Variables:" + classVarReplications.toString());

                        huginBN.findMAPConfigurations(classVarReplications, 0.001);

//                System.out.println("HUGIN MAP Sequences:");
//                for (int i = 0; i < huginBN.getNumberOfMAPConfigurations() && i < 3; i++) {
//                    System.out.println(Arrays.toString(huginBN.getMAPConfiguration(i)) + " with probability " + huginBN.getProbabilityOfMAPConfiguration(i));
//                }
                        sequence_Hugin = huginBN.getMAPConfiguration(0);
                    } catch (ExceptionHugin e) {
                        System.out.println("\nHUGIN EXCEPTION:");
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                    }
                }


                int [] sequence_original = model.getClassSequence();


                //System.out.println("ORIGINAL SEQUENCE:               " + Arrays.toString(sequence_original));


//                if (nTimeSteps<=maxTimeStepsHugin) {
//                    System.out.println("HUGIN MAP Sequence:              " + Arrays.toString(sequence_Hugin));
//                }

                /////////////////////////////////////////////////
                // UNGROUPED VARIABLES WITH I.S.
                /////////////////////////////////////////////////
                dynMAP = new DynamicMAPInference();
                dynMAP.setModel(DBNmodel);
                dynMAP.setMAPvariable(MAPVariable);
                dynMAP.setNumberOfTimeSteps(nTimeSteps);


                dynMAP.setSampleSize(nSamplesForIS);
                dynMAP.setEvidence(evidence);

                dynMAP.runInferenceUngroupedMAPVariable(DynamicMAPInference.SearchAlgorithm.IS);
                int [] sequence_UngroupedIS = dynMAP.getMAPsequence();

                //System.out.println("Ungrouped IS finished");

                /////////////////////////////////////////////////
                // UNGROUPED VARIABLES WITH VMP
                /////////////////////////////////////////////////
                dynMAP = new DynamicMAPInference();
                dynMAP.setModel(DBNmodel);
                dynMAP.setMAPvariable(MAPVariable);
                dynMAP.setNumberOfTimeSteps(nTimeSteps);
                dynMAP.setEvidence(evidence);

                dynMAP.runInferenceUngroupedMAPVariable(DynamicMAPInference.SearchAlgorithm.VMP);
                int [] sequence_UngroupedVMP = dynMAP.getMAPsequence();

                //System.out.println("Ungrouped VMP finished");


                /////////////////////////////////////////////////
                // 2-GROUPED VARIABLES WITH I.S.
                /////////////////////////////////////////////////
                dynMAP = new DynamicMAPInference();
                dynMAP.setModel(DBNmodel);
                dynMAP.setMAPvariable(MAPVariable);
                dynMAP.setNumberOfTimeSteps(nTimeSteps);

                dynMAP.setSampleSize(nSamplesForIS);


                dynMAP.setNumberOfMergedClassVars(2);
                dynMAP.computeMergedClassVarModels();


                dynMAP.setEvidence(evidence);

                dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.IS);
                int [] sequence_2GroupedIS = dynMAP.getMAPsequence();
                List<int[]> submodel_sequences_2GroupedIS = dynMAP.getBestSequencesForEachSubmodel();


                //System.out.println("2-grouped IS finished");


                /////////////////////////////////////////////////
                // 2-GROUPED VARIABLES WITH VMP
                /////////////////////////////////////////////////
                dynMAP = new DynamicMAPInference();
                dynMAP.setModel(DBNmodel);
                dynMAP.setMAPvariable(MAPVariable);
                dynMAP.setNumberOfTimeSteps(nTimeSteps);

                dynMAP.setNumberOfMergedClassVars(2);
                dynMAP.computeMergedClassVarModels();

                dynMAP.setEvidence(evidence);

                dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.VMP);
                int [] sequence_2GroupedVMP = dynMAP.getMAPsequence();

                //System.out.println("2-grouped VMP finished");



                /////////////////////////////////////////////////
                // 3-GROUPED VARIABLES WITH I.S.
                /////////////////////////////////////////////////
                dynMAP = new DynamicMAPInference();
                dynMAP.setModel(DBNmodel);
                dynMAP.setMAPvariable(MAPVariable);
                dynMAP.setNumberOfTimeSteps(nTimeSteps);

                dynMAP.setSampleSize(nSamplesForIS);


                dynMAP.setNumberOfMergedClassVars(3);
                dynMAP.computeMergedClassVarModels();

                dynMAP.setEvidence(evidence);

                dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.IS);
                int [] sequence_3GroupedIS = dynMAP.getMAPsequence();

                //System.out.println("3-grouped IS finished");


                /////////////////////////////////////////////////
                // 3-GROUPED VARIABLES WITH VMP
                /////////////////////////////////////////////////
                dynMAP = new DynamicMAPInference();
                dynMAP.setModel(DBNmodel);
                dynMAP.setMAPvariable(MAPVariable);
                dynMAP.setNumberOfTimeSteps(nTimeSteps);

                dynMAP.setNumberOfMergedClassVars(3);
                dynMAP.computeMergedClassVarModels();

                dynMAP.setEvidence(evidence);

                dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.VMP);
                int [] sequence_3GroupedVMP = dynMAP.getMAPsequence();

                //System.out.println("3-grouped VMP finished");


                /////////////////////////////////////////////////
                // 4-GROUPED VARIABLES WITH I.S.
                /////////////////////////////////////////////////
                dynMAP = new DynamicMAPInference();
                dynMAP.setModel(DBNmodel);
                dynMAP.setMAPvariable(MAPVariable);
                dynMAP.setNumberOfTimeSteps(nTimeSteps);

                dynMAP.setSampleSize(nSamplesForIS);


                dynMAP.setNumberOfMergedClassVars(4);
                dynMAP.computeMergedClassVarModels();

                dynMAP.setEvidence(evidence);

                dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.IS);
                int [] sequence_4GroupedIS = dynMAP.getMAPsequence();

                //System.out.println("4-grouped IS finished");


                /////////////////////////////////////////////////
                // 4-GROUPED VARIABLES WITH VMP
                /////////////////////////////////////////////////
                dynMAP = new DynamicMAPInference();
                dynMAP.setModel(DBNmodel);
                dynMAP.setMAPvariable(MAPVariable);
                dynMAP.setNumberOfTimeSteps(nTimeSteps);

                dynMAP.setNumberOfMergedClassVars(4);
                dynMAP.computeMergedClassVarModels();

                dynMAP.setEvidence(evidence);

                dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.VMP);
                int [] sequence_4GroupedVMP = dynMAP.getMAPsequence();

                //System.out.println("4-grouped VMP finished\n\n");



                System.out.println("ORIGINAL SEQUENCE:               " + Arrays.toString(sequence_original));

                if (nTimeSteps<=maxTimeStepsHugin) {
                    System.out.println("HUGIN MAP Sequence:              " + Arrays.toString(sequence_Hugin));
                }

                System.out.println("DynMAP (Ungrouped-IS) Sequence:  " + Arrays.toString(sequence_UngroupedIS));
                System.out.println("DynMAP (2Grouped-IS) Sequence:   " + Arrays.toString(sequence_2GroupedIS));
                System.out.println("       (2Gr-IS) Seq. Submodel 0: " + Arrays.toString(submodel_sequences_2GroupedIS.get(0)));
                System.out.println("       (2Gr-IS) Seq. Submodel 1: " + Arrays.toString(submodel_sequences_2GroupedIS.get(1)));

                System.out.println("DynMAP (3Grouped-IS) Sequence:   " + Arrays.toString(sequence_3GroupedIS));
                System.out.println("DynMAP (4Grouped-IS) Sequence:   " + Arrays.toString(sequence_4GroupedIS));
                System.out.println("DynMAP (Ungrouped-VMP) Sequence: " + Arrays.toString(sequence_UngroupedVMP));
                System.out.println("DynMAP (2Grouped-VMP) Sequence:  " + Arrays.toString(sequence_2GroupedVMP));
                System.out.println("DynMAP (3Grouped-VMP) Sequence:  " + Arrays.toString(sequence_3GroupedVMP));
                System.out.println("DynMAP (4Grouped-VMP) Sequence:  " + Arrays.toString(sequence_4GroupedVMP));


                double current_precision_Hugin=0;
                if(nTimeSteps<=maxTimeStepsHugin) {

                    //COMPARE WITH HUGIN MAP SEQUENCE, INSTEAD WITH THE ORIGINAL
                    sequence_original=sequence_Hugin;

                    current_precision_Hugin = compareIntArrays(sequence_original, sequence_Hugin);

                }

                double current_precision_UngroupedIS=compareIntArrays(sequence_original,sequence_UngroupedIS);
                double current_precision_2GroupedIS=compareIntArrays(sequence_original,sequence_2GroupedIS);
                double current_precision_3GroupedIS=compareIntArrays(sequence_original,sequence_3GroupedIS);
                double current_precision_4GroupedIS=compareIntArrays(sequence_original,sequence_4GroupedIS);




//                System.out.println("Precision Ungrouped-IS: " + current_precision_UngroupedIS);
//                System.out.println("Precision 2Grouped-IS: " + current_precision_2GroupedIS);
//                System.out.println("Precision 3Grouped-IS: " + current_precision_3GroupedIS);
//                System.out.println("Precision 4Grouped-IS: " + current_precision_4GroupedIS);

                double current_precision_UngroupedVMP=compareIntArrays(sequence_original,sequence_UngroupedVMP);
                double current_precision_2GroupedVMP=compareIntArrays(sequence_original,sequence_2GroupedVMP);
                double current_precision_3GroupedVMP=compareIntArrays(sequence_original,sequence_3GroupedVMP);
                double current_precision_4GroupedVMP=compareIntArrays(sequence_original,sequence_4GroupedVMP);
//                System.out.println("Precision Ungrouped-VMP: " + current_precision_UngroupedVMP);
//                System.out.println("Precision 2Grouped-VMP: " + current_precision_2GroupedVMP);
//                System.out.println("Precision 3Grouped-VMP: " + current_precision_3GroupedVMP);
//                System.out.println("Precision 4Grouped-VMP: " + current_precision_4GroupedVMP);


                double current_precision_allZeros=compareIntArrays(sequence_original,sequenceAllZeros);
                double current_precision_allOnes=compareIntArrays(sequence_original,sequenceAllOnes);


                precision_UngroupedIS[experimentNumber]=current_precision_UngroupedIS;
                precision_2GroupedIS[experimentNumber]=current_precision_2GroupedIS;
                precision_3GroupedIS[experimentNumber]=current_precision_3GroupedIS;
                precision_4GroupedIS[experimentNumber]=current_precision_4GroupedIS;

                precision_UngroupedVMP[experimentNumber]=current_precision_UngroupedVMP;
                precision_2GroupedVMP[experimentNumber]=current_precision_2GroupedVMP;
                precision_3GroupedVMP[experimentNumber]=current_precision_3GroupedVMP;
                precision_4GroupedVMP[experimentNumber]=current_precision_4GroupedVMP;

                precision_Hugin[experimentNumber]=current_precision_Hugin;

                precision_allZeros[experimentNumber]=current_precision_allZeros;
                precision_allOnes[experimentNumber]=current_precision_allOnes;

                experimentNumber++;
            }

            double[] current_model_precision_Hugin = Arrays.copyOfRange(precision_Hugin,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel-1);
            double[] current_model_precision_UngroupedIS = Arrays.copyOfRange(precision_UngroupedIS,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel-1);
            double[] current_model_precision_2GroupedIS = Arrays.copyOfRange(precision_2GroupedIS,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel-1);
            double[] current_model_precision_3GroupedIS = Arrays.copyOfRange(precision_3GroupedIS,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel-1);
            double[] current_model_precision_4GroupedIS = Arrays.copyOfRange(precision_4GroupedIS,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel-1);
            double[] current_model_precision_UngroupedVMP = Arrays.copyOfRange(precision_UngroupedVMP,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel-1);
            double[] current_model_precision_2GroupedVMP = Arrays.copyOfRange(precision_2GroupedVMP,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel-1);
            double[] current_model_precision_3GroupedVMP = Arrays.copyOfRange(precision_3GroupedVMP,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel-1);
            double[] current_model_precision_4GroupedVMP = Arrays.copyOfRange(precision_4GroupedVMP,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel-1);

            double[] current_model_precision_allZeros = Arrays.copyOfRange(precision_allZeros,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel-1);
            double[] current_model_precision_allOnes = Arrays.copyOfRange(precision_allOnes,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel-1);

            System.out.println("\nMEAN PRECISIONS FOR THIS MODEL:");
            if(nTimeSteps<=maxTimeStepsHugin) {
                System.out.println("(PRECISION MEASURED WITH RESPECT TO HUGIN MAP SEQUENCE):");
            }
            System.out.println("         HUGIN: " + Arrays.stream(current_model_precision_Hugin).average().getAsDouble());

            System.out.println("  IS Ungrouped: " + Arrays.stream(current_model_precision_UngroupedIS).average().getAsDouble());
            System.out.println("  IS 2-Grouped: " + Arrays.stream(current_model_precision_2GroupedIS).average().getAsDouble());
            System.out.println("  IS 3-Grouped: " + Arrays.stream(current_model_precision_3GroupedIS).average().getAsDouble());
            System.out.println("  IS 4-Grouped: " + Arrays.stream(current_model_precision_4GroupedIS).average().getAsDouble());

            System.out.println(" VMP Ungrouped: " + Arrays.stream(current_model_precision_UngroupedVMP).average().getAsDouble());
            System.out.println(" VMP 2-Grouped: " + Arrays.stream(current_model_precision_2GroupedVMP).average().getAsDouble());
            System.out.println(" VMP 3-Grouped: " + Arrays.stream(current_model_precision_3GroupedVMP).average().getAsDouble());
            System.out.println(" VMP 4-Grouped: " + Arrays.stream(current_model_precision_4GroupedVMP).average().getAsDouble());
            System.out.println(" All-Zeros seq: " + Arrays.stream(current_model_precision_allZeros).average().getAsDouble());
            System.out.println(" All-Ones seq:  " + Arrays.stream(current_model_precision_allOnes).average().getAsDouble());

            System.out.println("\n\n");
        }

        System.out.println("\nGLOBAL MEAN PRECISIONS:");
        System.out.println("         HUGIN: " + Arrays.stream(precision_Hugin).average().getAsDouble());

        System.out.println("  IS Ungrouped: " + Arrays.stream(precision_UngroupedIS).average().getAsDouble());
        System.out.println("  IS 2-Grouped: " + Arrays.stream(precision_2GroupedIS).average().getAsDouble());
        System.out.println("  IS 3-Grouped: " + Arrays.stream(precision_3GroupedIS).average().getAsDouble());
        System.out.println("  IS 4-Grouped: " + Arrays.stream(precision_4GroupedIS).average().getAsDouble());

        System.out.println(" VMP Ungrouped: " + Arrays.stream(precision_UngroupedVMP).average().getAsDouble());
        System.out.println(" VMP 2-Grouped: " + Arrays.stream(precision_2GroupedVMP).average().getAsDouble());
        System.out.println(" VMP 3-Grouped: " + Arrays.stream(precision_3GroupedVMP).average().getAsDouble());
        System.out.println(" VMP 4-Grouped: " + Arrays.stream(precision_4GroupedVMP).average().getAsDouble());

        System.out.println(" All-Zeros seq: " + Arrays.stream(precision_allZeros).average().getAsDouble());
        System.out.println(" All-Ones seq:  " + Arrays.stream(precision_allOnes).average().getAsDouble());
    }

    private static double compareIntArrays(int[] array1, int[] array2) {
        if (array1.length!=array2.length) {
            System.out.println("Both arrays must be the same length");
            System.exit(-50);
        }

        AtomicInteger atomicInteger = new AtomicInteger();
        IntStream.range(0,array1.length).forEachOrdered(i -> {
            if (array1[i]==array2[i]) {
                atomicInteger.incrementAndGet();
            }
        });

        return ((double)atomicInteger.get())/((double)array1.length);
    }
}
