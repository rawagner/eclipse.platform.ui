package org.eclipse.jface.examples.databinding.contentprovider.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.eclipse.jface.internal.databinding.api.observable.set.AbstractObservableSet;
import org.eclipse.jface.internal.databinding.api.observable.set.SetDiff;
import org.eclipse.swt.widgets.Display;

/**
 * Test set that simulates asynchronously computed elements. The elements
 * of the set are randomly generated Integers. Whenever
 * the "recompute" method is called, the set will spin off a job that
 * sleeps for a period of time and then randomly adds and removes elements
 * from the set.
 * 
 * <p>
 * This simulates a set that wraps a database query or network communication. 
 * These would follow the same pattern (report the set as "stale", perform some
 * slow operation, then make changes to the set).
 * </p>
 * 
 * @since 3.2
 */
public class AsynchronousTestSet extends AbstractObservableSet {

	private static Random randomNumberGenerator = new Random();
	private Display display;
	private boolean stale = false;
	
	/**
	 * Average number of elements to add or remove
	 */ 
	private static final int AVERAGE_DELTA = 4;
	
	/** 
	 * Average "computation" time -- time taken to do the simulated work (ms)
	 */ 
	private static final int AVERAGE_BUSY_TIME = 1000;
	
	/**
	 * List of all undisposed AsynchronousTestSet instances. Used for the recomputeAll method.
	 */
	private static List allSets = new ArrayList();
	
	public AsynchronousTestSet() {
		super(new HashSet());
		display = Display.getCurrent();
		if (display == null) {
			throw new IllegalStateException("This object can only be created in the UI thread"); //$NON-NLS-1$
		}
		recompute();
	}
	
	protected void firstListenerAdded() {
		super.firstListenerAdded();
		allSets.add(this);
	}
	
	protected void lastListenerRemoved() {
		allSets.remove(this);
		super.lastListenerRemoved();
	}
	
	public static void recomputeAll() {
		for (Iterator iter = allSets.iterator(); iter.hasNext();) {
			AsynchronousTestSet next = (AsynchronousTestSet) iter.next();
			
			next.recompute();
		}
	}
	
	public void remove(Collection toRemove) {
		HashSet removed = new HashSet();
		removed.addAll(toRemove);
		removed.retainAll(wrappedSet);
		
		wrappedSet.removeAll(removed);
		fireSetChange(new SetDiff(Collections.EMPTY_SET, removed));
	}
	
	public boolean isStale() {
		return stale;
	}
	
	public void recompute() {
		if (!isStale()) {
			setStale(true);
			final int sleepTime = (int)(randomNumberGenerator.nextDouble() * (double)(AVERAGE_BUSY_TIME * 2));
			Thread newThread = new Thread(new Runnable() {
				public void run() {
					
					// Simulate work by sleeping
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
					}

					
					// Add and remove some elements -- important: fire all events in the UI thread
					display.asyncExec(new Runnable() {
						public void run() {
							final HashSet toAdd = new HashSet();
							final HashSet toRemove = new HashSet();
							
							// Compute elements to add and remove (basically just fills the toAdd 
							// and toRemove sets with random elements)
							int delta = (randomNumberGenerator.nextInt(AVERAGE_DELTA * 4) - AVERAGE_DELTA * 2);
							int extraAdds = randomNumberGenerator.nextInt(AVERAGE_DELTA);
							int addCount = delta + extraAdds;
							int removeCount = -delta + extraAdds;
							
							if (addCount > 0) {
								for (int i = 0; i < addCount; i++) {
									toAdd.add(new Integer(randomNumberGenerator.nextInt(20)));
								}
							} 
							
							if (removeCount > 0) {
								Iterator oldElements = wrappedSet.iterator();
								for (int i = 0; i < removeCount && oldElements.hasNext(); i++) {
									toRemove.add(oldElements.next());
								}
							}
							
							toAdd.removeAll(wrappedSet);
							wrappedSet.addAll(toAdd);
							wrappedSet.removeAll(toRemove);

							setStale(false);
							fireSetChange(new SetDiff(toAdd, toRemove));
						}
					});
				}
			});
			
			newThread.start();
		}
	}
}
