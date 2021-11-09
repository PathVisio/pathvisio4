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
package org.pathvisio.desktop.debug;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import org.bridgedb.DataSource;
import org.bridgedb.Xref;
import org.pathvisio.core.Engine;
import org.pathvisio.debug.StopWatch;
import org.pathvisio.io.ConverterException;
import org.pathvisio.model.PathwayModel;
import org.pathvisio.model.type.DataNodeType;
import org.pathvisio.util.XrefUtils;
import org.pathvisio.model.DataNode;
import org.pathvisio.model.Interaction;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.core.view.model.VDataNode;
import org.pathvisio.core.view.model.VElement;
import org.pathvisio.core.view.model.VLineElement;
import org.pathvisio.core.view.model.VPathwayModel;
import org.pathvisio.core.view.model.VPathwayObject;
import org.pathvisio.gui.view.VPathwayModelSwing;

import buildsystem.Measure;
import junit.framework.TestCase;

/**
 * Test memory usage and speed of object creation, pathway loading, selection,
 * and drag operations.
 */
public class TestAndMeasure extends TestCase {
	private static final File PATHVISIO_BASEDIR = new File("../..");
	private static final File TEST_PATHWAY = new File(PATHVISIO_BASEDIR, "testData/WP248_2008a.gpml");

	private Measure measure;

	@Override
	public void setUp() {
		measure = new Measure("pv_mut.log");
	}

	private interface ObjectTester {
		String getName();

		public Object create();
	}

	private static class MemWatch {
		private Runtime runtime = Runtime.getRuntime();

		private void runGC() {
			for (int i = 0; i < 20; ++i) {
				System.gc();
				try {
					Thread.sleep(100);
				} catch (InterruptedException ex) {
				}
			}
		}

		private long memStart;

		public void start() {
			runGC();
			memStart = (runtime.totalMemory() - runtime.freeMemory());
		}

		public long stop() {
			runGC();
			long memEnd = (runtime.totalMemory() - runtime.freeMemory());
			return (memEnd - memStart);
		}
	}

	static final int N = 1000;

	private void individialTest(ObjectTester tester) {
		// 1000 warm-up rounds
		for (int i = 0; i < 1000; ++i) {
			Object o = tester.create();
		}

		Runtime runtime = Runtime.getRuntime();
		for (int i = 0; i < 20; ++i) {
			System.gc();
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
			}
		}

