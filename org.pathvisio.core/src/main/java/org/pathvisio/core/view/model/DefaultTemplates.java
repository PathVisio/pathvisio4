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

import java.awt.Color;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.pathvisio.model.type.ConnectorType;
import org.pathvisio.model.type.DataNodeType;
import org.pathvisio.model.type.HAlignType;
import org.pathvisio.model.type.LineStyleType;
import org.pathvisio.model.type.AnchorShapeType;
import org.pathvisio.model.type.ArrowHeadType;
import org.pathvisio.model.DataNode.State;
import org.pathvisio.model.GraphicalLine;
import org.pathvisio.model.Interaction;
import org.pathvisio.model.DataNode;
import org.pathvisio.model.Shape;
import org.pathvisio.model.LineElement;
import org.pathvisio.model.LineElement.LinePoint;
import org.pathvisio.model.LineElement.Anchor;
import org.pathvisio.model.Label;
import org.pathvisio.model.PathwayModel;
import org.pathvisio.model.PathwayObject;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.model.type.ShapeType;
import org.pathvisio.model.type.StateType;
import org.pathvisio.model.type.VAlignType;
import org.pathvisio.util.ColorUtils;
import org.pathvisio.core.preferences.GlobalPreference;
import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.core.util.Resources;
import org.pathvisio.core.view.Template;

/**
 * Contains a set of templates, patterns of PathwayElements that can be added to
 * a Pathway, including default values.
 */
public abstract class DefaultTemplates {

	/* Some default colors */
	private final static Color COLOR_DEFAULT = Color.BLACK;
	private final static Color COLOR_METABOLITE = Color.BLUE;
	private final static Color COLOR_PATHWAY = new Color(20, 150, 30);
	private final static Color COLOR_LABEL = Color.DARK_GRAY;
	private final static Color COLOR_TRANSPARENT = ColorUtils.hexToColor("#00000000");

	/* Some default graphics */
	private static final int FONTSIZE = 12;
	private static final HAlignType HALIGN = HAlignType.CENTER;
	private static final VAlignType VALIGN = VAlignType.MIDDLE;
	private static final LineStyleType LINESTYLETYPE = LineStyleType.SOLID;
	private static final double LINEWIDTH = 1;
	private static final double ROTATION = 0;

	/* Initial sizes */
	private static final double DATANODE_WIDTH = 90; // NB: "DATANODE" used to be named "GENEPRODUCT"
	private static final double DATANODE_HEIGHT = 25;
	private static final double LABEL_WIDTH = 90;
	private static final double LABEL_HEIGHT = 25;
	private static final double LINE_LENGTH = 30;
	private static final double STATE_SIZE = 15;
	private static final double SHAPE_SIZE = 30;
	private static final double CELLCOMP_LENGTH_1 = 100;
	private static final double CELLCOMP_LENGTH_2 = 200;
	private static final double BRACE_HEIGHT = 15;
	private static final double BRACE_WIDTH = 60;

	/* Default Z-order values */
	public static final int Z_ORDER_GROUP = 0x1000;
	public static final int Z_ORDER_DATANODE = 0x8000;
	public static final int Z_ORDER_STATE = 0x8000 + 10;
	public static final int Z_ORDER_LABEL = 0x7000;
	public static final int Z_ORDER_SHAPE = 0x4000;
	public static final int Z_ORDER_LINE = 0x3000;
	public static final int Z_ORDER_DEFAULT = 0x0000; // default order of uninteresting elements.

