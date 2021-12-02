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
package org.pathvisio.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;

import org.bridgedb.bio.Organism;
import org.pathvisio.core.ApplicationEvent;
import org.pathvisio.core.Engine;
import org.pathvisio.core.Globals;
import org.pathvisio.core.Engine.ApplicationEventListener;
import org.pathvisio.core.data.GdbManager;
import org.pathvisio.debug.Logger;
import org.pathvisio.model.ConverterException;
import org.pathvisio.model.GpmlFormat;
import org.pathvisio.model.PathwayModel;
import org.pathvisio.model.PathwayModel.StatusFlagEvent;
import org.pathvisio.io.PathwayModelIO;
import org.pathvisio.core.preferences.GlobalPreference;
import org.pathvisio.core.preferences.Preference;
import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.core.util.ProgressKeeper;
import org.pathvisio.util.Utils;
import org.pathvisio.core.view.model.VPathwayWrapper;
import org.pathvisio.gui.dialogs.NewPathwayDialog;
import org.pathvisio.gui.dialogs.OkCancelDialog;
import org.pathvisio.gui.dialogs.PopupDialogHandler;
import org.pathvisio.gui.view.VPathwayModelSwing;

/**
 * SwingEngine ties together a number of global objects needed both in the
 * WikiPathways applet and in the Standalone App, but not in command-line tools.
 *
 * It keeps the main panel, the main frame and has helper functions for opening,
 * closing, importing and exporting Pathways.
 * 
 * @author unknown
 */
public class SwingEngine implements ApplicationEventListener, PathwayModel.StatusFlagListener, HyperlinkListener {

	private Engine engine;
	private GdbManager gdbManager = null;
	// private final Compat compat;
	private CommonActions actions;
	private MainPanel mainPanel;
	private JFrame frame; // may be null (for applet...) TODO

	/**
	 * Constructor for swing engine.
	 * 
	 * @param engine the engine.
	 */
	public SwingEngine(Engine engine) {
		this.engine = engine;
		gdbManager = new GdbManager();
		actions = new CommonActions(this);
		engine.addApplicationEventListener(this);
		// compat = new Compat(this);
		// engine.addApplicationEventListener(compat);
	}

	/**
	 * Returns the {@link Engine}.
	 * 
	 * @return engine the engine.
	 */
	public Engine getEngine() {
		return engine;
	}

	/**
	 * Returns the {@link GdbManager}.
	 * 
	 * @return gdbManager
	 */
	public GdbManager getGdbManager() {
		return gdbManager;
	}

	/**
	 * Returns the {@link CommonActions}.
	 * 
	 * @return actions the common actions
	 */
	public CommonActions getActions() {
		return actions;
	}

	/**
	 * Returns the MainPanel.
	 * 
	 * @return mainPanel the main panel.
	 */
	public MainPanel getApplicationPanel() {
		return getApplicationPanel(false);
	}

	/**
	 * Returns the MainPanel given boolean forceNew.
	 * 
	 * @param forceNew the boolean.
	 * @return mainPanel the main panel.
	 */
	public MainPanel getApplicationPanel(boolean forceNew) {
		if (forceNew || !hasApplicationPanel()) {
			mainPanel = new MainPanel(this);
		}
		return mainPanel;
	}

	/**
	 * Sets the MainPanel.
	 * 
	 * @param mp the main panel.
	 */
	public void setApplicationPanel(MainPanel mp) {
		if (mainPanel != null) {
			Container parent = mainPanel.getParent();
			if (parent != null)
				parent.remove(mainPanel);
		}
		mainPanel = mp;
	}

	/**
	 * Returns true if main panel not null.
	 * 
	 * @return true if main panel not null, false otherwise.
	 */
	public boolean hasApplicationPanel() {
		return mainPanel != null;
	}

	/**
	 * @param message
	 * @param c
	 * @param e
	 */
	public void handleConverterException(String message, Component c, Throwable e) {
		if (e.getMessage() != null && e.getMessage().contains("Cannot find the declaration of element 'Pathway'")) {
			JOptionPane.showMessageDialog(c, Utils.formatExceptionMsg(message) + "\n\n"
					+ "The most likely cause for this error is that you are trying to open an old Gpml file. "
					+ "Please note that the Gpml format has changed as of March 2007. "
					+ "The standard pathway set can be re-downloaded from http://pathvisio.org "
					+ "Non-standard pathways need to be recreated or upgraded. " + "Please contact the authors at "
					+ Globals.DEVELOPER_EMAIL + " if you need help with this.\n", "Error", JOptionPane.ERROR_MESSAGE);
			Logger.log.error("Converter exception", e);
		} else {
			JOptionPane.showMessageDialog(c,
					Utils.formatExceptionMsg(message) + "\nSee error log for details\n" + e.getClass(), "Error",
					JOptionPane.ERROR_MESSAGE);
			Logger.log.error("Converter exception", e);
		}
	}

