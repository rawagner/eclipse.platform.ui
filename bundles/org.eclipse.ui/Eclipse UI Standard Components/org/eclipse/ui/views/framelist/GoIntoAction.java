package org.eclipse.ui.views.framelist;

/**********************************************************************
Copyright (c) 2000, 2001, 2002, International Business Machines Corp and others.
All rights reserved.   This program and the accompanying materials
are made available under the terms of the Common Public License v0.5
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v05.html
 
Contributors:
**********************************************************************/

/**
 * Generic "Go Into" action which goes to the frame for the current selection. 
 */
public class GoIntoAction extends FrameAction {

	/**
	 * Constructs a new action for the specified frame list.
	 * 
	 * @param frameList the frame list
	 */
	public GoIntoAction(FrameList frameList) {
		super(frameList);
		setText(FrameListMessages.getString("GoInto.text")); //$NON-NLS-1$
		setToolTipText(FrameListMessages.getString("GoInto.toolTip")); //$NON-NLS-1$
		update();
	}
	
	private Frame getSelectionFrame(int flags) {
		return getFrameList().getSource().getFrame(IFrameSource.SELECTION_FRAME, flags);
	}
	
	/**
	 * Calls <code>gotoFrame</code> on the frame list with a frame
	 * representing the currently selected container.
	 */
	public void run() {
		Frame selectionFrame = getSelectionFrame(IFrameSource.FULL_CONTEXT);
		if (selectionFrame != null) {
			getFrameList().gotoFrame(selectionFrame);
		}
	}
	
	/**
	 * Updates this action's enabled state.
	 * This action is enabled only when there is a frame for the current selection.
	 */
	public void update() {
		super.update();
		Frame selectionFrame = getSelectionFrame(0);
		setEnabled(selectionFrame != null);
	}
}