/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal;


import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.misc.UIStats;
import org.eclipse.ui.part.WorkbenchPart;


/**
 * Provides the common behavior for both views
 * and editor panes.
 */
public abstract class PartPane extends LayoutPart
	implements Listener
{
	public static final String PROP_ZOOMED = "zoomed"; //$NON-NLS-1$
	private boolean isZoomed = false;
	private MenuManager paneMenuManager;
	protected IWorkbenchPartReference partReference;
	protected WorkbenchPage page;
	protected Composite control;
	
	public static class Sashes {
		public Sash left;
		public Sash right;
		public Sash top;
		public Sash bottom;
	}
	
/**
 * Construct a pane for a part.
 */
public PartPane(IWorkbenchPartReference partReference, WorkbenchPage workbenchPage) {
	super(partReference.getId());
	this.partReference = partReference;
	this.page = workbenchPage;
	((WorkbenchPartReference)partReference).setPane(this);
}
/**
 * Factory method for creating the SWT Control hierarchy for this Pane's child.
 */
protected void createChildControl() {
	final IWorkbenchPart part[] = new IWorkbenchPart[]{partReference.getPart(false)};
	if(part[0] == null)
		return;

	if(control == null)
		return;
	
	final Composite content = new Composite(control, SWT.NONE);
	content.setLayout(new FillLayout());
	
	String error = WorkbenchMessages.format("PartPane.unableToCreate", new Object[] {partReference.getTitle()}); //$NON-NLS-1$
	Platform.run(new SafeRunnable(error) {
		public void run() {
			try {
				UIStats.start(UIStats.CREATE_PART_CONTROL,id);
				part[0].createPartControl(content);
			} finally {
				UIStats.end(UIStats.CREATE_PART_CONTROL,id);
			}
		}
		public void handleException(Throwable e) {
			// Log error.
			Workbench wb = (Workbench)PlatformUI.getWorkbench();
			if (!wb.isStarting())
				super.handleException(e);

			// Dispose old part.
			Control children[] = content.getChildren();
			for (int i = 0; i < children.length; i++){
				children[i].dispose();
			}
			
			// Create new part.
			IWorkbenchPart newPart = createErrorPart((WorkbenchPart)part[0]);
			part[0].getSite().setSelectionProvider(null);
			newPart.createPartControl(content);
			((WorkbenchPartReference)partReference).setPart(newPart);
			part[0] = newPart;
		}
	});
	page.addPart(partReference);
	page.firePartOpened(part[0]);	
}

public void addSizeMenuItem (Menu menu, int index) {
	//Add size menu
	MenuItem item = new MenuItem(menu, SWT.CASCADE, index);
	item.setText(WorkbenchMessages.getString("PartPane.size")); //$NON-NLS-1$
	Menu sizeMenu = new Menu(menu);
	item.setMenu(sizeMenu);
	addSizeItems(sizeMenu);
}

/**
 * 
 */
public void createControl(Composite parent) {
	if (getControl() != null)
		return;

	// Create view form.	
	control = new Composite(parent, SWT.NONE);
	control.setLayout(new FillLayout());
	// the part should never be visible by default.  It will be made visible 
	// by activation.  This allows us to have views appear in tabs without 
	// becoming active by default.
	control.setVisible(false);

	// Create a title bar.
	createTitleBar();

	// Create content.
	createChildControl();
	
	// When the pane or any child gains focus, notify the workbench.
	control.addListener(SWT.Activate, this);
}

protected abstract WorkbenchPart createErrorPart(WorkbenchPart oldPart);
/**
 * Create a title bar for the pane if required.
 */
protected abstract void createTitleBar();
/**
 * @private
 */
public void dispose() {
	super.dispose();

	if ((control != null) && (!control.isDisposed())) {
		control.removeListener(SWT.Activate, this);
		control.dispose();
		control = null;
	}
	if ((paneMenuManager != null)) {
		paneMenuManager.dispose();	
		paneMenuManager = null;
	}
}
/**
 * User has requested to close the pane.
 * Take appropriate action depending on type.
 */
abstract public void doHide();

/**
 * Zooms in on the part contained in this pane.
 */
protected void doZoom() {
	if (getWindow() instanceof IWorkbenchWindow)
		page.toggleZoom(partReference);
}
/**
 * Gets the presentation bounds.
 */
public Rectangle getBounds() {
	return getControl().getBounds();
}
/**
 * Get the control.
 */
public Control getControl() {
	return control;
}

/*
 * @see LayoutPart#getMinimumHeight()
 */
public int getMinimumHeight() {
	if (control == null || control.isDisposed())
		return super.getMinimumHeight();
	
	// account for the borders
	return control.computeTrim(0, 0, 0, 0).height;
}

/**
 * Answer the part child.
 */
public IWorkbenchPartReference getPartReference() {
	return partReference;
}

/**
 * @see Listener
 */
public void handleEvent(Event event) {
	if (event.type == SWT.Activate)
		requestActivation();
}
/**
 * Return whether the pane is zoomed or not
 */
public boolean isZoomed() {
	return isZoomed;
}
/**
 * Move the control over another one.
 */
public void moveAbove(Control refControl) {
	if (getControl() != null)
		getControl().moveAbove(refControl);
}
/**
 * Notify the workbook page that the part pane has
 * been activated by the user.
 */
protected void requestActivation() {
	this.page.requestActivation(partReference.getPart(true));
}
/**
 * Sets the parent for this part.
 */
public void setContainer(ILayoutContainer container) {
	super.setContainer(container);
}

/**
 * Shows the receiver if <code>visible</code> is true otherwise hide it.
 */
public void setVisible(boolean makeVisible) {
	super.setVisible(makeVisible);
	if(makeVisible) //Make sure that the part is restored.
		partReference.getPart(true);
}
/**
 * Sets focus to this part.
 */
public void setFocus() {
	requestActivation();
	IWorkbenchPart part = partReference.getPart(true);
	if (part != null) {
		part.setFocus();
	}
}
/**
 * Sets the workbench page of the view. 
 */
public void setWorkbenchPage(WorkbenchPage workbenchPage) {
	this.page = workbenchPage;
}
/**
 * Set whether the pane is zoomed or not
 */
public void setZoomed(boolean isZoomed) {
	if (this.isZoomed == isZoomed)
		return; // do nothing if we're already in the right state.
	
	super.setZoomed(isZoomed);
	
	this.isZoomed = isZoomed;
	
	final Object[] listeners = getPropertyListeners().getListeners();
	if (listeners.length > 0) {
		Boolean oldValue = isZoomed ? Boolean.FALSE : Boolean.TRUE;
		Boolean zoomed = isZoomed ? Boolean.TRUE : Boolean.FALSE;
		PropertyChangeEvent event = new PropertyChangeEvent(this, PROP_ZOOMED, oldValue, zoomed);
		for (int i = 0; i < listeners.length; ++i)
			((IPropertyChangeListener)listeners[i]).propertyChange(event);
	}
}
/**
 * Informs the pane that it's window shell has
 * been activated.
 */
/* package */ abstract void shellActivated();
/**
 * Informs the pane that it's window shell has
 * been deactivated.
 */
/* package */ abstract void shellDeactivated();
/**
 * Indicate focus in part.
 */
public abstract void showFocus(boolean inFocus);
/**
 * @see IPartDropTarget::targetPartFor
 */
public LayoutPart targetPartFor(LayoutPart dragSource) {
	return this;
}

/**
 * Show a title label menu for this pane.
 */
public abstract void showPaneMenu();
/**
 * Show the context menu for this part.
 */
public abstract void showViewMenu();

/**
 * Return the sashes around this part.
 */
protected abstract Sashes findSashes();
/**
 * Enable the user to resize this part using
 * the keyboard to move the specified sash
 */
protected void moveSash(final Sash sash) {
	moveSash(sash, this);
}

public static void moveSash(final Sash sash, final LayoutPart toGetFocusWhenDone) {
	final KeyListener listener = new KeyAdapter() {
		public void keyPressed(KeyEvent e) {
			if (e.character == SWT.ESC || e.character == '\r') {
				if(toGetFocusWhenDone != null)
					toGetFocusWhenDone.setFocus();
			}
		}
	};
	sash.addFocusListener(new FocusAdapter() {
		public void focusGained(FocusEvent e) {
			sash.setBackground(sash.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
			sash.addKeyListener(listener);
		}
		public void focusLost(FocusEvent e) {
			sash.setBackground(null);
			sash.removeKeyListener(listener);
		}
	});
	sash.setFocus();
	
}

/**
 * Add a menu item to the Size Menu
 */
protected void addSizeItem(Menu sizeMenu, String labelMessage, final Sash sash) {
	MenuItem item = new MenuItem(sizeMenu, SWT.NONE);
	item.setText(labelMessage); //$NON-NLS-1$
	item.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			moveSash(sash);
		}
	});
	item.setEnabled(!isZoomed() && sash != null);
}
/**
 * Returns the workbench page of this pane.
 */
public WorkbenchPage getPage() {
	return page;
}
/**
 * Add the Left,Right,Up,Botton menu items to the Size menu.
 */
protected void addSizeItems(Menu sizeMenu) {
    Sashes sashes = findSashes();
    addSizeItem(sizeMenu,
            WorkbenchMessages.getString("PartPane.sizeLeft"), sashes.left); //$NON-NLS-1$
    addSizeItem(sizeMenu,
            WorkbenchMessages.getString("PartPane.sizeRight"), sashes.right); //$NON-NLS-1$
    addSizeItem(sizeMenu,
            WorkbenchMessages.getString("PartPane.sizeTop"), sashes.top); //$NON-NLS-1$
    addSizeItem(sizeMenu,
            WorkbenchMessages.getString("PartPane.sizeBottom"), sashes.bottom); //$NON-NLS-1$
}


/**
 * Pin this part.
 */
protected void doDock() {
	// do nothing
}

/**
 * Set the busy state of the pane.
 */
public void setBusy(boolean isBusy) {
	//Do nothing by default
}

/**
 * Show a highlight for the receiver if it is 
 * not currently the part in the front of its
 * presentation.
 *
 */
public void showHighlight(){
    //No nothing by default
}

}
