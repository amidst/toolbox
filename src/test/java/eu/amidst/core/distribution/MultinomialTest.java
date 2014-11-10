package eu.amidst.core.distribution;
import java.util.ArrayList;
import java.util.List;
import eu.amidst.core.database.statics.readers.Attribute;
import eu.amidst.core.database.statics.readers.Attributes;
import eu.amidst.core.header.Variable;
import eu.amidst.core.header.VariableBuilder;
import eu.amidst.core.database.statics.readers.DistType;
import eu.amidst.core.database.statics.*;
import eu.amidst.core.header.Assignment;
import eu.amidst.core.header.statics.StaticModelHeader;


import eu.amidst.core.utils.MultinomialIndex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MultinomialTest {

    private List<Variable> vars;
    private Assignment assignment;

    @Before
    public void setUp() throws Exception {

        Attributes atts = new AttributesForTesting();
        StaticModelHeader modelHeader = new StaticModelHeader(atts);

        this.vars = modelHeader.getVariables();
        this.assignment = new Assignment(3);

        assignment.setValue(vars.get(0),new Double(0));
        assignment.setValue(vars.get(1),new Double(0));
        assignment.setValue(vars.get(2),new Double(2));

       int index = MultinomialIndex.getIndexFromVariableAssignment(vars,assignment);
        System.out.println(index);

       /* ArrayList vars = new ArrayList();
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
        */
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetLogProbability() throws Exception {

    }

    @Test
    public void testGetProbability() throws Exception {

    }

    @Test
    public void testGetVariable() throws Exception {

    }
}