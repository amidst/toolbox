package eu.amidst.core.distribution;
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