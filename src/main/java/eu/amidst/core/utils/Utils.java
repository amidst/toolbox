package eu.amidst.core.utils;

import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.variables.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andresmasegosa on 28/08/14.
 */
public final class Utils {

    private Utils(){
        //Not called
    }

    public static <E extends Vector> E normalize(E vector){

        double sum = Utils.sum(vector);
        for (int i = 0; i < vector.size(); i++) {
            vector.set(i,vector.get(i)/sum);
        }

        return vector;
    }


    public static <E extends Vector> E logNormalize(E vector){

        int maxIndex = Utils.maxIndex(vector);
        double maxValue = vector.get(maxIndex);
        for (int i = 0; i < vector.size(); i++) {
            vector.set(i,vector.get(i)-maxValue);
        }

        return vector;
    }

    public static double sum(Vector vector){
        double sum = 0;
        for (int i=0; i<vector.size(); i++){
            sum += vector.get(i);
        }
        return sum;
    }

    public static int maxIndex(Vector vector){
        double max = Double.NEGATIVE_INFINITY;
        int index = -1;
        for (int i=0; i<vector.size(); i++){
            if (vector.get(i)>max) {
                max = vector.get(i);
                index = i;
            }
        }
        return index;
    }

    public static double missingValue(){
        return Double.NaN;
    }

    public static boolean isMissingValue(double val){
        return Double.isNaN(val);
    }

    public static void accumulatedSumVectors(double[] a, double[] b){
        for (int i=0; i<a.length; i++){
            a[i]+=b[i];
        }
    }

    public static int maxIndex(double[] vals){
        double max = Double.NEGATIVE_INFINITY;
        int index = -1;
        for (int i=0; i<vals.length; i++){
            if (vals[i]>max) {
                max = vals[i];
                index = i;
            }
        }
        return index;
    }

    public static double[] normalize(double[] vals) {
        double sum = 0;
        for (int i=0; i<vals.length; i++) {
            sum+=vals[i];
        }

        for (int i=0; i<vals.length; i++) {
            vals[i] /= sum;
        }

        return vals;

    }

    public static double[] newNormalize(double[] vals) {
        double sum = 0;
        for (int i=0; i<vals.length; i++) {
            sum+=vals[i];
        }

        double[] normalizedVals = new double[vals.length];

        for (int i=0; i<vals.length; i++) {
            normalizedVals[i] = vals[i]/sum;
        }

        return normalizedVals;

    }

    public static double[] logs2probs(double[] vals){
        double max = vals[Utils.maxIndex(vals)];
        double[] normalizedVals = new double[vals.length];
        for (int i=0; i<vals.length; i++) {
            normalizedVals[i] = Math.exp(vals[i]+max);
        }
        return Utils.normalize(normalizedVals);
    }

    public static boolean isLinkCLG(Variable child, Variable parent){
        return !(child.isMultinomial() && parent.isNormal());
    }

    public static List<Variable> getCausalOrder(DAG dag){
        StaticVariables variables = dag.getStaticVariables();
        int nNrOfAtts = variables.getNumberOfVars();
        List<Variable> order = new ArrayList();
        boolean[] bDone = new boolean[variables.getNumberOfVars()];

        for (Variable var: variables){
            bDone[var.getVarID()] = false;
        }

        for (int iAtt = 0; iAtt < nNrOfAtts; iAtt++) {
            boolean allParentsDone = false;
            for (Variable var2 : variables){
                if (!bDone[var2.getVarID()]) {
                    allParentsDone = true;
                    int iParent = 0;
                    for (Variable parent: dag.getParentSet(var2))
                        allParentsDone = allParentsDone && bDone[parent.getVarID()];

                    if (allParentsDone){
                        order.add(var2);
                        bDone[var2.getVarID()] = true;
                    }
                }
            }
        }
        return order;
    }

    public static List<Variable> getCausalOrderTime0(DynamicDAG dag){
        DynamicVariables variables = dag.getDynamicVariables();
        int nNrOfAtts = variables.getNumberOfVars();
        List<Variable> order = new ArrayList();
        boolean[] bDone = new boolean[variables.getNumberOfVars()];

        for (Variable var: variables){
            bDone[var.getVarID()] = false;
        }
        for (int iAtt = 0; iAtt < nNrOfAtts; iAtt++) {
            boolean allParentsDone = false;
            for (Variable var2 : variables){
                if (!bDone[var2.getVarID()]) {
                    allParentsDone = true;
                    int iParent = 0;
                    for (Variable parent: dag.getParentSetTime0(var2))
                        allParentsDone = allParentsDone && bDone[parent.getVarID()];

                    if (allParentsDone){
                        order.add(var2);
                        bDone[var2.getVarID()] = true;
                    }
                }
            }
        }
        return order;
    }