	/**
	 * This sets the object to a suitable default size.
	 *
	 * This method is intended to be called right after the object is placed on the
	 * drawing with a click.
	 */
	public static void setInitialSize(PathwayObject o) {
		// set size for Shape (depends on shape type)
		if (o.getClass() == Shape.class) {
			ShapeType type = ((Shape) o).getShapeType();
			if (type.equals(ShapeType.BRACE)) {
				((Shape) o).setWidth(BRACE_WIDTH);
				((Shape) o).setHeight(BRACE_HEIGHT);
			} else if (type.equals(ShapeType.MITOCHONDRIA) || type.equals(ShapeType.CELL)
					|| type.equals(ShapeType.NUCLEUS) || type.equals(ShapeType.ORGANELLE)) {
				((Shape) o).setWidth(CELLCOMP_LENGTH_2);
				((Shape) o).setHeight(CELLCOMP_LENGTH_1);
			} else if (type.equals(ShapeType.SARCOPLASMIC_RETICULUM) || type.equals(ShapeType.ENDOPLASMIC_RETICULUM)
					|| type.equals(ShapeType.GOLGI_APPARATUS)) {
				((Shape) o).setWidth(CELLCOMP_LENGTH_1);
				((Shape) o).setHeight(CELLCOMP_LENGTH_2);
			} else {
				((Shape) o).setWidth(SHAPE_SIZE);
				((Shape) o).setHeight(SHAPE_SIZE);
			}
		} else if (o.getClass() == DataNode.class) {
			((DataNode) o).setWidth(DATANODE_WIDTH);
			((DataNode) o).setHeight(DATANODE_HEIGHT);
		} else if (o.getClass() == State.class) {
			((State) o).setWidth(STATE_SIZE);
			((State) o).setHeight(STATE_SIZE);

		} else if (o.getClass() == Label.class) {
			((Label) o).setWidth(LABEL_WIDTH);
			((Label) o).setHeight(LABEL_HEIGHT);
		} else if (o instanceof LineElement) {
			((LineElement) o).getEndLinePoint().setX(((LineElement) o).getStartLinePoint().getX() + LINE_LENGTH);
			((LineElement) o).getEndLinePoint().setY(((LineElement) o).getStartLinePoint().getX() + LINE_LENGTH);
		} else {
			// nothing
		}
	}

	/**
	 * Abstract base for templates that only add a single PathwayElement to a
	 * Pathway
	 */
	static abstract class SingleElementTemplate implements Template {
		PathwayElement lastAdded;

		protected void addElement(PathwayElement e, PathwayModel p) {
			p.add(e);
			lastAdded = e;
		}

		/**
		 * Default implementation returns the view of the last added object
		 */
		public VElement getDragElement(VPathwayModel vp) {
			if (lastAdded != null) {
				VPathwayObject g = vp.getPathwayElementView(lastAdded);
				if (g == null) {
					throw new IllegalArgumentException("Given VPathway doesn't contain last added element");
				}
				return g;
			}
			return null; // No last object
		}

		public String getDescription() {
			return "Draw new " + getName();
		}

		public URL getIconLocation() {
			return Resources.getResourceURL("new" + getName().toLowerCase() + ".gif");
		}

		public void postInsert(PathwayElement[] newElements) {
		}
	}

	/**
	 * Template for adding a DataNode to a Pathway. Pass a DataNodeType upon
	 * creation.
	 */
	public static class DataNodeTemplate extends SingleElementTemplate {
		DataNodeType type;

		public DataNodeTemplate(DataNodeType type) {
			this.type = type;
		}

		/**
		 * 
		 */
		public DataNode[] addElements(PathwayModel p, double mx, double my) {
			// instantiate data node
			DataNode e = new DataNode(type.toString(), type);
			// set graphics
			Color color = COLOR_DEFAULT;
			ShapeType shapeType = ShapeType.RECTANGLE;
			boolean fontWeight = false;
			if (type.equals(DataNodeType.METABOLITE)) {
				color = COLOR_METABOLITE;
				shapeType = ShapeType.RECTANGLE; // TODO
			} else if (type.equals(DataNodeType.PATHWAY)) {
				color = COLOR_PATHWAY;
				shapeType = ShapeType.ROUNDED_RECTANGLE;//
				fontWeight = true;
			}
			e.setCenterX(mx);
			e.setCenterY(my);
			e.setWidth(DATANODE_WIDTH);
			e.setHeight(DATANODE_HEIGHT);
			// default font-Name/Style/Decoration/StrikeThru/Size, hAlign, vAlign
			e.setTextColor(color);
			e.setFontWeight(fontWeight);
			// default borderStyle, borderWidth, fillColor, rotation
			e.setBorderColor(color);
			e.setShapeType(shapeType);
			e.setZOrder(Z_ORDER_DATANODE);
			if (PreferenceManager.getCurrent().getBoolean(GlobalPreference.DATANODES_ROUNDED)) {
				e.setShapeType(ShapeType.ROUNDED_RECTANGLE);// TODO what is this for???
			}
			// add datanode to pathway model
			p.addDataNode(e);
			lastAdded = e;
			return new DataNode[] { e };
		}

		public VElement getDragElement(VPathwayModel vp) {
			VDataNode g = (VDataNode) super.getDragElement(vp);
			return g.handleSE;
		}

		public String getName() {
			return type.toString();
		}
	}

	/**
	 * Template for adding a Label to a Pathway
	 */
	public static class LabelTemplate extends SingleElementTemplate {

