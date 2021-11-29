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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.pathvisio.core.model.BatikImageExporter;
import org.pathvisio.core.model.DataNodeListExporter;
import org.pathvisio.core.model.EUGeneExporter;
import org.pathvisio.core.model.ImageExporter;
import org.pathvisio.core.model.RasterImageExporter;
import org.pathvisio.event.PathwayModelEvent;
import org.pathvisio.event.PathwayModelListener;
import org.pathvisio.event.PathwayObjectEvent;
import org.pathvisio.event.PathwayObjectListener;
import org.pathvisio.model.ConverterException;
import org.pathvisio.model.DataNode;
import org.pathvisio.model.Interaction;
import org.pathvisio.model.Label;
import org.pathvisio.model.Pathway;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.model.PathwayModel;
import org.pathvisio.model.type.DataNodeType;
import org.pathvisio.core.preferences.PreferenceManager;

import junit.framework.TestCase;

/**
 * 
 * @author unknown
 */
public class Test2 extends TestCase implements PathwayModelListener, PathwayObjectListener {

	PathwayModel data;
	DataNode o;
	List<PathwayModelEvent> received;
	List<PathwayObjectEvent> receivedElementEvents;
	Interaction l;

	private static final File PATHVISIO_BASEDIR = new File("../..");

	public void setUp() {
		PreferenceManager.init();
		data = new PathwayModel();
		data.addListener(this);
		o = new DataNode("", DataNodeType.UNDEFINED);
		received = new ArrayList<PathwayModelEvent>();
		receivedElementEvents = new ArrayList<PathwayObjectEvent>();
		o.addListener(this);
		data.add(o);
		l = new Interaction();
		data.add(l);
		received.clear();
		receivedElementEvents.clear();
	}

	/**
	 * test exporting of .svg
	 */
	public void testSvg() throws IOException, ConverterException {
		data.readFromXml(new File(PATHVISIO_BASEDIR, "testData/test.gpml"), false);
		assertTrue("Loaded a bunch of objects from xml", data.getPathwayObjects().size() > 20);
		File temp = File.createTempFile("data.test", ".svg");
		temp.deleteOnExit();

		// used to be: data.writeToSvg(temp);
		new BatikImageExporter(ImageExporter.TYPE_SVG).doExport(temp, data);

	}

	/**
	 * test exporting of .png
	 */
	public void testPng() throws IOException, ConverterException {
		data.readFromXml(new File(PATHVISIO_BASEDIR, "testData/test.gpml"), false);
		assertTrue("Loaded a bunch of objects from xml", data.getPathwayObjects().size() > 20);
		File temp = File.createTempFile("data.test", ".png");
		temp.deleteOnExit();

		BatikImageExporter exporter = new BatikImageExporter(BatikImageExporter.TYPE_PNG);
		exporter.doExport(temp, data);
	}

	/**
	 * test exporting of .png
	 */
	public void testPng2() throws IOException, ConverterException {
		data.readFromXml(new File(PATHVISIO_BASEDIR, "testData/test.gpml"), false);
		assertTrue("Loaded a bunch of objects from xml", data.getPathwayObjects().size() > 20);
		File temp = File.createTempFile("data.test", ".png");
		temp.deleteOnExit();

		RasterImageExporter exporter = new RasterImageExporter(BatikImageExporter.TYPE_PNG);
		exporter.doExport(temp, data);
	}

	/**
	 * test exporting of .pdf
	 */
	public void testPdf() throws IOException, ConverterException {
		data.readFromXml(new File(PATHVISIO_BASEDIR, "testData/test.gpml"), false);
		assertTrue("Loaded a bunch of objects from xml", data.getPathwayObjects().size() > 20);
		File temp = File.createTempFile("data.test", ".pdf");
		temp.deleteOnExit();

		BatikImageExporter exporter = new BatikImageExporter(BatikImageExporter.TYPE_PDF);
		exporter.doExport(temp, data);
	}

	/**
	 * test exporting of .txt
	 */
	public void testTxt() throws IOException, ConverterException {
		data.readFromXml(new File(PATHVISIO_BASEDIR, "testData/test.gpml"), false);
		assertTrue("Loaded a bunch of objects from xml", data.getPathwayObjects().size() > 20);
		File temp = File.createTempFile("data.test", ".txt");
		temp.deleteOnExit();

		DataNodeListExporter exporter = new DataNodeListExporter();
		exporter.doExport(temp, data);
	}

	/**
	 * test exporting of .pwf
	 */
	public void testPwf() throws IOException, ConverterException {
		data.readFromXml(new File(PATHVISIO_BASEDIR, "testData/test.gpml"), false);
		assertTrue("Loaded a bunch of objects from xml", data.getPathwayObjects().size() > 20);
		File temp = File.createTempFile("data.test", ".pwf");
		temp.deleteOnExit();

		EUGeneExporter exporter = new EUGeneExporter();
		exporter.doExport(temp, data);
	}

	/**
	 * Test that there is one and only one Pathway object
	 *
	 */
	public void testMappInfo() {
		Pathway mi;

		mi = data.getPathway();
		assertTrue(data.getPathwayObjects().contains(mi));
		assertNotNull(mi);

		// test that adding a new mappinfo object replaces the old one.
		Pathway mi2 = new Pathway();
		data.add(mi2);
		assertSame("MappInfo should be replaced", data.getPathway(), mi2);
		assertNotSame("Old MappInfo should be gone", data.getPathway(), mi);
		assertNull("Old MappInfo should not have a parent anymore", mi.getPathwayModel());
		assertSame("New MappInfo should now have a parent", mi2.getPathwayModel(), data);

		mi = data.getPathway();
		try {
			data.remove(mi);
			fail("Shouldn't be able to remove mappinfo object!");
		} catch (IllegalArgumentException e) {
		}
	}

	public void testValidator() throws IOException {
		File tmp = File.createTempFile("test", ".gpml");
		o.setCenterX(50.0);
		o.setCenterY(50.0);
		DefaultTemplates.setInitialSize(o);
		data.add(o);
//		o.setElementId(data.getUniqueElementId());
		Interaction o2 = new Interaction();
		o2.setStartLinePointX(10.0);
		o2.setStartLinePointY(10.0);
		DefaultTemplates.setInitialSize(o2);
		data.add(o2);
		Label o3 = new Label("");
		o3.setCenterX(100.0);
		o3.setCenterY(50);
		data.add(o3);
		PathwayElement mi;

		mi = data.getPathway();
		assertTrue("Mi shouldn't be null", mi != null);
		try {
			data.writeToXml(tmp, false);
		} catch (ConverterException e) {
			e.printStackTrace();
			fail("Exception while writing newly created pathway");
		}
	}

	@Override
	public void gmmlObjectModified(PathwayObjectEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pathwayModified(PathwayModelEvent e) {
		// TODO Auto-generated method stub

	}

}
