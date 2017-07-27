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

package com.actelion.research.util.ui;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import com.actelion.research.util.FormatterUtils;


public class JCustomTextField extends JTextField {

	/**
	 * PROPERTY_TEXTCHANGED is fired whenever the user selected an item on the list, typed enter, or changed the data AND this data has been changed
	 * an eventaction is fired even if the data has not been changed
	 */
	public static final String PROPERTY_TEXTCHANGED = "text_changed";
	public static final int ALPHANUMERIC = 1;
	public static final int DOUBLE = 2;
	public static final int INTEGER = 3;
	public static final int DATE = 4;

	protected static final Color LABEL_COLOR = new Color(120, 120, 160, 180);
	protected static final Color LABEL_COLOR_DISABLED = new Color(180, 180, 200, 180);

	private Icon icon = null;
	private int maxChars;
	private String textWhenEmpty;
	private boolean warningWhenEdited;
	private String previous = "";

	private List<TextChangeListener> textChangeListeners = new ArrayList<>();


	protected class MyCustomDocument extends PlainDocument {
		public MyCustomDocument() {
			super();
		}
		@Override
		public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
			if(str==null) return;

			if (maxChars<=0 || (getLength() + str.length()) <= maxChars) {
				super.insertString(offs, str, a);
			}
		}
	}

	public JCustomTextField() {
		this(ALPHANUMERIC);
	}

	public JCustomTextField(int type) {
		setType(type);
		init();
		if(type==DOUBLE || type==INTEGER) setMaxChars(10);
	}

	public JCustomTextField(int columns, String initial, String textWhenEmpty) {
		this(ALPHANUMERIC, initial, columns);
		setTextWhenEmpty(textWhenEmpty);
	}

	public JCustomTextField(int type, int columns) {
		this(type);
		setType(type);
		setColumns(columns);
		init();
	}

	public JCustomTextField(int type, String initial) {
		super();
		setType(type);
		init();
		super.setText(initial);
	}

	public JCustomTextField(int type, String initial, int columns) {
		this(type, initial);
		setColumns(columns);
	}

	public void setIcon(Icon icon) {
		Insets insets = getMargin();
		if(insets==null) insets = new Insets(0, 0, 0, 12);
		setMargin(new Insets(insets.top, insets.left - (this.icon==null?0: this.icon.getIconWidth()) + (icon==null?0: icon.getIconWidth()), insets.bottom, insets.right));
		this.icon = icon;
		repaint();
	}

	public Icon getIcon() {
		return icon;
	}

	public void setType(int type) {
		setHorizontalAlignment(type==DOUBLE || type==INTEGER? JTextField.RIGHT: JTextField.LEFT);
		setColumns(type==INTEGER? 3: type==DOUBLE? 5: type==DATE? 8: 14);
	}

	@Override
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	@Override
	public void setPreferredSize(Dimension preferredSize) {
		setColumns(0);
		super.setPreferredSize(preferredSize);
	}

	public Integer getTextInt() {
		try {
			return Integer.parseInt(getText());
		} catch (Exception e) {
			return null;
		}
	}

	public Double getTextDouble() {
		try {
			return Double.valueOf(getText());
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Returns the date, parsed according to the format set up in FormatterUtils
	 * @return
	 * @throws Exception if the text is not empty and the date is not well formatted
	 */
	public Date getTextDate() throws Exception {
		Date parsed = FormatterUtils.parseDate(getText());
		if(parsed==null && getText().length()>0) throw new Exception("The date not well formatted");
		return parsed;
	}

	public void setTextInteger(Integer v) {
		if(v==null) {
			setText("");
		} else {
			setText("" + v);
		}
	}

	public void setTextDouble(Double v) {
		if(v==null) {
			setText("");
		} else if( (int) (double) v == v) {
			setText("" + (int) (double) v);
		} else {
			setText("" + v);
		}
	}

	public void setTextDate(Date d) {
		setText(FormatterUtils.formatDate(d));
	}

	@Override
	public void setText(String t) {
		super.setText(t);
		fireTextChanged();

		if(warningWhenEdited) {
			setEditable(getText().length()==0);
		}
	}

	private void init() {
		setDocument(new MyCustomDocument());

		addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				setText(getText().trim());
				fireTextChanged();
			}
		});

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(isEnabled() && warningWhenEdited && !isEditable()) {
					int res = JOptionPane.showConfirmDialog(JCustomTextField.this, "Would you like to re-enter a value?", "Reedit Value", JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION);
					if(res!=JOptionPane.YES_OPTION) return;

					setEditable(true);
					selectAll();
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							requestFocusInWindow();
						}
					});

				}

			}
		});

		addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JCustomTextField.this.fireTextChanged();
			}
		});
	}

	/**
	 * @param maxChars the maxChars to set
	 */
	public void setMaxChars(int maxChars) {
		this.maxChars = maxChars;
	}

	/**
	 * @return the maxChars
	 */
	public int getMaxChars() {
		return maxChars;
	}

	public void setTextWhenEmpty(String textWhenEmpty) {
		this.textWhenEmpty = textWhenEmpty;
	}

	public String getTextWhenEmpty() {
		return textWhenEmpty;
	}

	@Override
	protected void paintComponent(Graphics g) {
		UIUtils.applyDesktopProperties(g);

		super.paintComponent(g);


		//Paint Icon
		int textX = 5;
		if(this.icon!=null){
			int iconWidth = icon.getIconWidth();
			int iconHeight = icon.getIconHeight();
			textX = textX + iconWidth + 2;
			int y = (this.getHeight() - iconHeight)/2;
			this.icon.paintIcon(this, g, 2, y);
		}

		//Paint textWhenEmpty
		if(getText().length()==0 && textWhenEmpty!=null) {
			g.setColor(isEnabled()? LABEL_COLOR: LABEL_COLOR_DISABLED);
			g.setFont(getFont());
			Shape clip = g.getClip();
			g.setClip(new Rectangle(textX, 2, getWidth()-textX-16, getHeight()-4));
			if(getHorizontalAlignment()==SwingConstants.RIGHT) {
				g.drawString(textWhenEmpty, getWidth() - g.getFontMetrics().stringWidth(textWhenEmpty)- 5, getHeight()/2+5);
			} else {
				g.drawString(textWhenEmpty, textX, getHeight()/2+5);
			}
			g.setClip(clip);
		}
	}

	public void setWarningWhenEdited(boolean v) {
		warningWhenEdited = v;
		if(warningWhenEdited) {
			setEditable(getText().length()==0);
		}
	}

	public void addTextChangeListener(TextChangeListener listener) {
		textChangeListeners.add(listener);
	}

	/**
	 * Called when the user updated the data
	 * @param newValue
	 */
	public void fireTextChanged() {
		String s = getText();
		if(!s.equals(previous)) {
			previous = s;

			for (TextChangeListener listener : textChangeListeners) {
				listener.textChanged(this);
			}

			firePropertyChange(PROPERTY_TEXTCHANGED, null, getText());
		}
	}

	public void setBorderColor(Color color) {
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2,2,2,2), BorderFactory.createLineBorder(color)), BorderFactory.createEmptyBorder(2,2,2,2)));
	}

}
