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

package eu.amidst.core.datastream;

import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.distributionTypes.IndicatorType;

import java.util.Set;

/**
 *  The DataInstance interface represents a data sample.
 *  A {@link DataStream} object consists of a collection of {@link DataInstance} objects, which can be seen
 *  as specific assignments of the {@link Attribute} objects.
 *
 *  <p> To simplify the use of the DataInstance interface across the toolbox, it inherits from the
 *  {@link Assignment} interface. Hence, it can be also interpreted as an assignment to some {@link Variable} objects.
 *  This variable objects have associated an Attribute object. So, a DataInstance object can be queried either
 *  using an{@link Attribute} object or a {@link Variable} object. </p>
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#datastreamsexample"> http://amidst.github.io/toolbox/CodeExamples.html#datastreamsexample </a>  </p>
 *
 * <p> For further details about the implementation of this class using Java 8 functional-style programming look at the following paper: </p>
 *
 * <i> Masegosa et al. Probabilistic Graphical Models on Multi-Core CPUs using Java 8. IEEE-CIM (2015). </i>
 *
 */
public interface DataInstance extends Assignment {

    /**
     * {@inheritDoc}
     */
    @Override
    default double getValue(Variable var) {
        if (var.getAttribute()==null)
            return Utils.missingValue();
        else if(var.isIndicator() && (!Utils.isMissingValue(this.getValue(var.getAttribute()))
                                        || (Utils.isMissingValue(this.getValue(var.getAttribute())) &&
                                            ((IndicatorType)var.getDistributionType()).getDeltaValue()!= Double.NaN))){
            return (this.getValue(var.getAttribute())==((IndicatorType)var.getDistributionType()).getDeltaValue())?
                    0.0:1.0;
        }else
            return this.getValue(var.getAttribute());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default void setValue(Variable var, double value) {
        if (var.getAttribute()!=null)
            this.setValue(var.getAttribute(), value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default Set<Variable> getVariables(){
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default String outputString(){
        StringBuilder builder = new StringBuilder(this.getAttributes().getFullListOfAttributes().size()*2);
        builder.append("{");
        this.getAttributes().getFullListOfAttributes().stream().forEach(att -> builder.append(att.getName()+ " = "+ att.stringValue(this.getValue(att))+", "));
        builder.append("}");
        return builder.toString();
    }

    /**
     * Returns the set of attributes that have assigned values stored in this DataInstance.
     * @return a valid Attributes object.
     */
    Attributes getAttributes();

    /**
     * Returns the value of a given Attribute stored in this DataInstance.
     * @param att the Attribute object we want to query and extract its value.
     * @return the assigned value of the given Attribute. Returns a Double.NaN if
     * the attribute is not observed in this assignment.
     */
    double getValue(Attribute att);

    /**
     * Sets the value of a given Attribute in this DataInstance.
     * If the value is already included, then its value is updated accordingly.
     * @param att the Attribute object to which the value will be assigned.
     * @param val the value that will be assigned.
     */
    void setValue(Attribute att, double val);

    /**
     * Returns the values of this DataInstance in the form of an Array of doubles.
     * @return an array of values.
     */
    double[] toArray();
}