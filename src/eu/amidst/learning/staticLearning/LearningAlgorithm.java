package eu.amidst.learning.staticLearning;

import eu.amidst.core.StaticDataBase.DataInstance;
import eu.amidst.core.StaticDataBase.DataStream;
import eu.amidst.models.staticmodels.LearnableModel;

/**
 * Created by andresmasegosa on 28/08/14.
 */
public interface LearningAlgorithm {

    public void setLearnableModel(LearnableModel model);

    public void initLearning();

    public void updateModel(DataInstance data);

    public void learnModelFromStream(DataStream data);

}
