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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import org.pathvisio.model.type.AnchorShapeType;
import org.pathvisio.model.GraphLink.LinkableFrom;
import org.pathvisio.model.LineElement.Anchor;
import org.pathvisio.model.LineElement.LinePoint;
import org.pathvisio.model.PathwayObject;
import org.pathvisio.core.view.Adjustable;
import org.pathvisio.model.shape.AnchorShape;
import org.pathvisio.model.shape.ShapeRegistry;

/**
 * VAnchor is the view representation of {@link Anchor}.
 *
 * It is stuck to a Line and can move one-dimensionally across it. It has a
 * handle so the user can drag it.
 * 
 * @author unknown, finterly
 */
public class VAnchor extends VElement implements VLinkableTo, Adjustable {

	private Anchor anchor;
	private VLineElement vLineElement;
	private Handle handle;

	private double mx = Double.NaN;
	private double my = Double.NaN;

	public VAnchor(Anchor mAnchor, VLineElement parent) {
		super(parent.getDrawing());
		this.anchor = mAnchor;
		this.vLineElement = parent;
		updatePosition();
	}

	/**
	 * TODO
	 */
	@Override
	public PathwayObject getPathwayObject() {
		// TODO Auto-generated method stub
		return null;
	}

	public double getVx() {
		return vFromM(mx);
	}

	public double getVy() {
		return vFromM(my);
	}

	public Handle getHandle() {
		return handle;
	}

	public Anchor getAnchor() {
		return anchor;
	}

	protected void destroy() {
		super.destroy();
		vLineElement.removeVAnchor(this);
	}

	@Override
	protected void destroyHandles() {
		if (handle != null) {
			handle.destroy();
			handle = null;
		}
	}

	protected void createHandles() {
		handle = new Handle(Handle.Freedom.FREE, this, this);
		double lc = anchor.getPosition();
		Point2D position = vLineElement.vFromL(lc);
		handle.setVLocation(position.getX(), position.getY());
	}

	void updatePosition() {
		double lc = anchor.getPosition();

		Point2D position = vLineElement.vFromL(lc);
		if (handle != null)
			handle.setVLocation(position.getX(), position.getY());

		mx = mFromV(position.getX());
		my = mFromV(position.getY());

		// Redraw graphRefs
		for (LinkableFrom ref : anchor.getLinkableFroms()) {
			if (ref instanceof LinePoint) {
				VLinePoint vp = canvas.getPoint((LinePoint) ref);
				if (vp != null && vp.getLine() != vLineElement) {
					vp.getLine().recalculateConnector();
				}
			}
		}
	}

	public void adjustToHandle(Handle h, double vx, double vy) {
		double position = vLineElement.lFromV(new Point2D.Double(vx, vy));
		anchor.setPosition(position);
	}

	private AnchorShape getAnchorShape() {
		AnchorShape shape = ShapeRegistry.getAnchor(anchor.getShapeType().getName());

		if (shape != null) {
			AffineTransform f = new AffineTransform();
			double scaleFactor = vFromM(1.0);
			f.translate(getVx(), getVy());
			f.scale(scaleFactor, scaleFactor);
			Shape sh = f.createTransformedShape(shape.getShape());
			shape = new AnchorShape(sh);
		}
		return shape;
	}

	private Shape getShape() {
		AnchorShape shape = getAnchorShape();
		return shape != null ? shape.getShape() : handle.getVOutline();
	}

	protected void doDraw(Graphics2D g) {
		if (getAnchor().getShapeType().equals(AnchorShapeType.NONE) && getAnchor().getElementId() != null) {
			return;
		}
		Color c;

		if (isSelected()) {
			c = selectColor;
		} else {
			c = vLineElement.getPathwayObject().getLineColor();
		}

		AnchorShape arrowShape = getAnchorShape();
		if (arrowShape != null) {
			g.setStroke(new BasicStroke());
			g.setPaint(c);
			g.fill(arrowShape.getShape());
			g.draw(arrowShape.getShape());
		}

		if (isHighlighted()) {
			Color hc = getHighlightColor();
			g.setColor(new Color(hc.getRed(), hc.getGreen(), hc.getBlue(), 128));
			g.setStroke(new BasicStroke(HIGHLIGHT_STROKE_WIDTH));
			g.draw(getShape());
		}
	}

	// Minimum outline diameter of 15px
	static final double MIN_OUTLINE = 15;

	protected Shape calculateVOutline() {
		Shape s = getShape();
		Rectangle b = s.getBounds();
		// Create a larger shape if the given shape
		// is smaller than the minimum
		if (b.getWidth() < MIN_OUTLINE || b.getHeight() < MIN_OUTLINE) {
			s = new Ellipse2D.Double(getVx() - MIN_OUTLINE / 2, getVy() - MIN_OUTLINE / 2, MIN_OUTLINE, MIN_OUTLINE);
		}
		return s;
	}

	LinkAnchor linkAnchor = null;

	public LinkAnchor getLinkAnchorAt(Point2D p) {
		if (linkAnchor != null && linkAnchor.getMatchArea().contains(p)) {
			return linkAnchor;
		}
		return null;
	}

	public void hideLinkAnchors() {
		if (linkAnchor != null)
			linkAnchor.destroy();
		linkAnchor = null;
	}

	public void showLinkAnchors() {
		linkAnchor = new LinkAnchor(canvas, this, anchor, 0, 0);
	}

	/**
	 * Returns the z-order from the model //TODO public?
	 */
	@Override
	public int getZOrder() {
		return getAnchor().getZOrder();
	}

}