	/**
	 * @param message
	 * @param c
	 * @param e
	 */
	public void handleMalformedURLException(String message, Component c, Throwable e) {
		if (e.getMessage() != null && e.getMessage().contains("no protocol:")) {
			JOptionPane.showMessageDialog(c, Utils.formatExceptionMsg(message) + "\n\n"
					+ "Please correct the specified hyperlink for this Label in the \"properties pane\" on the right.\n"
					+ "(http://www.example.com)\n\n" + "Please contact the authors at " + Globals.DEVELOPER_EMAIL
					+ " if you need help with this.\n", "Error", JOptionPane.ERROR_MESSAGE);
			Logger.log.error("MalformedURLException", e);
		} else {
			JOptionPane.showMessageDialog(c,
					Utils.formatExceptionMsg(message) + "\nSee error log for details\n" + e.getClass(), "Error",
					JOptionPane.ERROR_MESSAGE);
			Logger.log.error("MalformedURLException", e);
		}
	}

	/**
	 * @return
	 */
	public VPathwayWrapper createWrapper() {
		return new VPathwayModelSwing(getApplicationPanel().getScrollPane());
	}

	// TODO: deprecate
	public boolean processTask(ProgressKeeper pk, ProgressDialog d, SwingWorker<Boolean, Boolean> sw) {
		sw.execute();
		d.setVisible(true);
		try {
			return sw.get();
		} catch (ExecutionException e) {
			handleConverterException("Exception during conversion", null, e.getCause());
			return false;
		} catch (InterruptedException e) {
			handleConverterException("Conversion was cancelled or interrupted", null, e);
			return false;
		}
	}

	/**
	 * @param url
	 * @return
	 */
	public boolean openPathwayModel(final URL url) {
		final ProgressKeeper pk = new ProgressKeeper();
		final ProgressDialog d = new ProgressDialog(JOptionPane.getFrameForComponent(getApplicationPanel()), "", pk,
				false, true);

		SwingWorker<Boolean, Boolean> sw = new SwingWorker<Boolean, Boolean>() {
			protected Boolean doInBackground() {
				pk.setTaskName("Opening pathway");
				try {
					engine.setWrapper(createWrapper());
					engine.openPathwayModel(url);
					return true;
				} catch (ConverterException e) {
					handleConverterException(e.getMessage(), null, e);
					return false;
				} finally {
					pk.finished();
				}
			}
		};

		return processTask(pk, d, sw);
	}

	/**
	 * @param f
	 * @return
	 */
	public boolean openPathwayModel(final File f) {
		final ProgressKeeper pk = new ProgressKeeper();
		final ProgressDialog d = new ProgressDialog(JOptionPane.getFrameForComponent(getApplicationPanel()), "", pk,
				false, true);

		engine.setWrapper(createWrapper());
		SwingWorker<Boolean, Boolean> sw = new SwingWorker<Boolean, Boolean>() {
			protected Boolean doInBackground() {
				pk.setTaskName("Opening pathway");
				try {
					engine.openPathwayModel(f);
					return true;
				} catch (ConverterException e) {
					handleConverterException(e.getMessage(), null, e);
					return false;
				} finally {
					pk.finished();
				}
			}
		};

		return processTask(pk, d, sw);
	}

	/**
	 * @param f
	 * @return
	 */
	public boolean importPathwayModel(final File f) {
		final ProgressKeeper pk = new ProgressKeeper();
		final ProgressDialog d = new ProgressDialog(JOptionPane.getFrameForComponent(getApplicationPanel()), "", pk,
				false, true);

		SwingWorker<Boolean, Boolean> sw = new SwingWorker<Boolean, Boolean>() {
			protected Boolean doInBackground() {
				pk.setTaskName("Importing pathway");
				try {
					boolean editMode = engine.hasVPathwayModel() ? engine.getActiveVPathwayModel().isEditMode() : true;
					engine.setWrapper(createWrapper());
					engine.importPathwayModel(f);
					engine.getActiveVPathwayModel().setEditMode(editMode);
					return true;
				} catch (ConverterException e) {
					handleConverterException(e.getMessage(), frame, e);
					return false;
				} finally {
					pk.finished();
				}
			}
		};

		return processTask(pk, d, sw);

	}

