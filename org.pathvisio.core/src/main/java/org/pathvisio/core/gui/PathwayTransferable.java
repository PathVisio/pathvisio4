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
package org.pathvisio.core.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.pathvisio.core.Engine;
import org.pathvisio.debug.Logger;
import org.pathvisio.io.ConverterException;
import org.pathvisio.io.GpmlFormat;
import org.pathvisio.model.PathwayModel;
import org.pathvisio.model.PathwayObject;
import org.pathvisio.model.Pathway;
import org.pathvisio.model.Group;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.model.CopyElement;
import org.pathvisio.model.DataNode;
import org.pathvisio.model.GraphLink.LinkableTo;
import org.pathvisio.model.Groupable;
import org.pathvisio.model.LineElement;
import org.pathvisio.model.LineElement.Anchor;
import org.pathvisio.model.LineElement.LinePoint;
import org.pathvisio.model.type.DataNodeType;

/**
 * helps transfer Pathways or bits of pathway over the clipboard
 */
public class PathwayTransferable implements Transferable {
	public static final String INFO_DATASOURCE = "COPIED";

	/**
	 * DataFlavor used for transferring raw GPML code. Mimetype is 'text/xml'. Note
	 * that the equals method of DataFlavor only checks for the main mimetype
	 * ('text'), so returns true for any DataFlavor that stores text, not only xml.
	 */
	public static final DataFlavor GPML_DATA_FLAVOR = new DataFlavor(String.class, "text/xml");
	/** @deprecated use GPML_DATA_FLAVOR instead */
	public static final DataFlavor gpmlDataFlavor = GPML_DATA_FLAVOR;

	List<PathwayElement> elements;
	PathwayModel pathway;

	public PathwayTransferable(List<PathwayElement> elements) {
		this(null, elements);
	}

	public PathwayTransferable(PathwayModel source, List<PathwayElement> elements) {
		this.elements = elements;
		if (source == null) {
			source = new PathwayModel();
		}
		this.pathway = source;
	}

	/**
	 *
	 */
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		Object out = null;

		XMLOutputter xmlout = new XMLOutputter(Format.getPrettyFormat());

		PathwayModel pnew = new PathwayModel();

		Set<String> ids = new HashSet<String>();
		Set<String> groupIds = new HashSet<String>();

		boolean infoFound = false;
		for (PathwayElement e : elements) {
			if (e.getElementId() != null) {
				ids.add(e.getElementId());
			}
			if (e instanceof Groupable) {
				String groupRefStr = ((Groupable) e).getGroupRef().getElementId();
				if (groupRefStr != null) {
					groupIds.add(groupRefStr);
				}
			}
			if (e.getClass() == Pathway.class) {
				infoFound = true;
			}
			if (e instanceof LineElement) {
				for (Anchor ma : ((LineElement) e).getAnchors()) {
					ids.add(ma.getElementId());
				}
			}
		}

//		// Create dummy parent so we can copy over TODO 
//		// the referenced biopax elements?
//		// How to copy over references? 
//		PathwayModel dummyParent = new PathwayModel();
//		for (Citation citation: pathway.getCitations()) {
//			Citation newCitation = citation.copy();
//			dummyParent.addCitation(newCitation);
//		}

		BidiMap<PathwayElement, PathwayElement> copyMap = new DualHashBidiMap<>();

		for (PathwayElement e : elements) {
			// Check for valid graphRef (with respect to other copied elements)
			CopyElement c = e.copy();
			PathwayElement enew = c.getNewElement();
			copyMap.put(enew, e);
			// do not add group yet
			if (e.getClass() == Group.class) {
				continue;
			}
			pnew.add(enew);
			c.loadReferences(); // load annotations/citations/evidences/ref
		}

		/*
		 * Below we handle proper linking of LinePoints, adding of Groups the new
		 * pathway, and Alias DataNode.
		 */
		for (PathwayElement enew : copyMap.keySet()) {
			PathwayElement esrc = copyMap.get(enew);
			/*
			 * If both LineElement and the LinkableTo elementRef are copied. Link
			 * corresponding LinePoint(s) to LinkableTo(s) in the new pathway.
			 */
			if (enew instanceof LineElement && esrc instanceof LineElement) {
				// link Start elementRef if applicable
				LinkableTo srcRef = ((LineElement) esrc).getStartElementRef();
				if (srcRef != null) {
					LinkableTo newRef = (LinkableTo) copyMap.getKey(srcRef);
					if (newRef != null) {
						((LineElement) enew).getStartLinePoint().linkTo(newRef);
					}
				}
				// link End elementRef if applicable
				srcRef = ((LineElement) esrc).getEndElementRef();
				if (srcRef != null) {
					LinkableTo newRef = (LinkableTo) copyMap.getKey(srcRef);
					if (newRef != null) {
						((LineElement) enew).getEndLinePoint().linkTo(newRef);
					}
				}
			}

			/*
			 * If all pathway element members of a group were also copied, we may add a
			 * Group(s) to the new pathway by setting groupRef.
			 */
			if (enew.getClass() == Group.class && esrc.getClass() == Group.class) {
				List<Groupable> newMembers = new ArrayList<Groupable>();
				boolean complete = true;
				while (complete) {
					for (Groupable srcMember : ((Group) esrc).getPathwayElements()) {
						if (!copyMap.containsValue((PathwayElement) srcMember)) {
							complete = false;
							System.out.println(
									"Group not copied, not all members of the group were selected to be copied.");
						} else {
							newMembers.add((Groupable) copyMap.getKey(srcMember));
						}
					}
				}
				if (complete) {
					pnew.addGroup((Group) enew);
					((Group) enew).addPathwayElements(newMembers);
				}
			}
			/*
			 * If Alias DataNode:
			 * 
			 * write warning if the Group aliasRef refers to is not present in the new
			 * pathway. TODO...depends on if pasting to same pathway or not and if group is
			 * also included...
			 */
			if (enew.getClass() == DataNode.class && ((DataNode) enew).getType() == DataNodeType.ALIAS) {
				if (pnew.hasPathwayObject(((DataNode) enew).getAliasRef())) {
				}
				//TODO 
			}
		}

