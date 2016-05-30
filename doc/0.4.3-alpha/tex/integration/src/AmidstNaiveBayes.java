package weka.classifier.bayes;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.filereaders.DataInstanceFromDataRow;
import eu.amidst.latentvariablemodels.staticmodels.classifiers.NaiveBayesClassifier;
import eu.amidst.wekalink.converterFromWekaToAmidst.Converter;
import eu.amidst.wekalink.converterFromWekaToAmidst.DataRowWeka;
import weka.classifiers.AbstractClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Randomizable;

/**
 * Created by rcabanas on 26/05/16.
 */
public class AmidstNaiveBayes extends AbstractClassifier
//        implements  Randomizable
{

    private NaiveBayesClassifier model = null;
    private Attributes attributes;

    @Override
    public void buildClassifier(Instances data) throws Exception {

        attributes = Converter.convertAttributes(data.enumerateAttributes(), data.classAttribute());
        DataOnMemoryListContainer<DataInstance> dataAmidst = new DataOnMemoryListContainer(attributes);
        data.stream().forEach(instance ->
                dataAmidst.add(new DataInstanceFromDataRow(new DataRowWeka(instance, attributes)))
        );

        model = new NaiveBayesClassifier(attributes);
        model.updateModel(dataAmidst);
    }


    @Override
    public double[] distributionForInstance(Instance instance) throws Exception {
        DataInstance amidstInstance = new DataInstanceFromDataRow(new DataRowWeka(instance, this.attributes));
        return model.predict(amidstInstance).getParameters();

    }


/*    @Override
    public void setSeed(int seed) {

    }

    @Override
    public int getSeed() {
        return 0;
    }*/
}
