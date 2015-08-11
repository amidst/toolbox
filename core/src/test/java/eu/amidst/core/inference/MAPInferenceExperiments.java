package eu.amidst.core.inference;


import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by dario on 01/06/15.
 */
public class MAPInferenceExperiments {

    private static Assignment randomEvidence(long seed, double evidenceRatio, BayesianNetwork bn) throws UnsupportedOperationException {

        if (evidenceRatio<=0 || evidenceRatio>=1) {
            throw new UnsupportedOperationException("Error: invalid ratio");
        }

        int numVariables = bn.getVariables().getNumberOfVars();

        Random random=new Random(seed); //1823716125
        int numVarEvidence = (int) Math.ceil(numVariables*evidenceRatio); // Evidence on 20% of variables
        //numVarEvidence = 0;
        //List<Variable> varEvidence = new ArrayList<>(numVarEvidence);
        double [] evidence = new double[numVarEvidence];
        Variable aux;
        HashMapAssignment assignment = new HashMapAssignment(numVarEvidence);

        int[] indexesEvidence = new int[numVarEvidence];
        //indexesEvidence[0]=varInterest.getVarID();
        //System.out.println(variable.getVarID());

        System.out.println("Evidence:");
        for( int k=0; k<numVarEvidence; k++ ) {
            int varIndex=-1;
            do {
                varIndex = random.nextInt( bn.getNumberOfVars() );
                //System.out.println(varIndex);
                aux = bn.getVariables().getVariableById(varIndex);

                double thisEvidence;
                if (aux.isMultinomial()) {
                    thisEvidence = random.nextInt( aux.getNumberOfStates() );
                }
                else {
                    thisEvidence = random.nextGaussian();
                }
                evidence[k] = thisEvidence;

            } while (ArrayUtils.contains(indexesEvidence, varIndex) );

            indexesEvidence[k]=varIndex;
            //System.out.println(Arrays.toString(indexesEvidence));
            System.out.println("Variable " + aux.getName() + " = " + evidence[k]);

            assignment.setValue(aux,evidence[k]);
        }
        System.out.println();
        return assignment;
    }


    /**
     * The class constructor.
     * @param args Array of options: "filename variable a b N useVMP" if variable is continuous or "filename variable w N useVMP" for discrete
     */
    public static void main(String[] args) throws Exception {

        int seedNetwork = 61236719 + 123;

        String filename=""; //Filename with the Bayesian Network

        int nDiscrete = 10;
        int nStates = 2;
        int nContin = 10;
        int nLinks = (int)Math.round(1.25*(nDiscrete+nContin));

        filename = "networks/randomlyGeneratedBN.bn";
        BayesianNetworkGenerator.generateBNtoFile(nDiscrete, nStates, nContin, nLinks, seedNetwork, filename);

        int seed = seedNetwork + 2315;

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile(filename);
        System.out.println(bn.getDAG());

        System.out.println(bn.toString());



        MAPInference mapInference = new MAPInference();
        mapInference.setModel(bn);
        mapInference.setParallelMode(true);
        mapInference.setSampleSize(1);

        List<Variable> causalOrder = Utils.getCausalOrder(mapInference.getOriginalModel().getDAG());

        System.out.println("CausalOrder: " + Arrays.toString(Utils.getCausalOrder(mapInference.getOriginalModel().getDAG()).stream().map(Variable::getName).toArray()));
        System.out.println();

        // Including evidence:
        double observedVariablesRate = 0.1;
        Assignment evidence = randomEvidence(seed, observedVariablesRate, bn);



        mapInference.setEvidence(evidence);

        long timeStart = System.nanoTime();
        mapInference.runInference(1);

        Assignment mapEstimate1 = mapInference.getMAPestimate();
        List<Variable> modelVariables = mapInference.getOriginalModel().getVariables().getListOfVariables();
        System.out.println("MAP estimate: " + mapEstimate1.toString()); //toString(modelVariables);
        System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate1)));
        long timeStop = System.nanoTime();
        double execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        //System.out.println(.toString(mapInference.getOriginalModel().getVariables().iterator().));




        // MAP INFERENCE WITH A BIG SAMPLE TO CHECK
        mapInference.setSampleSize(50000);
        timeStart = System.nanoTime();
        mapInference.runInference(-1);

        Assignment mapEstimate2 = mapInference.getMAPestimate();
        modelVariables = mapInference.getOriginalModel().getVariables().getListOfVariables();
        System.out.println("MAP estimate (huge sample): " + mapEstimate2.toString());
        System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate2)));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");



        // DETERMINISTIC SEQUENTIAL SEARCH ON DISCRETE VARIABLES
        timeStart = System.nanoTime();
        mapInference.runInference(-2);

        Assignment mapEstimate3 = mapInference.getMAPestimate();
        modelVariables = mapInference.getOriginalModel().getVariables().getListOfVariables();
        System.out.println("MAP estimate (sequential): " + mapEstimate3.toString());
        System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate3)));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");

        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();

        mapInference.changeCausalOrder(bn,evidence);

        /*// AD-HOC MAP
        mapEstimate.setValue(bn.getVariables().getVariableByName("GaussianVar0"),-0.11819702417804305);
        mapEstimate.setValue(bn.getVariables().getVariableByName("GaussianVar1"),-1.706);
        mapEstimate.setValue(bn.getVariables().getVariableByName("GaussianVar2"),4.95);
        mapEstimate.setValue(bn.getVariables().getVariableByName("GaussianVar3"),14.33);
        mapEstimate.setValue(bn.getVariables().getVariableByName("GaussianVar4"),11.355);

        modelVariables = mapInference.getOriginalModel().getVariables().getListOfParamaterVariables();
        System.out.println("Other estimate: " + mapEstimate.toString(modelVariables));
        System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate)));
        */
    }
}
