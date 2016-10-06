/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package eu.amidst.latentvariablemodels.staticmodels;

import eu.amidst.core.conceptdrift.utils.GaussianHiddenTransitionMethod;
import eu.amidst.core.datastream.*;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.learning.parametric.bayesian.utils.PlateuIIDReplication;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.flinklink.core.conceptdrift.IdentifiableIDAModel;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.learning.parametric.ParallelVB;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;
import org.apache.flink.api.java.ExecutionEnvironment;

import java.util.ArrayList;
import java.util.List;

/**

 */
public class ConceptDriftDetector extends Model<ConceptDriftDetector> {

    /** Represents the drift detection mode. Only the global mode is currently provided.*/
    public enum DriftDetector {GLOBAL};

    /** Represents the variance added when making a transition*/
    double transitionVariance;

    /** Represents the index of the class variable of the classifier*/
    int classIndex;

    /** Represents the drift detection mode. Only the global mode is currently provided.*/
    DriftDetector conceptDriftDetector;

    /** Represents the seed of the class*/
    int seed;


    /** Represents the list of hidden vars modelling concept drift*/
    List<Variable> hiddenVars;

    /** Represents the fading factor.*/
    double fading;

    /** Represents the number of global hidden variables*/
    int numberOfGlobalVars;

    /** Represents whether there is or not a global hidden variable modelling concept drift*/
    boolean globalHidden;


    /**
     * Constructor of classifier from a list of attributes (e.g. from a datastream).
     * The following parameters are set to their default values: numStatesHiddenVar = 2
     * and diagonal = true.
     * @param attributes object of the class Attributes
     */
    public ConceptDriftDetector(Attributes attributes) throws WrongConfigurationException {
        super(attributes);

        transitionVariance=0.1;
        classIndex = atts.getNumberOfAttributes()-1;
        conceptDriftDetector = DriftDetector.GLOBAL;
        seed = 0;
        fading = 1.0;
        numberOfGlobalVars = 1;
        globalHidden = true;
        super.windowSize = 1000;
    }




    /**
     * Builds the DAG over the set of variables given with the structure of the model
     */
    @Override
    protected void buildDAG() {


        String className = atts.getFullListOfAttributes().get(classIndex).getName();
        hiddenVars = new ArrayList<Variable>();

        for (int i = 0; i < this.numberOfGlobalVars ; i++) {
            hiddenVars.add(vars.newGaussianVariable("GlobalHidden_"+i));
        }

        Variable classVariable = vars.getVariableByName(className);

        dag = new DAG(vars);

        for (Attribute att : atts.getListOfNonSpecialAttributes()) {
            if (att.getName().equals(className))
                continue;

            Variable variable = vars.getVariableByName(att.getName());
            dag.getParentSet(variable).addParent(classVariable);
            if (this.globalHidden) {
                for (int i = 0; i < this.numberOfGlobalVars ; i++) {
                    dag.getParentSet(variable).addParent(hiddenVars.get(i));
                }
            }
        }
    }

    @Override
   protected  void initLearning() {

        if (this.getDAG()==null)
            buildDAG();

        if(learningAlgorithm==null) {
            SVB svb = new SVB();
            svb.setSeed(this.seed);
            svb.setPlateuStructure(new PlateuIIDReplication(hiddenVars));
            GaussianHiddenTransitionMethod gaussianHiddenTransitionMethod = new GaussianHiddenTransitionMethod(hiddenVars, 0, this.transitionVariance);
            gaussianHiddenTransitionMethod.setFading(fading);
            svb.setTransitionMethod(gaussianHiddenTransitionMethod);
            svb.setDAG(dag);

            svb.setOutput(false);
            svb.getPlateuStructure().getVMP().setMaxIter(1000);
            svb.getPlateuStructure().getVMP().setThreshold(0.001);

            learningAlgorithm = svb;
        }
        learningAlgorithm.setWindowsSize(windowSize);
        if (this.getDAG()!=null)
            learningAlgorithm.setDAG(this.getDAG());
        else
            throw new IllegalArgumentException("Non provided dag");

        learningAlgorithm.setOutput(false);
        learningAlgorithm.initLearning();
        initialized=true;
    }


