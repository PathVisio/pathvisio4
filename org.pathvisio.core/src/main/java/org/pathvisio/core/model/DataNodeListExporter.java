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

package org.pathvisio.core.model;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.core.data.GdbManager;
import org.pathvisio.io.PathwayModelExporter;
import org.pathvisio.model.ConverterException;
import org.pathvisio.model.DataNode;
import org.pathvisio.model.PathwayModel;

/**
 * Exporter that writes a pathway as a list of DataNodes, using their database
 * references
 * 
 * @author thomas
 */
public class DataNodeListExporter implements PathwayModelExporter {
	/**
	 * Use this String as argument in {@link #setResultCode(String)} to indicate
	 * that the exporter has to keep the original database code as used in the
	 * pathway
	 */
	public static final String DB_ORIGINAL = "original"; // Use the id/code as in database
	private DataSource resultDs = DataSource.getExistingBySystemCode(DB_ORIGINAL);
	private String multiRefSep = ", ";

	/**
	 * Set the separator used to separate multiple references for a single DataNode
	 * on the pathway. Default is ", ".
	 * 
	 * @param sep the String seperator to set.
	 */
	public void setMultiRefSep(String sep) {
		multiRefSep = sep;
	}

	/**
	 * Get the separator used to separate multiple references for a single DataNode
	 * on the pathway. Default is ", ".
	 * 
	 * @return multiRefSep the separator used to separate multiple references.
	 */
	public String getMultiRefSep() {
		return multiRefSep;
	}

	/**
	 * Set the database code to which every datanode reference will be mapped to in
	 * the output file.
	 * 
	 * NB: replace the deprecated setResultCode(). TODO
	 * 
	 * @see #DB_ORIGINAL
	 * @param value the data source.
	 */
	public void setResultDataSource(DataSource value) {
		resultDs = value;
	}

	/**
	 * Get the database code to which every datanode reference will be mapped to in
	 * the output file.
	 * 
	 * NB: replaced the deprecated getResultCode() TODO
	 */
	public DataSource getResultDataSource() {
		return resultDs;
	}

	/**
	 * Set the database code to which every datanode reference will be mapped to in
	 * the output file.
	 * 
	 * @see #DB_ORIGINAL
	 * @param code
	 * @deprecated use setResultDataSource();
	 */
	public void setResultCode(String code) {
		resultDs = DataSource.getExistingBySystemCode(code);
	}

	/**
	 * Get the database code to which every datanode reference will be mapped to in
	 * the output file.
	 * 
	 * @deprecated use getResultDataSouce()
	 */
	public String getResultCode() {
		return resultDs.getSystemCode();
	}

	/**
	 *
	 * @param file
	 * @param pathwayModel
	 */
	public void doExport(File file, PathwayModel pathwayModel) throws ConverterException {
		if (!DB_ORIGINAL.equals(getResultCode())) { // TODO
			// Check gene database connection
			if (gdbManager == null || !gdbManager.isConnected()) {
				throw new ConverterException("No gene database loaded");
			}
		}
		PrintStream out = null;
		try {
			out = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)));
		} catch (FileNotFoundException e) {
			throw new ConverterException(e);
		}
		printHeaders(out);
		for (DataNode elm : pathwayModel.getDataNodes()) { // TODO datanodes instead of elm
			String line = "";
			String id = elm.getXref().getId();
			DataSource ds = ((DataNode) elm).getXref().getDataSource();
			if (!checkString(id) || ds == null) {
				continue; // Skip empty id/codes
			}
			// Use the original id, if code is already the one asked for
			if (DB_ORIGINAL.equals(getResultCode()) || ds.equals(resultDs)) { // TODO
				line = id + "\t" + ds.getFullName();
			} else { // Lookup the cross-references for the wanted database code
				try {
					Set<Xref> refs = gdbManager.getCurrentGdb().mapID(elm.getXref(), resultDs);
					for (Xref ref : refs) {
						line += ref.getId() + multiRefSep;
					}
					if (line.length() > multiRefSep.length()) { // Remove the last ', '
						line = line.substring(0, line.length() - multiRefSep.length());
						line += "\t" + resultDs.getFullName();
					}
				} catch (IDMapperException ex) {
					throw new ConverterException(ex);
				}
			}
			out.println(line);
		}
		out.close();
	}

	/**
	 * Print the file headers.
	 * 
	 * @param out The output stream to print to
	 */
	protected void printHeaders(PrintStream out) {
		// print headers
		out.println("Identifier\tDatabase");
	}

	/**
	 * Returns true if given string is valid.
	 * 
	 * @param string the string to check.
	 * @return true is string is valid, not null with length greater than 0.
	 */
	private boolean checkString(String string) {
		return string != null && string.length() > 0;
	}

	public String[] getExtensions() {
		return new String[] { "txt" };
	}

	/**
	 *
	 */
	public String getName() {
		return "DataNode list";
	}

	private GdbManager gdbManager = null;

	/**
	 * Creates an exporter that uses the given GdbManager to lookup cross references
	 * for each datanode
	 * 
	 * @param gdbManager
	 */
	public DataNodeListExporter(GdbManager gdbManager) {
		this.gdbManager = gdbManager;
	}

	public DataNodeListExporter() {
	}

	@Override
	public List<String> getWarnings() {
		return Collections.emptyList();
	}

	/**
	 *
	 */
	@Override
	public void doExport(File file, PathwayModel pathway, int zoom) throws ConverterException {
		// TODO Auto-generated method stub

	}
}
