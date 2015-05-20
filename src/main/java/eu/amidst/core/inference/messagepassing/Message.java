/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.inference.messagepassing;

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
