package org.geogebra.desktop.gui.dialog;

import org.geogebra.common.euclidian.EuclidianController;
import org.geogebra.common.gui.InputHandler;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoPolygon;
import org.geogebra.common.kernel.kernelND.GeoPointND;
import org.geogebra.common.main.DialogManager;
import org.geogebra.desktop.main.AppD;

public class InputDialogRotatePoint extends InputDialogRotate {

	GeoPointND[] points;

	public InputDialogRotatePoint(AppD app, String title, InputHandler handler,
			GeoPolygon[] polys, GeoPointND[] points, GeoElement[] selGeos,
			EuclidianController ec) {

		super(app, title, handler, polys, selGeos, ec);

		this.points = points;

	}

	protected boolean processInput() {

		String defaultRotateAngle1 = DialogManager.rotateObject(app,
				inputPanel.getText(), rbClockWise.isSelected(), polys, points,
				selGeos, ec);
		if (defaultRotateAngle1 != null) {
			defaultRotateAngle = defaultRotateAngle1;
		}

		return defaultRotateAngle1 != null;
		/*
		 * 
		 * // avoid labeling of num Construction cons =
		 * kernel.getConstruction(); boolean oldVal =
		 * cons.isSuppressLabelsActive(); cons.setSuppressLabelCreation(true);
		 * 
		 * inputText = inputPanel.getText();
		 * 
		 * // negative orientation ? if (rbClockWise.isSelected()) { inputText =
		 * "-(" + inputText + ")"; }
		 * 
		 * boolean success = inputHandler.processInput(inputText);
		 * 
		 * cons.setSuppressLabelCreation(oldVal);
		 * 
		 * if (success) { // GeoElement circle = kernel.Circle(null, geoPoint1,
		 * // ((NumberInputHandler)inputHandler).getNum()); NumberValue num =
		 * ((NumberInputHandler) inputHandler).getNum(); //
		 * geogebra.gui.AngleInputDialog dialog = //
		 * (geogebra.gui.AngleInputDialog) ob[1]; String angleText = getText();
		 * 
		 * // keep angle entered if it ends with 'degrees' if
		 * (angleText.endsWith("\u00b0")) defaultRotateAngle = angleText; else
		 * defaultRotateAngle = "45" + "\u00b0";
		 * 
		 * if (polys.length == 1) {
		 * 
		 * GeoElement[] geos = kernel.Rotate(null, polys[0], num, points[0]); if
		 * (geos != null) { app.storeUndoInfo();
		 * kernel.getApplication().getActiveEuclidianView()
		 * .getEuclidianController() .memorizeJustCreatedGeos(geos); } return
		 * true; } ArrayList<GeoElement> ret = new ArrayList<GeoElement>(); for
		 * (int i = 0; i < selGeos.length; i++) { if (selGeos[i] != geoPoint1) {
		 * if (selGeos[i] instanceof Transformable) {
		 * ret.addAll(Arrays.asList(kernel.Rotate(null, selGeos[i], num,
		 * geoPoint1))); } else if (selGeos[i].isGeoPolygon()) {
		 * ret.addAll(Arrays.asList(kernel.Rotate(null, selGeos[i], num,
		 * geoPoint1))); } } } if (!ret.isEmpty()) { app.storeUndoInfo();
		 * kernel.getApplication().getActiveEuclidianView()
		 * .getEuclidianController().memorizeJustCreatedGeos(ret); } return
		 * true; }
		 * 
		 * return false;
		 */
	}

}