		// If no mappinfo, create a dummy one that we can recognize later on
		if (!infoFound) {
			Pathway info = new Pathway();
			info.setSource(INFO_DATASOURCE);
			pnew.add(info);
		}

		try {
			Document doc = GpmlFormat.createJdom(pnew);
			out = xmlout.outputString(doc);
		} catch (Exception e) {
			Logger.log.error("Unable to copy to clipboard", e);
		}

		return out;
	}

	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { DataFlavor.stringFlavor };
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return GPML_DATA_FLAVOR.equals(flavor);
	}

	/**
	 * Gets the file url from the transferable if available. If the transferable
	 * contains a file list, the url of the first file in that list is returned.
	 * 
	 * @param t
	 * @return The {@link URL} of the file, or null if no file is available
	 * @throws UnsupportedFlavorException
	 * @throws IOException
	 */
	public static URL getFileURL(Transferable t) throws UnsupportedFlavorException, IOException {
		// Find out if there is a javaFileListFlavor, since that's the preferred type
		DataFlavor fallback = null;

		for (DataFlavor df : t.getTransferDataFlavors()) {
			if (DataFlavor.javaFileListFlavor.equals(df)) {
				// Return the first element of the file list
				return ((List<File>) t.getTransferData(df)).get(0).toURI().toURL();
			}
			// Gnome fix:
			// Check for text/uri-list mime type, an uri list separated by \n
			if (String.class.equals(df.getRepresentationClass())) {
				if ("uri-list".equalsIgnoreCase(df.getSubType())) {
					fallback = df;
				}
			}
		}
		if (fallback != null) {
			String uriList = (String) t.getTransferData(fallback);
			// Try if this is really an URL (needed, because in windows
			// raw text also gets the 'text/uri-list' mimetype (why???)
			try {
				return new URL(uriList.substring(0, uriList.indexOf("\n") - 1));
			} catch (MalformedURLException e) {
				// Not an url after all...
			}
		}
		return null;
	}

	/**
	 * Get the text in the transferable if available. There is no guarantee that the
	 * text is xml code!
	 * 
	 * @param t
	 * @return
	 * @throws UnsupportedFlavorException
	 * @throws IOException
	 */
	public static String getText(Transferable t) throws UnsupportedFlavorException, IOException {
		for (DataFlavor df : t.getTransferDataFlavors()) {
			if (DataFlavor.stringFlavor.equals(df)) {
				// Make sure this is not the gnome's uri-list
				if (!"uri-list".equalsIgnoreCase(df.getSubType())) {
					return (String) t.getTransferData(df);
				}
			}
		}
		return null;
	}

	/**
	 * Creates a pathway from the data in the provided {@link Transferable}.
	 * 
	 * @param t
	 * @return
	 * @throws ConverterException
	 * @throws MalformedURLException
	 * @throws UnsupportedFlavorException
	 * @throws IOException
	 */
	public static PathwayModel pathwayFromTransferable(Transferable t)
			throws ConverterException, MalformedURLException, UnsupportedFlavorException, IOException {
		PathwayModel pnew = new PathwayModel();

		String xml = getText(t);
		if (xml != null) {
			GpmlFormat.readFromXml(pnew, new StringReader(xml), true);

			List<PathwayObject> elements = new ArrayList<PathwayObject>();
			for (PathwayObject elm : pnew.getPathwayObjects()) { // TODO pathway object or element?
				if (elm.getClass() != Pathway.class) {
					elements.add(elm);
				} else {
					// Only add mappinfo if it's not generated by the transferable
					String source = ((Pathway) elm).getSource();
					if (!PathwayTransferable.INFO_DATASOURCE.equals(source)) {
						elements.add(elm);
					}
				}
			}
			return pnew;
		}

		URL url = getFileURL(t);
		if (url != null) {
			File file = new File(url.getFile());
			pnew.readFromXml(file, true);
			return pnew;
		}
		return null;
	}

	/**
	 * Opens a new pathway from the data in the {@link Transferable}, using the
	 * provided {@link Engine}. If the {@link Transferable} contains a link to a
	 * file, the pathway in this file will be opened. If the {@link Transferable}
	 * contains gpml code, a new pathway will be created, and the gpml will be
	 * loaded into this pathway.
	 * 
	 * @param t
	 * @param engine
	 * @throws IOException
	 * @throws UnsupportedFlavorException
	 * @throws ConverterException
	 */
	public static void openPathwayFromTransferable(Transferable t, Engine engine)
			throws UnsupportedFlavorException, IOException, ConverterException {
		URL url = getFileURL(t);
		if (url != null) {
			engine.openPathwayModel(url);
		}

		String xml = getText(t);
		if (xml != null) {
			engine.newPathwayModel();
			engine.getActivePathwayModel().readFromXml(new StringReader(xml), true);
		}
	}
}