    public static List<Variable> getCausalOrderTimeT(DynamicDAG dag){
        DynamicVariables variables = dag.getDynamicVariables();
        int nNrOfAtts = variables.getNumberOfVars();
        List<Variable> order = new ArrayList();
        boolean[] bDone = new boolean[variables.getNumberOfVars()];

        for (Variable var: variables){
            bDone[var.getVarID()] = false;
        }
        for (int iAtt = 0; iAtt < nNrOfAtts; iAtt++) {
            boolean allParentsDone = false;
            for (Variable var2 : variables){
                if (!bDone[var2.getVarID()]) {
                    allParentsDone = true;
                    for (Variable parent: dag.getParentSetTimeT(var2)) {
                        if (parent.isTemporalClone())
                            continue;
                        allParentsDone = allParentsDone && bDone[parent.getVarID()];
                    }

                    if (allParentsDone){
                        order.add(var2);
                        bDone[var2.getVarID()] = true;
                    }
                }
            }
        }
        return order;
    }

    public static int getConditionalDistributionType(Variable amidstVar, BayesianNetwork amidstBN) {

        int type = -1;
        List<Variable> conditioningVariables = amidstBN.getDistribution(amidstVar).getConditioningVariables();

        if (amidstVar.isMultinomial()){
            return 0;
        }
        else if (amidstVar.isNormal()) {

            boolean multinomialParents = false;
            boolean normalParents = false;

            for (Variable v : conditioningVariables) {
                if (v.isMultinomial()) {
                    multinomialParents = true;
                } else if (v.isNormal()) {
                    normalParents = true;
                } else {
                    throw new IllegalArgumentException("Unrecognized DistributionType. ");
                }
            }
            if (normalParents && !multinomialParents) {
                return 1;
            } else if ((!normalParents & multinomialParents) || (conditioningVariables.size() == 0)) {
                return 2;
            } else if (normalParents & multinomialParents) {
                return 3;
            } else {
                throw new IllegalArgumentException("Unrecognized DistributionType. ");
            }
        }
        return type;
    }

    public static BayesianNetwork DBNToBN(DynamicBayesianNetwork dynamicBayesianNetwork){
        List<Variable> areParents = new ArrayList<>();
        DynamicVariables dynamicVariables = dynamicBayesianNetwork.getDynamicVariables();
        DynamicDAG dynamicDAG = dynamicBayesianNetwork.getDynamicDAG();

        for (Variable dyVar: dynamicVariables) {
            if (!areParents.contains(dyVar)) {
                areParents.add(dyVar);
            }
            for (Variable dyParent : dynamicDAG.getParentSetTimeT(dyVar)) {
                if (!areParents.contains(dyParent)) {
                    areParents.add(dyParent);
                }
            }
        }

        StaticVariables staticVariables = new StaticVariables();
        for (Variable dyVar: areParents){
            VariableBuilder builder = VariableToVariableBuilder(dyVar);
            staticVariables.addHiddenVariable(builder);
        }

        DAG dag = new DAG(staticVariables);

        for (Variable dyVar: dynamicVariables){
            Variable staticVar = staticVariables.getVariableByName(dyVar.getName());
            for (Variable dyParent : dynamicDAG.getParentSetTimeT(dyVar)){
                Variable staticParent = staticVariables.getVariableByName(dyParent.getName());
                dag.getParentSet(staticVar).addParent(staticParent);
            }
        }


        BayesianNetwork bayesianNetwork = BayesianNetwork.newBayesianNetwork(dag);


        return bayesianNetwork;
    }

    public static VariableBuilder VariableToVariableBuilder(Variable variable){
        VariableBuilder builder = new VariableBuilder();

        builder.setName(variable.getName());
        int nstates=0;
        if (variable.getNumberOfStates()<0) {
            nstates = 2;
        }
        else {
            nstates = variable.getNumberOfStates();
        }

        builder.setStateSpace(new FiniteStateSpace(nstates));
        builder.setDistributionType(DistType.MULTINOMIAL);

        /*
        builder.setStateSpace(variable.getStateSpace());
        switch (variable.getStateSpace().getStateSpaceType()) {
            case REAL:
                builder.setDistributionType(DistType.NORMAL);
                break;
            case FINITE_SET:
                builder.setDistributionType(DistType.MULTINOMIAL);
                break;
            default:
                throw new IllegalArgumentException(" The string \"" + variable.getStateSpace() + "\" does not map to any Type.");
        }
        */
        return builder;
    }


    //*********************************************************************************
//            //Simulate a sample from a Hugin network
//            int nsamples = 100;
//            for (int j=0;j< nodeList.size();j++) {
//                System.out.print(((Node)nodeList.get(j)).getName());
//                if(j<nodeList.size()-1)
//                    System.out.print(",");
//            }
//            System.out.println();
//            for (int i=0;i<nsamples;i++){
//                domain.simulate();
//                for (int j=0;j<nodeList.size();j++){
//                    System.out.print(((ContinuousChanceNode)nodeList.get(j)).getSampledValue());
//                    if(j<nodeList.size()-1)
//                        System.out.print(",");
//                }
//                System.out.println();
//            }
//            //*********************************************************************************



}
