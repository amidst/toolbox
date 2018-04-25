package eu.amidst.latentvariablemodels.staticmodels;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.StateSpaceType;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.latentvariablemodels.staticmodels.FactorAnalysis;
import eu.amidst.latentvariablemodels.staticmodels.Model;
import eu.amidst.latentvariablemodels.staticmodels.classifiers.Classifier;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rcabanas on 23/05/16.
 */

public class CustomGaussianMixture extends Classifier<CustomGaussianMixture>{

	Attributes attributes;



	public CustomGaussianMixture(Attributes attributes) throws WrongConfigurationException {
		super(attributes);
		this.attributes=attributes;

	}

	@Override
	protected void buildDAG() {



		//Obtain the predictive attributes
		List<Variable> attrVars = vars.getListOfVariables().stream()
				.filter(v -> !v.equals(classVar)).collect(Collectors.toList());

		int numAttr = attrVars.size();

		/** Create a hidden variable with two hidden states */
		Variable globalHiddenVar = vars.newMultinomialVariable("globalHiddenVar",2);

		/** Create a list of local hidden variables */
		List<Variable> localHidden = new ArrayList<Variable>();
		for(int i= 0; i< numAttr; i++) {
			localHidden.add(vars.newMultinomialVariable("locallHiddenVar_"+i,2));
		}


		/** We create a standard naive Bayes */
		DAG dag = new DAG(vars);


		/** Add the links */
		for (int i=0; i<numAttr; i++) {
			dag.getParentSet(attrVars.get(i)).addParent(localHidden.get(i));
			dag.getParentSet(attrVars.get(i)).addParent(globalHiddenVar);
			dag.getParentSet(attrVars.get(i)).addParent(classVar);

		}


		//This is needed to maintain coherence in the Model class.
		this.dag=dag;


	}

	//Method for testing the custom model
	public static void main(String[] args) {
		String filename = "datasets/bnaic2015/BCC/Month0.arff";
		DataStream<DataInstance> data = DataStreamLoader.open(filename);

		//Learn the model
		Model model = new CustomGaussianMixture(data.getAttributes());

		model.updateModel(data);
		BayesianNetwork bn = model.getModel();

		System.out.println(bn);
	}


}
