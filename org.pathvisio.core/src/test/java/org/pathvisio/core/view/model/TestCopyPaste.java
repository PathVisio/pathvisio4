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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.pathvisio.model.PathwayModel;
import org.pathvisio.model.PathwayObject;
import org.pathvisio.model.Shape;
import org.pathvisio.model.type.DataNodeType;
import org.pathvisio.util.XrefUtils;
import org.pathvisio.model.DataNode;
import org.pathvisio.model.Interaction;
import org.pathvisio.model.Label;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.core.view.model.VDataNode;
import org.pathvisio.core.view.model.VDrawable;
import org.pathvisio.core.view.model.VElement;
import org.pathvisio.core.view.model.VPathwayModel;
import org.pathvisio.core.view.model.VPathwayObject;

public class TestCopyPaste extends TestCase {

	PathwayModel pwy = null;
	VPathwayModel vPwy = null;
	DataNode eltDn = null;
	Shape eltSh = null;
	Interaction eltLi = null;
	Label eltLa = null;
	VPathwayObject vDn = null, vSh = null, vLi = null, vLa = null;

	public void setUp() {
		PreferenceManager.init();
		pwy = new PathwayModel();
		eltDn = new DataNode("", DataNodeType.UNDEFINED);
		eltDn.setCenterX(3000);
		eltDn.setCenterY(3000);
		eltDn.setXref(XrefUtils.createXref("1234", "ensembl"));
		eltDn.setTextLabel("Gene");
		eltDn.setWidth(1000);
		eltDn.setHeight(1000);
		eltSh = new Shape();
		eltSh.setCenterX(6000);
		eltSh.setCenterY(3000);
		eltSh.setWidth(300);
		eltSh.setHeight(700);
		eltLi = new Interaction();
		eltLi.setStartLinePointX(500);
		eltLi.setStartLinePointY(1000);
		eltLi.setEndLinePointX(2500);
		eltLi.setEndLinePointY(4000);
		eltLa = new Label("Test");
		eltLa.setCenterX(6000);
		eltLa.setCenterY(6000);
		eltLa.setWidth(300);
		eltLa.setHeight(700);
		eltLa.setTextLabel("Test");
		pwy.add(eltDn);
		pwy.add(eltSh);
		pwy.add(eltLi);
		pwy.add(eltLa);
		vPwy = new VPathwayModel(null);
		vPwy.fromModel(pwy);

		for (VElement e : vPwy.getDrawingObjects()) {
			if (e instanceof VPathwayObject) {
				PathwayObject pe = ((VPathwayObject) e).getPathwayObject();
				if (pe == eltDn) {
					vDn = (VPathwayObject) e;
				} else if (pe == eltSh) {
					vSh = (VPathwayObject) e;
				} else if (pe == eltLi) {
					vLi = (VPathwayObject) e;
				} else if (pe == eltLa) {
					vLa = (VPathwayObject) e;
				}
			}
		}

		assertFalse(vDn == null);
		assertFalse(vSh == null);
		assertFalse(vLi == null);
		assertFalse(vLa == null);
	}

	public void testCopyPaste() {
		PathwayModel pTarget = new PathwayModel();
		VPathwayModel vpTarget = new VPathwayModel(null);
		vpTarget.fromModel(pTarget);

		vPwy.selectObject(vDn);
		vPwy.copyToClipboard();

		vpTarget.pasteFromClipboard();

		PathwayObject pasted = null;
		for (PathwayObject e : pTarget.getPathwayObjects()) {
			if ("1234".equals(((DataNode) e).getXref().getId())) {
				pasted = e;
			}
		}
		// TODO: does not work if VPathwayWrapper is not VPathwaySwing.
//		assertNotNull(pasted);

		// Now copy mappinfo
//		PathwayElement info = pSource.getMappInfo();
//		info.setMapInfoName("test pathway");
//		vpSource.selectObject(vpSource.getPathwayElementView(info));
//		vpSource.copyToClipboard();

//		vpTarget.pasteFromClipboard();

		// test if mappinfo has been pasted to the target pathway
//		assertTrue("test pathway".equals(pTarget.getMappInfo().getMapInfoName()));
	}

}
