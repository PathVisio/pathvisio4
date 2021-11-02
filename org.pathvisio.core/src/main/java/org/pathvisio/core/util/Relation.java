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
package org.pathvisio.core.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.pathvisio.core.debug.Logger;
import org.pathvisio.model.type.ArrowHeadType;
import org.pathvisio.model.PathwayModel;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.model.GraphLink.LinkableFrom;
import org.pathvisio.model.Group;
import org.pathvisio.model.Groupable;
import org.pathvisio.model.LineElement;
import org.pathvisio.model.LineElement.Anchor;
import org.pathvisio.model.LineElement.LinePoint;

/**
 * <p>
 * Class to parse a relation between GPML objects, e.g. a biochemical reaction.
 * A relation can be created from a line that connects two objects of type
 * datanode, shape or label. The following fields will be created:
 * <ol>
 * <li>- LEFT: an element that acts on the left side of an interaction
 * <li>- RIGHT: an element that acts on the right side of an interaction
 * <li>- MEDIATOR: an element that acts as mediator of an interaction
 * <li>- SOURCE: the pathway containing the relation
 * </ol>
 * 
 * The following example illustrates how the fields will be assigned.
 *
 * Consider the following interaction:
 * 
 * <pre>
 *            F      E
 *            |	    /
 *            v    /
 * A ---o-----o---o--> B
 *     /      T
 *    /       |
 *   D        C(C1, C2)
 * </pre>
 * 
 * Where C is a group that contains C1 and C2.
 *
 * The line A-B will serve as base for the relation, A will be added to the LEFT
 * field, B to the RIGHT field. For all other elements that are connected to
 * anchors of this line, the following rules apply:
 *
 * <ol>
 * <li>- If the line starts at the anchor and ends at the element, the element
 * will be added to the LEFT field
 * <li>- If the line starts at the element, ends at the anchor and has *no* end
 * arrowHeadType (no arrow, T-bar or other shape), the element will be added to
 * the RIGHT field
 * <li>- Else, the element will be added to the MEDIATOR field.
 * </ol>
 * Additionally, if the element to be added is a group, all nested elements will
 * be added recursively.
 *
 * So in the example, the following fields will be created:
 * <ol>
 * <li>A: LEFT
 * <li>D: LEFT
 * <li>F: MEDIATOR
 * <li>C: MEDIATOR
 * <li>C1:MEDIATOR
 * <li>C2:MEDIATOR
 * <li>E: RIGHT
 * <li>B: RIGHT
 * </ol>
 * 
 * @author thomas, finterly
 */
public class Relation {
	private Set<PathwayElement> lefts = new HashSet<PathwayElement>();
	private Set<PathwayElement> rights = new HashSet<PathwayElement>();
	private Set<PathwayElement> mediators = new HashSet<PathwayElement>();

	private Map<PathwayElement, PathwayElement> mediatorLines = new HashMap<PathwayElement, PathwayElement>();

	/**
	 * Parse a relation.
	 * 
	 * @param relationLine The line that defines the relation.
	 */
	public Relation(LineElement relationLine) {
//		if (relationLine() != ObjectType.LINE) { // TODO LINE as in Interaction or LineElement????
//			throw new IllegalArgumentException("Object type should be line!");
//		}
		PathwayModel pathway = relationLine.getPathwayModel();
		if (pathway == null) {
			throw new IllegalArgumentException("Object has no parent pathway");
		}
		// Add obvious left and right
		addLeft(pathway.getElementById(relationLine.getStartLinePoint().getGraphRef()));
		addRight(pathway.getElementById(relationLine.getEndLinePoint().getGraphRef()));
		// Find all connecting lines (via anchors)
		for (Anchor ma : ((LineElement) relationLine).getAnchors()) {
			for (LinkableFrom grc : ma.getLinkableFroms()) {
				if (grc instanceof LinePoint) {
					LinePoint mp = (LinePoint) grc;
					LineElement line = mp.getLineElement();
					if (line.getStartLinePoint() == mp) {
						// Start linked to anchor, make it a 'right'
						if (line.getEndLinePoint().isLinked()) {
							addRight(pathway.getElementById(line.getEndLinePoint().getGraphRef()));
						}
					} else {
						// End linked to anchor
						if (line.getEndLineType() == ArrowHeadType.UNDIRECTED) {
							// Add as 'left'
							addLeft(pathway.getElementById(line.getStartLinePoint().getGraphRef()));
						} else {
							// Add as 'mediator'
							addMediator(line, pathway.getElementById(line.getStartLinePoint().getGraphRef()));
						}
					}
				} else {
					Logger.log.warn("unsupported LinkableFrom: " + grc);
				}
			}
		}
	}

	void addLeft(PathwayElement pwe) {
		addElement(pwe, lefts);
	}

	void addRight(PathwayElement pwe) {
		addElement(pwe, rights);
	}

	void addMediator(PathwayElement line, PathwayElement pwe) {
		Set<PathwayElement> added = addElement(pwe, mediators);
		for (PathwayElement m : added)
			mediatorLines.put(m, line);
	}

	Set<PathwayElement> addElement(PathwayElement pwe, Set<PathwayElement> set) {
		Set<PathwayElement> added = new HashSet<PathwayElement>();

		if (pwe != null) {
			// If it's a group, add all subelements
			if (pwe.getClass() == Group.class) {
				for (Groupable ge : ((Group) pwe).getPathwayElements()) {
					added.addAll(addElement((PathwayElement) ge, set)); // TODO groupable?
				}
			}
			set.add(pwe);
			added.add(pwe);
		}
		return added;
	}

	public Set<PathwayElement> getLefts() {
		return lefts;
	}

	public Set<PathwayElement> getRights() {
		return rights;
	}

	public Set<PathwayElement> getMediators() {
		return mediators;
	}

	/**
	 * Get the line that connects the given mediator to the relation. This can be
	 * used to determine how the mediator influences the relation (e.g. inhibition
	 * or activation).
	 */
	public PathwayElement getMediatorLine(PathwayElement mediator) {
		return mediatorLines.get(mediator);
	}
}
