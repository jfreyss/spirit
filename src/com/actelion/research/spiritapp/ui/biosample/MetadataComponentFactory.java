/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2016 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16,
 * CH-4123 Allschwil, Switzerland.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * @author Joel Freyss
 */

package com.actelion.research.spiritapp.ui.biosample;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JLabel;

import com.actelion.research.spiritapp.ui.util.component.DocumentTextField;
import com.actelion.research.spiritapp.ui.util.component.DocumentZipTextField;
import com.actelion.research.spiritapp.ui.util.lf.LF;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.DateTextField;
import com.actelion.research.util.ui.JComboCheckBox;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JTextComboBox;
import com.actelion.research.util.ui.TextChangeListener;
import com.actelion.research.util.ui.exceltable.JLargeTextField;

public class MetadataComponentFactory {

	/**
	 * @param metadataType
	 * @param metadata - if not null, prepopulate the component
	 * @param shortVersion
	 * @return a Jcomponent which implements MetadataComponent
	 */
	public static JComponent getComponentFor(final BiotypeMetadata metadataType) {
		if(metadataType.getDataType()==DataType.AUTO) {
			JComponent res = new AutoCompleteComponent(metadataType.getName()) {
				@Override
				public Collection<String> getChoices() {
					return DAOBiotype.getAutoCompletionFields(metadataType, null);
				}
			};
			if(metadataType.isRequired()) {
				res.setBackground(LF.BGCOLOR_REQUIRED);
			}
			return res;
		}
		return getComponentFor(metadataType.getDataType(), metadataType.getParameters(), metadataType.isRequired());
	}

	public static JComponent getComponentFor(final DataType datatype, String parameters, boolean required) {
		JComponent res = null;
		switch (datatype) {
		case ALPHA:
			res = new AlphaNumericalComponent();
			break;
		case NUMBER:
			res = new NumericalComponent();
			break;
		case LIST:
			res = new ComboComponent(parameters);
			break;
		case DATE:
			res =new DateStringComponent(true);
			break;
		case MULTI:
			res = new MultiComponent(parameters);
			break;
		case D_FILE:
			res = new FileComponent();
			break;
		case FILES:
			res = new ZipComponent();
			break;
		case LARGE:
			res = new LargeComponent();
			break;
		case BIOSAMPLE:
			res = new BiosampleComponent(parameters);
			break;
		case FORMULA:
			res = new FormulaComponent(parameters);
			break;
		default:
			res = new JLabel("Not editable: [" + datatype+"]");
		}
		if(required) {
			res.setBackground(LF.BGCOLOR_REQUIRED);
		}
		return res;
	}

	public static class AlphaNumericalComponent extends JCustomTextField implements MetadataComponent {
		public AlphaNumericalComponent() {
			super(CustomFieldType.ALPHANUMERIC);
			setMaxChars(255);
			setColumns(28);
		}
		@Override
		public String getData() {
			return getText();
		}
		@Override
		public void setData(String val) {
			if(val==null) setText("");
			else setText(val);
		}
		@Override
		public void updateModel(Biosample b, BiotypeMetadata m) {
			if(b!=null && m!=null) b.setMetadataValue(m, getData());
		}
		@Override
		public void updateView(Biosample b, BiotypeMetadata m) {
			setData(b==null || m==null? null: b.getMetadataValue(m));
		}
		@Override
		public void addTextChangeListener(TextChangeListener listener) {
			super.addTextChangeListener(listener);
		}
	}

	public static class BiosampleComponent extends SampleIdBrowser implements MetadataComponent {
		public BiosampleComponent(String parameters) {
			super();
			setColumns(28);
			if(parameters!=null && parameters.length()>0) {
				try {
					Biotype biotype = DAOBiotype.getBiotype(parameters);
					setBiotype(biotype);
				} catch(Exception e) {
					e.printStackTrace();
					setBiotype(null);
				}
			}
		}
		@Override
		public String getData() {
			return getText();
		}
		@Override
		public void setData(String metadata) {
			new IllegalArgumentException("Not supported");
		}
		@Override
		public void updateModel(Biosample b, BiotypeMetadata m) {
			if(b!=null && m!=null) b.setMetadataBiosample(m, getBiosample());

		}
		@Override
		public void updateView(Biosample b, BiotypeMetadata m) {
			setBiosample(b==null || m==null? null: b.getMetadataBiosample(m)==null? new Biosample(b.getMetadataValue(m)) :b.getMetadataBiosample(m));
		}
		@Override
		public void addTextChangeListener(TextChangeListener listener) {
			super.addTextChangeListener(listener);
		}
	}

	public static class ZipComponent extends DocumentZipTextField implements MetadataComponent {
		public ZipComponent() {
			super();
		}
		@Override
		public String getData() {
			return getText();
		}
		@Override
		public void setData(String val) {
			throw new IllegalArgumentException("Not supported");
		}
		@Override
		public void updateModel(Biosample b, BiotypeMetadata m) {
			if(b!=null && m!=null) b.setMetadataDocument(m, getSelectedDocument());
		}
		@Override
		public void updateView(Biosample b, BiotypeMetadata m) {
			setSelectedDocument(b==null || m==null? null: b.getMetadataDocument(m));
		}
		@Override
		public void addTextChangeListener(TextChangeListener listener) {
			super.addTextChangeListener(listener);
		}
	}

