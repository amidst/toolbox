package eu.amidst.core.inference.VMP_;

import eu.amidst.core.exponentialfamily.NaturalParameters;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 03/02/15.
 */
public class Message<E extends Vector> {
    Variable variable;
    E vector;
    boolean done=false;

    public Message(Variable variable_) {
        this.variable = variable_;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public Variable getVariable() {
        return variable;
    }

    public void setVector(E vector) {
        this.vector = vector;
    }

    public E getVector() {
        return vector;
    }

    public static <E extends Vector> Message<E> combine(Message<E> m1, Message<E> m2){
        if (m1.getVariable()!=m2.getVariable()) {
            throw new IllegalArgumentException();
        }

        m2.getVector().sum(m1.vector);
        m2.setDone(m2.isDone() && m1.isDone());
        return m2;
    }
}
