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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;

import org.pathvisio.model.type.LineStyleType;
import org.pathvisio.model.Label;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.model.ShapedElement;
import org.pathvisio.event.PathwayObjectEvent;
import org.pathvisio.model.type.ShapeType;
import org.pathvisio.util.ColorUtils;
import org.pathvisio.core.preferences.GlobalPreference;
import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.core.view.Adjustable;
import org.pathvisio.core.view.model.Handle.Freedom;
import org.pathvisio.core.view.model.LinAlg.Point;
import org.pathvisio.core.view.shape.ShapeRegistry;

/**
 * This is an {@link VPathwayObject} class representing shapelike forms, and
 * provides implementation for containing 8 handles placed in a (rotated)
 * rectangle around the shape and a rotation handle
 * 
 * @author unknown, finterly
 */
public abstract class VShapedElement extends VPathwayElement
		implements VLinkableTo, VGroupable, LinkProvider, Adjustable {

	private static final double M_ROTATION_HANDLE_POSITION = 20.0;

	// Side handles
	Handle handleN;
	Handle handleE;
	Handle handleS;
	Handle handleW;
	// Corner handles
	Handle handleNE;
	Handle handleSE;
	Handle handleSW;
	Handle handleNW;
	// Rotation handle
	Handle handleR;

	Handle[] handles = new Handle[] {};

	/**
	 * @param canvas
	 * @param o
	 */
	public VShapedElement(VPathwayModel canvas, PathwayElement o) {
		super(canvas, o);
	}

	/**
	 * Gets the model representation (PathwayElement) of this class
	 * 
	 * @return
	 */
	@Override
	public ShapedElement getPathwayElement() {
		return (ShapedElement) super.getPathwayElement();
	}

	/**
	 * Returns the z-order from the model //TODO public
	 */
	@Override
	public int getZOrder() {
		return getPathwayElement().getZOrder();
	}

	protected void createHandles() {
		ShapeType shapeType = getPathwayElement().getShapeType();
		boolean isResizeable = shapeType.isResizeable();
		boolean isRotatable = shapeType.isRotatable();
		if (shapeType != null && !isResizeable && !isRotatable) {
			return; // no resizing, no handles
		} else if (shapeType != null && !isResizeable && isRotatable) {
			handleR = new Handle(Handle.Freedom.ROTATION, this, this);
			handleR.setAngle(1);
			handles = new Handle[] { handleR };
		} else if (this instanceof VState) {
			handleNE = new Handle(Handle.Freedom.NEGFREE, this, this);
			handleSE = new Handle(Handle.Freedom.FREE, this, this);
			handleSW = new Handle(Handle.Freedom.NEGFREE, this, this);
			handleNW = new Handle(Handle.Freedom.FREE, this, this);

			handleNE.setAngle(315);
			handleSE.setAngle(45);
			handleSW.setAngle(135);
			handleNW.setAngle(225);

			handles = new Handle[] { handleNE, handleSE, handleSW, handleNW, };
		} else {
			handleN = new Handle(Handle.Freedom.Y, this, this);
			handleE = new Handle(Handle.Freedom.X, this, this);
			handleS = new Handle(Handle.Freedom.Y, this, this);
			handleW = new Handle(Handle.Freedom.X, this, this);

			handleNE = new Handle(Handle.Freedom.NEGFREE, this, this);
			handleSE = new Handle(Handle.Freedom.FREE, this, this);
			handleSW = new Handle(Handle.Freedom.NEGFREE, this, this);
			handleNW = new Handle(Handle.Freedom.FREE, this, this);

			handleN.setAngle(270);
			handleE.setAngle(0);
			handleS.setAngle(90);
			handleW.setAngle(180);
			handleNE.setAngle(315);
			handleSE.setAngle(45);
			handleSW.setAngle(135);
			handleNW.setAngle(225);

			if (this instanceof VDataNode || this instanceof VLabel || !isRotatable) {
				// No rotation handle for these objects
				handles = new Handle[] { handleN, handleNE, handleE, handleSE, handleS, handleSW, handleW, handleNW, };
			} else {
				handleR = new Handle(Handle.Freedom.ROTATION, this, this);
				handleR.setAngle(1);

				handles = new Handle[] { handleN, handleNE, handleE, handleSE, handleS, handleSW, handleW, handleNW,
						handleR };
			}
		}
		setHandleLocation();
	}

	protected void setVScaleRectangle(Rectangle2D r) {
		ShapedElement gdata = getPathwayElement();
		gdata.setWidth(mFromV(r.getWidth()));
		gdata.setHeight(mFromV(r.getHeight()));
		gdata.setLeft(mFromV(r.getX()));
		gdata.setTop(mFromV(r.getY()));
	}

	protected void vMoveBy(double vdx, double vdy) {
		// both setM operations fire the exact same objectModifiedEvent, one should be
		// enough
		ShapedElement gdata = getPathwayElement();
		gdata.dontFireEvents(1);
		gdata.setLeft(gdata.getLeft() + mFromV(vdx));
		gdata.setTop(gdata.getTop() + mFromV(vdy));
	}

	public Handle[] getHandles() {
		return handles;
	}

	/**
	 * Translate the given point to internal coordinate system (origin in center and
	 * axis direction rotated with this objects rotation
	 * 
	 * @param MPoint p
	 */
	private Point mToInternal(Point p) {
		Point pt = mRelativeToCenter(p);
		Point pr = LinAlg.rotate(pt, getPathwayElement().getRotation());
		return pr;
	}

	/**
	 * Translate the given coordinates to external coordinate system (of the drawing
	 * canvas)
	 * 
	 * @param x
	 * @param y
	 */
	private Point mToExternal(double x, double y) {
		Point p = new Point(x, y);
		Point pr = LinAlg.rotate(p, -getPathwayElement().getRotation());
		pr.x += getPathwayElement().getCenterX();
		pr.y += getPathwayElement().getCenterY();
		return pr;
	}

	/**
	 * Get the coordinates of the given point relative to this object's center
	 * 
	 * @param p
	 */
	private Point mRelativeToCenter(Point p) {
		return p.subtract(new Point(getPathwayElement().getCenterX(), getPathwayElement().getCenterY()));
	}

	/**
	 * Set the rotation of this object
	 * 
	 * @param angle angle of rotation in radians
	 */
	public void setRotation(double angle) {
		if (angle < 0)
			getPathwayElement().setRotation(angle + Math.PI * 2);
		else if (angle > Math.PI * 2)
			getPathwayElement().setRotation(angle - Math.PI * 2);
		else
			getPathwayElement().setRotation(angle);
	}

	/**
	 *
	 */
	public void adjustToHandle(Handle h, double vnewx, double vnewy) {
		ShapedElement gdata = getPathwayElement();
		// Rotation
		if (h == handleR) {
			Point cur = mRelativeToCenter(new Point(mFromV(vnewx), mFromV(vnewy)));

			double rotation = Math.atan2(cur.y, cur.x);
			if (PreferenceManager.getCurrent().getBoolean(GlobalPreference.SNAP_TO_ANGLE)
					|| canvas.isSnapModifierPressed()) {
				// Snap the rotation angle
				double snapStep = PreferenceManager.getCurrent().getInt(GlobalPreference.SNAP_TO_ANGLE_STEP) * Math.PI
						/ 180;
				rotation = Math.round(rotation / snapStep) * snapStep;
			}
			setRotation(rotation);
			return;
		}

		/*
		 * if point is restricted to a certain range of movement, project handle to the
		 * closest point in that range.
		 * 
		 * This is true for all handles, except for Freedom.FREE and Freedom.NEGFREE
		 * when the snap modifier is not pressed.
		 */
		Freedom freedom = h.getFreedom();
		if (!(freedom == Freedom.FREE || freedom == Freedom.NEGFREE) || canvas.isSnapModifierPressed()) {
			Point v = new Point(0, 0);
			Rectangle2D b = getVBounds();
			Point base = new Point(b.getCenterX(), b.getCenterY());
			if (freedom == Freedom.X) {
				v = new Point(1, 0);
			} else if (freedom == Freedom.Y) {
				v = new Point(0, 1);
			}
			if (freedom == Freedom.FREE) {
				v = new Point(getVWidth(), getVHeight());
			} else if (freedom == Freedom.NEGFREE) {
				v = new Point(getVWidth(), -getVHeight());
			}
			Point yr = LinAlg.rotate(v, -gdata.getRotation());
			Point prj = LinAlg.project(base, new Point(vnewx, vnewy), yr);
			vnewx = prj.x;
			vnewy = prj.y;
		}

		// Transformation
		Point iPos = mToInternal(new Point(mFromV(vnewx), mFromV(vnewy)));

		double idx = 0;
		double idy = 0;
		double idw = 0;
		double idh = 0;
		double halfh = gdata.getHeight() / 2;
		double halfw = gdata.getWidth() / 2;

		if (h == handleN || h == handleNE || h == handleNW) {
			idh = -(iPos.y + halfh);
			idy = -idh / 2;
		}
		if (h == handleS || h == handleSE || h == handleSW) {
			idh = (iPos.y - halfh);
			idy = idh / 2;
		}
		if (h == handleE || h == handleNE || h == handleSE) {
			idw = (iPos.x - halfw);
			idx = idw / 2;
		}
		if (h == handleW || h == handleNW || h == handleSW) {
			idw = -(iPos.x + halfw);
			idx = -idw / 2;
		}
		;

		double neww = gdata.getWidth() + idw;
		double newh = gdata.getHeight() + idh;

		// In case object had negative width, switch handles
		if (neww < 0) {
			setHorizontalOppositeHandle(h);
			neww = -neww;
		}
		if (newh < 0) {
			setVerticalOppositeHandle(h);
			newh = -newh;
		}

		gdata.setWidth(neww);
		gdata.setHeight(newh);
		Point vcr = LinAlg.rotate(new Point(idx, idy), -gdata.getRotation());
		gdata.setCenterX(gdata.getCenterX() + vcr.x);
		gdata.setCenterY(gdata.getCenterY() + vcr.y);

	}

	/**
	 * @param h
	 */
	private void setHorizontalOppositeHandle(Handle h) {
		Handle opposite = null;
		if (h == handleE)
			opposite = handleW;
		else if (h == handleW)
			opposite = handleE;
		else if (h == handleNE)
			opposite = handleNW;
		else if (h == handleSE)
			opposite = handleSW;
		else if (h == handleNW)
			opposite = handleNE;
		else if (h == handleSW)
			opposite = handleSE;
		else
			opposite = h;
		canvas.setPressedObject(opposite);
	}

	/**
	 * @param h
	 */
	private void setVerticalOppositeHandle(Handle h) {
		Handle opposite = null;
		if (h == handleN)
			opposite = handleS;
		else if (h == handleS)
			opposite = handleN;
		else if (h == handleNE)
			opposite = handleSE;
		else if (h == handleSE)
			opposite = handleNE;
		else if (h == handleNW)
			opposite = handleSW;
		else if (h == handleSW)
			opposite = handleNW;
		else
			opposite = h;
		canvas.setPressedObject(opposite);
	}

	/**
	 * Sets the handles at the correct location;
	 * 
	 * @param ignore the position of this handle will not be adjusted
	 */
	protected void setHandleLocation() {
		Point p;
		ShapedElement gdata = getPathwayElement();
		ShapeType shapeType = gdata.getShapeType();
		if (shapeType == null || shapeType.isResizeable()) {
			if (handleN != null) {
				p = mToExternal(0, -gdata.getHeight() / 2);
				handleN.setMLocation(p.x, p.y);
				p = mToExternal(gdata.getWidth() / 2, 0);
				handleE.setMLocation(p.x, p.y);
				p = mToExternal(0, gdata.getHeight() / 2);
				handleS.setMLocation(p.x, p.y);
				p = mToExternal(-gdata.getWidth() / 2, 0);
				handleW.setMLocation(p.x, p.y);
			}

			p = mToExternal(gdata.getWidth() / 2, -gdata.getHeight() / 2);
			handleNE.setMLocation(p.x, p.y);
			p = mToExternal(gdata.getWidth() / 2, gdata.getHeight() / 2);
			handleSE.setMLocation(p.x, p.y);
			p = mToExternal(-gdata.getWidth() / 2, gdata.getHeight() / 2);
			handleSW.setMLocation(p.x, p.y);
			p = mToExternal(-gdata.getWidth() / 2, -gdata.getHeight() / 2);
			handleNW.setMLocation(p.x, p.y);
		}
		if ((shapeType == null || shapeType.isRotatable()) && (handleR != null)) {
			p = mToExternal(getPathwayElement().getWidth() / 2 + M_ROTATION_HANDLE_POSITION, 0);
			handleR.setMLocation(p.x, p.y);
		}

		for (Handle h : getHandles())
			h.rotation = gdata.getRotation();
	}

	protected Shape calculateVOutline() {
		// Include rotation and stroke
		Area a = new Area(getShape(true, true));
		return a;
	}

	protected Shape getVShape(boolean rotate) {
		return getShape(rotate, false); // Get the shape without border
	}

	/**
	 * Returns the shape that should be drawn
	 * 
	 * @parameter rotate whether to take into account rotation or not
	 * @parameter stroke whether to include the stroke or not
	 * @return
	 */
	protected Shape getShape(boolean rotate, boolean stroke) {
		if (stroke) {
			return getShape(rotate, (float) getPathwayElement().getBorderWidth());
		} else {
			return getShape(rotate, 0);
		}
	}

	public Shape getShape() {
		return getShape(false, 0);
	}

	/**
	 * Returns the shape that should be drawn
	 * 
	 * @parameter rotate whether to take into account rotation or not
	 * @parameter sw the width of the stroke to include
	 * @return
	 */
	protected java.awt.Shape getShape(boolean rotate, float sw) {
		ShapedElement gdata = getPathwayElement();
		double mx = gdata.getLeft();
		double my = gdata.getTop();
		double mw = gdata.getWidth();
		double mh = gdata.getHeight();
		double mcx = gdata.getCenterX();
		double mcy = gdata.getCenterY();

		java.awt.Shape s = null;

		if (gdata.getShapeType() == null || gdata.getShapeType() == ShapeType.NONE) {
			s = ShapeRegistry.DEFAULT_SHAPE.getShape(mw, mh);
		} else {
			s = gdata.getShapeType().getShape(mw, mh);
		}

		AffineTransform t = new AffineTransform();
		t.scale(canvas.getZoomFactor(), canvas.getZoomFactor());

		if (rotate) {
			t.rotate(gdata.getRotation(), mcx, mcy);
		}
		t.translate(mx, my);
		s = t.createTransformedShape(s);

		if (sw > 0)
			if (mw * mh > 0) // Workaround, batik balks if the shape is zero sized.
			{
				if (gdata.getBorderStyle() == LineStyleType.DOUBLE) {
					// correction factor for composite stroke
					sw = (float) (gdata.getBorderWidth() * 4);
				}
				Stroke stroke = new BasicStroke(sw);
				s = stroke.createStrokedShape(s);
			}
		return s;
	}

	public void gmmlObjectModified(PathwayObjectEvent e) {
		markDirty(); // mark everything dirty
		checkCitation();
		if (handles.length > 0)
			setHandleLocation();
	}

	LinkProvider linkAnchorDelegate = new DefaultLinkAnchorDelegate(this);

	/**
	 * Use this to override default linkAnchorDelegate
	 */
	public void setLinkAnchorDelegate(LinkProvider delegate) {
		if (delegate == null)
			throw new NullPointerException("passed illegal null value for delegate");
		linkAnchorDelegate = delegate;
	}

	public void showLinkAnchors() {
		linkAnchorDelegate.showLinkAnchors();
	}

	public void hideLinkAnchors() {
		linkAnchorDelegate.hideLinkAnchors();
	}

	public LinkAnchor getLinkAnchorAt(Point2D p) {
		return linkAnchorDelegate.getLinkAnchorAt(p);
	}

	@Override
	protected void destroyHandles() {
		for (Handle h : handles) {
			h.destroy();
		}
		handles = new Handle[] {};
	}

	protected void doDraw(Graphics2D g2d) {
		g2d.setColor(getLineColor());
		setLineStyle(g2d);
		drawShape(g2d);

		// return to normal stroke
		g2d.setStroke(new BasicStroke());

		g2d.setFont(getVFont());
		drawTextLabel(g2d);

		drawHighlight(g2d);
	}

	protected void drawTextLabel(Graphics2D g) {
		ShapedElement gdata = getPathwayElement();
		int margin = (int) vFromM(5);
		Rectangle area = getVShape(true).getBounds();
		String label = gdata.getTextLabel();
		if (label != null && !"".equals(label)) {
			// Split by newline, to enable multi-line labels
			String[] lines = label.split("\n");

			FontMetrics fm = g.getFontMetrics();
			int lh = fm.getHeight();
			int yoffset = area.y + fm.getAscent();
			switch (gdata.getVAlign()) {
			case MIDDLE:
				yoffset += (area.height - (lines.length * lh)) / 2;
				break;
			case TOP:
				yoffset += margin;
				break;
			case BOTTOM:
				yoffset += area.height - margin - (lines.length * lh);
			}

			for (int i = 0; i < lines.length; i++) {
				if (lines[i].equals(""))
					continue; // Can't have attributed string with 0 length
				AttributedString ats = getVAttributedString(lines[i]);
				if (gdata.getClass() == Label.class) {
					if (!((Label) gdata).getHref().equals("")) {
						ats.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
					}
				}
				Rectangle2D tb = fm.getStringBounds(ats.getIterator(), 0, lines[i].length(), g);

				int xoffset = area.x;
				switch (gdata.getHAlign()) {
				case CENTER:
					xoffset += (int) (area.width / 2) - (int) (tb.getWidth() / 2);
					break;
				case LEFT:
					xoffset += margin;
					break;
				case RIGHT:
					xoffset += area.width - margin - tb.getWidth();
					break;
				}
				g.drawString(ats.getIterator(), xoffset, yoffset + (int) (i * tb.getHeight()));
			}

		}
	}

	/**
	 * @param text
	 * @return
	 */
	private AttributedString getVAttributedString(String text) {
		AttributedString ats = new AttributedString(text);
		if (getPathwayElement().getFontStrikethru()) {
			ats.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
		}
		if (getPathwayElement().getFontDecoration()) {
			ats.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		}

		ats.addAttribute(TextAttribute.FONT, getVFont());
		return ats;
	}

	/**
	 * @return
	 */
	protected Font getVFont() {
		String name = getPathwayElement().getFontName();
		int style = getVFontStyle();
		return new Font(name, style, 12).deriveFont((float) vFromM(getPathwayElement().getFontSize()));
	}

	/**
	 * @param g
	 */
	protected void drawShape(Graphics2D g) {
		ShapedElement gdata = getPathwayElement();
		Color fillcolor = gdata.getFillColor();
		if (!hasOutline()) {
			return; // nothing to draw.
		}
		java.awt.Shape shape = getShape(true, false);

		if (gdata.getShapeType() == ShapeType.BRACE || gdata.getShapeType() == ShapeType.ARC) {
			// don't fill arcs or braces
			// TODO: this exception should disappear in the future,
			// when we've made sure all pathways on wikipathways have
			// transparent arcs and braces
		} else {
			// fill the rest
			if (!ColorUtils.isTransparent(gdata.getFillColor())) {
				g.setColor(fillcolor);
				g.fill(shape);
			}
		}
		g.setColor(getLineColor());
		g.draw(shape);
	}

	/**
	 * @return
	 */
	private boolean hasOutline() {
		ShapeType shapeType = getPathwayElement().getShapeType();
		return (!(shapeType == null || shapeType == ShapeType.NONE));
	}

	/**
	 * Draw a translucent marker around the shape so that it stands out. Used e.g.
	 * to indicate search results. HighlightColor is customizable.
	 * 
	 * @param g
	 */
	protected void drawHighlight(Graphics2D g) {
		if (isHighlighted()) {
			Color hc = getHighlightColor();
			g.setColor(new Color(hc.getRed(), hc.getGreen(), hc.getBlue(), 128));

			if (hasOutline()) {
				// highlight the outline
				java.awt.Shape shape = getShape(true, false);
				g.setStroke(new BasicStroke(HIGHLIGHT_STROKE_WIDTH));
				g.draw(shape);
			} else {
				// outline invisible, fill the entire area
				g.setStroke(new BasicStroke());
				Rectangle2D r = new Rectangle2D.Double(getVLeft(), getVTop(), getVWidth(), getVHeight());
				g.fill(r);
			}
		}
	}

	/**
	 * {@inheritDoc} GraphicsShape overrides vContains, because the base
	 * implementation only considers a hit with the outline, which makes it hard to
	 * grab with the mouse.
	 */
	@Override
	protected boolean vContains(Point2D point) {
		// first use getVBounds as a rough approximation
		if (getVBounds().contains(point)) {
			// if the shape is transparent, only check against the outline
			if (!ColorUtils.isTransparent(getPathwayElement().getFillColor())) {
				return getVOutline().contains(point);
			} else {
				// otherwise check against the whole shape
				return getVShape(true).contains(point);
			}
		} else {
			return false;
		}
	}

	// ================================================================================
	// Methods from VPathwayObject
	// ================================================================================
	/**
	 * Returns the x-coordinate of the center point of this object adjusted to the
	 * current zoom factor
	 * 
	 * @return the center x-coordinate
	 */
	public double getVCenterX() {
		return vFromM(getPathwayElement().getCenterX());
	}

	/**
	 * Returns the y-coordinate of the center point of this object adjusted to the
	 * current zoom factor
	 *
	 * @return the center y-coordinate
	 */
	public double getVCenterY() {
		return vFromM(getPathwayElement().getCenterY());
	}

	/**
	 * Returns the width of this object adjusted to the current zoom factor, but not
	 * taking into account rotation
	 * 
	 * @note if you want the width of the rotated object's boundary, use
	 *       {@link #getVShape(true)}.getWidth();
	 * @return
	 */
	public double getVWidth() {
		return vFromM(getPathwayElement().getWidth());
	}

	/**
	 * Returns the height of this object adjusted to the current zoom factor, but
	 * not taking into account rotation
	 * 
	 * @note if you want the height of the rotated object's boundary, use
	 *       {@link #getVShape(true)}.getY();
	 * @return
	 */
	public double getVHeight() {
		return vFromM(getPathwayElement().getHeight());
	}

	/**
	 * Returns the x-coordinate of the left side of this object adjusted to the
	 * current zoom factor, but not taking into account rotation
	 * 
	 * @note if you want the left side of the rotated object's boundary, use
	 *       {@link #getVShape(true)}.getX();
	 * @return
	 */
	public double getVLeft() {
		return vFromM(getPathwayElement().getLeft());
	}

	/**
	 * Returns the y-coordinate of the top side of this object adjusted to the
	 * current zoom factor, but not taking into account rotation
	 * 
	 * @note if you want the top side of the rotated object's boundary, use
	 *       {@link #getVShape(true)}.getY();
	 * @return
	 */
	public double getVTop() {
		return vFromM(getPathwayElement().getTop());
	}

	/**
	 * Returns the fontstyle to create a java.awt.Font
	 * 
	 * @return the fontstyle, or Font.PLAIN if no font is available
	 */
	public int getVFontStyle() {
		int style = Font.PLAIN;
		if (getPathwayElement().getFontName() != null) {
			if (getPathwayElement().getFontWeight()) {
				style |= Font.BOLD;
			}
			if (getPathwayElement().getFontStyle()) {
				style |= Font.ITALIC;
			}
		}
		return style;
	}

	/**
	 * TODO rename to border?
	 * 
	 * @return
	 */
	protected Color getLineColor() {
		Color linecolor = getPathwayElement().getBorderColor();
		/*
		 * the selection is not colored red when in edit mode it is possible to see a
		 * color change immediately
		 */
		if (isSelected() && !canvas.isEditMode()) {
			linecolor = selectColor;
		}
		return linecolor;
	}

	/**
	 * TODO rename to border?
	 * 
	 * @param g
	 */
	protected void setLineStyle(Graphics2D g) {
		LineStyleType ls = getPathwayElement().getBorderStyle();
		float lt = (float) vFromM(getPathwayElement().getBorderWidth());
		if (ls == LineStyleType.SOLID) {
			g.setStroke(new BasicStroke(lt));
		} else if (ls == LineStyleType.DASHED) {
			g.setStroke(
					new BasicStroke(lt, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10, new float[] { 4, 4 }, 0));
		} else if (ls == LineStyleType.DOUBLE) {
			g.setStroke(new CompositeStroke(new BasicStroke(lt * 2), new BasicStroke(lt)));
		}
	}

}
