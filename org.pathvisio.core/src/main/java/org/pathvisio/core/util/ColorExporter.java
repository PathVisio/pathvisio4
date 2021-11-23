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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pathvisio.debug.Logger;
import org.pathvisio.core.model.BatikImageExporter;
import org.pathvisio.io.ConverterException;
import org.pathvisio.core.model.ImageExporter;
import org.pathvisio.model.PathwayModel;
import org.pathvisio.model.PathwayObject;
import org.pathvisio.model.ShapedElement;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.model.DataNode;
import org.pathvisio.model.Group;
import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.core.view.model.VElement;
import org.pathvisio.core.view.model.VPathwayModelEvent;
import org.pathvisio.core.view.model.VPathwayModelListener;
import org.pathvisio.core.view.model.VPathwayModel;
import org.pathvisio.core.view.model.VPathwayObject;
import org.pathvisio.core.view.model.VShapedElement;
import org.pathvisio.core.view.model.VPathwayModelEvent.VPathwayEventType;

/**
 * Utility that takes a set of graphId/Color pairs and exports a pathway image
 * after coloring the objects with the specified graphIds. TODO Move to jar
 * file?
 * 
 * TODO Color ElementIds or Xrefs...Only DataNodes
 * 
 * @author thomas
 */
public class ColorExporter implements VPathwayModelListener {
	
	Map<PathwayObject, List<Color>> colors;
	VPathwayModel vPathway;

	public ColorExporter(PathwayModel pathway, Map<PathwayObject, List<Color>> colors) {
		this.colors = colors;
		vPathway = new VPathwayModel(null);
		vPathway.fromModel(pathway);
	}

	public void dispose() {
		vPathway.dispose();
	}

	public void export(BatikImageExporter exporter, File outputFile) throws ConverterException {
		vPathway.addVPathwayListener(this);
		doHighlight();
		exporter.doExport(outputFile, vPathway);
	}

	public void vPathwayModelEvent(VPathwayModelEvent e) {
		if (e.getType() == VPathwayEventType.ELEMENT_DRAWN) {
			VElement vpwe = e.getAffectedElement();
			if (vpwe instanceof VPathwayObject) {
				PathwayObject pwe = ((VPathwayObject) vpwe).getPathwayObject();
				List<Color> elmColors = colors.get(pwe);
				if (elmColors != null && elmColors.size() > 0) {
					Logger.log.info("Coloring " + pwe + " with " + elmColors);
					if (pwe.getClass() == DataNode.class) {
						doColor(e.getGraphics2D(), (VPathwayObject) vpwe, elmColors);
						drawLabel(e.getGraphics2D(), (VPathwayObject) vpwe);
					} else if (pwe.getClass() == Group.class) {
						doColor(e.getGraphics2D(), (VPathwayObject) vpwe, elmColors);
					}
				}
			}
		}
	}

	/**
	 * TODO
	 * 
	 * @param g
	 * @param pwe
	 */
	private void drawLabel(Graphics2D g, VPathwayObject pwe) {
		Graphics2D g2d = (Graphics2D) g.create();
		Rectangle2D area = pwe.getVBounds();
		g2d.setClip(area);
		g2d.setColor(Color.black);
		if (pwe instanceof VShapedElement) {
			String label = ((ShapedElement) pwe.getPathwayObject()).getTextLabel();
			if (label != null && !"".equals(label)) {
				TextLayout tl = new TextLayout(label, g2d.getFont(), g2d.getFontRenderContext());
				Rectangle2D tb = tl.getBounds();

				tl.draw(g2d, (int) area.getX() + (int) (area.getWidth() / 2) - (int) (tb.getWidth() / 2),
						(int) area.getY() + (int) (area.getHeight() / 2) + (int) (tb.getHeight() / 2));
			}
		}
	}

	private void doColor(Graphics2D g, VPathwayObject vpe, List<Color> colors) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setClip(vpe.getVBounds());

