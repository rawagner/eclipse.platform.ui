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

package org.eclipse.jface.internal.databinding.api.observable.value;

import org.eclipse.jface.internal.databinding.api.observable.IDiff;

/**
 * @since 3.2
 * 
 */
public interface IValueDiff extends IDiff {

	public Object getOldValue();

	public Object getNewValue();
}
