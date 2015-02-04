package eu.amidst.core.inference.VMP_;

import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 03/02/15.
 */
public class Message<E extends Vector> {
    Variable variable;
    E vector;
    boolean done;

    public Message(Variable variable_, E vector_, boolean done_) {
        this.variable = variable_;
        this.vector = vector_;
        this.done = done_;
    }

    public Message(Variable variable, E vector) {
        this(variable,vector,false);
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
