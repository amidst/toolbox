package eu.amidst.core.utils;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by afa on 03/11/14.
 */
public class MultinomialIndex {

    public static int getIndexFromVariableAssignment (List vars, double[] assignment) {

        int[] numStates = new int[]{2,2,3};
        int n = vars.size();
        int lastPhiStride = 1;
        int index = 0;
        for (int i=0; i<n; i++){
            index = index + (int)assignment[i]*lastPhiStride;
            lastPhiStride=lastPhiStride*numStates[i];
        }
        return index;
    }


    public static double[] getVariableAssignmentFromIndex (List vars, int index) {
        double[] assignment = new double[vars.size()];

        int n = vars.size();
        int lastPhiStride = 1;
        int[] numStates = new int[]{2,2,3};

        for (int i=0; i<n; i++){
            assignment[i]=Math.floor(index/lastPhiStride) % numStates[i];
            lastPhiStride=lastPhiStride*numStates[i];
        }
        return assignment;
    }


    //TEST

    public static void main(String args[]){

        ArrayList vars = new ArrayList();
        int index = 0;
        double[] assignment;
        vars.add("A");  vars.add("B"); vars.add("C");

        index = MultinomialIndex.getIndexFromVariableAssignment(vars,new double[]{0,0,0}); System.out.println(index);
        index = MultinomialIndex.getIndexFromVariableAssignment(vars,new double[]{1,0,0}); System.out.println(index);
        index = MultinomialIndex.getIndexFromVariableAssignment(vars,new double[]{0,1,0}); System.out.println(index);
        index = MultinomialIndex.getIndexFromVariableAssignment(vars,new double[]{1,1,0}); System.out.println(index);
        index = MultinomialIndex.getIndexFromVariableAssignment(vars,new double[]{0,0,1}); System.out.println(index);
        index = MultinomialIndex.getIndexFromVariableAssignment(vars,new double[]{1,0,1}); System.out.println(index);
        index = MultinomialIndex.getIndexFromVariableAssignment(vars,new double[]{0,1,1}); System.out.println(index);
        index = MultinomialIndex.getIndexFromVariableAssignment(vars,new double[]{1,1,1}); System.out.println(index);
        index = MultinomialIndex.getIndexFromVariableAssignment(vars,new double[]{0,0,2}); System.out.println(index);
        index = MultinomialIndex.getIndexFromVariableAssignment(vars,new double[]{1,0,2}); System.out.println(index);
        index = MultinomialIndex.getIndexFromVariableAssignment(vars,new double[]{0,1,2}); System.out.println(index);
        index = MultinomialIndex.getIndexFromVariableAssignment(vars,new double[]{1,1,2}); System.out.println(index);


        assignment = MultinomialIndex.getVariableAssignmentFromIndex(vars,0); System.out.print((int)assignment[0]);System.out.print((int)assignment[1]);System.out.println((int)assignment[2]);
        assignment = MultinomialIndex.getVariableAssignmentFromIndex(vars,1); System.out.print((int)assignment[0]);System.out.print((int)assignment[1]);System.out.println((int)assignment[2]);
        assignment = MultinomialIndex.getVariableAssignmentFromIndex(vars,2); System.out.print((int)assignment[0]);System.out.print((int)assignment[1]);System.out.println((int)assignment[2]);
        assignment = MultinomialIndex.getVariableAssignmentFromIndex(vars,3); System.out.print((int)assignment[0]);System.out.print((int)assignment[1]);System.out.println((int)assignment[2]);
        assignment = MultinomialIndex.getVariableAssignmentFromIndex(vars,4); System.out.print((int)assignment[0]);System.out.print((int)assignment[1]);System.out.println((int)assignment[2]);
        assignment = MultinomialIndex.getVariableAssignmentFromIndex(vars,5); System.out.print((int)assignment[0]);System.out.print((int)assignment[1]);System.out.println((int)assignment[2]);
        assignment = MultinomialIndex.getVariableAssignmentFromIndex(vars,6); System.out.print((int)assignment[0]);System.out.print((int)assignment[1]);System.out.println((int)assignment[2]);
        assignment = MultinomialIndex.getVariableAssignmentFromIndex(vars,7); System.out.print((int)assignment[0]);System.out.print((int)assignment[1]);System.out.println((int)assignment[2]);
        assignment = MultinomialIndex.getVariableAssignmentFromIndex(vars,8); System.out.print((int)assignment[0]);System.out.print((int)assignment[1]);System.out.println((int)assignment[2]);
        assignment = MultinomialIndex.getVariableAssignmentFromIndex(vars,9); System.out.print((int)assignment[0]);System.out.print((int)assignment[1]);System.out.println((int)assignment[2]);
        assignment = MultinomialIndex.getVariableAssignmentFromIndex(vars,10); System.out.print((int)assignment[0]);System.out.print((int)assignment[1]);System.out.println((int)assignment[2]);
        assignment = MultinomialIndex.getVariableAssignmentFromIndex(vars,11); System.out.print((int)assignment[0]);System.out.print((int)assignment[1]);System.out.println((int)assignment[2]);


    }


}