		Rectangle area = vpe.getVBounds().getBounds();

		int nr = colors.size();
		int left = area.width % nr; // Space left after dividing, give to last rectangle
		int w = area.width / nr;
		for (int i = 0; i < nr; i++) {
			g2d.setColor(colors.get(i));
			Rectangle r = new Rectangle(area.x + w * i, area.y, w + ((i == nr - 1) ? left : 0), area.height);
			g2d.fill(r);
		}
		g2d.setColor(((ShapedElement) vpe.getPathwayObject()).getTextColor()); // TODO
		g2d.drawRect(area.x, area.y, area.width - 1, area.height - 1);
	}

	/**
	 * Highlight all object but DataNodes and Groups. Only the first color from the
	 * hashmap will be used.
	 */
	private void doHighlight() {
		for (VElement vpe : vPathway.getDrawingObjects()) {
			if (vpe instanceof VPathwayObject) {
				PathwayObject pwe = ((VPathwayObject) vpe).getPathwayObject(); // TODO object or element?
				List<Color> elmColors = colors.get(pwe);
				if (elmColors != null && elmColors.size() > 0) {
					if (pwe.getClass() != DataNode.class && pwe.getClass() != Group.class) {
						vpe.highlight(elmColors.get(0));
					}
				}
			}
		}
	}

	public static void main(String[] args) {
		PreferenceManager.init();

		if (args.length < 2) {
			printHelp();
			System.exit(-1);
		}

		try {
			String inStr = args[0];
			String outStr = args[1];

			// Enable MiM support (for export to graphics formats)
//			MIMShapes.registerShapes(); TODO 

			Logger.log.setStream(System.err);
			Logger.log.setLogLevel(false, false, true, true, true, true);

			File inputFile = new File(inStr);
			File outputFile = new File(outStr);
			PathwayModel pathway = new PathwayModel();
			pathway.readFromXml(inputFile, true);

			// Parse command line arguments
			Map<PathwayObject, List<Color>> colors = new HashMap<PathwayObject, List<Color>>();

			for (int i = 2; i < args.length - 1; i++) {
				if ("-c".equals(args[i])) {
					PathwayObject pwe = pathway.getPathwayObject(args[++i]);
					String colorStr = args[++i];
					if (pwe != null) {
						List<Color> pweColors = colors.get(pwe);
						if (pweColors == null)
							colors.put(pwe, pweColors = new ArrayList<Color>());
						int cv = Integer.parseInt(colorStr, 16);
						pweColors.add(new Color(cv));
					}
				}
			}

			BatikImageExporter exporter = null;
			if (outStr.endsWith(ImageExporter.TYPE_PNG)) {
				exporter = new BatikImageExporter(ImageExporter.TYPE_PNG);
			} else if (outStr.endsWith(ImageExporter.TYPE_PDF)) {
				exporter = new BatikImageExporter(ImageExporter.TYPE_PDF);
			} else if (outStr.endsWith(ImageExporter.TYPE_TIFF)) {
				exporter = new BatikImageExporter(ImageExporter.TYPE_TIFF);
			} else {
				exporter = new BatikImageExporter(ImageExporter.TYPE_SVG);
			}
			ColorExporter colorExp = new ColorExporter(pathway, colors);
			colorExp.export(exporter, outputFile);
			colorExp.dispose();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-2);
			printHelp();
		}
	}

	static void printHelp() {
		System.err.println(
				"Usage:\n" + "\tjava org.pathvisio.data.ColorExporter <inputFile> <outputFile> [-c graphId color]\n"
						+ "Parameters:\n" + "\t-c\tA string containing the graphId of the object to color, followed "
						+ "by the color to be used for that object (hexadecimal, e.g. FF0000 for red)\n"
						+ "The export format is determined by the output file extension and can be one of: "
						+ "svg, pdf, png, tiff");
	}
}