	protected void initLearningFlink() {

		if (this.getDAG()==null)
			buildDAG();



		if(learningAlgorithmFlink==null) {

			ParallelVB svb = new ParallelVB();
			svb.setSeed(this.seed);
			svb.setPlateuStructure(new PlateuIIDReplication(hiddenVars));
			GaussianHiddenTransitionMethod gaussianHiddenTransitionMethod = new GaussianHiddenTransitionMethod(hiddenVars, 0, this.transitionVariance);
			gaussianHiddenTransitionMethod.setFading(1.0);
			svb.setTransitionMethod(gaussianHiddenTransitionMethod);
			svb.setBatchSize(this.windowSize);
			svb.setDAG(dag);
			svb.setIdenitifableModelling(new IdentifiableIDAModel());

			svb.setOutput(false);
			svb.setMaximumGlobalIterations(100);
			svb.setMaximumLocalIterations(100);
			svb.setGlobalThreshold(0.001);
			svb.setLocalThreshold(0.001);

			learningAlgorithmFlink = svb;

		}

		learningAlgorithmFlink.setBatchSize(windowSize);

		if (this.getDAG()!=null)
			learningAlgorithmFlink.setDAG(this.getDAG());
		else
			throw new IllegalArgumentException("Non provided dag");


		learningAlgorithmFlink.initLearning();
		initialized=true;

		System.out.println("Window Size = "+windowSize);
	}



    /////// Getters and setters

    public double getTransitionVariance() {
        return transitionVariance;
    }

    public ConceptDriftDetector setTransitionVariance(double transitionVariance) {
        this.transitionVariance = transitionVariance;
        resetModel();
		return this;
    }

    public int getClassIndex() {
        return classIndex;
    }

    public ConceptDriftDetector setClassIndex(int classIndex) {
        this.classIndex = classIndex;
        resetModel();
		return this;
    }

    public int getSeed() {
        return seed;
    }

    public ConceptDriftDetector setSeed(int seed) {
        this.seed = seed;
        resetModel();
		return this;
    }

    public double getFading() {
        return fading;
    }

    public ConceptDriftDetector setFading(double fading) {
        this.fading = fading;
        resetModel();
		return this;
    }

    public int getNumberOfGlobalVars() {
        return numberOfGlobalVars;
    }

    public ConceptDriftDetector setNumberOfGlobalVars(int numberOfGlobalVars) {
        this.numberOfGlobalVars = numberOfGlobalVars;
        resetModel();
		return this;
    }

    //////////// example of use

    public static void main(String[] args) throws Exception {



  		//// Multi-core example
        int windowSize = 1000;

    // DataStream<DataInstance> data = DataSetGenerator.generate(1234,100, 1, 3);
        //We can open the data stream using the static class DataStreamLoader
        DataStream<DataInstance> data = DataStreamLoader.open("./datasets/DriftSets/sea.arff");

        System.out.println(data.getAttributes().toString());


        //Build the model
        Model model =
				new ConceptDriftDetector(data.getAttributes())
						.setWindowSize(windowSize)
						.setClassIndex(1)
						.setTransitionVariance(0.1);


        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)){
            model.updateModel(batch);
            System.out.println(model.getPosteriorDistribution("GlobalHidden_0").
                    toString());


        }

        System.out.println(model.getDAG());




		////// Flink example
	/*	int windowSize = 500;
		ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
		env.getConfig().disableSysoutLogging();

		//Load the data stream
		String filename = "./datasets/simulated/dataFlink_month0.arff";
		DataFlink<DataInstance> data =
				DataFlinkLoader.open(env, filename, false);

		//Build the model
		Model model = new ConceptDriftDetector(data.getAttributes());
		model.setWindowSize(windowSize);
		//((ConceptDriftDetector)model).setClassIndex(3);

		model.updateModel(data);

		for(int i=1; i<12;i++) {
			filename = "./datasets/simulated/dataFlink_month"+i+".arff";
			data = DataFlinkLoader.open(env, filename, false);
			model.updateModel(data);
			System.out.println(model.getPosteriorDistribution("GlobalHidden_0").
					toString());


		}

*/
		// Old flink example
/*		String filename = "./datasets/simulated/dataFlink_month0.arff";
		DataFlink<DataInstance> data =
				DataFlinkLoader.open(env, filename, false);
		System.out.println(data.getDataSet().count());


		IDAConceptDriftDetector learn = new IDAConceptDriftDetector();
		learn.setBatchSize(windowSize);
		learn.setClassIndex(data.getAttributes().getNumberOfAttributes()-1);
		learn.setAttributes(data.getAttributes());
		learn.setNumberOfGlobalVars(1);
		learn.setTransitionVariance(0.1);
		learn.setSeed(0);

		learn.initLearning();


		learn.updateModelWithNewTimeSlice(data);

		for(int i=1; i<3;i++) {
			filename = "./datasets/simulated/dataFlink_month"+i+".arff";
			data = DataFlinkLoader.open(env, filename, false);
			double[] out = learn.updateModelWithNewTimeSlice(data);
			System.out.println(out[0]);


		}


*/

    }
}

