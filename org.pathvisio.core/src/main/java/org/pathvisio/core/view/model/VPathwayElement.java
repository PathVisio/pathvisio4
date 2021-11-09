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

import java.awt.geom.Point2D;
import java.util.List;

import org.pathvisio.event.PathwayObjectEvent;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.model.PathwayElement.CitationRef;

/**
 * This class is a parent class for all graphics which can have a
 * {@link VCitation}. This class represents the view of a {@link ElementInfo}
 * pathway element.
 * 
 * 
 * @author finterly
 */
public abstract class VPathwayElement extends VPathwayObject {

	private VCitation vCitation;

	/**
	 * @param canvas
	 * @param gdata
	 */
	public VPathwayElement(VPathwayModel canvas, PathwayElement gdata) {
		super(canvas, gdata);
		checkCitation(); // TODO
	}

	/**
	 * Gets the model representation (PathwayElement) of this class
	 * 
	 * @return gdata
	 */
	@Override
	public PathwayElement getPathwayObject() {
		return (PathwayElement) super.getPathwayObject();
	}

	/**
	 * @return
	 */
	protected VCitation createCitation() {
		return new VCitation(canvas, this, new Point2D.Double(1, -1));
	}

	/**
	 * @return
	 */
	protected VCitation getCitation() {
		return vCitation;
	}

	/**
	 * @param vCitation
	 */
	protected void setCitation(VCitation vCitation) {
		this.vCitation = vCitation;
	}

	/**
	 * Check for {@link VCitation} if object has {@link CitationRef}s. Create or
	 * destroy vCitation if necessary.
	 */
	public final void checkCitation() {
		List<CitationRef> citationRefs = getPathwayObject().getCitationRefs();
		// if object has citationRefs but no vCitation, create
		if (citationRefs.size() > 0 && vCitation == null) {
			vCitation = createCitation();
			addChild(vCitation);
		}
		// if object has no citationRefs but has vCitation, destroy
		else if (citationRefs.size() == 0 && vCitation != null) {
			vCitation.destroy();
			removeChild(vCitation);
			vCitation = null;
		}
		// if object already has citationRefs and vCitation, redraw
		if (vCitation != null) {
			vCitation.markDirty();
		}
	}

	/**
	 *
	 */
	@Override
	public void gmmlObjectModified(PathwayObjectEvent e) {
		if (listen) {
			markDirty(); // mark everything dirty
			checkCitation();
		}
	}

	/**
	 *
	 */
	@Override
	protected void destroy() {
		super.destroy();
		vCitation = null;
		// View should not remove its model
//		Pathway parent = gdata.getParent();
//		if(parent != null) parent.remove(gdata);
	}

}
