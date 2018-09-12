/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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

package com.actelion.research.spiritapp.ui.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.FocusListener;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JPanel;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.UIUtils;

/**
 * Simple Component to allow picking of dates using the FormatterUtils registered format
 *
 * @author Karim Mankour, Joel Freyss
 *
 */
public class DatePicker extends JPanel {

	private UtilDateModel model = new UtilDateModel();
	private JDatePanelImpl datePanel;
	private JDatePickerImpl datePicker;
	private String textWhenEmpty = null;

	protected static final Color LABEL_COLOR = new Color(120, 120, 160, 150);
	protected static final Color LABEL_COLOR_DISABLED = new Color(180, 180, 200, 150);

	public DatePicker() {
		super(new GridLayout(1,1));
		Properties p = new Properties();
		p.put("text.today", "Today");
		p.put("text.month", "Month");
		p.put("text.year", "Year");
		datePanel = new JDatePanelImpl(model, p);
		datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());
		datePicker.setOpaque(false);
		setToolTipText(FormatterUtils.getLocaleFormat().getLocaleDateFormat());
		datePicker.getJFormattedTextField().setEditable(true);
		datePicker.getJFormattedTextField().setColumns(11);
		setOpaque(false);
		add(datePicker);
	}

	@Override
	public void setToolTipText(String text) {
		datePicker.getJFormattedTextField().setToolTipText(text);
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		for (Component c : getComponents()) {
			setEnabled(c, enabled);
		}
	}

	@Override
	public synchronized void addFocusListener(FocusListener l) {
		super.addFocusListener( l);
		for (Component c : getComponents()) {
			addFocusListener(c, l);
		}
	}

	private void addFocusListener(Component comp, FocusListener l) {
		comp.addFocusListener(l);
		if(comp instanceof Container) {
			for (Component c : ((Container)comp).getComponents()) {
				addFocusListener(c, l);
			}
		}
	}


	private void setEnabled(Component comp, boolean enabled) {
		comp.setEnabled(enabled);
		if(comp instanceof Container) {
			for (Component c : ((Container)comp).getComponents()) {
				setEnabled(c, enabled);
			}
		}
	}

	public void setText(String text) {
		model.setValue(FormatterUtils.parseDateTime(text));
		datePicker.getJFormattedTextField().setText(text);
	}

	public String getText() {
		return datePicker.getJFormattedTextField().getText();
	}

	public class DateLabelFormatter extends AbstractFormatter {

		@Override
		public Object stringToValue(String text) throws ParseException {
			Date date = FormatterUtils.parseDateTime(text);
			if(date==null) return null;
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			return cal;
		}

		@Override
		public String valueToString(Object value) throws ParseException {
			if (value != null && (value instanceof Calendar)) {
				return FormatterUtils.formatDate(((Calendar) value).getTime());
			}
			return "";
		}

	}

	public void setTextWhenEmpty(String textWhenEmpty) {
		this.textWhenEmpty = textWhenEmpty;
	}

	public String getTextWhenEmpty() {
		return textWhenEmpty;
	}

	@Override
	public void paint(Graphics g) {
		UIUtils.applyDesktopProperties(g);

		super.paint(g);


		//Paint Icon
		int textX = 5;
		//Paint textWhenEmpty
		if(getText().length()==0 && textWhenEmpty!=null) {
			g.setColor(isEnabled()? LABEL_COLOR: LABEL_COLOR_DISABLED);
			g.setFont(datePicker.getJFormattedTextField().getFont());
			Shape clip = g.getClip();
			Rectangle rect = datePicker.getJFormattedTextField().getBounds();
			g.setClip(rect);
			g.drawString(textWhenEmpty, textX, getHeight()/2+5);
			g.setClip(clip);
		}
	}
}
