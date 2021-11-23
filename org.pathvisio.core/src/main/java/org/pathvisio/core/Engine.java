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
package org.pathvisio.core;

import java.awt.Color;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.pathvisio.debug.Logger;
import org.pathvisio.io.ConverterException;
import org.pathvisio.model.PathwayModel;
import org.pathvisio.event.PathwayModelExporter;
import org.pathvisio.event.PathwayModelImporter;
import org.pathvisio.util.FileUtils;
import org.pathvisio.util.Utils;
import org.pathvisio.core.view.model.VPathwayModel;
import org.pathvisio.core.view.model.VPathwayWrapper;

/**
 * This class manages loading, importing and exporting a Pathway and VPathway
 * together.
 *
 * TODO: there are some unrelated Global functions in here, but the intention is
 * to move them away in the future.
 * 
 * @author unknown
 */
public class Engine {

	private VPathwayModel vPathwayModel; // may be null
	/*
	 * TODO: standalone below is a hack to make Converter work, only used when
	 * vPathway is null
	 */
	private PathwayModel standalone = null;
	/*
	 * Wrapper may also be null in case you don't need to interact with the pathway.
	 */
	private VPathwayWrapper wrapper;

	public static final String SVG_FILE_EXTENSION = "svg";
	public static final String SVG_FILTER_NAME = "Scalable Vector Graphics (*." + SVG_FILE_EXTENSION + ")";
	public static final String PATHWAY_FILE_EXTENSION = "gpml";
	public static final String PATHWAY_FILTER_NAME = "PathVisio Pathway (*." + PATHWAY_FILE_EXTENSION + ")";
	public static final String GENMAPP_FILE_EXTENSION = "mapp";
	public static final String GENMAPP_FILTER_NAME = "GenMAPP Pathway (*." + GENMAPP_FILE_EXTENSION + ")";

	/**
	 * The transparent color used in the icons for visualization of protein/mrna
	 * data
	 */
	public static final Color TRANSPARENT_COLOR = new Color(255, 0, 255);

	// ================================================================================
	// Accessors
	// ================================================================================
	/**
	 * Sets this to the toolkit-specific wrapper before opening or creating a new
	 * pathway otherwise Engine can't create a vPathway.
	 */
	public void setWrapper(VPathwayWrapper value) {
		wrapper = value;
	}

	/**
	 * Gets the currently open drawing.
	 */
	public VPathwayModel getActiveVPathwayModel() {
		return vPathwayModel;
	}

	/**
	 * Returns the currently open {@link PathwayModel} .
	 */
	public PathwayModel getActivePathwayModel() {
		if (vPathwayModel == null) {
			return standalone;
		} else {
			return vPathwayModel.getPathwayModel();
		}
	}

	/**
	 * Finds out whether a VPathway is currently available or not (whether a drawing
	 * is currently open or not).
	 * 
	 * NB: this method replaced the deprecated isDrawingOpen() method.
	 * 
	 * @return true if a VPathway is currently available (drawing is open), false if
	 *         not
	 */
	public boolean hasVPathwayModel() {
		return vPathwayModel != null;
	}

	// ================================================================================
	// Import, Open, and Create Methods
	// ================================================================================
	/**
	 * Imports pathway model from the given file.
	 * 
	 * @param file the file.
	 * @throws ConverterException
	 */
	public void importPathwayModel(File file) throws ConverterException {
		Logger.log.trace("Importing pathway from " + file);

		Set<PathwayModelImporter> set = getPathwayModelImporters(file);
		if (set != null && set.size() == 1) {
			PathwayModelImporter importer = Utils.oneOf(set);
			PathwayModel pathway = importer.doImport(file);
			pathway.setSourceFile(file);
			newPathwayModelHelper(pathway);
		} else
			throw new ConverterException(
					"Could not determine importer for '" + FileUtils.getExtension(file.toString()) + "' files");
	}

