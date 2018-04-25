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

package eu.amidst.core.conceptdrift;

/**
 * This interface defines the Fading Learner.
 * <p> For an example of use follow this link
 * <a href="http://amidst.github.io/toolbox/CodeExamples.html#conceptdriftexample"> http://amidst.github.io/toolbox/CodeExamples.html#conceptdriftexample </a>  </p>
 */
public interface FadingLearner {

    /**
     * Sets the fading rate.
     * @param fading a {@code double} that represents the fading rate.
     */
    void setFadingFactor(double fading);

}
