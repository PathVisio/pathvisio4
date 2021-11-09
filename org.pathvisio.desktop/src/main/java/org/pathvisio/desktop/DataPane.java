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

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLEditorKit;

import org.bridgedb.Xref;
import org.pathvisio.core.ApplicationEvent;
import org.pathvisio.core.Engine;
import org.pathvisio.core.Engine.ApplicationEventListener;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.model.PathwayObject;
import org.pathvisio.model.Xrefable;
import org.pathvisio.event.PathwayObjectEvent;
import org.pathvisio.event.PathwayObjectListener;
import org.pathvisio.prop.StaticProperty;
import org.pathvisio.core.view.model.VElement;
import org.pathvisio.core.view.model.VPathwayModel;
import org.pathvisio.core.view.model.VPathwayObject;
import org.pathvisio.core.view.model.SelectionBox.SelectionEvent;
import org.pathvisio.core.view.model.SelectionBox.SelectionListener;
import org.pathvisio.gui.DataPaneTextProvider;

/**
 * The backpage panel for the Swing version of PathVisio. This pane shows
 * annotation information from the individual Databases when a datanode or
 * interaction is clicked.
 * <p>
 * BackpagePane listens to selection events and other event types to update its
 * contents when necessary.
 * <p>
 * It uses a BackpageTextProvider to generate the html content, which has to be
 * inserted at construction time. Backpage generation may take a noticable
 * amount of time, therefore this task is always done in a background thread.
 * <p>
 * It is the responsibility of the instantiator to also call the dispose()
 * method, otherwise the background thread is not killed.
 */
public class DataPane extends JEditorPane
		implements ApplicationEventListener, SelectionListener, PathwayObjectListener {
	private final DataPaneTextProvider dpt;
	private Engine engine;
	private ExecutorService executor;

	public DataPane(DataPaneTextProvider dpt, Engine engine) {
		super();

		engine.addApplicationEventListener(this);
		VPathwayModel vp = engine.getActiveVPathwayModel();
		if (vp != null)
			vp.addSelectionListener(this);

		this.engine = engine;

		setEditable(false);
		setContentType("text/html");
		this.dpt = dpt;

		executor = Executors.newSingleThreadExecutor();

		// Workaround for #1313
		// Cause is java bug:
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6993691
		setEditorKit(new HTMLEditorKit() {
			protected Parser getParser() {
				try {
					Class c = Class.forName("javax.swing.text.html.parser.ParserDelegator");
					Parser defaultParser = (Parser) c.newInstance();
					return defaultParser;
				} catch (Throwable e) {
				}
				return null;
			}
		});
	}

	private PathwayObject input;

	public void setInput(final PathwayObject e) {
		// System.err.println("===== SetInput Called ==== " + e);
		if (e == input)
			return; // Don't set same input twice

		// Remove pathwaylistener from old input
		if (input != null)
			input.removeListener(this);

		if (e == null) {
			input = null;
			setText(dpt.getAnnotationHTML(null));
		} else {
			input = e;
			input.addListener(this);
			doQuery();
		}
	}

	private void doQuery() {
		setText("Loading");
		currRef = ((Xrefable) input).getXref();

		executor.execute(new Runnable() {
			public void run() {
				if (input == null)
					return;
				final String txt = dpt.getAnnotationHTML(input);

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						setText(txt);
						setCaretPosition(0); // scroll to top.
					}
				});
			}
		});
	}

	public void selectionEvent(SelectionEvent e) {
		switch (e.type) {
		case SelectionEvent.OBJECT_ADDED:
			// Just take the first DataNode in the selection
			Iterator<VElement> it = e.selection.iterator();
			while (it.hasNext()) {
				VElement o = it.next();
				// works for all Graphics object
				// the backpage checks and gives the correct error if
				// it's not a datanode or line
				if (o instanceof VPathwayObject) {
					setInput(((VPathwayObject) o).getPathwayObject());
					break;
				}
			}
			break;
		case SelectionEvent.OBJECT_REMOVED:
			if (e.selection.size() != 0)
				break;
		case SelectionEvent.SELECTION_CLEARED:
			setInput(null);
			break;
		}
	}

	public void applicationEvent(ApplicationEvent e) {
		switch (e.getType()) {
		case VPATHWAY_CREATED:
			((VPathwayModel) e.getSource()).addSelectionListener(this);
			break;
		case VPATHWAY_DISPOSED:
			((VPathwayModel) e.getSource()).removeSelectionListener(this);
			// remove content of backpage when pathway is closed
			input = null;
			setText(dpt.getAnnotationHTML(null));
			break;
		}
	}

	Xref currRef;

	public void gmmlObjectModified(PathwayObjectEvent e) {
		PathwayObject pe = e.getModifiedPathwayObject();
		// static property for xref? TODO
		if (input != null && (e.affectsProperty(StaticProperty.XREF))) {
			Xref nref = new Xref(((Xrefable) pe).getXref().getId(), ((Xrefable) input).getXref().getDataSource());
			if (!nref.equals(currRef)) {
				doQuery();
			}
		}
	}

	private boolean disposed = false;

	public void dispose() {
		assert (!disposed);
		engine.removeApplicationEventListener(this);
		VPathwayModel vpwy = engine.getActiveVPathwayModel();
		if (vpwy != null)
			vpwy.removeSelectionListener(this);
		executor.shutdown();
		disposed = true;
	}
}