	/**
	 * After loading a {@link PathwayModel} from disk, run createVPathway on EDT
	 * thread to prevent concurrentModificationException
	 * 
	 * @param p the pathway model.
	 */
	private void newPathwayModelHelper(final PathwayModel pathwayModel) throws ConverterException {
		try {
			// switch back to EDT
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					createVPathwayModel(pathwayModel);
					fireApplicationEvent(new ApplicationEvent(pathwayModel, ApplicationEvent.Type.PATHWAY_OPENED));
					if (vPathwayModel != null) {
						fireApplicationEvent(
								new ApplicationEvent(vPathwayModel, ApplicationEvent.Type.VPATHWAY_OPENED));
					}
				}
			});
		} catch (InterruptedException e) {
			throw new ConverterException(e);
		} catch (InvocationTargetException e) {
			throw new ConverterException(e);
		}
	}

	/**
	 * Opens a pathway model from memory.
	 * 
	 * @param p the pathway model.
	 * @throws ConverterException
	 */
	public void openPathwayModelFromMemory(PathwayModel pathwayModel) throws ConverterException {
		newPathwayModelHelper(pathwayModel);
	}

	/**
	 * Opens a pathway model from a gpml file
	 * 
	 * @param file the gpml file.
	 */
	public void openPathwayModel(File file) throws ConverterException {
		String pwf = file.toString();

		// initialize new JDOM gpml representation and read the file
		final PathwayModel pathwayModel = new PathwayModel();
		pathwayModel.readFromXml(new File(pwf), true);
		// Only set the pathway field after the data is loaded
		// (Exception thrown on error, this part will not be reached)
		newPathwayModelHelper(pathwayModel);
	}

	/**
	 * Opens a pathway model from an url and returns file.
	 * 
	 * @param url the file to open.
	 * @return the file.
	 * @throws ConverterException
	 */
	public File openPathwayModel(URL url) throws ConverterException {
		// TODO insert in recent pathways
		String protocol = url.getProtocol();
		File file = null;
		if (protocol.equals("file")) {
			file = new File(url.getFile());
			openPathwayModel(file);
		} else {
			try {
				file = File.createTempFile("urlPathway", "." + Engine.PATHWAY_FILE_EXTENSION);
				FileUtils.downloadFile(url, file);
				openPathwayModel(file);
			} catch (Exception e) {
				throw new ConverterException(e);
			}
		}
		return file;
	}

	// ================================================================================
	// Save and Dispose Methods
	// ================================================================================

	/**
	 * Saves the pathway model.
	 * 
	 * @param p      the pathway model to save
	 * @param toFile the file to save to
	 * @throws ConverterException
	 */
	public void savePathwayModel(PathwayModel pathwayModel, File toFile) throws ConverterException {
		// make sure there are no problems with references.
		// p.fixReferences(); TODO not needed anymore?
		pathwayModel.writeToXml(toFile, true);
		fireApplicationEvent(new ApplicationEvent(pathwayModel, ApplicationEvent.Type.PATHWAY_SAVE));
	}

	/**
	 * Saves the currently active pathway model.
	 * 
	 * @param file the file to save to
	 * @throws ConverterException
	 */
	public void savePathwayModel(File file) throws ConverterException {
		savePathwayModel(getActivePathwayModel(), file);
	}

	/**
	 * Opposite of createVPathwayModel.
	 */
	public void disposeVPathwayModel() {
		assert (vPathwayModel != null);
		// signal destruction of vPathway
		fireApplicationEvent(new ApplicationEvent(vPathwayModel, ApplicationEvent.Type.VPATHWAY_DISPOSED));
		vPathwayModel.dispose();
		vPathwayModel = null;
	}

	// ================================================================================
	// Create and Replace Methods
	// ================================================================================
	/**
	 * Tries to make a VPathwayModel, replacing pathway with a new one.
	 * 
	 * @param p the pathway model.
	 */
	public void createVPathwayModel(PathwayModel pathwayModel) {
		if (wrapper == null) {
			standalone = pathwayModel;
		} else {
			double zoom = 100;
			if (hasVPathwayModel()) {
				// save zoom Level
				zoom = getActiveVPathwayModel().getPctZoom();

				disposeVPathwayModel();
			}

			vPathwayModel = wrapper.createVPathway();
			vPathwayModel.registerKeyboardActions(this);
			vPathwayModel.activateUndoManager(this);
			vPathwayModel.fromModel(pathwayModel);

			vPathwayModel.setPctZoom(zoom);
			fireApplicationEvent(new ApplicationEvent(vPathwayModel, ApplicationEvent.Type.VPATHWAY_CREATED));
		}
	}

	/**
	 * Used by undo manager.
	 * 
	 * @param p the pathway model.
	 */
	public void replacePathwayModel(PathwayModel p) {
		vPathwayModel.replacePathway(p);
		fireApplicationEvent(new ApplicationEvent(vPathwayModel, ApplicationEvent.Type.VPATHWAY_CREATED));
	}

	/**
	 * Creates a new {@link PathwayModel} and view {@link VPathwayModel}. Pathway
	 * model has {@link Pathway} initialized with default values.
	 */
	public void newPathwayModel() {
		PathwayModel pathway = new PathwayModel();
//		pathway.initMappInfo();
		createVPathwayModel(pathway);
		fireApplicationEvent(new ApplicationEvent(pathway, ApplicationEvent.Type.PATHWAY_NEW));
		if (vPathwayModel != null) {
			fireApplicationEvent(new ApplicationEvent(vPathwayModel, ApplicationEvent.Type.VPATHWAY_NEW));
		}
	}

	// ================================================================================
	// Importers and Exporters Methods
	// ================================================================================
	private Map<String, Set<PathwayModelExporter>> exporters = new HashMap<String, Set<PathwayModelExporter>>();
	private Map<String, Set<PathwayModelImporter>> importers = new HashMap<String, Set<PathwayModelImporter>>();

	/**
	 * Adds a {@link PathwayModelImporter} that handles import of GPML to another
	 * file format.
	 * 
	 * @param importer the pathway model importer to add.
	 */
	public void addPathwayModelImporter(PathwayModelImporter importer) {
		for (String ext : importer.getExtensions()) {
			Utils.multimapPut(importers, ext.toLowerCase(), importer);
		}
	}

	/**
	 * Removes a {@link PathwayModelImporter}.
	 * 
	 * @param importer the pathway model importer to remove.
	 */
	public void removePathwayModelImporter(PathwayModelImporter importer) {
		for (String ext : importer.getExtensions()) {
			if (importers.containsKey(ext)) {
				if (importers.get(ext).size() == 1) {
					importers.remove(ext);
				} else {
					importers.get(ext).remove(importer);
				}
			}
		}
	}

	/**
	 * Adds a {@link PathwayModelExporter} that handles export of GPML to another
	 * file format.
	 * 
	 * @param exporter the pathway exporter to add.
	 */
	public void addPathwayModelExporter(PathwayModelExporter exporter) {
		for (String ext : exporter.getExtensions()) {
			Utils.multimapPut(exporters, ext.toLowerCase(), exporter);
		}
	}

	/**
	 * Removes a {@link PathwayModelExporter} that handles export of GPML to another
	 * file format.
	 * 
	 * @param exporter the pathway exporter to remove.
	 */
	public void removePathwayModelExporter(PathwayModelExporter exporter) {
		for (String ext : exporter.getExtensions()) {
			if (exporters.containsKey(ext)) {
				if (exporters.get(ext).size() == 1) {
					exporters.remove(ext);
				} else {
					exporters.get(ext).remove(exporter);
				}
			}
		}
	}

	/**
	 * Finds importers suitable for a given file. In case multiple importers match
	 * the file extension, the files may be inspected.
	 * 
	 * @param file the file.
	 * @returns null if no suitable importer could be found
	 */
	public Set<PathwayModelImporter> getPathwayModelImporters(File file) {
		Set<PathwayModelImporter> set = new HashSet<PathwayModelImporter>();

		// deep copy, so that we can safely modify our set
		set.addAll(importers.get(FileUtils.getExtension(file.toString()).toLowerCase()));

		if (set != null && set.size() > 1) {
			Iterator<PathwayModelImporter> i = set.iterator();
			while (i.hasNext()) {
				PathwayModelImporter j = i.next();
				if (!j.isCorrectType(file))
					i.remove();
			}
		}
		return set;
	}

	/**
	 * Finds a suitable exporter for the given filename
	 * 
	 * @returns null if no suitable exporter could be found
	 */
	public Set<PathwayModelExporter> getPathwayModelExporters(File file) {
		return exporters.get(FileUtils.getExtension(file.toString()).toLowerCase());
	}

	/**
	 * Returns all registered pathway model importers.
	 * 
	 * @return all registered pathway model importers
	 */
	public Set<PathwayModelImporter> getPathwayModelImporters() {
		return Utils.multimapValues(importers);
	}

	/**
	 * Returns all registered pathway exporters.
	 * 
	 * @return all registered pathway exporters
	 */
	public Set<PathwayModelExporter> getPathwayModelExporters() {
		return Utils.multimapValues(exporters);
	}

	// ================================================================================
	// Application Event Listener Methods
	// ================================================================================
	private List<ApplicationEventListener> applicationEventListeners = new ArrayList<ApplicationEventListener>();

	/**
	 * Adds an {@link ApplicationEventListener}, that will be notified if a property
	 * changes that has an effect throughout the program (e.g. opening a pathway)
	 * 
	 * @param listener the {@link ApplicationEventListener} to add.
	 */
	public void addApplicationEventListener(ApplicationEventListener listener) {
		if (listener == null)
			throw new NullPointerException();
		applicationEventListeners.add(listener);
	}

	/**
	 * Removes an {@link ApplicationEventListener}.
	 * 
	 * @param listener the {@link ApplicationEventListener} to remove.
	 */
	public void removeApplicationEventListener(ApplicationEventListener listener) {
		applicationEventListeners.remove(listener);
	}

	/**
	 * Fires a {@link ApplicationEvent} to notify all
	 * {@link ApplicationEventListener}s registered to this class
	 * 
	 * @param e the application event.
	 */
	private void fireApplicationEvent(ApplicationEvent event) {
		for (ApplicationEventListener listener : applicationEventListeners)
			listener.applicationEvent(event);
	}

	/**
	 * Implement this if you want to receive events upon opening / closing pathways
	 */
	public interface ApplicationEventListener {
		public void applicationEvent(ApplicationEvent event);
	}

	// ================================================================================
	// Application Name and Revision Methods
	// ================================================================================
	String appName = "Application name undefined";

	/**
	 * Returns full application name, including version No.
	 * 
	 * @return appName the application name.
	 */
	public String getApplicationName() {
		return appName;
	}

	/**
	 * Sets full application name, including version No.
	 * 
	 * @param value the value to set application name to.
	 */
	public void setApplicationName(String value) {
		appName = value;
	}

	/**
	 * Returns the subversion revision at the time of building. TODO not used
	 * 
	 * @return ""
	 */
	public static String getRevision() {
		return "";
	}

	/**
	 * Returns the current PathVisio version.
	 * 
	 * @return the current version
	 */
	public static String getVersion() {
		return Revision.VERSION;
	}

	// ================================================================================
	// Close and Dispose Methods
	// ================================================================================
	/**
	 * Fires a close event TODO: move APPLICATION_CLOSE to other place.
	 */
	public void close() {
		ApplicationEvent e = new ApplicationEvent(this, ApplicationEvent.Type.APPLICATION_CLOSE);
		fireApplicationEvent(e);
	}

	private boolean disposed = false;

	/**
	 * Frees all resources (such as listeners) held by this class. Owners of this
	 * class must explicitly dispose of it to clean up.
	 */
	public void dispose() {
		assert (!disposed);
		if (vPathwayModel != null)
			disposeVPathwayModel();
		applicationEventListeners.clear();
		disposed = true;
	}

	// ================================================================================
	// Non-Active Pathway Methods
	// ================================================================================
	// TODO: No reason to keep this in engine, it doesn't act on active pathway
	/**
	 * Exports given pathway to file. This function doesn't act on the active
	 * pathway.
	 * 
	 * @param pathway the pathway model to export
	 * @param file    file to write to.
	 * @returns a list of warnings that occurred during export, or an empty list if
	 *          there were none.
	 */
	public List<String> exportPathwayModel(File file, PathwayModel pathwayModel) throws ConverterException {
		Logger.log.trace("Exporting pathway to " + file);

		Set<PathwayModelExporter> set = getPathwayModelExporters(file);

		if (set != null && set.size() == 1) {
			PathwayModelExporter exporter = Utils.oneOf(set);
			exporter.doExport(file, pathwayModel);
			return exporter.getWarnings();
		} else
			throw new ConverterException(
					"Could not determine exporter for '" + FileUtils.getExtension(file.toString()) + "' files");
	}

	// TODO: No reason to keep this in engine, it doesn't act on active pathway
	/**
	 * Exports given pathway to file. This function doesn't act on the active
	 * pathway.
	 * 
	 * @param pathwayModel pathway model to export
	 * @param file    file to write to.
	 * @returns a list of warnings that occurred during export, or an empty list if
	 *          there were none.
	 */
	public List<String> exportPathwayModel(File file, PathwayModel pathwayModel, int zoom) throws ConverterException {
		Logger.log.trace("Exporting pathway to " + file);

		Set<PathwayModelExporter> set = getPathwayModelExporters(file);

		if (set != null && set.size() == 1) {
			PathwayModelExporter exporter = Utils.oneOf(set);
			exporter.doExport(file, pathwayModel, zoom);
			return exporter.getWarnings();
		} else
			throw new ConverterException(
					"Could not determine exporter for '" + FileUtils.getExtension(file.toString()) + "' files");
	}

	// TODO: No reason to keep this in engine, it doesn't act on active pathway
	/**
	 * Exports given pathway to file. This function doesn't act on the active
	 * pathway.
	 * 
	 * @param pathwayModel pathway model to export
	 * @param file    file to write to.
	 * @returns a list of warnings that occurred during export, or an empty list if
	 *          there were none.
	 */
	public List<String> exportPathwayModel(File file, PathwayModel pathwayModel, String exporterName)
			throws ConverterException {
		Logger.log.trace("Exporting pathway to " + file);

		Set<PathwayModelExporter> set = getPathwayModelExporters(file);
		try {
			for (PathwayModelExporter pExporter : set) {

				if (pExporter.getName().equals(exporterName)) {
					System.out.println(pExporter.getName());
					pExporter.doExport(file, pathwayModel);
					return pExporter.getWarnings();
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		// if (set != null && set.size() == 1) {
		// PathwayExporter exporter = Utils.oneOf(set);
		// exporter.doExport(file, pathway);
		// return exporter.getWarnings();
		// } else
		// throw new ConverterException(
		// "Could not determine exporter for '" +
		// FileUtils.getExtension(file.toString()) + "' files");
	}

}
