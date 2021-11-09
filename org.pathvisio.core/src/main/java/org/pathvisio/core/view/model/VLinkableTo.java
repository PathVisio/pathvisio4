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

import org.pathvisio.model.DataNode;
import org.pathvisio.model.DataNode.State;
import org.pathvisio.model.LineElement.Anchor;
import org.pathvisio.model.LineElement.LinePoint;
import org.pathvisio.model.PathwayObject;
import org.pathvisio.model.Label;
import org.pathvisio.model.Shape;
import org.pathvisio.model.Group;
import org.pathvisio.model.GraphLink.LinkableTo;

/**
 * This class represents the view of a {@link LinkableTo} PathwayElement.
 * Pathway elements {@link DataNode}, {@link State}, {@link Anchor},
 * {@link Label}, {@link Shape}, and {@link Group} can all be referred to by a
 * end {@link LinePoint}.
 * 
 * @author finterly
 */
public interface VLinkableTo extends LinkProvider {

	public int getZOrder(); //TODO has to be public 

	abstract PathwayObject getPathwayObject();

	public VPathwayModel getDrawing();

}