		public Label[] addElements(PathwayModel p, double mx, double my) {
			// instantiate a label
			Label e = new Label("Label");
			// set graphics
			e.setCenterX(mx);
			e.setCenterY(my);
			e.setWidth(LABEL_WIDTH);
			e.setHeight(LABEL_HEIGHT);
			// default font-Name/Style/Decoration/StrikeThru/Size, hAlign, vAlign
			e.setTextColor(COLOR_LABEL);
			// default borderColor, borderStyle, borderWidth, fillColor
			e.setShapeType(ShapeType.NONE);
			e.setZOrder(Z_ORDER_LABEL);
			// add label to pathway model
			p.addLabel(e);
			lastAdded = e;
			return new Label[] { e };
		}

		public VElement getDragElement(VPathwayModel vp) {
			return null; // Don't drag label on insert
		}

		public String getName() {
			return "Label";
		}
	}

	/**
	 * Template for adding a Shape to a Pathway. Pass a ShapeType upon creation.
	 */
	public static class ShapeTemplate extends SingleElementTemplate {
		ShapeType shapeType;

		Set<ShapeType> CELL_COMPONENT_SET = new HashSet<>(Arrays.asList(ShapeType.CELL, ShapeType.NUCLEUS,
				ShapeType.ENDOPLASMIC_RETICULUM, ShapeType.GOLGI_APPARATUS, ShapeType.MITOCHONDRIA,
				ShapeType.SARCOPLASMIC_RETICULUM, ShapeType.ORGANELLE, ShapeType.VESICLE));

		public ShapeTemplate(ShapeType shapeType) {
			this.shapeType = shapeType;
		}

		public Shape[] addElements(PathwayModel p, double mx, double my) {
			// instantiate a shape (pathway element)
			Shape e = new Shape();
			// set graphics
			setInitialSize(e);
			setInitialBorderStyle(e);
			Color color;
			double borderWidth;
			if (CELL_COMPONENT_SET.contains(shapeType)) {
				color = Color.lightGray;
				borderWidth = 3;
			} else {
				color = COLOR_DEFAULT;
				borderWidth = LINEWIDTH;
			}
			// default font-Name/Style/Decoration/StrikeThru/Size, hAlign, vAlign
			e.setTextColor(color);
			// default fillColor
			e.setBorderColor(color);
			e.setBorderWidth(borderWidth);
			e.setShapeType(shapeType);
			e.setZOrder(Z_ORDER_SHAPE);
//			if (e.getShapeType()==ShapeType.BRACE) {
//				e.setRotation(0); // brace TODO
//			}
			// add shape to pathway model
			p.addShape(e);
			lastAdded = e;

			return new Shape[] { e };
		}

		public VElement getDragElement(VPathwayModel vp) {
			VShapedElement s = (VShapedElement) super.getDragElement(vp);
			return s.handleSE;
		}

		public String getName() {
			return shapeType.toString();
		}

		public void setInitialBorderStyle(Shape shape) {
			ShapeType type = shape.getShapeType();
			// set borderStyle depending on shape type
			if (type.equals(ShapeType.CELL) || type.equals(ShapeType.NUCLEUS) || type.equals(ShapeType.ORGANELLE)) {
				shape.setBorderStyle(LineStyleType.DOUBLE);
			} else if (type.equals(ShapeType.CYTOSOL) || type.equals(ShapeType.EXTRACELLULAR)
					|| type.equals(ShapeType.MEMBRANE)) {
				shape.setBorderStyle(LineStyleType.DASHED); // TODO membrane/cytosol never implemented?
			} else {
				shape.setBorderStyle(LINESTYLETYPE);
			}
		}

	}

	/**
	 * Template for adding a single line denoting an interaction to a Pathway.
	 */
	public static class InteractionTemplate extends SingleElementTemplate {
		LineStyleType lineStyle;
		ArrowHeadType startType;
		ArrowHeadType endType;
		ConnectorType connectorType;
		String name;

		public InteractionTemplate(String name, LineStyleType lineStyle, ArrowHeadType startType, ArrowHeadType endType,
				ConnectorType connectorType) {
			this.lineStyle = lineStyle;
			this.startType = startType;
			this.endType = endType;
			this.connectorType = connectorType;
			this.name = name;
		}

