/*
 *    CacheFilter.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package moa.streams.filters;

import moa.core.InstancesHeader;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Filter for representing a stream that is cached in memory.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class CacheFilter extends AbstractStreamFilter {

    @Override
    public String getPurposeString() {
        return "Stores a dataset in memory.";
    }

    private static final long serialVersionUID = 1L;

    protected Instances cache;

    protected int indexCache;

    protected int totalCacheInstances;

    protected boolean isInitialized;

    @Override
    protected void restartImpl() {
        this.isInitialized = false;
        if (this.inputStream != null) {
            this.init();
        }
    }

    protected void init() {
        this.cache = new Instances(this.inputStream.getHeader(), 0);
        this.indexCache = 0;
        this.totalCacheInstances = 0;
        while (this.inputStream.hasMoreInstances()) {
            cache.add(this.inputStream.nextInstance());
            this.totalCacheInstances++;
        }
        this.isInitialized = true;
    }

    @Override
    public InstancesHeader getHeader() {
        return this.inputStream.getHeader();
    }

    @Override
    public Instance nextInstance() {
        Instance inst = null;
        if (this.hasMoreInstances()) {
            inst = (Instance) this.cache.get(indexCache);
            this.indexCache++;
        }
        return inst;
    }

    @Override
    public boolean hasMoreInstances() {
        return (this.indexCache < this.totalCacheInstances);
    }

    @Override
    public long estimatedRemainingInstances() {
        return this.totalCacheInstances - this.indexCache;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-ge                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         nerated method stub
    }
}
