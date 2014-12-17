package eu.amidst.core.database;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by andresmasegosa on 11/12/14.
 */
public class DataOnMemoryListContainer implements DataOnMemory {
    List<DataInstance> instanceList;
    Attributes attributes;

    public DataOnMemoryListContainer(Attributes attributes1){
        this.instanceList=new ArrayList();
        this.attributes=attributes1;
    }

    public void add(int id, DataInstance data){
        this.instanceList.add(id,data);
    }

    @Override
    public int getNumberOfDataInstances() {
        return this.instanceList.size();
    }

    @Override
    public DataInstance getDataInstance(int i) {
        return this.instanceList.get(i);
    }

    @Override
    public Attributes getAttributes() {
        return this.attributes;
    }

    @Override
    public void close() {

    }

    @Override
    public Iterator<DataInstance> iterator() {
        return this.instanceList.iterator();
    }

}
