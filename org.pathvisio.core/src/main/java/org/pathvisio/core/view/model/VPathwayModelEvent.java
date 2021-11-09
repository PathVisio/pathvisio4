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

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import org.pathvisio.core.view.MouseEvent;

/**
 * event sent by VPathway upon modification of one or more VPathwayElements.
 */
public class VPathwayModelEvent extends EventObject {

	/** Possible event types */
	public static enum VPathwayEventType
	{
		ELEMENT_ADDED,
		EDIT_MODE_ON,
		EDIT_MODE_OFF,
		MODEL_LOADED,
		ELEMENT_DOUBLE_CLICKED,
		ELEMENT_DRAWN,
		ELEMENT_CLICKED_UP,
		ELEMENT_CLICKED_DOWN,
		ELEMENT_HOVER,
		HREF_ACTIVATED;
	}

	private VPathwayEventType type;
	private List<VElement> affectedElements;
	private Graphics2D g2d;
	private MouseEvent mouseEvent;

	public VPathwayModelEvent(VPathwayModel source, VPathwayEventType type) {
		super(source);
		this.type = type;
	}

	public VPathwayModelEvent(VPathwayModel source, List<VElement> affectedElements, VPathwayEventType type) {
		this(source, type);
		this.affectedElements = affectedElements;
	}

	public VPathwayModelEvent(VPathwayModel source, VElement affectedElement, VPathwayEventType type) {
		this(source, type);
		List<VElement> afe = new ArrayList<VElement>();
		afe.add(affectedElement);
		this.affectedElements = afe;
	}

	public VPathwayModelEvent(VPathwayModel source, VElement affectedElement, Graphics2D g2d, VPathwayEventType type) {
		this(source, affectedElement, type);
		this.g2d = g2d;
	}

	public VPathwayModelEvent(VPathwayModel source, VElement affectedElement, MouseEvent e, VPathwayEventType type) {
		this(source, affectedElement, type);
		mouseEvent = e;
	}

	public VPathwayModelEvent(VPathwayModel source, List<VElement> affectedElements, MouseEvent e, VPathwayEventType type) {
		this(source, affectedElements, type);
		mouseEvent = e;
	}

	public MouseEvent getMouseEvent() {
		return mouseEvent;
	}

	public VElement getAffectedElement() {
		return affectedElements.get(0);
	}

	public List<VElement> getAffectedElements() {
		return affectedElements;
	}

	public VPathwayEventType getType() {
		return type;
	}

	public Graphics2D getGraphics2D() {
		return g2d;
	}

	public VPathwayModel getVPathway() {
		return (VPathwayModel)getSource();
	}
}
