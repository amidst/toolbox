package eu.amidst.core.datastream;

/**
 * Created by andresmasegosa on 19/02/15.
 */
public interface DataSequence extends DataStream<DynamicDataInstance> {
    public int getSequenceID();
}
