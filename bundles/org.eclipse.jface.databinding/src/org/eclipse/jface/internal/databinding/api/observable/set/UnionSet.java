/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jface.internal.databinding.api.observable.set;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.internal.databinding.nonapi.observable.IStalenessConsumer;
import org.eclipse.jface.internal.databinding.nonapi.observable.StalenessTracker;

/**
 * Represents a set consisting of the union of elements from one or more other
 * sets. This object does not need to be explicitly disposed. If nobody is
 * listening to the UnionSet, the set will remove its listeners.
 * 
 * @since 3.2
 */
public final class UnionSet extends AbstractObservableSet {

	/**
	 * child sets
	 */
	private IObservableSet[] childSets;

	private boolean stale = false;

	/**
	 * Map of elements onto Integer reference counts. This map is constructed
	 * when the first listener is added to the union set. Null if nobody is
	 * listening to the UnionSet.
	 */
	private HashMap refCounts = null;

	private StalenessTracker stalenessTracker;

	/**
	 * @param childSets
	 */
	public UnionSet(IObservableSet[] childSets) {
		super(null);
		this.childSets = childSets;
		this.stalenessTracker = new StalenessTracker(childSets,
				stalenessConsumer);
	}

	private ISetChangeListener childSetChangeListener = new ISetChangeListener() {
		public void handleSetChange(IObservableSet source, ISetDiff diff) {
			processAddsAndRemoves(diff.getAdditions(), diff.getRemovals());
		}
	};

	private IStalenessConsumer stalenessConsumer = new IStalenessConsumer() {
		public void setStale(boolean stale) {
			boolean oldStale = UnionSet.this.stale;
			UnionSet.this.stale = stale;
			if (stale && !oldStale) {
				fireStale();
			}
		}
	};

	public boolean isStale() {
		if (refCounts != null) {
			return stale;
		}

		for (int i = 0; i < childSets.length; i++) {
			IObservableSet childSet = childSets[i];

			if (childSet.isStale()) {
				return true;
			}
		}
		return false;
	}

	private void processAddsAndRemoves(Set adds, Set removes) {
		Set addsToFire = new HashSet();
		Set removesToFire = new HashSet();

		for (Iterator iter = adds.iterator(); iter.hasNext();) {
			Object added = iter.next();

			Integer refCount = (Integer) refCounts.get(added);
			if (refCount == null) {
				refCounts.put(added, new Integer(1));
				addsToFire.add(added);
			} else {
				int refs = refCount.intValue();
				refCount = new Integer(refs + 1);
				refCounts.put(added, refCount);
			}
		}

		for (Iterator iter = removes.iterator(); iter.hasNext();) {
			Object removed = iter.next();

			Integer refCount = (Integer) refCounts.get(removed);
			if (refCount != null) {
				int refs = refCount.intValue();
				if (refs <= 1) {
					removesToFire.add(removed);
					refCounts.remove(removed);
				} else {
					refCount = new Integer(refCount.intValue() - 1);
					refCounts.put(removed, refCount);
				}
			}
		}

		// just in case the removes overlapped with the adds
		addsToFire.removeAll(removesToFire);

		if (addsToFire.size() > 0 || removesToFire.size() > 0) {
			fireSetChange(new SetDiff(addsToFire, removesToFire));
		}
	}

	protected void firstListenerAdded() {
		super.firstListenerAdded();

		refCounts = new HashMap();
		for (int i = 0; i < childSets.length; i++) {
			IObservableSet next = childSets[i];
			next.addSetChangeListener(childSetChangeListener);
			incrementRefCounts(next);
		}
		stalenessTracker = new StalenessTracker(childSets, stalenessConsumer);
		setWrappedSet(refCounts.keySet());
	}

	protected void lastListenerRemoved() {
		super.lastListenerRemoved();

		for (int i = 0; i < childSets.length; i++) {
			IObservableSet next = childSets[i];

			next.removeSetChangeListener(childSetChangeListener);
			stalenessTracker.removeObservable(next);
		}
		refCounts = null;
		stalenessTracker = null;
		setWrappedSet(null);
	}

	private ArrayList incrementRefCounts(Collection added) {
		ArrayList adds = new ArrayList();

		for (Iterator iter = added.iterator(); iter.hasNext();) {
			Object next = iter.next();

			Integer refCount = (Integer) refCounts.get(next);
			if (refCount == null) {
				adds.add(next);
				refCount = new Integer(1);
				refCounts.put(next, refCount);
			} else {
				refCount = new Integer(refCount.intValue() + 1);
				refCounts.put(next, refCount);
			}
		}
		return adds;
	}

	protected void getterCalled() {
		super.getterCalled();
		if (refCounts == null) {
			// no listeners, recompute
			setWrappedSet(computeElements());
		}
	}

	private Set computeElements() {
		// If there is no cached value, compute the union from scratch
		if (refCounts == null) {
			Set result = new HashSet();
			for (int i = 0; i < childSets.length; i++) {
				result.addAll(childSets[i]);
			}
			return result;
		}

		// Else there is a cached value. Return it.
		return refCounts.keySet();
	}

}
