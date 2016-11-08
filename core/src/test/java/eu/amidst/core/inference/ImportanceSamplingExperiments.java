/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.core.inference;


import eu.amidst.core.Main;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Random;

/**
 * Created by dario on 15/05/15.
 */
public class ImportanceSamplingExperiments {

    private static Assignment randomEvidence(long seed, double evidenceRatio, BayesianNetwork bn, Variable varInterest) throws UnsupportedOperationException {

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
        HashMapAssignment assignment = new HashMapAssignment(2);

        int[] indexesEvidence = new int[numVarEvidence+1];
        indexesEvidence[0]=varInterest.getVarID();
        //if (Main.VERBOSE) System.out.println(variable.getVarID());

        if (Main.VERBOSE) System.out.println("Evidence:");
        for( int k=0; k<numVarEvidence; k++ ) {
            int varIndex=-1;
            do {
                varIndex = random.nextInt( bn.getNumberOfVars() );
                //if (Main.VERBOSE) System.out.println(varIndex);
                aux = bn.getVariables().getVariableById(varIndex);

                double thisEvidence;
                if (aux.isMultinomial()) {
                    thisEvidence = random.nextInt( aux.getNumberOfStates() );
                }
                else {
                    thisEvidence = random.nextGaussian();
                }
                evidence[k] = thisEvidence;

            } while ( ArrayUtils.contains(indexesEvidence, varIndex) );

            indexesEvidence[k+1]=varIndex;
            //if (Main.VERBOSE) System.out.println(Arrays.toString(indexesEvidence));
            if (Main.VERBOSE) System.out.println("Variable " + aux.getName() + " = " + evidence[k]);

            assignment.setValue(aux,evidence[k]);
        }
        if (Main.VERBOSE) System.out.println();

        return assignment;
    }



    /**
     * The class constructor.
     * @param args Array of options: "filename variable a b N useVMP" if variable is continuous or "filename variable w N useVMP" for discrete
     */
    public static void main(String[] args) throws Exception {

        //if (Main.VERBOSE) System.out.println("CONTINUOUS VARIABLE");
        boolean discrete=false;

        String filename=""; //Filename with the Bayesian Network
        String varname=""; // Variable of interest in the BN
        double a=0; // Lower endpoint of the interval
        double b=0; // Upper endpoint of the interval
        int N=0; // Sample size
        boolean useVMP=false; // Boolean indicating whether use VMP or not




        // FOR A CONTINUOUS VARIABLE OF INTEREST
        if (args.length==6) {

            filename = args[0]; //Filename with the Bayesian Network
            varname = args[1]; // Variable of interest in the BN
            String aa = args[2]; // Lower endpoint of the interval
            String bb = args[3]; // Upper endpoint of the interval
            String NN = args[4]; // Sample size
            String useVMParg = args[5]; // Boolean indicating whether use VMP or not

            try {
                a = Double.parseDouble(aa);
                b = Double.parseDouble(bb);
                N = Integer.parseInt(NN);
                useVMP = Boolean.parseBoolean(useVMParg);
            }
            catch (NumberFormatException e) {
                if (Main.VERBOSE) System.out.println(e.toString());
            }

        }
        // FOR A DISCRETE VARIABLE OF INTEREST
        else if (args.length==5) {
            //if (Main.VERBOSE) System.out.println("DISCRETE VARIABLE");
            discrete=true;
            if (Main.VERBOSE) System.out.println("Not available yet");
            System.exit(1);
        }
        else if (args.length==0) {
            filename="networks/simulated/Bayesian10Vars15Links.bn"; //Filename with the Bayesian Network
            //filename="networks/Bayesian2Vars1Link.bn";
            varname ="GaussianVar1"; // Variable of interest in the BN
            a = 0; // Lower endpoint of the interval
            b = 1; // Upper endpoint of the interval
            N = 10000; // Sample size
            useVMP = false; // Boolean indicating whether use VMP or not
        }
        else {
            if (Main.VERBOSE) System.out.println("Invalid number of arguments. See comments in main");
            System.exit(1);
        }

        BayesianNetwork bn;

        VMP vmp = new VMP();


        ImportanceSampling importanceSampling = new ImportanceSampling();


        try {

            bn = BayesianNetworkLoader.loadFromFile(filename);
            if (Main.VERBOSE) System.out.println(bn.toString());
            Variable varInterest = bn.getVariables().getVariableByName(varname);


            vmp.setModel(bn);
            vmp.runInference();


            importanceSampling.setModel(bn);
            //importanceSampling.setSamplingModel(vmp.getSamplingModel());
            importanceSampling.setSamplingModel(bn);
            importanceSampling.setParallelMode(true);
            importanceSampling.setKeepDataOnMemory(true);
            importanceSampling.setSampleSize(N);


            // Including evidence:
            Assignment assignment = randomEvidence(1823716125,0.05, bn, varInterest);
            importanceSampling.setEvidence(assignment);


            //importanceSampling.setSamplingModel(vmp.getSamplingModel());
            //importanceSampling.runInference(vmp);
            //if (Main.VERBOSE) System.out.println("Posterior of " + varInterest.getName() + "  (IS w. Evidence VMP) :" + importanceSampling.getPosterior(varInterest).toString());



            //importanceSampling.setSamplingModel(bn);
            importanceSampling.runInference();
            if (Main.VERBOSE) System.out.println("Posterior of " + varInterest.getName() + "  (IS w. Evidence) :" + importanceSampling.getPosterior(varInterest).toString());

            if (Main.VERBOSE) System.out.println("Posterior of " + varInterest.getName() + " (VMP) :" + vmp.getPosterior(varInterest).toString());

            if (Main.VERBOSE) System.out.println();


            if (Main.VERBOSE) System.out.println("Variable of interest: " + varInterest.getName());
            if (Main.VERBOSE) System.out.println();

            a = 1.5; // Lower endpoint of the interval
            b = 10000; // Upper endpoint of the interval

            final double finalA=a;
            final double finalB=b;

            double result = importanceSampling.getExpectedValue(varInterest, v -> (finalA < v && v < finalB) ? 1.0 : 0.0);
            if (Main.VERBOSE) System.out.println("Query: P(" + Double.toString(a) + " < " + varInterest.getName() + " < " + Double.toString(b) + ")");
            if (Main.VERBOSE) System.out.println("Probability result: " + result);

            /*
            if (Main.VERBOSE) System.out.println();


            varname = "DiscreteVar2";
            if (Main.VERBOSE) System.out.println();
            Variable discreteVarInterest = bn.getVariables().getVariableByName(varname);
            if (Main.VERBOSE) System.out.println("Variable of interest: " + discreteVarInterest.getName());

            importanceSampling.runInference();

            int w=1; // Value of interest
            double result2 = importanceSampling.runQuery(discreteVarInterest, w);
            if (Main.VERBOSE) System.out.println("Query: P(" + discreteVarInterest.getName() + " = " + Integer.toString(w) + ")");
            if (Main.VERBOSE) System.out.println("Probability result: " + result2);*/


        }
        catch (Exception e) {
            if (Main.VERBOSE) System.out.println("Error loading Bayesian Network from file");
            if (Main.VERBOSE) System.out.println(e.toString());
        }

    }
}
