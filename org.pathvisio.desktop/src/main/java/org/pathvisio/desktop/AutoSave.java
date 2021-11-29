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
package org.pathvisio.desktop;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.pathvisio.core.Engine;
import org.pathvisio.debug.Logger;
import org.pathvisio.model.ConverterException;
import org.pathvisio.model.GpmlFormat;
import org.pathvisio.model.PathwayModel;
import org.pathvisio.gui.SwingEngine;

/**
 * Collection of methods for autosave and recovery of PathVisio files
 * 
 * @author unknown
 */
public class AutoSave {
	
	private Timer timer;
	private final SwingEngine swingEngine;
	private final Engine engine;
	private final File autoSaveFile = autoSaveFileLocation();

	/**
	 * 
	 * @param se the swing engine. 
	 */
	public AutoSave(SwingEngine se) {
		engine = se.getEngine();
		swingEngine = se;
	}

	/**
	 * 
	 * @return autoSaveFile the file. 
	 */
	private File autoSaveFileLocation() {
		String tempDir = System.getProperty("java.io.tmpdir");
		File autoSaveFile = new File(tempDir, "PathVisioAutoSave.gpml");
		return autoSaveFile;
	}

	/**
	 * @throws ConverterException
	 */
	private void autoSaveFile() throws ConverterException {
		PathwayModel p = engine.getActivePathwayModel();
		if (p != null) {
			GpmlFormat.writeToXml(p, autoSaveFile, true);
			Logger.log.info("Autosaved");
		}
	}

	/**
	 * 
	 * @author unknown
	 */
	private class DoSave extends TimerTask {
		public void run() {
			try {
				// For reasons of thread-safety, autoSaveFile()
				// must be called on the GUI thread.
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						try {
							autoSaveFile();
						} catch (ConverterException e) {
							Logger.log.error("Autosave failed", e);
						}
					}
				});
			} catch (InterruptedException e) {
				Logger.log.error("Autosave failed", e);
			} catch (InvocationTargetException e) {
				Logger.log.error("Autosave failed", e);
			}
		}
	}

	/**
	 * 
	 * @param period the autosave period in seconds
	 */
	public void startTimer(int period) {
		if (autoSaveFile.exists()) {
			autoRecoveryDlg();
		}
		timer = new Timer();
		timer.schedule(new DoSave(), period * 1000, period * 1000);
	}

	/**
	 * 
	 */
	public void stopTimer() {
		timer.cancel();
		autoSaveFile.delete();
	}

	/**
	 * 
	 */
	private void autoRecoveryDlg() {
		int result = JOptionPane.showConfirmDialog(swingEngine.getApplicationPanel(),
				"Sorry, it seems PathVisio crashed.\n" + "Recover the auto-saved file?", "Crash recovery",
				JOptionPane.YES_NO_OPTION);
		if (result == JOptionPane.YES_OPTION) {
			swingEngine.openPathwayModel(autoSaveFile);
		}
	}
}