	public static class FileComponent extends DocumentTextField implements MetadataComponent {
		public FileComponent() {
			super();
		}
		@Override
		public String getData() {
			return getText();
		}
		@Override
		public void setData(String val) {
			throw new IllegalArgumentException("Not supported");
		}
		@Override
		public void updateModel(Biosample b, BiotypeMetadata m) {
			if(b!=null && m!=null) b.setMetadataDocument(m, getSelectedDocument());
		}
		@Override
		public void updateView(Biosample b, BiotypeMetadata m) {
			setSelectedDocument(b==null || m==null? null: b.getMetadataDocument(m));
		}
		@Override
		public void addTextChangeListener(TextChangeListener listener) {
			super.addTextChangeListener(listener);
		}
	}

	public static class LargeComponent extends JLargeTextField implements MetadataComponent {
		public LargeComponent() {
			super();
		}
		@Override
		public String getData() {
			return getText();
		}
		@Override
		public void setData(String val) {
			setText(val);
		}
		@Override
		public void updateModel(Biosample b, BiotypeMetadata m) {
			if(getText().length()==0) {
				b.setMetadataValue(m, null);
			} else {
				b.setMetadataValue(m, getText());
			}
		}
		@Override
		public void updateView(Biosample b, BiotypeMetadata m) {
			setData(b.getMetadataValue(m));
		}
		@Override
		public void addTextChangeListener(TextChangeListener listener) {
			//ignore
		}
	}

	public static class MultiComponent extends JComboCheckBox implements MetadataComponent {
		public MultiComponent(String parameters) {
			super(Arrays.asList(MiscUtils.split(parameters)));
			setColumns(28);
			setAllowTyping(false);
		}

		@Override
		public String getData() {
			return getText();
		}

		@Override
		public void setData(String metadata) {
			setText(metadata);
		}
		@Override
		public void updateModel(Biosample b, BiotypeMetadata m) {
			if(b!=null && m!=null) b.setMetadataValue(m, getData());
		}
		@Override
		public void updateView(Biosample b, BiotypeMetadata m) {
			setData(b==null || m==null? null: b.getMetadataValue(m));
		}
		@Override
		public void addTextChangeListener(TextChangeListener listener) {
			super.addTextChangeListener(listener);
		}
	}

	public static class ComboComponent extends MetadataComboBox implements MetadataComponent {
		public ComboComponent(String parameters) {
			super();
			if(parameters==null) parameters = "";
			String[] split = MiscUtils.split(parameters, ",");
			String[] choices = new String[split.length+1];
			choices[0] = "";
			System.arraycopy(split, 0, choices, 1, split.length);
			setModel(new DefaultComboBoxModel<String>(choices));
		}
		@Override
		public String getData() {
			return (String)getSelectedItem();
		}
		@Override
		public void setData(String val) {
			if(val==null) {
				setSelectedItem("");
			} else {
				setSelectedItem(val);
			}
		}
		@Override
		public void updateModel(Biosample b, BiotypeMetadata m) {
			if(b!=null && m!=null) b.setMetadataValue(m, getData());
		}
		@Override
		public void updateView(Biosample b, BiotypeMetadata m) {
			setData(b==null || m==null? null: b.getMetadataValue(m));
		}
		@Override
		public void addTextChangeListener(final TextChangeListener listener) {
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					listener.textChanged(ComboComponent.this);
				}
			});
		}
	}

	public static class AutoCompleteComponent extends JTextComboBox implements MetadataComponent {
		public AutoCompleteComponent(final String name) {
			super();
			setEditable(true);
			setColumns(28);
		}
		@Override
		public String getData() {
			return getText();
		}
		@Override
		public void setData(String val) {
			if(val==null) {
				setText("");
			} else {
				setText(val);
			}
		}
		@Override
		public void updateModel(Biosample b, BiotypeMetadata m) {
			if(b!=null && m!=null) b.setMetadataValue(m, getData());
		}
		@Override
		public void updateView(Biosample b, BiotypeMetadata m) {
			setData(b==null || m==null? null: b.getMetadataValue(m));
		}
		@Override
		public void addTextChangeListener(TextChangeListener listener) {
			super.addTextChangeListener(listener);
		}
	}

	public static class NumericalComponent extends JCustomTextField implements MetadataComponent {
		public NumericalComponent() {
			super(CustomFieldType.DOUBLE, 4);
		}
		@Override
		public String getData() {
			return getText();
		}
		@Override
		public void setData(String val) {
			if(val==null) {
				setText("");
			} else {
				setText(val);
			}
		}
		@Override
		public void updateModel(Biosample b, BiotypeMetadata m) {
			if(b!=null && m!=null) b.setMetadataValue(m, getData());
		}
		@Override
		public void updateView(Biosample b, BiotypeMetadata m) {
			setData(b==null || m==null? null: b.getMetadataValue(m));
		}
		@Override
		public void addTextChangeListener(TextChangeListener listener) {
			super.addTextChangeListener(listener);
		}
	}

	public static class FormulaComponent extends NumericalComponent implements MetadataComponent {
		public FormulaComponent(String parameters) {
			setToolTipText(parameters);
			setEditable(false);
			setForeground(Color.BLUE);
		}
	}

	public static class DateStringComponent extends DateTextField implements MetadataComponent {
		public DateStringComponent(boolean showToday) {
			super(showToday);
		}
		@Override
		public String getData() {
			return getText();
		}
		@Override
		public void setData(String val) {setText(val);}
		@Override
		public void updateModel(Biosample b, BiotypeMetadata m) {
			if(b!=null && m!=null) b.setMetadataValue(m, getData());
		}
		@Override
		public void updateView(Biosample b, BiotypeMetadata m) {
			setData(b==null || m==null? null: b.getMetadataValue(m));
		}
		@Override
		public void addTextChangeListener(TextChangeListener listener) {
			super.addTextChangeListener(listener);
		}
	}
}
