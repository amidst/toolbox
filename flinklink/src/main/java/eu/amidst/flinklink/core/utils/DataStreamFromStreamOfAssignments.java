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

package eu.amidst.flinklink.core.utils;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 24/9/15.
 */
public class DataStreamFromStreamOfAssignments implements DataStream<DataInstance>, Serializable {
    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    Attributes atts;
    Variables variables;
    Stream<Assignment> stream;

    public DataStreamFromStreamOfAssignments(Variables variables, Stream<Assignment> stream){
        this.variables = variables;
        this.stream = stream;
        List<Attribute> list = this.variables.getListOfVariables().stream()
                .map(var -> new Attribute(var.getVarID(), var.getName(), var.getStateSpaceType())).collect(Collectors.toList());
        this.atts= new Attributes(list);
    }

    @Override
    public Attributes getAttributes() {
        return atts;
    }

    @Override
    public Stream<DataInstance> stream() {
        return this.stream().map(a -> new DataInstanceFromAssignment(a, this.atts, this.variables.getListOfVariables()));
    }

    @Override
    public void close() {

    }

    @Override
    public boolean isRestartable() {
        return false;
    }

    @Override
    public void restart() {

    }
    public static class DataInstanceFromAssignment implements DataInstance, Serializable {

        /** Represents the serial version ID for serializing the object. */
        private static final long serialVersionUID = -3436599636425587512L;

        Assignment assignment;
        Attributes attributes;
        List<Variable> variables;

        public DataInstanceFromAssignment(Assignment assignment1, Attributes atts, List<Variable> variables){
            this.assignment=assignment1;
            this.attributes = atts;
            this.variables = variables;
        }

        @Override
        public double getValue(Variable var) {
            return this.assignment.getValue(var);
        }

        @Override
        public void setValue(Variable var, double value) {
            this.assignment.setValue(var, value);
        }

        @Override
        public Attributes getAttributes() {
            return attributes;
        }

        @Override
        public Set<Variable> getVariables(){
            return assignment.getVariables();
        }

        @Override
        public double getValue(Attribute att) {
            return this.assignment.getValue(variables.get(att.getIndex()));
        }

        @Override
        public void setValue(Attribute att, double value) {
            if (!att.isSpecialAttribute())
                this.assignment.setValue(variables.get(att.getIndex()), value);
        }

        @Override
        public double[] toArray() {
            throw new UnsupportedOperationException("Operation not supported for an Assignment object");
        }

        @Override
        public String toString(){
            return this.outputString();
        }
    }
}
