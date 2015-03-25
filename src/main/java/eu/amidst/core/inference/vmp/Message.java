package eu.amidst.core.inference.vmp;

import eu.amidst.core.utils.Vector;

/**
 * Created by andresmasegosa on 03/02/15.
 */
public class Message<E extends Vector> {
    Node node;
    E vector;
    boolean done=false;

    public Message(Node node_) {
        this.node = node_;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public Node getNode() {
        return node;
    }

    public void setVector(E vector) {
        this.vector = vector;
    }

    public E getVector() {
        return vector;
    }

    public static <E extends Vector> Message<E> combine(Message<E> m1, Message<E> m2){
        if (m1.getNode().getMainVariable()!=m2.getNode().getMainVariable()) {
            throw new IllegalArgumentException();
        }

        m2.getVector().sum(m1.vector);
        m2.setDone(m2.isDone() && m1.isDone());
        return m2;
    }
}