		public Interaction[] addElements(PathwayModel p, double mx, double my) {
			// instantiates an interaction
			Interaction e = new Interaction();
			List<LinePoint> points = new ArrayList<LinePoint>();
			points.add(e.new LinePoint(startType, mx, my));
			points.add(e.new LinePoint(endType, mx, my));
			e.setLinePoints(points);
			// setInitialSize(e); TODO not needed?
			// default lineColor, lineWidth
			e.setLineStyle(lineStyle);
			e.setConnectorType(connectorType);
			p.addInteraction(e);
			// add interaction to pathway model
			lastAdded = e;
			return new Interaction[] { e };
		}

		public VElement getDragElement(VPathwayModel vp) {
			VLineElement l = (VLineElement) super.getDragElement(vp);
			return l.getEnd().getHandle();
		}

		public String getName() {
			return name;
		}
	}

	/**
	 * Template for adding a Graphical line to a Pathway.
	 */
	public static class GraphicalLineTemplate extends SingleElementTemplate {
		LineStyleType lineStyle;
		ArrowHeadType startType;
		ArrowHeadType endType;
		ConnectorType connectorType;
		String name;

		public GraphicalLineTemplate(String name, LineStyleType lineStyle, ArrowHeadType startType,
				ArrowHeadType endType, ConnectorType connectorType) {
			this.lineStyle = lineStyle;
			this.startType = startType;
			this.endType = endType;
			this.connectorType = connectorType;
			this.name = name;
		}

		public GraphicalLine[] addElements(PathwayModel p, double mx, double my) {
			// instantiates a graphical line
			GraphicalLine e = new GraphicalLine();
			List<LinePoint> points = new ArrayList<LinePoint>();
			points.add(e.new LinePoint(startType, mx, my));
			points.add(e.new LinePoint(endType, mx, my));
			e.setLinePoints(points);
			// setInitialSize(e); TODO not needed?
			// default lineColor, lineWidth
			e.setLineStyle(lineStyle);
			e.setConnectorType(connectorType);
			// add graphical line to pathway model
			p.addGraphicalLine(e);
			lastAdded = e;
			return new GraphicalLine[] { e };
		}

		public VElement getDragElement(VPathwayModel vp) {
			VLineElement l = (VLineElement) super.getDragElement(vp);
			return l.getEnd().getHandle();
		}

		public String getName() {
			return name;
		}
	}

	/**
	 * Template for an interaction, two datanodes with a connecting line.
	 */
	public static class DataNodeInteractionTemplate implements Template {
		final static int OFFSET_LINE = 5;
		DataNode lastStartNode;
		DataNode lastEndNode;
		Interaction lastLine;

		ArrowHeadType endType;
		ArrowHeadType startType;

		LineStyleType lineStyle;

		public DataNodeInteractionTemplate() {
			endType = ArrowHeadType.UNDIRECTED;
			startType = ArrowHeadType.UNDIRECTED;
			lineStyle = LineStyleType.SOLID;
		}

		public PathwayElement[] addElements(PathwayModel p, double mx, double my) {
			// Add two GeneProduct DataNodes, connected by a line
			DataNodeTemplate dnt = new DataNodeTemplate(DataNodeType.GENEPRODUCT);
			lastStartNode = dnt.addElements(p, mx, my)[0];
//			setInitialSize(lastStartNode); TODO 
			lastEndNode = dnt.addElements(p, mx + 2 * lastStartNode.getWidth(), my)[0];
//			setInitialSize(lastEndNode); TODO 

			InteractionTemplate lnt = new InteractionTemplate("defaultline", lineStyle, startType, endType,
					ConnectorType.STRAIGHT);
			lastLine = lnt.addElements(p, mx, my)[0];
			lastLine.getStartLinePoint().linkTo(lastStartNode, 1, 0);
			lastLine.getEndLinePoint().linkTo(lastEndNode, -1, 0);

			return new PathwayElement[] { lastLine, lastStartNode, lastEndNode };
		}

		public VElement getDragElement(VPathwayModel vp) {
			return null;
		}

		public String getName() {
			return "interaction";
		}

		public String getDescription() {
			return "Draw new " + getName();
		}

		public URL getIconLocation() {
			return Resources.getResourceURL("new" + getName().toLowerCase() + ".gif");
		}

		public void postInsert(PathwayElement[] newElements) {
		}
	}

	/**
	 * Template for an inhibition interaction, two datanodes with a MIM_INHIBITION
	 * line.
	 */
	public static class InhibitionInteractionTemplate extends DataNodeInteractionTemplate {
		@Override
		public PathwayElement[] addElements(PathwayModel p, double mx, double my) {
			super.addElements(p, mx, my);
			lastLine.setEndLineType(ArrowHeadType.INHIBITION);
			return new PathwayElement[] { lastLine, lastStartNode, lastEndNode };
		}