	/**
	 * 
	 */
	public void newPathwayModel() {
		engine.setWrapper(createWrapper());
		engine.newPathwayModel();
		NewPathwayDialog dlg = new NewPathwayDialog(this, "Pathway attributes");
		dlg.setVisible(true);
	}

	/**
	 * @return
	 */
	public boolean exportPathwayModel() {
		PathwayModelChooser pc = new PathwayModelChooser("Export", JFileChooser.SAVE_DIALOG,
				GlobalPreference.DIR_LAST_USED_EXPORT, engine.getPathwayModelExporters());
		int status = pc.show();

		if (status == JFileChooser.APPROVE_OPTION) {
			File f = pc.getSelectedFile();

			PathwayModelFileFilter ff = (PathwayModelFileFilter) pc.getFileFilter();
			if (!f.toString().toUpperCase().endsWith("." + ff.getDefaultExtension().toUpperCase())) {
				f = new File(f.toString() + "." + ff.getDefaultExtension());
			}
//			return exportPathway(f);
			return exportPathwayModel(f, ff.name);

		}
		return false;
	}

	/**
	 * A wrapper around JFileChooser that has the right defaults and File Filters.
	 * 
	 * @author unknown
	 */
	private class PathwayModelChooser {
		private final JFileChooser jfc;
		private final String taskName;
		private final Preference dirPreference;

		public PathwayModelChooser(String taskName, int dialogType, Preference dirPreference,
				Set<? extends PathwayModelIO> set) {
			jfc = new JFileChooser();
			this.taskName = taskName;
			this.dirPreference = dirPreference;
			createFileFilters(set);
			jfc.setDialogTitle(taskName + " pathway");
			jfc.setDialogType(dialogType);
			jfc.setCurrentDirectory(PreferenceManager.getCurrent().getFile(dirPreference));
		}

