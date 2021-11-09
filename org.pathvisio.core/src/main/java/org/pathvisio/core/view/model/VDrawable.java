/*******************************************************************************
 * PathVisio, a tool for data visualization and analysis using biological pathways
 * Copyright 2006-2021 BiGCaT Bioinformatics, WikiPathways
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.pathvisio.core.view.model;

import org.pathvisio.model.Drawable;

/**
 * This class represents the view of a {@link Drawable} PathwayElement. This
 * class is implemented by {@link VShapedElement} and {@link VLineElement}, as
 * well as by {@link VLinePoint}, and {@link VAnchor}.
 * 
 * @author finterly
 */
public interface VDrawable {

	/**
	 * Gets the model representation (PathwayElement) of this class
	 * 
	 * @return
	 */
	public Drawable getPathwayObject();
	
	/**
	 * Returns the z-order from the model //TODO public?
	 */
	public int getZOrder();

}