		@Override
		public String getName() {
			return "inhibition interaction";
		}
	}

	/**
	 * Template for a stimulation interaction, two datanodes with a MIM_STIMULATION
	 * line.
	 */
	public static class StimulationInteractionTemplate extends DataNodeInteractionTemplate {
		@Override
		public PathwayElement[] addElements(PathwayModel p, double mx, double my) {
			super.addElements(p, mx, my);
			lastLine.setEndLineType(ArrowHeadType.STIMULATION);
			return new PathwayElement[] { lastLine, lastStartNode, lastEndNode };
		}

		@Override
		public String getName() {
			return "stimulation interaction";
		}
	}

	/**
	 * Template for a phosphorylation interaction, two Protein Datanodes with a
	 * MIM_MODIFICATION line.
	 */

	public static class PhosphorylationTemplate extends DataNodeInteractionTemplate {
		// static final double OFFSET_CATALYST = 50;
		PathwayElement lastPhosphorylation;
		// PathwayElement lastPhosLine;

		public PathwayElement[] addElements(PathwayModel p, double mx, double my) {
			super.addElements(p, mx, my);
			lastStartNode.setType(DataNodeType.PROTEIN);
			lastEndNode.setType(DataNodeType.PROTEIN);
			lastStartNode.setTextLabel("Protein");
			lastEndNode.setTextLabel("P-Protein");
			lastLine.getEndLinePoint().setArrowHead(ArrowHeadType.CONVERSION);
			// instantiates and adds a state to data node and pathway model
			State e = lastEndNode.addState("P", StateType.PROTEIN_MODIFICATION, 1.0, 1.0);
			// set graphics
			setInitialSize(e);
			// default textColor, font-Name/tWeight/Style/Decoration/Strikethru/Size,
			// hAlign, vAlign
			// default borderColor, borderStyle, borderWidth, fillColor
			e.setShapeType(ShapeType.OVAL);
			e.setZOrder(Z_ORDER_STATE);
			return new PathwayElement[] { lastStartNode, lastEndNode, lastLine };
		}

		public String getName() {
			return "Phosphorylation";
		}
	}

	/**
	 * Template for a reaction, two Metabolites with a connecting arrow, and a
	 * GeneProduct (enzyme) pointing to an anchor on that arrow.
	 */
	public static class ReactionTemplate extends DataNodeInteractionTemplate {
		static final double OFFSET_CATALYST = 50;
		DataNode lastCatalyst;
		Interaction lastCatLine;

		public PathwayElement[] addElements(PathwayModel p, double mx, double my) {
			super.addElements(p, mx, my);
			DataNodeTemplate dnt = new DataNodeTemplate(DataNodeType.GENEPRODUCT);
			lastCatalyst = dnt.addElements(p, mx + lastStartNode.getWidth(), my - OFFSET_CATALYST)[0];
//			setInitialSize(lastCatalyst); TODO 
			lastCatalyst.setTextLabel("Catalyst");

			lastStartNode.setType(DataNodeType.METABOLITE);
			lastStartNode.setBorderColor(COLOR_METABOLITE);
			lastStartNode.setTextColor(COLOR_METABOLITE);
			lastStartNode.setShapeType(ShapeType.ROUNDED_RECTANGLE);
			lastStartNode.setTextLabel("Substrate");

			lastEndNode.setType(DataNodeType.METABOLITE);
			lastEndNode.setBorderColor(COLOR_METABOLITE);
			lastEndNode.setTextColor(COLOR_METABOLITE);
			lastEndNode.setShapeType(ShapeType.ROUNDED_RECTANGLE);
			lastEndNode.setTextLabel("Product");

			lastLine.setEndLineType(ArrowHeadType.CONVERSION);
			Anchor anchor = lastLine.addAnchor(0.5, AnchorShapeType.SQUARE);

			InteractionTemplate lnt = new InteractionTemplate("line", LineStyleType.SOLID, ArrowHeadType.UNDIRECTED,
					ArrowHeadType.UNDIRECTED, ConnectorType.STRAIGHT);
			lastCatLine = lnt.addElements(p, mx, my)[0];

			lastCatLine.getStartLinePoint().linkTo(lastCatalyst, 0, 1);
			lastCatLine.getEndLinePoint().linkTo(anchor, 0, 0);
			lastCatLine.setEndLineType(ArrowHeadType.CATALYSIS);

			return new PathwayElement[] { lastStartNode, lastEndNode, lastLine, lastCatalyst };
		}