		/**
		 * Creates a file chooser populated with file filters for the given pathway
		 * importers / exporters
		 * 
		 * @param set
		 */
		private void createFileFilters(Set<? extends PathwayModelIO> set) {
			jfc.setAcceptAllFileFilterUsed(false);

			SortedSet<PathwayModelIO> exporters = new TreeSet<PathwayModelIO>(new Comparator<PathwayModelIO>() {
				public int compare(PathwayModelIO o1, PathwayModelIO o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			exporters.addAll(set);

			FileFilter selectedFilter = null;
			for (PathwayModelIO exp : exporters) {
				FileFilter ff = new PathwayModelFileFilter(exp);
				jfc.addChoosableFileFilter(ff);
				if (exp instanceof GpmlFormat) {
					selectedFilter = ff;
				}
			}
			if (selectedFilter != null)
				jfc.setFileFilter(selectedFilter);
		}

		/**
		 * @return
		 */
		public FileFilter getFileFilter() {
			return jfc.getFileFilter();
		}

		/**
		 * @return
		 */
		public int show() {
			int status = jfc.showDialog(getApplicationPanel(), taskName);
			if (status == JFileChooser.APPROVE_OPTION) {
				PreferenceManager.getCurrent().setFile(dirPreference, jfc.getCurrentDirectory());
			}
			return status;
		}

		/**
		 * @return
		 */
		public File getSelectedFile() {
			return jfc.getSelectedFile();
		}
	}

	/**
	 * @param f
	 * @param exporterName
	 * @return
	 */
	public boolean exportPathwayModel(final File f, final String exporterName) {
		if (mayOverwrite(f)) {
			final ProgressKeeper pk = new ProgressKeeper();
			final ProgressDialog d = new ProgressDialog(JOptionPane.getFrameForComponent(getApplicationPanel()), "", pk,
					false, true);

			// create a clone so we can safely act on it in a worker thread.
			final PathwayModel clone = engine.getActivePathwayModel().clone();

			SwingWorker<Boolean, Boolean> sw = new SwingWorker<Boolean, Boolean>() {
				private List<String> warnings;

				/**
				 *
				 */
				@Override
				protected Boolean doInBackground() {
					try {
						pk.setTaskName("Exporting pathway");
						warnings = engine.exportPathwayModel(f, clone, exporterName);
						return true;
					} catch (Exception e) {
						handleConverterException(e.getMessage(), frame, e);
						return false;
					} finally {
						pk.finished();
					}
				}

				/**
				 *
				 */
				@Override
				public void done() {
					if (warnings != null && warnings.size() > 0) {
						OkCancelDialog dlg = new OkCancelDialog(frame, "Conversion warnings", getFrame(), true);
						JTextArea area = new JTextArea(60, 30);
						for (String w : warnings)
							area.append(w + "\n");
						dlg.setDialogComponent(area);
						dlg.pack();
						dlg.setVisible(true);
					}
				}
			};

			return processTask(pk, d, sw);
		}
		return false;
	}

	/**
	 * @param f
	 * @return
	 */
	public boolean exportPathwayModel(final File f) {
		if (mayOverwrite(f)) {
			final ProgressKeeper pk = new ProgressKeeper();
			final ProgressDialog d = new ProgressDialog(JOptionPane.getFrameForComponent(getApplicationPanel()), "", pk,
					false, true);

			// create a clone so we can safely act on it in a worker thread.
			final PathwayModel clone = engine.getActivePathwayModel().clone();

			SwingWorker<Boolean, Boolean> sw = new SwingWorker<Boolean, Boolean>() {
				private List<String> warnings;

				@Override
				protected Boolean doInBackground() {
					try {
						pk.setTaskName("Exporting pathway");
						warnings = engine.exportPathwayModel(f, clone);
						return true;
					} catch (ConverterException e) {
						handleConverterException(e.getMessage(), frame, e);
						return false;
					} finally {
						pk.finished();
					}
				}

				@Override
				public void done() {
					if (warnings != null && warnings.size() > 0) {
						OkCancelDialog dlg = new OkCancelDialog(frame, "Conversion warnings", getFrame(), true);
						JTextArea area = new JTextArea(60, 30);
						for (String w : warnings)
							area.append(w + "\n");
						dlg.setDialogComponent(area);
						dlg.pack();
						dlg.setVisible(true);
					}
				}
			};

			return processTask(pk, d, sw);
		}
		return false;
	}

	/**
	 * @return
	 */
	public boolean importPathwayModel() {
		PathwayModelChooser pc = new PathwayModelChooser("Import", JFileChooser.OPEN_DIALOG,
				GlobalPreference.DIR_LAST_USED_IMPORT, engine.getPathwayModelImporters());
		int status = pc.show();

		if (status == JFileChooser.APPROVE_OPTION) {
			File f = pc.getSelectedFile();
			return importPathwayModel(f);
		}
		return false;
	}

	private final Set<PathwayModelIO> GPML_FORMAT_ONLY = Utils.setOf((PathwayModelIO) new GpmlFormat());

	/**
	 * Opens a file chooser dialog, and opens the chosen pathway.
	 * 
	 * @return true if a pathway was openend, false if the operation was cancelled
	 */
	public boolean openPathwayModel() {
		PathwayModelChooser pc = new PathwayModelChooser("Open", JFileChooser.OPEN_DIALOG,
				GlobalPreference.DIR_LAST_USED_OPEN, GPML_FORMAT_ONLY);
		int status = pc.show();

		if (status == JFileChooser.APPROVE_OPTION) {
			File f = pc.getSelectedFile();
			return openPathwayModel(f);
		}
		return false;
	}

	/**
	 * @param f
	 * @return
	 */
	public boolean mayOverwrite(File f) {
		boolean allow = true;
		if (f.exists()) {
			int status = JOptionPane.showConfirmDialog(frame, "File " + f.getName() + " already exists, overwrite?",
					"File already exists", JOptionPane.YES_NO_OPTION);
			allow = status == JOptionPane.YES_OPTION;
		}
		return allow;
	}

	/**
	 * @return
	 */
	public boolean savePathwayModelAs() {
		PathwayModelChooser pc = new PathwayModelChooser("Save", JFileChooser.SAVE_DIALOG,
				GlobalPreference.DIR_LAST_USED_SAVE, GPML_FORMAT_ONLY);
		int status = pc.show();

		if (status == JFileChooser.APPROVE_OPTION) {
			File toFile = pc.getSelectedFile();
			String fn = toFile.toString();
			if (!fn.toLowerCase().endsWith(Engine.PATHWAY_FILE_EXTENSION)) {
				toFile = new File(fn + "." + Engine.PATHWAY_FILE_EXTENSION);
			}
			try {
				if (mayOverwrite(toFile)) {
					engine.savePathwayModel(toFile);
					return true;
				}
			} catch (ConverterException e) {
				handleConverterException(e.getMessage(), null, e);
			}
		}
		return false;
	}

	/**
	 * @return
	 */
	public boolean savePathwayModel() {
		PathwayModel pathway = engine.getActivePathwayModel();

		boolean result = true;

		// Overwrite the existing xml file.
		// If the target file is read-only, let the user select a new pathway
		if (pathway.getSourceFile() != null && pathway.getSourceFile().canWrite()) {
			try {
				engine.savePathwayModel(pathway.getSourceFile());
			} catch (ConverterException e) {
				handleConverterException(e.getMessage(), null, e);
			}
		} else {
			result = savePathwayModelAs();
		}

		return result;
	}

	/**
	 * Call this when the user is about to perform an action that could lead to
	 * discarding the current pathway. (For example when creating a new pathway)
	 *
	 * Checks if there are any unsaved changes, and asks the user if they want to
	 * save those changes.
	 *
	 * @return true if the user allows discarding the pathway, possibly after
	 *         saving.
	 */
	public boolean canDiscardPathwayModel() {
		PathwayModel pathway = engine.getActivePathwayModel();
		// checking not necessary if there is no pathway or if pathway is not changed.

		if (pathway == null || !pathway.hasChanged())
			return true;
		int result = JOptionPane.showConfirmDialog(frame, "Save changes?",
				"Your pathway has changed. Do you want to save?", JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE);
		if (result == JOptionPane.CANCEL_OPTION) // cancel
		{
			return false;
		} else if (result == JOptionPane.YES_OPTION) // yes
		{
			// return false if save is cancelled.
			return (savePathwayModel());
		}
		// yes or no
		return true;
	}

	/**
	 *
	 */
	public void applicationEvent(ApplicationEvent e) {
		switch (e.getType()) {
		case PATHWAY_OPENED:
		case PATHWAY_NEW:
			updateTitle();
			engine.getActivePathwayModel().addStatusFlagListener(SwingEngine.this);
			break;
		}
	}

	/**
	 * 
	 */
	public void updateTitle() {
		if (frame != null) {
			if (engine.getActivePathwayModel() == null) {
				frame.setTitle(engine.getApplicationName());
			} else {
				boolean changeStatus = engine.getActivePathwayModel().hasChanged();
				// get filename, or (New Pathway) if current pathway hasn't been opened yet
				String fname = (engine.getActivePathwayModel().getSourceFile() == null) ? "(New Pathway)"
						: engine.getActivePathwayModel().getSourceFile().getName();
				frame.setTitle((changeStatus ? "*" : "") + fname + " - " + engine.getApplicationName());
			}
		}
	}

	/**
	 *
	 */
	public void statusFlagChanged(StatusFlagEvent e) {
		updateTitle();
	}

	/**
	 * @param frame
	 */
	public void setFrame(JFrame frame) {
		this.frame = frame;
	}

	/**
	 * @return
	 */
	public JFrame getFrame() {
		return frame;
	}

	private Browser browser = null;

	/**
	 * Set the browser launcher that will be used to open urls in the system's
	 * default web browser.
	 * 
	 * @param b
	 */
	public void setUrlBrowser(Browser b) {
		this.browser = b;
	}

	/**
	 * Opens an URL in the system's default browser if a browser is set
	 * 
	 * @see #setUrlBrowser
	 * @param url
	 * @throws UnsupportedOperationException when there is no browser set.
	 */
	public void openUrl(URL url) throws UnsupportedOperationException {
		if (browser != null)
			browser.openUrl(url);
	}

	/**
	 * Simple interface to allow different browser launcher implementations. Note:
	 * Java 1.6 provides an easy method to open an url in the default system browser
	 * using {@link Desktop#browse(java.net.URI)}, we should use this in the future.
	 * 
	 * @author thomas
	 */
	public interface Browser {
		public void openUrl(URL url);
	}

	private boolean disposed = false;

	/**
	 * Frees all resources (such as listeners) held by this class. Owners of this
	 * class must explicitly dispose of it to clean up.
	 */
	public void dispose() {
		assert (!disposed);
		engine.removeApplicationEventListener(this);
		// engine.removeApplicationEventListener(compat);
		disposed = true;
	}

	/**
	 * Returns the organism set in the active pathway. May return null if there is
	 * no current organism set
	 */
	public Organism getCurrentOrganism() {
		String organism = getEngine().getActivePathwayModel().getPathway().getOrganism();
		return Organism.fromLatinName(organism);
	}

	/**
	 * @param e the hyperlink event.
	 */
	public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			try {
				openUrl(e.getURL());
			} catch (UnsupportedOperationException ex) {
				Logger.log.error("Unable to open URL", ex);
				JOptionPane.showMessageDialog(mainPanel, "No browser launcher specified", "Unable to open link",
						JOptionPane.ERROR_MESSAGE);
			}
		} else {
			// TODO: show URL in status bar when mousing over
		}
	}

	private PopupDialogHandler popupDlgHandler = new PopupDialogHandler(this);

	/**
	 * Returns the {@link PopupDialogHandler}.
	 * 
	 * @return
	 */
	public PopupDialogHandler getPopupDialogHandler() {
		return popupDlgHandler;
	}
}
