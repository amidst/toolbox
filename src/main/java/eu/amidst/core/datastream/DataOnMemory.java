package eu.amidst.core.datastream;

import java.util.List;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public interface DataOnMemory<E extends DataInstance> extends DataStream<E> {
    int getNumberOfDataInstances();
    E getDataInstance(int i);
    List<E> getList();
}

