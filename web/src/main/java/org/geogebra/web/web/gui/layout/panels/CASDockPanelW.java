package org.geogebra.web.web.gui.layout.panels;

import org.geogebra.common.cas.view.CASView;
import org.geogebra.common.main.App;
import org.geogebra.web.html5.gui.view.algebra.MathKeyboardListener;
import org.geogebra.web.html5.main.AppW;
import org.geogebra.web.web.cas.view.CASViewW;
import org.geogebra.web.web.gui.GuiManagerW;
import org.geogebra.web.web.gui.app.VerticalPanelSmart;
import org.geogebra.web.web.gui.layout.DockPanelW;

import com.google.gwt.resources.client.ResourcePrototype;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Top level GUI for the CAS view
 *
 */
public class CASDockPanelW extends DockPanelW {

	SimpleLayoutPanel toplevel;

	VerticalPanelSmart ancestor;
	CASViewW sview;

	public CASDockPanelW(App appl) {
		super(App.VIEW_CAS, // view id
				"CAS", // view title phrase
				getDefaultToolbar(), // toolbar string
				true, // style bar?
				4, // menu order
				'K' // ctrl-shift-K
		);

		// initWidget(toplevel = new SimpleLayoutPanel());
		// ancestor = new VerticalPanelSmart();
		// toplevel.add(ancestor);

		app = (AppW) appl;
	}

	protected Widget loadComponent() {
		setViewImage(getResources().styleBar_CASView());
		sview = (CASViewW) app.getGuiManager().getCasView();

		sview.maybeOpenKeyboard(true);


		return sview.getComponent();
	}

	public void onResize() {
		super.onResize();
		if (sview != null) {

			int width = getComponentInteriorWidth();
			int height = getComponentInteriorHeight();

			// <= is needed because otherwise the width/height would
			// be set to 0 (as getComponentInteriorWidth not being
			// ready)
			// so the style bar would be made invisible
			if (width <= 0 || height <= 0) {
				return;
			}

			sview.getComponent().setWidth(width + "px");
			sview.getComponent().setHeight(height + "px");

		}
	}

	public CASViewW getCAS() {
		return sview;
	}

	public App getApp() {
		return app;
	}

	private static String getDefaultToolbar() {
		return CASView.TOOLBAR_DEFINITION;
	}

	@Override
	protected Widget loadStyleBar() {
		return ((CASViewW) ((GuiManagerW) app.getGuiManager()).getCasView())
				.getCASStyleBar();
	}

	@Override
	public ResourcePrototype getIcon() {
		return getResources().menu_icon_cas();
	}

	public MathKeyboardListener getKeyboardListener() {
		return ((CASViewW) ((GuiManagerW) app.getGuiManager()).getCasView())
				.getEditor();
	}
}
