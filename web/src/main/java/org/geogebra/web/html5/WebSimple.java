package org.geogebra.web.html5;

import java.util.ArrayList;

import org.geogebra.common.util.debug.GeoGebraProfiler;
import org.geogebra.common.util.debug.SilentProfiler;
import org.geogebra.web.html5.util.ArticleElement;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class WebSimple implements EntryPoint {

	/**
	 * set true if Google Api Js loaded
	 */

	public void onModuleLoad() {
		if (RootPanel.getBodyElement().getAttribute("data-param-laf") != null
		        && !"".equals(RootPanel.getBodyElement().getAttribute(
		                "data-param-laf"))) {
			// loading touch, ignore.
			return;
		}
		Browser.checkFloat64();
		// use GeoGebraProfilerW if you want to profile, SilentProfiler for
		// production
		// GeoGebraProfiler.init(new GeoGebraProfilerW());
		GeoGebraProfiler.init(new SilentProfiler());

		GeoGebraProfiler.getInstance().profile();
		exportGGBElementRenderer();

		// WebStatic.currentGUI = checkIfNeedToLoadGUI();
		// setLocaleToQueryParam();

		// if (WebStatic.currentGUI.equals(GuiToLoad.VIEWER)) {
		// we dont want to parse out of the box sometimes...
		// loadAppletAsync();
		// }

		// loadAppletAsync();
		// instead, load it immediately
		startGeoGebra(ArticleElement.getGeoGebraMobileTags());
	}

	public static void loadAppletAsync() {
		GWT.runAsync(new RunAsyncCallback() {

			public void onSuccess() {
				startGeoGebra(ArticleElement.getGeoGebraMobileTags());
			}

			public void onFailure(Throwable reason) {
				// TODO Auto-generated method stub

			}
		});
	}

	static void startGeoGebra(ArrayList<ArticleElement> geoGebraMobileTags) {

		org.geogebra.web.html5.gui.GeoGebraFrameSimple.main(geoGebraMobileTags);

	}

	private native void exportGGBElementRenderer() /*-{
   		$wnd.renderGGBElement = $entry(@org.geogebra.web.html5.gui.GeoGebraFrameSimple::renderArticleElement(Lcom/google/gwt/dom/client/Element;Lcom/google/gwt/core/client/JavaScriptObject;))
   		@org.geogebra.web.html5.gui.GeoGebraFrame::renderGGBElementReady()();
   	}-*/;

}
