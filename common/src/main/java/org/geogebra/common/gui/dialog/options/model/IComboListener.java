package org.geogebra.common.gui.dialog.options.model;


public interface IComboListener extends PropertyListener {
	void setSelectedIndex(int index);
	void addItem(final String item);
	void setSelectedItem(String item);

	Object updatePanel(Object[] geos);
}
