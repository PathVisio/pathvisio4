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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.pathvisio.model.Citation;
import org.pathvisio.model.Groupable;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.model.PathwayElement.CitationRef;
import org.pathvisio.core.preferences.GlobalPreference;
import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.core.view.VElementMouseEvent;
import org.pathvisio.core.view.VElementMouseListener;

/**
 * Draws a citation number on top of a pathway object.
 * 
 * @author thomas, finterly
 */
public class VCitation extends VElement implements VElementMouseListener {
	static final int MFONT_SIZE = 8;
	static final String FONT_NAME = "Arial";
	static final Color FONT_COLOR = new Color(0, 0, 128);
	static final int M_PADDING = 3;
	private VPathwayElement parent;
	private Point2D rPosition;

	/**
	 * @param canvas    the parent VPathwayModel
	 * @param parent    the VPathwayElement for which the references need to be
	 *                  displayed
	 * @param rPosition the position to place the references, relative to the parent
	 *                  Graphics
	 */
	public VCitation(VPathwayModel canvas, VPathwayElement parent, Point2D rPosition) {
		super(canvas);
		this.parent = parent;
		this.rPosition = rPosition;
//		getRefMgr().addBiopaxListener(this);
		markDirty();
		canvas.addVElementMouseListener(this);
	}

	/**
	 * @return
	 */
	public VPathwayObject getParent() {
		return parent;
	}

	/**
	 *
	 */
	public void vElementMouseEvent(VElementMouseEvent e) {
		if (e.getElement() == parent) {
			if (e.getType() == VElementMouseEvent.TYPE_MOUSEENTER) {
				highlight();
			} else if (e.getType() == VElementMouseEvent.TYPE_MOUSEEXIT) {
				unhighlight();
			}
		}
	}

	/**
	 * @param rPosition
	 */
	public void setRPosition(Point2D rPosition) {
		this.rPosition = rPosition;
		markDirty();
	}

//	public BiopaxReferenceManager getRefMgr() { TODO 
//	return parent.getPathwayElement().getBiopaxReferenceManager();
//}

	/**
	 * @param g
	 * @return
	 */
	protected Rectangle2D getTextBounds(Graphics2D g) {
		Rectangle2D tb = null;
		Point2D vp = getVPosition();
		double vx = Double.isNaN(vp.getX()) ? 0 : vp.getX();
		double vy = Double.isNaN(vp.getY()) ? 0 : vp.getY();
		vp.setLocation(vx, vy);

		double pd = vFromM(M_PADDING);
		String xrefStr = getXRefText();

		if (xrefStr == null || "".equals(xrefStr)) {
			tb = new Rectangle2D.Double(vp.getX(), vp.getY(), 0, 0);
		} else if (g != null) {
			tb = g.getFontMetrics(getVFont()).getStringBounds(getXRefText(), g);
			tb.setRect(vp.getX() + tb.getX() - tb.getWidth() / 2 - pd, vp.getY() + tb.getY() - tb.getHeight() / 2 - pd,
					tb.getWidth() + 2 * pd, tb.getHeight() + 2 * pd);
		} else { // No graphics context, we can only guess...
			int w = xrefStr.length() * 5;
			tb = new Rectangle2D.Double(vp.getX() - w / 2 - pd, vp.getY() - pd, w + 2 * pd, 15 + 2 * pd);
		}
		return tb;
	}

	/**
	 *
	 */
	protected Shape calculateVOutline() {
		return getTextBounds(g2d);
	}

	/**
	 * @return
	 */
	protected int getVFontSize() {
		return (int) vFromM(MFONT_SIZE);
	}

	/**
	 * @return
	 */
	protected Font getVFont() {
		return new Font(FONT_NAME, Font.PLAIN, getVFontSize());
	}

