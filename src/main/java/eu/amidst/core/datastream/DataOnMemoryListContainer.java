package eu.amidst.core.datastream;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 11/12/14.
 */
public class DataOnMemoryListContainer <E extends DataInstance> implements DataOnMemory<E> {
    List<E> instanceList;
    Attributes attributes;

    public DataOnMemoryListContainer(Attributes attributes_){
        this.instanceList=new ArrayList();
        this.attributes=attributes_;
    }

    public void add(E data){
        this.instanceList.add(data);
    }

    public void set(int id, E data){
        this.instanceList.set(id,data);
    }

    @Override
    public int getNumberOfDataInstances() {
        return this.instanceList.size();
    }

    @Override
    public E getDataInstance(int i) {
        return this.instanceList.get(i);
    }

    @Override
    public List<E> getList() {
        return this.instanceList;
    }

    @Override
    public Attributes getAttributes() {
        return this.attributes;
    }

    @Override
    public Stream<E> stream() {
        return this.instanceList.stream();
    }

    @Override
    public void close() {

    }

    @Override
    public boolean isRestartable() {
        return true;
    }

    @Override
    public void restart() {

    }

}