		Object[] array = new Object[N];
		StopWatch sw = new StopWatch();
		for (int i = 0; i < 20; ++i) {
			System.gc();
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
			}
		}
		long memStart = (runtime.totalMemory() - runtime.freeMemory());
		sw.start();
		for (int i = 0; i < N; ++i) {
			array[i] = tester.create();
		}
		long msec = sw.stop();
		for (int i = 0; i < 20; ++i) {
			System.gc();
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
			}
		}
		long memEnd = (runtime.totalMemory() - runtime.freeMemory());
		measure.add("Memory::" + tester.getName() + " " + N + "x", "" + (memEnd - memStart) / N, "bytes");
		measure.add("Speed::" + tester.getName() + " " + N + "x", "" + (float) (msec) / (float) (N), "msec");
	}

	public void testFile() {
		assertTrue("Missing file required for test: " + TEST_PATHWAY, TEST_PATHWAY.exists());
	}

	public void testObjectCreation() {
		PreferenceManager.init();
		final PathwayModel pwy1 = new PathwayModel();
		final PathwayModel pwy2 = new PathwayModel();
		final PathwayModel pwy3 = new PathwayModel();
		final VPathwayModel vpwy3 = new VPathwayModel(null);
		final PathwayModel pwy4 = new PathwayModel();
		final VPathwayModel vpwy4 = new VPathwayModel(null);

		individialTest(new ObjectTester() {
			public Object create() {
				return new Xref("ENS0000001", DataSource.getByCompactIdentifierPrefix("ncbigene"));
			}

			public String getName() {
				return "Xref";
			}
		});

		individialTest(new ObjectTester() {
			public Object create() {
				DataNode elt = new DataNode("INSR", DataNodeType.GENEPRODUCT);
				elt.setCenterX(5);
				elt.setCenterY(10);
				elt.setWidth(8);
				elt.setHeight(10);
				elt.setXref(XrefUtils.createXref("3463", "ncbigene"));
				pwy1.add(elt);
				return elt;
			}

			public String getName() {
				return "PathwayElement - DataNode";
			}
		});
		individialTest(new ObjectTester() {
			public Object create() {
				Interaction elt = new Interaction();
				elt.setStartLinePointX(5);
				elt.setStartLinePointY(10);
				elt.setEndLinePointX(8);
				elt.setEndLinePointY(10);
//				elt.setStartGraphRef("abc"); TODO
//				elt.setEndGraphRef("def"); TODO
				pwy2.add(elt);
				return elt;
			}

			public String getName() {
				return "PathwayElement - Line";
			}
		});

		individialTest(new ObjectTester() {
			public Object create() {
				DataNode elt = new DataNode("INSR", DataNodeType.GENEPRODUCT);
				elt.setCenterX(5);
				elt.setCenterY(10);
				elt.setWidth(8);
				elt.setHeight(10);
				elt.setXref(XrefUtils.createXref("3463", "ncbigene"));
				pwy3.add(elt);
				VDataNode velt = new VDataNode(vpwy3, elt);
				return velt;
			}

			public String getName() {
				return "M/V GeneProduct pair";
			}
		});
		individialTest(new ObjectTester() {
			public Object create() {
				Interaction elt = new Interaction();
				elt.setStartLinePointX(5);
				elt.setStartLinePointY(10);
				elt.setEndLinePointX(8);
				elt.setEndLinePointY(10);
//				elt.setStartGraphRef("abc"); TODO
//				elt.setEndGraphRef("def"); TODO
				pwy4.add(elt);
				VLineElement velt = new VLineElement(vpwy4, elt);
				return velt;
			}

			public String getName() {
				return "M/V Line pair";
			}
		});
	}

	public void testPathwayLoading() throws ConverterException {
		PreferenceManager.init();

		StopWatch sw = new StopWatch();

		MemWatch mw = new MemWatch();

		mw.start();
		sw.start();
		PathwayModel pwy = new PathwayModel();
		pwy.readFromXml(TEST_PATHWAY, true);
		measure.add("Speed::Hs_Apoptosis readFromXml (+validate)", "" + sw.stop(), "msec");
		measure.add("Memory::Hs_Apoptosis readFromXml (+validate)", "" + mw.stop() / 1024, "kb");

		mw.start();
		sw.start();
		JScrollPane sp = new JScrollPane();
		VPathwayModelSwing wrapper = new VPathwayModelSwing(sp);

		Engine engine = new Engine();
		VPathwayModel vpwy = wrapper.createVPathway();
		vpwy.activateUndoManager(engine);
		vpwy.fromModel(pwy);

		measure.add("Speed::Hs_Apoptosis create VPathway", "" + sw.stop(), "msec");
		measure.add("Memory::Hs_Apoptosis create VPathway", "" + mw.stop() / 1024, "kb");

		mw.start();
		sw.start();
		wrapper.setSize(vpwy.getVWidth(), vpwy.getVHeight());
		BufferedImage image = new BufferedImage(vpwy.getVWidth(), vpwy.getVHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = image.createGraphics();
		wrapper.paint(g2);
		measure.add("Speed::Hs_Apoptosis paint", "" + sw.stop(), "msec");
		measure.add("Memory::Hs_Apoptosis paint", "" + mw.stop() / 1024, "kb");
		g2.dispose();

		mw.start();
		sw.start();
		for (VElement elt : vpwy.getDrawingObjects()) {
			elt.select();
		}
		measure.add("Speed::Hs_Apoptosis select all", "" + sw.stop(), "msec");
		measure.add("Memory::Hs_Apoptosis select all", "" + mw.stop() / 1024, "kb");

		image = new BufferedImage(vpwy.getVWidth(), vpwy.getVHeight(), BufferedImage.TYPE_INT_RGB);

		g2 = image.createGraphics();
		wrapper.paint(g2);
		g2.dispose();

		mw.start();
		sw.start();
		// move all selected items, triggers undo action
		for (int i = 0; i < 10; ++i)
			vpwy.moveByKey(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), 10);
		measure.add("Speed::Hs_Apoptosis move up 10x", "" + sw.stop(), "msec");
		measure.add("Memory::Hs_Apoptosis move up 10x", "" + mw.stop() / 1024, "kb");
	}

}