		public String getName() {
			return "reaction";
		}
	}

	/**
	 * Template for a reaction, two Metabolites with a connecting arrow, and a
	 * GeneProduct (enzyme) pointing to an anchor on that arrow.
	 */
	public static class ReversibleReactionTemplate extends DataNodeInteractionTemplate {
		static final double OFFSET_CATALYST = 50;
		DataNode lastCatalyst;
		DataNode lastCatalyst2;
		Interaction lastCatLine;
		Interaction lastCatLine2;
		Interaction lastReverseLine;

		public PathwayElement[] addElements(PathwayModel p, double mx, double my) {
			super.addElements(p, mx, my);
			DataNodeTemplate dnt = new DataNodeTemplate(DataNodeType.PROTEIN);
			lastCatalyst = dnt.addElements(p, mx + lastStartNode.getWidth(), my - OFFSET_CATALYST)[0];
//			setInitialSize(lastCatalyst); TODO
			lastCatalyst.setTextLabel("Catalyst 1");

			lastCatalyst2 = dnt.addElements(p, mx + lastStartNode.getWidth(), my + OFFSET_CATALYST)[0];
//			setInitialSize(lastCatalyst2); TODO
			lastCatalyst2.setTextLabel("Catalyst 2");

			lastStartNode.setType(DataNodeType.METABOLITE);
			lastStartNode.setBorderColor(COLOR_METABOLITE);
			lastStartNode.setTextColor(COLOR_METABOLITE);
			lastStartNode.setShapeType(ShapeType.ROUNDED_RECTANGLE);
			lastStartNode.setTextLabel("Metabolite 1");

			lastEndNode.setType(DataNodeType.METABOLITE);
			lastEndNode.setBorderColor(COLOR_METABOLITE);
			lastEndNode.setTextColor(COLOR_METABOLITE);
			lastEndNode.setShapeType(ShapeType.ROUNDED_RECTANGLE);
			lastEndNode.setTextLabel("Metabolite 2");
			lastLine.getEndLinePoint().setArrowHead(ArrowHeadType.CONVERSION);

			Anchor anchor = lastLine.addAnchor(0.5, AnchorShapeType.SQUARE);

			InteractionTemplate lnt = new InteractionTemplate("line", LineStyleType.SOLID, ArrowHeadType.UNDIRECTED,
					ArrowHeadType.UNDIRECTED, ConnectorType.STRAIGHT);
			lastCatLine = lnt.addElements(p, mx, my)[0]; // TODO Cast?

			lastCatLine.getStartLinePoint().linkTo(lastCatalyst, 0, 1);
			lastCatLine.getEndLinePoint().linkTo(anchor, 0, 0);
			lastCatLine.getEndLinePoint().setArrowHead(ArrowHeadType.CATALYSIS);

			InteractionTemplate rev = new InteractionTemplate("line", LineStyleType.SOLID, ArrowHeadType.UNDIRECTED,
					ArrowHeadType.UNDIRECTED, ConnectorType.STRAIGHT);
			lastReverseLine = rev.addElements(p, mx, my)[0]; // TODO Cast?

			lastReverseLine.getStartLinePoint().linkTo(lastEndNode, -1, 0.5);
			lastReverseLine.getEndLinePoint().linkTo(lastStartNode, 1, 0.5);
			lastReverseLine.getEndLinePoint().setArrowHead(ArrowHeadType.CONVERSION);

			Anchor anchor2 = lastReverseLine.addAnchor(0.5, AnchorShapeType.SQUARE);

			InteractionTemplate lnt2 = new InteractionTemplate("line", LineStyleType.SOLID, ArrowHeadType.UNDIRECTED,
					ArrowHeadType.UNDIRECTED, ConnectorType.STRAIGHT);
			lastCatLine2 = lnt2.addElements(p, mx, my)[0]; // TODO Cast?

			lastCatLine2.getStartLinePoint().linkTo(lastCatalyst2, 0, -1);
			lastCatLine2.getEndLinePoint().linkTo(anchor2, 0, 0);
			lastCatLine2.getEndLinePoint().setArrowHead(ArrowHeadType.CATALYSIS);
			// These elements are selected in PV, so users can move them around.
			return new PathwayElement[] { lastStartNode, lastEndNode, lastLine, lastCatalyst, lastCatalyst2 };
		}

		public String getName() {
			return "ReversibleReaction";
		}
	}

}
