package org.geogebra.web.web.main;

import org.geogebra.common.gui.view.consprotocol.ConstructionProtocolView;
import org.geogebra.web.html5.main.AppW;
import org.geogebra.web.web.gui.app.GeoGebraAppFrame;
import org.geogebra.web.web.gui.browser.BrowseGUI;
import org.geogebra.web.web.gui.browser.BrowseResources;
import org.geogebra.web.web.gui.dialog.image.ImageInputDialog;
import org.geogebra.web.web.gui.dialog.image.UploadImageDialog;
import org.geogebra.web.web.gui.view.consprotocol.ConstructionProtocolViewW;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;

public class BrowserDevice implements GDevice {

	public class MyButton extends FlowPanel {
		private Element input;

		public MyButton() {
			super();
			this.setStyleName("button");
			final Image icon = new Image(
			        BrowseResources.INSTANCE.location_local());
			final Element span = DOM.createElement("span");
			span.setAttribute(
			        "style",
			        "position: absolute; width: 50px; height: 50px; padding: 10px; top: 0px; left: 0px; overflow: hidden;");
			span.setInnerHTML("<img src=\"" + icon.getUrl() + "\"/>");
			input = DOM.createElement("input");
			input.setAttribute("type", "file");
			input.setAttribute(
			        "style",
			        "width: 500px; height: 60px; font-size: 56px;"
			                + "opacity: 0; position: absolute; right: 0px; top: 0px; cursor: pointer;");
			span.appendChild(input);

			DOM.insertChild(getElement(), span, 0);

		}

		public void setBrowseGUI(BrowseGUI bg) {
			addGgbChangeHandler(input, bg);
		}
		public native void addGgbChangeHandler(Element el, BrowseGUI bg) /*-{
			var dialog = this;
			//		el.setAttribute("accept", "application/vnd.geogebra.file, application/vnd.geogebra.tool");
			el.onchange = function(event) {
				var files = this.files;
				if (files.length) {
					bg.@org.geogebra.web.web.gui.browser.BrowseGUI::showLoading()();
					var fileToHandle = files[0];
					bg.@org.geogebra.web.web.gui.browser.BrowseGUI::openFile(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;)(fileToHandle);
				}

			};
		}-*/;
	}

	public boolean supportsExport() {
		return true;
	}

	@Override
	public FileManager createFileManager(AppW app) {
		return new FileManagerW(app);
	}

	@Override
	public void setMinWidth(GeoGebraAppFrame frame) {
		if (Window.getClientWidth() > 760) {
			frame.removeStyleName("minWidth");
			frame.syncPanelSizes();
		} else {
			frame.addStyleName("minWidth");
		}
	}

	@Override
	public boolean isOffline(AppW app) {
		return !app.getNetworkOperation().isOnline();
	}

	public UploadImageDialog getImageInputDialog(AppW app) {

		return new ImageInputDialog(app);
	}

	@Override
	public BrowseGUI createBrowseView(AppW app) {
		MyButton mb = new MyButton();
		BrowseGUI bg = new BrowseGUI(app, mb);
		mb.setBrowseGUI(bg);
		return bg;
	}

	@Override
	public ConstructionProtocolView getConstructionProtocolView(AppW app) {
		return new ConstructionProtocolViewW(app);
	}



}
