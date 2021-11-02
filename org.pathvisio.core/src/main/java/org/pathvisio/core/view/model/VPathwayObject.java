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
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.pathvisio.core.biopax.PublicationXref;
import org.pathvisio.core.debug.DebugList;
import org.pathvisio.model.type.LineStyleType;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.event.PathwayObjectEvent;
import org.pathvisio.event.PathwayObjectListener;

/**
 * This class is a parent class for all graphics that can be added to a
 * VPathway.
 */
public abstract class VPathwayObject extends VElement implements PathwayObjectListener {

	protected PathwayElement gdata = null;

	/**
	 * Children is everything that moves when this element is dragged, including
	 * Citation and State
	 */
	private List<VElement> children = new DebugList<VElement>();

	public VPathwayObject(VPathwayModel canvas, PathwayElement o) {
		super(canvas);
		o.addListener(this);
		gdata = o;
	}

	/**
	 * Gets the model representation (PathwayElement) of this class
	 * 
	 * @return
	 */
	public PathwayElement getPathwayElement() {
		return gdata;
	}

	/**
	 *
	 */
	@Override
	public void markDirty() {
		super.markDirty();
		for (VElement child : children)
			child.markDirty();
	}

	/**
	 * 
	 */
	boolean listen = true;

	/**
	 *
	 */
	@Override
	public void gmmlObjectModified(PathwayObjectEvent e) {
		if (listen) {
			markDirty(); // mark everything dirty
		}
	}

	public Area createVisualizationRegion() {
		return new Area(getVBounds());
	}

//	/**
//	 * Get the x-coordinate of the center point of this object adjusted to the
//	 * current zoom factor
//	 * 
//	 * @return the center x-coordinate
//	 */
//	public double getVCenterX() {
//		return vFromM(gdata.getCenterX());
//	}
//
//	/**
//	 * Get the y-coordinate of the center point of this object adjusted to the
//	 * current zoom factor
//	 *
//	 * @return the center y-coordinate
//	 */
//	public double getVCenterY() {
//		return vFromM(gdata.getCenterY());
//	}
//
//	/**
//	 * Get the width of this object adjusted to the current zoom factor, but not
//	 * taking into account rotation
//	 * 
//	 * @note if you want the width of the rotated object's boundary, use
//	 *       {@link #getVShape(true)}.getWidth();
//	 * @return
//	 */
//	public double getVWidth() {
//		return vFromM(gdata.getWidth());
//	}
//
//	/**
//	 * Get the height of this object adjusted to the current zoom factor, but not
//	 * taking into account rotation
//	 * 
//	 * @note if you want the height of the rotated object's boundary, use
//	 *       {@link #getVShape(true)}.getY();
//	 * @return
//	 */
//	public double getVHeight() {
//		return vFromM(gdata.getHeight());
//	}
//
//	/**
//	 * Get the x-coordinate of the left side of this object adjusted to the current
//	 * zoom factor, but not taking into account rotation
//	 * 
//	 * @note if you want the left side of the rotated object's boundary, use
//	 *       {@link #getVShape(true)}.getX();
//	 * @return
//	 */
//	public double getVLeft() {
//		return vFromM(gdata.getLeft());
//	}
//
//	/**
//	 * Get the y-coordinate of the top side of this object adjusted to the current
//	 * zoom factor, but not taking into account rotation
//	 * 
//	 * @note if you want the top side of the rotated object's boundary, use
//	 *       {@link #getVShape(true)}.getY();
//	 * @return
//	 */
//	public double getVTop() {
//		return vFromM(gdata.getTop());
//	}

	/**
	 * Get the direct view to model translation of this shape
	 * 
	 * @param rotate Whether to take into account rotation or not
	 * @return
	 */
	abstract protected Shape getVShape(boolean rotate);

	/**
	 * Get the rectangle that represents the bounds of the shape's direct
	 * translation from model to view, without taking into account rotation. Default
	 * implementation is equivalent to <code>getVShape(false).getBounds2D();</code>
	 */
	@Override
	protected Rectangle2D getVScaleRectangle() {
		return getVShape(false).getBounds2D();
	}

	/**
	 * Scales the object to the given rectangle, by taking into account the rotation
	 * (given rectangle will be rotated back before scaling)
	 * 
	 * @param r
	 */
	@Override
	protected abstract void setVScaleRectangle(Rectangle2D r);

	/**
	 * Default implementation returns the rotated shape. Subclasses may override
	 * (e.g. to include the stroke)
	 * 
	 * @see {@link VElement#calculateVOutline()}
	 */
	@Override
	protected Shape calculateVOutline() {
		return getVShape(true);
	}

	/**
	 *
	 */
	@Override
	protected void destroy() {
		super.destroy();
		gdata.removeListener(this);
		for (VElement child : children) {
			child.destroy();
		}
		children.clear();
		// View should not remove its model
//		Pathway parent = gdata.getParent();
//		if(parent != null) parent.remove(gdata);
	}

	/**
	 * Returns the z-order from the model
	 */
	@Override
	protected int getZOrder() {
		return 0x0000;
//		gdata.getZOrder(); TODO 
	}

	public void addChild(VElement elt) {
		children.add(elt);
	}

	public void removeChild(VElement elt) {
		children.remove(elt);
	}

}

/**
 * Generates double line stroke, e.g., for cellular compartment shapes.
 *
 * @author unknown
 */
final class CompositeStroke implements Stroke {
	private Stroke stroke1, stroke2;

	/**
	 * @param stroke1
	 * @param stroke2
	 */
	public CompositeStroke(Stroke stroke1, Stroke stroke2) {
		this.stroke1 = stroke1;
		this.stroke2 = stroke2;
	}

	/**
	 *
	 */
	@Override
	public Shape createStrokedShape(Shape shape) {
		return stroke2.createStrokedShape(stroke1.createStrokedShape(shape));
	}
}
