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
package org.pathvisio.core.util;

import java.io.File;

import org.bridgedb.DataSource;
import org.bridgedb.bio.DataSourceTxt;
import org.pathvisio.core.Engine;
import org.pathvisio.debug.Logger;
import org.pathvisio.core.model.BatikImageExporter;
import org.pathvisio.io.ConverterException;
import org.pathvisio.core.model.DataNodeListExporter;
import org.pathvisio.core.model.EUGeneExporter;
import org.pathvisio.io.GpmlFormat;
import org.pathvisio.core.model.ImageExporter;
import org.pathvisio.model.PathwayModel;
import org.pathvisio.event.PathwayModelExporter;
import org.pathvisio.event.PathwayModelImporter;
import org.pathvisio.core.model.RasterImageExporter;
import org.pathvisio.core.preferences.GlobalPreference;
import org.pathvisio.core.preferences.PreferenceManager;

/**
 * Converter.java
 * 
 * Command Line GenMAPP to GPML Converter. Converts to images. Move to separate Converter Jar File. 
 * 
 * Created on 15 augustus 2005, 20:28
 * 
 * @author Thomas Kelder (t.a.j.kelder@student.tue.nl)
 */
public class Converter {

	public static void printUsage() {
		System.out.println("GPML Converter\n" + "Usage:\n"
				+ "\tjava Converter <input filename> [<output filename>] [<zoom>] \n" + "\n"
				+ "Converts between GPML format and several other formats:\n"
				+ "\t- GPML (.gpml/.xml) <-> GenMAPP (.mapp)\n" + "\t- GPML (.gpml/.xml) -> SVG (.svg)\n"
				+ "\t- GPML (.gpml/.xml) -> PNG (.png); Zoom value at 100 by default, can be changed for PNG\n"
				+ "\t- GPML (.gpml/.xml) -> TIFF (.tiff)\n" + "\t- GPML (.gpml/.xml) -> PDF (.pdf)\n"
				+ "The conversion direction is determined from the extension of the input file.\n" + "Return codes:\n"
				+ "\t 0: OK\n" + "\t-1: Parameter or file error\n" + "\t-2: Conversion error\n");
	}

	/**
	 * Command line arguments:
	 *
	 */
	public static void main(String[] args) {
		// Handle command line arguments
		// Check for custom output path
		Logger.log.setStream(System.err);
		// debug, trace, info, warn, error, fatal
		Logger.log.setLogLevel(false, false, true, true, true, true);

		DataSourceTxt.init();
		PreferenceManager.init();
		Engine engine = new Engine();
		engine.addPathwayModelImporter(new GpmlFormat());
//    	engine.addPathwayImporter(new MappFormat()); TODO
//		engine.addPathwayExporter(new MappFormat()); TODO 
		engine.addPathwayModelExporter(new GpmlFormat());
		engine.addPathwayModelExporter(new BatikImageExporter(ImageExporter.TYPE_SVG));
		engine.addPathwayModelExporter(new RasterImageExporter(ImageExporter.TYPE_PNG));
		engine.addPathwayModelExporter(new BatikImageExporter(ImageExporter.TYPE_TIFF));
		engine.addPathwayModelExporter(new BatikImageExporter(ImageExporter.TYPE_PDF));
		engine.addPathwayModelExporter(new EUGeneExporter());
//		engine.addPathwayExporter(new DataNodeListExporter());

		// Transient dependency on Biopax converter
		try {
			Class<?> c = Class.forName("org.pathvisio.biopax3.BiopaxFormat");
			Object o = c.newInstance();
			engine.addPathwayModelExporter((PathwayModelExporter) o);
			engine.addPathwayModelImporter((PathwayModelImporter) o);
		} catch (ClassNotFoundException ex) {
			Logger.log.warn("BioPAX converter not in classpath, BioPAX conversion not available today.");
		} catch (InstantiationException e) {
			Logger.log.error("BioPAX instantiation error", e);
		} catch (IllegalAccessException e) {
			Logger.log.warn("Access to BioPAX class is Illegal", e);
		}

		// Enable MiM support (for export to graphics formats)
		PreferenceManager.getCurrent().setBoolean(GlobalPreference.MIM_SUPPORT, true);
//		MIMShapes.registerShapes(); TODO 

		File inputFile = null;
		File outputFile = null;

		int zoom = 100;

		boolean error = false;
		if (args.length == 0) {
			Logger.log.error("Need at least one command line argument");
			error = true;
		} else if (args.length > 3) {
			Logger.log.error("Too many arguments");
			error = true;
		} else {
			inputFile = new File(args[0]);
			outputFile = new File(args[1]);

			System.out.println(inputFile.exists());

			if (inputFile == null || !inputFile.canRead()) {
				Logger.log.error("Unable to read inputfile: " + inputFile);
				error = true;
			}
		}

		if (!error) {
			try {
				engine.importPathwayModel(inputFile);
				PathwayModel pathway = engine.getActivePathwayModel();
				if (args.length == 2)
					engine.exportPathwayModel(outputFile, pathway);
				if (args.length == 3) {
					zoom = Integer.parseInt(args[2]);
					engine.exportPathwayModel(outputFile, pathway, zoom);
				}
			} catch (ConverterException e) {
				e.printStackTrace();
				System.exit(-2);
			}
		} else {
			printUsage();
			System.exit(-1);
		}
		System.exit(0); // Everything OK, now force exit
	}
}
