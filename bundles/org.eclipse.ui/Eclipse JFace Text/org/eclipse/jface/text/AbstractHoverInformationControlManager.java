package org.eclipse.jface.text;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.util.Assert;


/**
 * An information control manager that shows information on mouse hover events.
 * The mouse hover events are caught by registering a <code>MouseTrackListener</code>
 * on the manager's subject control. The manager has by default an information control closer
 * that closes the information control as soon as the mouse pointer leaves the 
 * subject area.
 */
abstract public class AbstractHoverInformationControlManager extends AbstractInformationControlManager {		
	
	
	/**
	 * The  information control closer for the hover information.
	 */
	class Closer extends MouseTrackAdapter 
		implements IInformationControlCloser, MouseListener, MouseMoveListener, ControlListener, KeyListener {
		
		/** The closer's subject control */
		private Control fSubjectControl;
		/** The closer's information control */
		private IInformationControl fInformationControl;
		/** The subject area */
		private Rectangle fSubjectArea;
		/** Indicates whether this closer is active */
		private boolean fIsActive= false;
		
		/**
		 * Creates a new information control closer.
		 */
		public Closer() {
		}
		
		/*
		 * @see IInformationControlCloser#setSubjectControl(Control)
		 */
		public void setSubjectControl(Control control) {
			fSubjectControl= control;
		}
		
		/*
		 * @see IInformationControlCloser#setHoverControl(IHoverControl)
		 */
		public void setInformationControl(IInformationControl control) {
			fInformationControl= control;
		}
		
		/*
		 * @see IInformationControlCloser#start(Rectangle)
		 */
		public void start(Rectangle subjectArea) {
			
			if (fIsActive)
				return;
			fIsActive= true;
			
			fSubjectArea= subjectArea;
			
			setEnabled(false);
			
			if (fSubjectControl != null && !fSubjectControl.isDisposed()) {
				fSubjectControl.addMouseListener(this);
				fSubjectControl.addMouseMoveListener(this);
				fSubjectControl.addMouseTrackListener(this);
				fSubjectControl.addControlListener(this);
				fSubjectControl.addKeyListener(this);
			}
		}
		
		/*
		 * @see IInformationControlCloser#stop()
		 */
		public void stop() {
			stop(false);
		}
		
		/**
		 * Stops the information control and if <code>delayRestart</code> is set
		 * allows restart only after a certain delay.
		 */
		protected void stop(boolean delayRestart) {
			
			if (!fIsActive)
				return;
			fIsActive= false;
			
			hideInformationControl();
			
			if (fSubjectControl != null && !fSubjectControl.isDisposed()) {
				fSubjectControl.removeMouseListener(this);
				fSubjectControl.removeMouseMoveListener(this);
				fSubjectControl.removeMouseTrackListener(this);
				fSubjectControl.removeControlListener(this);
				fSubjectControl.removeKeyListener(this);
			}			
		}
		
		/*
		 * @see MouseMoveListener#mouseMove
		 */
		public void mouseMove(MouseEvent event) {
			if (!fSubjectArea.contains(event.x, event.y))
				stop();
		}
				
		/*
		 * @see MouseListener#mouseUp(MouseEvent)
		 */
		public void mouseUp(MouseEvent event) {
		}
		
		/*
		 * @see MouseListener#mouseDown(MouseEvent)
		 */
		public void mouseDown(MouseEvent event) {
			stop();
		}
		
		/*
		 * @see MouseListener#mouseDoubleClick(MouseEvent)
		 */
		public void mouseDoubleClick(MouseEvent event) {
			stop();
		}
		
		/*
		 * @see MouseTrackAdapter#mouseExit(MouseEvent)
		 */
		public void mouseExit(MouseEvent event) {
			stop();
		}
		
		/*
		 * @see ControlListener#controlResized(ControlEvent)
		 */
		public void controlResized(ControlEvent event) {
			stop();
		}
		
		/*
		 * @see ControlListener#controlMoved(ControlEvent)
		 */
		public void controlMoved(ControlEvent event) {
			stop();
		}
		
		/*
		 * @see KeyListener#keyReleased(KeyEvent)
		 */
		public void keyReleased(KeyEvent event) {
		}
		
		/*
		 * @see KeyListener#keyPressed(KeyEvent)
		 */
		public void keyPressed(KeyEvent event) {
			stop(true);
		}
	};
	
	
	/**
	 * The mouse tracker to be installed on the manager's subject control.
	 */
	class MouseTracker extends ShellAdapter implements MouseTrackListener, MouseMoveListener {
		
		// http://bugs.eclipse.org/bugs/show_bug.cgi?id=18393
		// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19686
		// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19719
		
		private final static int EPSILON= 3;
		
		private Rectangle fHoverArea;
		private Rectangle fSubjectArea;
		private Control fSubjectControl;
		
		private boolean fIsActive= false;
		private boolean fMouseLost= false;
		private boolean fShellDeactivated= false;
		
		
		public MouseTracker() {
		}
				
		public void setSubjectArea(Rectangle subjectArea) {
			Assert.isNotNull(subjectArea);
			fSubjectArea= subjectArea;
		}
		
