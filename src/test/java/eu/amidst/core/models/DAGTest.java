package eu.amidst.core.models;

import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.variables.*;
import java.util.*;

import org.junit.Test;
import static org.junit.Assert.*;


/**
 * Created by Hanen on 14/11/14.
 */
public class DAGTest {

/* Very simple example to test the DAG class*/

    WekaDataFileReader reader = new WekaDataFileReader("data/dataWeka/contact-lenses.arff");
    StaticVariables variables = new StaticVariables(reader.getAttributes());
    DAG dag = new DAG(variables);

    @Test
    public void testingDAG(){
        List<Variable> variables =  dag.getStaticVariables().getListOfVariables();
        Variable A = variables.get(0);
        Variable B = variables.get(1);
        Variable C = variables.get(2);
        Variable D = variables.get(3);
        Variable E = variables.get(4);

        /* test cyclic dag */

        dag.getParentSet(A).addParent(C);
        dag.getParentSet(B).addParent(A);
        dag.getParentSet(C).addParent(B);
        dag.getParentSet(D).addParent(B);
        dag.getParentSet(E).addParent(B);

        assertTrue(dag.containCycles());

        /*remove the cycle and test again */

        dag.getParentSet(A).removeParent(C);
        dag.getParentSet(C).addParent(A);

        assertFalse(dag.containCycles());


        /*test the parent set*/

        assertEquals(2, dag.getParentSet(C).getNumberOfParents());
        assertEquals(0, dag.getParentSet(A).getNumberOfParents());

    }

}
