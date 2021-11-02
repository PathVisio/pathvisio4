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

import org.pathvisio.core.view.AbstractLinkAnchorDelegate;
import org.pathvisio.model.GraphLink.LinkableTo;
import org.pathvisio.model.ShapedElement;

/**
 * Utility class for creating and destroying LinkAnchors around a rectangular
 * element.
 */
public class DefaultLinkAnchorDelegate extends AbstractLinkAnchorDelegate {

	private final VLinkableTo parent;
	private final VPathwayModel canvas;

	DefaultLinkAnchorDelegate(VLinkableTo parent) {
		this.parent = parent;
		this.canvas = parent.getDrawing();
	}

	private int numLinkanchorsH = -1;
	private int numLinkanchorsV = -1;

	private static final int MIN_SIZE_LA = 25;

	public void showLinkAnchors() {
		// TODO disallowLinks?
//		if (parent instanceof VGroup && parent.gdata.getGroupType().isDisallowLinks()) {
//			return;
//		}
		// one link anchor per side. if large enough 3 link anchors per side.
		int numAnchors = 3;
		int numH = 1;
		int numV = 1;
		// more anchors if linkableTo is a shapedElement of a certain size
		if (parent.getPathwayElement() instanceof ShapedElement) {
			numH = ((ShapedElement) parent.getPathwayElement()).getWidth() < MIN_SIZE_LA ? 1 : numAnchors;
			numV = ((ShapedElement) parent.getPathwayElement()).getHeight() < MIN_SIZE_LA ? 1 : numAnchors;
		}
		if (numH != numLinkanchorsH || numV != numLinkanchorsV) {
			linkAnchors.clear();
			double deltaH = 2.0 / (numH + 1);
			for (int i = 1; i <= numH; i++) {
				linkAnchors.add(
						new LinkAnchor(canvas, parent, (LinkableTo) parent.getPathwayElement(), -1 + i * deltaH, -1));
				linkAnchors.add(
						new LinkAnchor(canvas, parent, (LinkableTo) parent.getPathwayElement(), -1 + i * deltaH, 1));
			}
			double deltaV = 2.0 / (numV + 1);
			for (int i = 1; i <= numV; i++) {
				linkAnchors.add(
						new LinkAnchor(canvas, parent, (LinkableTo) parent.getPathwayElement(), -1, -1 + i * deltaV));
				linkAnchors.add(
						new LinkAnchor(canvas, parent, (LinkableTo) parent.getPathwayElement(), 1, -1 + i * deltaV));
			}
			numLinkanchorsH = numH;
			numLinkanchorsV = numV;
		}
	}

	public void hideLinkAnchors() {
		super.hideLinkAnchors();
		numLinkanchorsV = -1;
		numLinkanchorsH = -1;
	}

}
