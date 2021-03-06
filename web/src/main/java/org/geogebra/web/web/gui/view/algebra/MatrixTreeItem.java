package org.geogebra.web.web.gui.view.algebra;

import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoList;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.web.html5.gui.util.CancelEvents;
import org.geogebra.web.html5.gui.util.ListItem;
import org.geogebra.web.html5.gui.util.UnorderedList;
import org.geogebra.web.html5.main.DrawEquationWeb;
import org.geogebra.web.web.css.GuiResources;
import org.geogebra.web.web.euclidian.EuclidianStyleBarW;
import org.geogebra.web.web.gui.util.ButtonPopupMenu;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TreeItem;

/**
 * MatrixRadioButtonTreeItem for creating matrices (2-dimensional lists in the
 * algebra view
 * 
 * File created by Arpad Fekete
 */
public class MatrixTreeItem extends RadioTreeItem {

	PushButton pButton;
	ButtonPopupMenu specialPopup;

	/**
	 * Creating a SpecialRadioButtonTreeItem from existing construction as we
	 * should allow special buttons for them, too... see
	 * RadioButtonTreeItem.create, which may call this constructor depending on
	 * situation (e.g. why not after NewRadioButtonTreeItem?)
	 */
	public MatrixTreeItem(final GeoElement ge, SafeUri showUrl,
			SafeUri hiddenUrl) {
		super(ge, showUrl, hiddenUrl);

		pButton = new PushButton(new Image(
				GuiResources.INSTANCE.algebra_matrix_size()));
		pButton.getUpHoveringFace().setImage(
				new Image(GuiResources.INSTANCE.algebra_matrix_size_hover()));
		pButton.addStyleName("XButtonNeighbour");
		pButton.addStyleName("shown");
		pButton.addMouseDownHandler(new MouseDownHandler() {
			public void onMouseDown(MouseDownEvent mde) {
				mde.preventDefault();
				mde.stopPropagation();

				if (specialPopup != null) {
					if (EuclidianStyleBarW.CURRENT_POP_UP != specialPopup
							|| !app.wasPopupJustClosed()) {
						if (EuclidianStyleBarW.CURRENT_POP_UP != null) {
							EuclidianStyleBarW.CURRENT_POP_UP.hide();
						}
						EuclidianStyleBarW.CURRENT_POP_UP = specialPopup;

						app.registerPopup(specialPopup);
						specialPopup.showRelativeTo(pButton);
						specialPopup.getFocusPanel().getElement().focus();
					} else {
						specialPopup.setVisible(false);
						EuclidianStyleBarW.CURRENT_POP_UP = null;
					}
				}
			}
		});
		pButton.addStyleName("MouseDownDoesntExitEditingFeature");
		pButton.addStyleName("BlurDoesntUpdateGUIFeature");

		// basically, everything except onClick,
		// static to prevent more instances
		pButton.addClickHandler(CancelEvents.instance);
		pButton.addDoubleClickHandler(CancelEvents.instance);
		// btnRow.addMouseDownHandler(cancelEvents);
		pButton.addMouseUpHandler(CancelEvents.instance);
		pButton.addMouseMoveHandler(CancelEvents.instance);
		// btnRow.addMouseOverHandler(cancelEvents);
		// pButton.addMouseOutHandler(CancelEvents.instance);

		// do not redefine TouchStartHandlers, as they are
		// simulate mouse events!

		specialPopup = new ButtonPopupMenu(app.getPanel()) {
			@Override
			public void setVisible(boolean visible) {
				super.setVisible(visible);

				// if another button is pressed only the visibility is changed,
				// by firing the event we can react as if it was closed
				CloseEvent.fire(this, this, false);
			}

			@Override
			public void hide() {
				super.hide();
				if (EuclidianStyleBarW.CURRENT_POP_UP.equals(this)) {
					EuclidianStyleBarW.CURRENT_POP_UP = null;
				}
			}
		};
		specialPopup.setAutoHideEnabled(true);
		specialPopup.getPanel().addStyleName("AVmenuListContainer");
		specialPopup.addStyleName("MouseDownDoesntExitEditingFeature");
		specialPopup.addStyleName("BlurDoesntUpdateGUIFeature");

		UnorderedList itemList = new UnorderedList();
		itemList.setStyleName("AVmenuListContent");
		specialPopup.getPanel().add(itemList);

		ListItem actual = new ListItem();
		actual.add(new Image(GuiResources.INSTANCE.algebra_new()));
		actual.add(new Label(app.getPlain("AddRow")));
		// ClickHandler is okay here, but maybe MouseDownHandler is better?
		actual.addDomHandler(new ClickHandler() {
			public void onClick(ClickEvent ce) {
				ce.stopPropagation();
				ce.preventDefault();
				specialPopup.setVisible(false);
				EuclidianStyleBarW.CURRENT_POP_UP = null;

				increaseRows();
			}
		}, ClickEvent.getType());
		itemList.add(actual);

		actual = new ListItem();
		actual.add(new Image(GuiResources.INSTANCE.algebra_remove()));
		actual.add(new Label(app.getPlain("RemoveRow")));
		actual.addDomHandler(new ClickHandler() {
			public void onClick(ClickEvent ce) {
				ce.stopPropagation();
				ce.preventDefault();
				specialPopup.setVisible(false);
				EuclidianStyleBarW.CURRENT_POP_UP = null;

				diminishRows();
			}
		}, ClickEvent.getType());
		itemList.add(actual);

		actual = new ListItem();
		actual.add(new Image(GuiResources.INSTANCE.algebra_new()));
		actual.add(new Label(app.getPlain("AddColumn")));
		actual.addDomHandler(new ClickHandler() {
			public void onClick(ClickEvent ce) {
				ce.stopPropagation();
				ce.preventDefault();
				specialPopup.setVisible(false);
				EuclidianStyleBarW.CURRENT_POP_UP = null;

				increaseCols();
			}
		}, ClickEvent.getType());
		itemList.add(actual);

		actual = new ListItem();
		actual.add(new Image(GuiResources.INSTANCE.algebra_remove()));
		actual.add(new Label(app.getPlain("RemoveColumn")));
		actual.addDomHandler(new ClickHandler() {
			public void onClick(ClickEvent ce) {
				ce.stopPropagation();
				ce.preventDefault();
				specialPopup.setVisible(false);
				EuclidianStyleBarW.CURRENT_POP_UP = null;

				diminishCols();
			}
		}, ClickEvent.getType());
		itemList.add(actual);
	}

