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

package eu.amidst.core.inference.messagepassing;

import eu.amidst.core.utils.Serialization;
import eu.amidst.core.utils.Vector;

/**
 * This class handles and defines the message passed between the {@link Node}s.
 */
public class Message<E extends Vector> {

    /** Represents a {@link Node} object. */
    Node node;

    /** Represents a vector. */
    E vector;

    /** Represents a {@code boolean} that indicates if this Message is performed or done. */
    boolean done=false;

    /**
     * Creates a new Message for a given {@link Node} object.
     * @param node_ a given {@link Node} object.
     */
    public Message(Node node_) {
        this.node = node_;
    }

    /**
     * Tests is this Message is done.
     * @return {@code true} if the Message is done, {@code false} otherwise.
     */
    public boolean isDone() {
        return done;
    }

    /**
     * Sets the state of this Message, i.e., done or not.
     * @param done a {@code boolean} to which the state of this Message will be set.
     */
    public void setDone(boolean done) {
        this.done = done;
    }

    /**
     * Returns the {@link Node} object of this Message.
     * @return the {@link Node} object of this Message.
     */
    public Node getNode() {
        return node;
    }

    /**
     * Sets the vector of this Message.
     * @param vector the vector to which tis Message will be set.
     */
    public void setVector(E vector) {
        this.vector = vector;
    }

    /**
     * Returns the vector of this Message.
     * @return the vector of this Message
     */
    public E getVector() {
        return vector;
    }

    /**
     * Combines the given message.
     * @param m1 a given message.
     */
    public void combine(Message<E> m1){
        if (m1.getNode().getMainVariable()!=this.getNode().getMainVariable()) {
            throw new IllegalArgumentException();
        }
        this.getVector().sum(m1.vector);
        this.setDone(this.isDone() && m1.isDone());
    }


    /**
     * Combines two given Messages.
     * @param <E> a class extending {@link Vector}
     * @param m1 a first given message.
     * @param m2 a second given message.
     * @return the message that represents the result of the combination of the two input messages.
     */
    public static <E extends Vector> Message<E> combineNonStateless(Message<E> m1, Message<E> m2){
        if (m1.getNode().getMainVariable()!=m2.getNode().getMainVariable()) {
            throw new IllegalArgumentException();
        }
        m1.getVector().sum(m2.vector);
        m1.setDone(m2.isDone() && m1.isDone());
        return m1;
    }

//    public static <E extends Vector> Message<E> combineNonStateless(Message<E> m1, Message<E> m2){
//        if (m1.getNode().getMainVariable()!=m2.getNode().getMainVariable()) {
//            throw new IllegalArgumentException();
//        }
//        m2.getVector().sum(m1.vector);
//        m2.setDone(m2.isDone() && m1.isDone());
//        return m2;
//    }

    /**
     * Combines two given Messages.
     * @param <E> a class extending {@link Vector}
     * @param m1 a first given message.
     * @param m2 a second given message.
     * @return the message that represents the result of the combination of the two input messages.
     */
    public static <E extends Vector> Message<E> combineStateless(Message<E> m1, Message<E> m2){
        if (m1.getNode().getMainVariable()!=m2.getNode().getMainVariable()) {
            throw new IllegalArgumentException();
        }
        Message<E> newmessage = new Message<>(m1.getNode());
        newmessage.setVector(Serialization.deepCopy(m1.getVector()));
        newmessage.getVector().sum(m2.vector);
        newmessage.setDone(m2.isDone() && m1.isDone());
        return newmessage;
    }
}
