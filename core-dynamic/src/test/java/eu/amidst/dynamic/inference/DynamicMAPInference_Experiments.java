package eu.amidst.dynamic.inference;

import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.io.DynamicBayesianNetworkLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.dynamic.variables.HashMapDynamicAssignment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by dario on 11/11/15.
 */
public class DynamicMAPInference_Experiments {

    public static void main(String[] arguments) throws IOException, ClassNotFoundException {

        /*
         * LOADS THE DYNAMIC NETWORK AND PRINTS IT
         */
        String file = "./networks/CajamarDBN.dbn";
        DynamicBayesianNetwork cajamarDBN = DynamicBayesianNetworkLoader.loadFromFile(file);
        System.out.println(cajamarDBN.toString());

        /*
         *  INITIALIZE THE DYNAMIC MAP OBJECT
         */
        int nTimeSteps = 10;
        DynamicMAPInference dynMAP = new DynamicMAPInference();
        dynMAP.setModel(cajamarDBN);
        dynMAP.setNumberOfTimeSteps(nTimeSteps);

        Variable mapVariable = cajamarDBN.getDynamicVariables().getVariableByName("DEFAULT");
        dynMAP.setMAPvariable(mapVariable);


        /*
         * GENERATE AN EVIDENCE FOR T=0,...,nTimeSteps-1
         */
        List<Variable> varsDynamicModel = cajamarDBN.getDynamicVariables().getListOfDynamicVariables();

        System.out.println("DYNAMIC VARIABLES:");
        varsDynamicModel.forEach(var -> System.out.println("Var ID " + var.getVarID() + ": " + var.getName()));
        System.out.println();
        int indexVarEvidence1 = 1;
        int indexVarEvidence2 = 3;
        int indexVarEvidence3 = 4;
        Variable varEvidence1 = varsDynamicModel.get(indexVarEvidence1);
        Variable varEvidence2 = varsDynamicModel.get(indexVarEvidence2);
        Variable varEvidence3 = varsDynamicModel.get(indexVarEvidence3);

        List<Variable> varsEvidence = new ArrayList<>(3);
        varsEvidence.add(0,varEvidence1);
        varsEvidence.add(1,varEvidence2);
        varsEvidence.add(2,varEvidence3);

        double varEvidenceValue;

        Random random = new Random(23739303);

        List<DynamicAssignment> evidence = new ArrayList<>(nTimeSteps);

        for (int t = 0; t < nTimeSteps; t++) {
            HashMapDynamicAssignment dynAssignment = new HashMapDynamicAssignment(varsEvidence.size());

            for (int i = 0; i < varsEvidence.size(); i++) {

                dynAssignment.setSequenceID(12302253);
                dynAssignment.setTimeID(t);
                Variable varEvidence = varsEvidence.get(i);

                if (varEvidence.isMultinomial()) {
                    varEvidenceValue = random.nextInt(varEvidence1.getNumberOfStates());
                }
                else {
                    varEvidenceValue = -5 + 10 * random.nextDouble();
                }
                dynAssignment.setValue(varEvidence, varEvidenceValue);
            }
            evidence.add(dynAssignment);
        }
        System.out.println("EVIDENCE:");
        evidence.forEach(evid -> {
            System.out.println("Evidence at time " + evid.getTimeID());
            evid.getVariables().forEach(variable -> System.out.println(variable.getName() + ": " + Integer.toString((int) evid.getValue(variable))));
            System.out.println();
        });

        /*
         *  SET THE EVIDENCE AND MAKE INFERENCE
         */
        dynMAP.setEvidence(evidence);
        dynMAP.runInference();

        /*
         *  SHOW RESULTS
         */
        Assignment MAPestimate = dynMAP.getMAPestimate();
        double MAPestimateProbability = dynMAP.getMAPestimateProbability();

        System.out.println("MAP sequence over " + mapVariable.getName() + ":");
        List<Variable> MAPvarReplications = MAPestimate.getVariables().stream().sorted((var1,var2) -> (var1.getVarID()>var2.getVarID()? 1 : -1)).collect(Collectors.toList());
        StringBuilder sequence = new StringBuilder();
        MAPvarReplications.stream().forEachOrdered(var -> sequence.append( Integer.toString((int)MAPestimate.getValue(var)) + ", "));
        //System.out.println(MAPestimate.outputString(MAPvarReplications));
        System.out.println(sequence.toString());
        System.out.println("with probability prop. to: " + MAPestimateProbability);


    }
}