	@Override
	public void replaceXButtonDOM(TreeItem item) {
		buttonPanel.add(pButton);
		super.replaceXButtonDOM(item);
	}

	@Override
	protected void maybeSetPButtonVisibility(boolean bool) {
		pButton.setVisible(bool);
	}

	public static GeoList create2x2IdentityMatrix(Kernel kern) {

		Construction cons = kern.getConstruction();
		// this works in a similar was as AlgoIdentity
		GeoList ret = new GeoList(cons);
		GeoList row = new GeoList(cons);
		row.add(new GeoNumeric(cons, 1));
		row.add(new GeoNumeric(cons, 0));
		ret.add(row);
		row = new GeoList(cons);
		row.add(new GeoNumeric(cons, 0));
		row.add(new GeoNumeric(cons, 1));
		ret.add(row);
		ret.setLabel(ret.getDefaultLabel());
		return ret;
	}

	public void increaseRows() {
		Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
			public void execute() {
				boolean wasEditing = commonEditingCheck();

				if (!wasEditing)
					ensureEditing();

				DrawEquationWeb.addNewRowToMatrix(seMayLatex);

				// it is a good question whether shall we save the result
				// in a permanent way, and in which case (wasEditing?)
				// why not?
				DrawEquationWeb.endEditingEquationMathQuillGGB(
						MatrixTreeItem.this, seMayLatex);

				if (wasEditing) {
					av.startEditing(geo);
				}
			}
		});
	}

	public void increaseCols() {
		Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
			public void execute() {
				boolean wasEditing = commonEditingCheck();

				if (!wasEditing)
					ensureEditing();

				DrawEquationWeb.addNewColToMatrix(seMayLatex);

				// it is a good question whether shall we save the result
				// in a permanent way, and in which case (wasEditing?)
				// why not?
				DrawEquationWeb.endEditingEquationMathQuillGGB(
						MatrixTreeItem.this, seMayLatex);

				if (wasEditing) {
					av.startEditing(geo);
				}
			}
		});
	}

	public void diminishCols() {
		Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
			public void execute() {
				boolean wasEditing = commonEditingCheck();

				if (!wasEditing)
					ensureEditing();

				DrawEquationWeb.removeColFromMatrix(seMayLatex);

				// it is a good question whether shall we save the result
				// in a permanent way, and in which case (wasEditing?)
				// why not?
				DrawEquationWeb.endEditingEquationMathQuillGGB(
						MatrixTreeItem.this, seMayLatex);

				if (wasEditing) {
					av.startEditing(geo);
				}
			}
		});
	}

	public void diminishRows() {
		Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
			public void execute() {
				boolean wasEditing = commonEditingCheck();

				if (!wasEditing)
					ensureEditing();

				DrawEquationWeb.removeRowFromMatrix(seMayLatex);

				// it is a good question whether shall we save the result
				// in a permanent way, and in which case (wasEditing?)
				// why not?
				DrawEquationWeb.endEditingEquationMathQuillGGB(
						MatrixTreeItem.this, seMayLatex);

				if (wasEditing) {
					av.startEditing(geo);
				}
			}
		});
	}
}