	/**
	 * TODO rename Xref??? What does this method actually do?
	 * 
	 * @return
	 */
	protected String getXRefText() {
		if (getParent().getPathwayObject().getPathwayModel() == null) {
			return ""; // In case a redraw is called after deletion of the model element
		}
		int maxNr = PreferenceManager.getCurrent().getInt(GlobalPreference.MAX_NR_CITATIONS);
		if (maxNr == 0)
			return ""; // Show nothing if limit is set to 0

		String xrefStr = "";
		int lastOrdinal = -2;
		int sequence = 0;
		int nrShowed = 0; // Counter to check maximum citation numbers

		List<CitationRef> citationRefs = parent.getPathwayObject().getCitationRefs();
		for (int i = 0; i < citationRefs.size(); i++) {
			if (nrShowed > 0 && nrShowed >= maxNr) {
				xrefStr = xrefStr.substring(0, xrefStr.length() - 2) + "...  ";
				break; // Stop after maximum number of citations showed
			}
			// TODO use for now the index of citation
			CitationRef citationRef = citationRefs.get(i);
			Citation citation = citationRef.getCitation();
			int n = canvas.getPathwayModel().getCitations().indexOf(citation);
			if (n != lastOrdinal + 1) { // End sequence
				if (sequence > 2) {
					xrefStr = xrefStr.substring(0, xrefStr.length() - 2);
					xrefStr += "-" + lastOrdinal + ", ";
					nrShowed += 2;
				} else if (sequence == 2) {
					xrefStr += lastOrdinal + ", ";
					nrShowed++;
				}
				xrefStr += n + ", ";
				nrShowed++;
				sequence = 0;
			}
			lastOrdinal = n;
			sequence++;
		}
		if (xrefStr.length() > 2) {
			xrefStr = xrefStr.substring(0, xrefStr.length() - 2);
		}
		if (sequence > 2) {
			xrefStr += "-" + lastOrdinal;
		} else if (sequence == 2) {
			xrefStr += ", " + lastOrdinal;
		}
		return xrefStr;
	}

	/**
	 * @return
	 */
	protected Point2D getVPosition() {
		PathwayElement mParent = parent.getPathwayObject();

		Point2D vp = null;
		// Check for mappinfo object, needs a special treatment,
		// since it has no bounds in the model
		if (parent.getClass() == VInfoBox.class) {
			Rectangle2D vb = parent.getVBounds();
			double x = rPosition.getX();
			double y = rPosition.getY();
			if (vb.getWidth() != 0)
				x *= vb.getWidth() / 2;
			if (vb.getHeight() != 0)
				y *= vb.getHeight() / 2;
			x += vb.getCenterX();
			y += vb.getCenterY();
			vp = new Point2D.Double(x, y);
		} else { // For other objects, use the model bounds
			Point2D mp = ((Groupable) mParent).toAbsoluteCoordinate(rPosition);
			vp = new Point2D.Double(vFromM(mp.getX()), vFromM(mp.getY()));
		}
		return vp;
	}

	Graphics2D g2d;

	protected void doDraw(Graphics2D g2d) {
		Graphics2D g = (Graphics2D) g2d.create();

		if (this.g2d == null)
			resetShapeCache();
		this.g2d = g;

		String xrefStr = getXRefText();
		if ("".equals(xrefStr))
			return;

		g.setFont(getVFont());

		Rectangle2D bounds = getTextBounds(g);
		g.setClip(bounds);

		if (isHighlighted()) {
			Color hc = getHighlightColor();
			g.setColor(new Color(hc.getRed(), hc.getGreen(), hc.getBlue(), (int) (255 * 0.3)));
			g.fill(bounds);
		}

		g.setColor(FONT_COLOR);
		int pd = (int) vFromM(M_PADDING);
		g.drawString(xrefStr, (int) bounds.getX() + pd, (int) bounds.getMaxY() - pd);

	}

	protected int getZOrder() {
		return parent.getZOrder() + 1;
	}

//	public void biopaxEvent(BiopaxEvent e) {
//		refresh();
//		canvas.redrawDirtyRect();
//	}

	@Override
	protected void destroy() {
		super.destroy();
//		getRefMgr().removeBiopaxListener(this);
		canvas.removeVElementMouseListener(this);
	}
}