		public void start(Control subjectControl) {
			fSubjectControl= subjectControl;
			if (fSubjectControl != null && !fSubjectControl.isDisposed())
				fSubjectControl.addMouseTrackListener(this);
				
			fIsActive= false;
			fMouseLost= false;
			fShellDeactivated= false;
		}
		
		public void stop() {
			if (fSubjectControl != null && !fSubjectControl.isDisposed()) {
				fSubjectControl.removeMouseTrackListener(this);
				fSubjectControl.removeMouseMoveListener(this);
				fSubjectControl.getShell().removeShellListener(this);
			}
			
			fMouseLost= false;
			fShellDeactivated= false;
		}
		
		/*
		 * @see MouseTrackAdapter#mouseHover
		 */
		public void mouseHover(MouseEvent event) {
			
			if (fIsActive)
				return;
			
			fIsActive= true;
			fMouseLost= false;
			fShellDeactivated= false;
			
			setEnabled(false);
			
			fHoverEventLocation= new Point(event.x, event.y);
			fHoverArea= new Rectangle(event.x - EPSILON, event.y - EPSILON, 2 * EPSILON, 2 * EPSILON );
			if (fHoverArea.x < 0) fHoverArea.x= 0;
			if (fHoverArea.y < 0) fHoverArea.y= 0;
			setSubjectArea(fHoverArea);
			
			if (fSubjectControl != null && !fSubjectControl.isDisposed()) {
				fSubjectControl.addMouseMoveListener(this);
				fSubjectControl.getShell().addShellListener(this);
			}
			
			doShowInformation();
		}
		
		protected void deactivate() {
			if (fIsActive)
				return;
			
			if (fSubjectControl != null && !fSubjectControl.isDisposed()) {
				fSubjectControl.removeMouseMoveListener(this);
				fSubjectControl.getShell().removeShellListener(this);
			}
			
			setEnabled(true);
		}
		
		/*
		 * @see MouseTrackListener#mouseEnter(MouseEvent)
		 */
		public void mouseEnter(MouseEvent e) {
		}
		
		/*
		 * @see MouseTrackListener#mouseExit(MouseEvent)
		 */
		public void mouseExit(MouseEvent e) {
			fMouseLost= true;
			deactivate();
		}
		
		/*
		 * @see MouseMoveListener#mouseMove(MouseEvent)
		 */
		public void mouseMove(MouseEvent event) {
			if (!fSubjectArea.contains(event.x, event.y))
				deactivate();
		}
		
		/*
		 * @see ShellListener#shellDeactivated(ShellEvent)
		 */
		public void shellDeactivated(ShellEvent e) {
			fShellDeactivated= true;
			deactivate();
		}
		
		/*
		 * @see ShellListener#shellIconified(ShellEvent)
		 */
		public void shellIconified(ShellEvent e) {
			fShellDeactivated= true;
			deactivate();
		}
		
		public void computationCompleted() {
			fIsActive= false;
			fMouseLost= false;
			fShellDeactivated= false;
		}
		
		public boolean isMouseLost() {
			
			if (fMouseLost || fShellDeactivated)
				return true;
								
			if (fSubjectControl != null && !fSubjectControl.isDisposed()) {
				Display display= fSubjectControl.getDisplay();
				Point p= display.getCursorLocation();
				p= fSubjectControl.toControl(p);
				if (!fSubjectArea.contains(p) && !fHoverArea.contains(p))
					return true;
			}
			
			return false;
		}
	};
		
	/** The mouse tracker on the subject control */
	private MouseTracker fMouseTracker= new MouseTracker();
	/** The remembered hover event location */
	private Point fHoverEventLocation= new Point(-1, -1);
	
	/**
	 * Creates a new hover information control manager using the given information control creator.
	 * By default a <code>Closer</code> instance is set as this manager's closer.
	 *
	 * @param creator the information control creator
	 */
	protected AbstractHoverInformationControlManager(IInformationControlCreator creator) {
		super(creator);
		setCloser(new Closer());
	}
	
	/*
	 * @see AbstractInformationControlManager#presentInformation()
	 */
	protected void presentInformation() {
		
		Rectangle area= getSubjectArea();
		if (area != null)
			fMouseTracker.setSubjectArea(area);
		
		if (fMouseTracker.isMouseLost()) {
			fMouseTracker.computationCompleted();
			fMouseTracker.deactivate();
		} else {
			fMouseTracker.computationCompleted();
			super.presentInformation();
		}
	}
	
	/*
	 * @see AbstractInformationControlManager#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled) {
		
		boolean was= isEnabled();
		super.setEnabled(enabled);
		boolean is= isEnabled();
		
		if (was != is) {
			if (is)
				fMouseTracker.start(getSubjectControl());
			else
				fMouseTracker.stop();
		}
	}
	
	/**
	 * Returns the location at which the most recent mouse hover event
	 * has been issued.
	 * 
	 * @return the location of the most recent mouse hover event
	 */
	protected Point getHoverEventLocation() {
		return fHoverEventLocation;
	}
}