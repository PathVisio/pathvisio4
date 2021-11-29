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
package org.pathvisio.gui.dialogs;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapperException;
import org.bridgedb.IDMapperStack;
import org.bridgedb.Xref;
import org.pathvisio.model.type.ArrowHeadType;
import org.pathvisio.model.DataNode;
import org.pathvisio.model.Interaction;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.gui.DataSourceModel;
import org.pathvisio.gui.SwingEngine;
import org.pathvisio.gui.completer.CompleterQueryTextField;
import org.pathvisio.gui.completer.OptionProvider;
import org.pathvisio.gui.util.PermissiveComboBox;

/**
 * 
 * @author unknown
 */
public class LineDialog extends PathwayObjectDialog implements ItemListener {

	/**
	 * Dialog for editing Reactions/ Interactions. In addition to the standard
	 * comments and literature tabs, this has a tab for looking up accession numbers
	 * of reactions/interactions.
	 */
	private static final long serialVersionUID = 1L;
	private CompleterQueryTextField idText;

	private PermissiveComboBox dbCombo;
	private PermissiveComboBox typeCombo;
	private DataSourceModel dsm;

	protected LineDialog(final SwingEngine swingEngine, final Interaction e, final boolean readonly, final Frame frame,
			final Component locationComp) {
		super(swingEngine, e, readonly, frame, "Interaction properties", locationComp);
		getRootPane().setDefaultButton(null);
		setButton.requestFocus();
	}

	/**
	 * Get the pathway element for this dialog
	 */
	protected Interaction getInput() {
		return (Interaction) super.getInput();
	}

	public final void refresh() {
		super.refresh();
		Interaction input = getInput();
		idText.setText(input.getXref().getId());
		dsm.setSelectedItem(input.getXref().getDataSource());
		String lType = getInput().getEndLineType().toString();
		typeCombo.setSelectedItem(ArrowHeadType.fromName(lType));
		dsm.setInteractionFilter(true);
		pack();
	}

	protected final void addCustomTabs(final JTabbedPane parent) {

		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());

		JPanel fieldPanel = new JPanel();

		fieldPanel.setBorder(BorderFactory.createTitledBorder("Manual entry"));
		GridBagConstraints panelConstraints = new GridBagConstraints();
		panelConstraints.fill = GridBagConstraints.BOTH;
		panelConstraints.gridx = 0;
		panelConstraints.weightx = 1;
		panelConstraints.weighty = 1;
		panelConstraints.insets = new Insets(2, 2, 2, 2);
		panelConstraints.gridy = GridBagConstraints.RELATIVE;

		panel.add(fieldPanel, panelConstraints);

		GridBagConstraints searchConstraints = new GridBagConstraints();
		searchConstraints.gridx = GridBagConstraints.RELATIVE;
		searchConstraints.fill = GridBagConstraints.HORIZONTAL;

		// Manual entry panel elements
		fieldPanel.setLayout(new GridBagLayout());

		JLabel typeLabel = new JLabel("Biological Type");
		JLabel idLabel = new JLabel("Identifier");
		JLabel dbLabel = new JLabel("Database");

		idText = new CompleterQueryTextField(new OptionProvider() {
			public List<String> provideOptions(final String text) {
				if (text == null) {
					return Collections.emptyList();
				}

				IDMapperStack gdb = swingEngine.getGdbManager().getCurrentGdb();
				Set<Xref> refs = new HashSet<Xref>();
				try {
					refs = gdb.freeSearch(text, 100);
				} catch (IDMapperException ignore) {
				}

				// Only take identifiers
				List<String> ids = new ArrayList<String>();
				for (Xref ref : refs)
					ids.add(ref.getId());
				return ids;
			}
		}, true);

		idText.setCorrectCase(false);

		dsm = new DataSourceModel();
		dsm.setPrimaryFilter(true);
		dsm.setSpeciesFilter(swingEngine.getCurrentOrganism());
		dbCombo = new PermissiveComboBox(dsm);
		typeCombo = new PermissiveComboBox((ComboBoxModel) ArrowHeadType.getValues()); // TODO

		GridBagConstraints c = new GridBagConstraints();
		c.ipadx = c.ipady = 5;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.weightx = 0;
		c.gridx = 0;
		c.gridy = GridBagConstraints.RELATIVE;
		fieldPanel.add(typeLabel, c);
		fieldPanel.add(idLabel, c);
		fieldPanel.add(dbLabel, c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;

		fieldPanel.add(typeCombo, c);
		fieldPanel.add(idText, c);
		fieldPanel.add(dbCombo, c);

		idText.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(final DocumentEvent e) {
				setText();
			}

			public void insertUpdate(final DocumentEvent e) {
				setText();
			}

			public void removeUpdate(final DocumentEvent e) {
				setText();
			}

			private void setText() {
				// Sets Xref id by creating new Xref
				getInput().setXref(new Xref(idText.getText(), getInput().getXref().getDataSource()));
			}
		});

		dsm.addListDataListener(new ListDataListener() {

			public void contentsChanged(final ListDataEvent arg0) {
				// Sets Xref dataSource by creating new Xref
				getInput().setXref(new Xref(getInput().getXref().getId(), (DataSource) dsm.getSelectedItem()));
			}

			public void intervalAdded(final ListDataEvent arg0) {
			}

			public void intervalRemoved(final ListDataEvent arg0) {
			}
		});

		typeCombo.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				ArrowHeadType item = (ArrowHeadType) typeCombo.getSelectedItem();
				getInput().setEndLineType(item);
				refresh();
			}
		});

		idText.setEnabled(!readonly);
		dbCombo.setEnabled(!readonly);
		typeCombo.setEnabled(!readonly);

		parent.add("Annotation", panel);
		parent.setSelectedComponent(panel);
	}

	@Override
	public void itemStateChanged(final ItemEvent arg0) {
		// TODO Auto-generated method stub
	}
}
