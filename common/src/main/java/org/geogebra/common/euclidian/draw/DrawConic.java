/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

/*
 * DrawConic.java
 *
 * Created on 16. Oktober 2001, 15:13
 */

package org.geogebra.common.euclidian.draw;

import java.util.ArrayList;

import org.geogebra.common.awt.GAffineTransform;
import org.geogebra.common.awt.GArc2D;
import org.geogebra.common.awt.GArea;
import org.geogebra.common.awt.GEllipse2DDouble;
import org.geogebra.common.awt.GGeneralPath;
import org.geogebra.common.awt.GGraphics2D;
import org.geogebra.common.awt.GRectangle;
import org.geogebra.common.awt.GRectangularShape;
import org.geogebra.common.awt.GShape;
import org.geogebra.common.euclidian.Drawable;
import org.geogebra.common.euclidian.EuclidianConstants;
import org.geogebra.common.euclidian.EuclidianView;
import org.geogebra.common.euclidian.GeneralPathClipped;
import org.geogebra.common.euclidian.Previewable;
import org.geogebra.common.euclidian.clipping.ClipShape;
import org.geogebra.common.factories.AwtFactory;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.Matrix.CoordMatrix;
import org.geogebra.common.kernel.Matrix.Coords;
import org.geogebra.common.kernel.algos.AlgoCirclePointRadius;
import org.geogebra.common.kernel.algos.AlgoCircleThreePoints;
import org.geogebra.common.kernel.algos.AlgoCircleTwoPoints;
import org.geogebra.common.kernel.algos.AlgoConicFivePoints;
import org.geogebra.common.kernel.algos.AlgoEllipseHyperbolaFociPoint;
import org.geogebra.common.kernel.algos.AlgoParabolaPointLine;
import org.geogebra.common.kernel.arithmetic.MyDouble;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoElement.HitType;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.geos.GeoVec2D;
import org.geogebra.common.kernel.kernelND.GeoConicND;
import org.geogebra.common.kernel.kernelND.GeoConicNDConstants;
import org.geogebra.common.kernel.kernelND.GeoLineND;
import org.geogebra.common.kernel.kernelND.GeoPointND;
import org.geogebra.common.kernel.kernelND.GeoSegmentND;
import org.geogebra.common.main.App;
import org.geogebra.common.plugin.EuclidianStyleConstants;

/**
 * 
 * @author Markus
 */
public class DrawConic extends Drawable implements Previewable {

	/** plotpoints per quadrant for hyperbola */
	protected static final int PLOT_POINTS = 32;
	/** maximum number of plot points */
	public static final int MAX_PLOT_POINTS = 300;
	/**
	 * maximum of pixels for a standard circle radius bigger circles are drawn
	 * via Arc2D
	 */
	public static final double HUGE_RADIUS = 1E12;

	/**
	 * the conic being drawn (not necessarily the same as geo, eg, for ineq
	 * drawing)
	 */
	protected GeoConicND conic;

	/** whether this is euclidian visible and onscreen */
	protected boolean isVisible;
	/** whether the label is visible */
	protected boolean labelVisible;
	private int type;
	/** label coordinates */
	protected double[] labelCoords = new double[2];

	// CONIC_SINGLE_POINT
	private boolean firstPoint = true;
	private GeoPoint point;
	private DrawPoint drawPoint;

	// CONIC_INTERSECTING_LINES
	private boolean firstLines = true;
	private GeoLine[] lines;
	private DrawLine[] drawLines;

	// CONIC_CIRCLE
	private boolean firstCircle = true;
	private GeoVec2D midpoint;
	private GArc2D arc;
	private GeneralPathClipped arcFiller, gp;
	private GRectangularShape circle;
	private double mx, my, radius, yradius, angSt, angEnd;

	/** transform for ellipse, hyperbola, parabola */
	protected GAffineTransform transform = AwtFactory.prototype
			.newAffineTransform();
	/** shape to be filled (eg. ellipse, space between paralel lines) */
	protected GShape shape;

	// CONIC_ELLIPSE
	private boolean firstEllipse = true;
	/** lengths of half axes */
	protected double[] halfAxes;
	private GEllipse2DDouble ellipse;

	// CONIC_PARABOLA
	private boolean firstParabola = true;
	/** x coord of start point for parabola/hyperbola */
	protected double x0;
	/** y coord of start point for parabola/hyperbola */
	protected double y0;
	private double k2;
	private GeoVec2D vertex;
	/** parabolic path */
	protected GGeneralPath parabola;
	private double[] parpoints = new double[8];

	// CONIC_HYPERBOLA
	/** whether this is the first time we draw a hyperbola */
	protected boolean firstHyperbola = true;
	/** first half-axis */
	protected double a;
	private double b;
	private double tsq;
	private double step;
	private double t;
	private double denom;
	private double x, y;
	private int index0, index1, n;
	/** number of points used for hyperbola path */
	protected int points = PLOT_POINTS;
	private GeneralPathClipped hypLeft, hypRight;
	private boolean hypLeftOnScreen, hypRightOnScreen;

	// preview of circle (two points or three points)
	private ArrayList<GeoPointND> prevPoints;
	private ArrayList<GeoSegmentND> prevSegments;
	private ArrayList<GeoLineND> prevLines;
	private ArrayList<GeoConicND> prevConics;
	private GeoPoint[] previewTempPoints;
	private GeoLine previewTempLine;
	private GeoNumeric previewTempRadius;
	private int previewMode, neededPrevPoints;
	private boolean isPreview = false;
	private boolean ignoreSingularities;

	@Override
	public org.geogebra.common.awt.GArea getShape() {
		org.geogebra.common.awt.GArea area = super.getShape() != null ? super
				.getShape() : (shape == null ? AwtFactory.prototype.newArea()
				: AwtFactory.prototype.newArea(shape));
		// App.debug(conic.isInverseFill() + "," + shape +
		// ","+super.getShape());
		if (conic.isInverseFill()) {
			GArea complement = AwtFactory.prototype.newArea(view
					.getBoundingPath());
			complement.subtract(area);
			return complement;
		}
		return area;
	}

	/**
	 * Creates new DrawConic
	 * 
	 * @param view
	 *            view
	 * @param c
	 *            conic
	 * @param ignoreSingularities
	 *            true to avoid drawing points
	 */
	public DrawConic(EuclidianView view, GeoConicND c,
			boolean ignoreSingularities) {
		this.view = view;
		isPreview = false;
		this.ignoreSingularities = ignoreSingularities;
		initConic(c);
		update();
	}

	private void initConic(GeoConicND c) {
		conic = c;
		geo = c;

		vertex = c.getTranslationVector(); // vertex
		midpoint = vertex;
		halfAxes = c.getHalfAxes();
		c.getAffineTransform();
	}

	/**
	 * Creates a new DrawConic for preview of a circle
	 * 
	 * @param view
	 *            view
	 * @param mode
	 *            preview mode
	 * @param points
	 *            preview points
	 */
	public DrawConic(EuclidianView view, int mode, ArrayList<GeoPointND> points) {
		this.view = view;
		prevPoints = points;
		previewMode = mode;

		Construction cons = view.getKernel().getConstruction();

		switch (mode) {
		case EuclidianConstants.MODE_CIRCLE_TWO_POINTS:
		case EuclidianConstants.MODE_CIRCLE_POINT_RADIUS:
			neededPrevPoints = 1;
			break;
		case EuclidianConstants.MODE_CIRCLE_THREE_POINTS:
		case EuclidianConstants.MODE_ELLIPSE_THREE_POINTS:
		case EuclidianConstants.MODE_HYPERBOLA_THREE_POINTS:
			neededPrevPoints = 2;
			break;
		case EuclidianConstants.MODE_CONIC_FIVE_POINTS:
			neededPrevPoints = 4;
			break;

		}

		// neededPrevPoints = mode == EuclidianConstants.MODE_CIRCLE_TWO_POINTS
		// ? 1
		// : 2;
		previewTempPoints = new GeoPoint[neededPrevPoints + 1];
		for (int i = 0; i < previewTempPoints.length; i++) {
			previewTempPoints[i] = new GeoPoint(cons);
		}

		initPreview();

	}

	/**
	 * Creates a new DrawConic for preview of a parabola
	 * 
	 * @param view
	 *            view
	 * @param selectedLines
	 *            possible directrix
	 * @param points
	 *            preview points
	 */
	public DrawConic(EuclidianView view, ArrayList<GeoPointND> points,
			ArrayList<GeoLineND> selectedLines) {
		this.view = view;
		prevPoints = points;
		prevLines = selectedLines;
		neededPrevPoints = 1;
		previewMode = EuclidianConstants.MODE_PARABOLA;

		Construction cons = view.getKernel().getConstruction();

		if (selectedLines.size() == 0) {
			previewTempLine = new GeoLine(cons);
		} else {
			previewTempLine = (GeoLine) selectedLines.get(0);
		}

		previewTempPoints = new GeoPoint[1];
		previewTempPoints[0] = new GeoPoint(cons);

		initPreview();

	}

	/**
	 * Creates a new DrawConic for preview of a compass circle (radius or
	 * segment first, then center point)
	 * 
	 * @param view
	 *            view
	 * @param mode
	 *            preview mode
	 * @param points
	 *            preview points
	 * @param segments
	 *            preview segments
	 * @param conics
	 *            preview conics
	 */
	public DrawConic(EuclidianView view, int mode,
			ArrayList<GeoPointND> points, ArrayList<GeoSegmentND> segments,
			ArrayList<GeoConicND> conics) {
		this.view = view;
		prevPoints = points;
		prevSegments = segments;
		prevConics = conics;
		previewMode = mode;

		Construction cons = view.getKernel().getConstruction();
		previewTempRadius = new GeoNumeric(cons);
		previewTempPoints = new GeoPoint[1];
		previewTempPoints[0] = new GeoPoint(cons);

		initPreview();
	}

	@Override
	final public void update() {
		isVisible = geo.isEuclidianVisible();
		if (!isVisible)
			return;
		labelVisible = geo.isLabelVisible();

		updateStrokes(conic);
		type = conic.getType();

		switch (type) {
		case GeoConicNDConstants.CONIC_EMPTY:
			setShape(null);
			shape = null;
			break;
		case GeoConicNDConstants.CONIC_SINGLE_POINT:
			updateSinglePoint();
			break;

		case GeoConicNDConstants.CONIC_DOUBLE_LINE:
			updateDoubleLine();
			break;

		case GeoConicNDConstants.CONIC_INTERSECTING_LINES:
		case GeoConicNDConstants.CONIC_PARALLEL_LINES:
		case GeoConicNDConstants.CONIC_LINE:
			updateLines();
			break;

		case GeoConicNDConstants.CONIC_CIRCLE:
			updateCircle();
			break;

		case GeoConicNDConstants.CONIC_ELLIPSE:
			updateEllipse();
			break;

		case GeoConicNDConstants.CONIC_HYPERBOLA:
			updateHyperbola();
			break;

		case GeoConicNDConstants.CONIC_PARABOLA:
			updateParabola();
			break;
		}

		if (!isVisible)
			return;

		// shape on screen?
		GRectangle viewRect = AwtFactory.prototype.newRectangle(0, 0,
				view.getWidth(), view.getHeight());
		switch (type) {
		case GeoConicNDConstants.CONIC_CIRCLE:
		case GeoConicNDConstants.CONIC_ELLIPSE:
		case GeoConicNDConstants.CONIC_PARABOLA:
			isVisible = checkCircleEllipseParabolaOnScreen(viewRect);
			break;

		case GeoConicNDConstants.CONIC_HYPERBOLA:
			isVisible = checkHyperbolaOnScreen(viewRect);
			break;
		}

		if (!isVisible)
			return;

		// draw trace
		if (conic.getTrace()) {
			isTracing = true;
			org.geogebra.common.awt.GGraphics2D g2 = view.getBackgroundGraphics();
			if (g2 != null)
				drawTrace(g2);
		} else {
			if (isTracing) {
				isTracing = false;
				// view.updateBackground();
			}
		}

		if (labelVisible) {
			labelDesc = geo.getLabelDescription();
			addLabelOffset();
		}
	}

	/**
	 * check circle/ellipse/parabola intersects the screen
	 * 
	 * @param viewRect
	 *            view rectangle
	 * @return if hyperbola intersects the screen
	 */
	protected boolean checkCircleEllipseParabolaOnScreen(GRectangle viewRect) {
		boolean includesScreenCompletely = shape.contains(viewRect);

		// offScreen = includesScreenCompletely or the shape does not
		// intersect the view rectangle
		boolean offScreen = includesScreenCompletely
				|| !shape.getBounds2D().intersects(viewRect);
		if (geo.getAlphaValue() == 0f) {
			// no filling
			return !offScreen;
		}
		// filling
		if (includesScreenCompletely) {
			return true;
		}
		return !offScreen;
	}

	/**
	 * check hyperbola intersects the screen
	 * 
	 * @param viewRect
	 *            view rectangle
	 * @return if hyperbola intersects the screen
	 */
	protected boolean checkHyperbolaOnScreen(GRectangle viewRect) {
		// hyperbola wings on screen?
		hypLeftOnScreen = hypLeft.intersects(AwtFactory.prototype
				.newRectangle(viewRect));
		hypRightOnScreen = hypRight.intersects(AwtFactory.prototype
				.newRectangle(viewRect));
		if (!hypLeftOnScreen && !hypRightOnScreen) {
			return false;
		}
		return true;
	}

	final private void updateSinglePoint() {
		// we want to determine the sign of the result but we can't use fixed
		// point
		// as it may be equal to the single point. Point (b.x+1,0) differs in
		// one coord.

		shape = null;

		if (firstPoint) {
			firstPoint = false;
			point = conic.getSinglePoint();
			if (point == null)
				point = new GeoPoint(conic.getConstruction());
			drawPoint = new DrawPoint(view, point, isPreview);
			drawPoint.setGeoElement(conic);
			// drawPoint.font = view.fontConic;
		}

		setShape(!ignoreSingularities ? drawPoint.getDot() : null);

		// looks if it's on view
		Coords p = view.getCoordsForView(conic.getMidpoint3D());
		// App.debug("\n"+view+"\n"+p);
		if (!Kernel.isZero(p.getZ())) {
			isVisible = false;
			return;
		}

		double[] coords = new double[2];
		coords[0] = p.getX();
		coords[1] = p.getY();

		point.copyLabel(conic);
		point.setObjColor(conic.getObjectColor());
		point.setPointSize(conic.lineThickness);

		drawPoint.update(coords);
	}

	/**
	 * Updates the double line and shape so that positive part is colored
	 */
	protected void updateDoubleLine() {
		updateLines();
	}

	/**
	 * Updates the lines and shape so that positive part is colored
	 */
	protected void updateLines() {
		shape = null;

		if (firstLines) {
			firstLines = false;
			lines = conic.getLines();
			drawLines = new DrawLine[2];
			drawLines[0] = new DrawLine(view, lines[0]);
			drawLines[1] = new DrawLine(view, lines[1]);
			drawLines[0].setGeoElement(geo);
			drawLines[1].setGeoElement(geo);
			// drawLines[0].font = view.fontConic;
			// drawLines[1].font = view.fontConic;
		}

		CoordMatrix m = null;
		if (!isPreview) {
			if (view.getMatrix() == null) {
				if (conic.isGeoElement3D()) {
					m = conic.getCoordSys().getMatrixOrthonormal().inverse();
				}
			} else {
				if (conic.isGeoElement3D()) {
					m = conic.getCoordSys().getMatrixOrthonormal().inverse()
							.mul(view.getMatrix());
				} else {
					m = view.getMatrix();
				}
			}
		}

		for (int i = 0; i < 2; i++) {
			drawLines[i].forceLineType(conic.lineType);
			drawLines[i].update(m);
			// thickness needs update #4087
			drawLines[i].updateStrokesJustLineThickness(geo);
		}

		if (conic.type == GeoConicNDConstants.CONIC_PARALLEL_LINES
				|| conic.type == GeoConicNDConstants.CONIC_INTERSECTING_LINES
				|| conic.type == GeoConicNDConstants.CONIC_LINE) {

			shape = drawLines[0].getShape(true);
			if (conic.type != GeoConicNDConstants.CONIC_LINE)
				((org.geogebra.common.awt.GArea) shape).exclusiveOr(drawLines[1]
						.getShape(true));
			// FIXME: buggy when conic(RW(0),RW(0))=0

			if (negativeColored()) {
				org.geogebra.common.awt.GArea complement = AwtFactory.prototype
						.newArea(view.getBoundingPath());
				complement.subtract((org.geogebra.common.awt.GArea) shape);
				shape = complement;

			}

		}

	}

	private boolean negativeColored() {
		double[] xTry = new double[] { 0, 10, 20, 0, 10, 20 };
		double[] yTry = new double[] { 0, 0, 0, 10, 10, 20 };
		for (int i = 0; i < 6; i++) {
			double val1 = conic.evaluate(view.toRealWorldCoordX(xTry[i]),
					view.toRealWorldCoordY(yTry[i]));
			if (conic.type == GeoConicNDConstants.CONIC_INTERSECTING_LINES)
				val1 *= conic.evaluate(
						conic.b.getX() + lines[0].x + lines[1].x,
						conic.b.getY() + lines[0].y + lines[1].y);
			if (conic.type == GeoConicNDConstants.CONIC_PARALLEL_LINES)
				val1 *= conic.evaluate(conic.b.getX(), conic.b.getY());
			if (!Kernel.isZero(val1))
				return (val1 > 0) ^ shape.contains(xTry[i], yTry[i]);
		}
		return false;
	}

	protected void updateCircle() {
		setShape(null);
		boolean fullAngle = false;
		// calc screen pixel of radius
		radius = halfAxes[0] * view.getXscale();
		yradius = halfAxes[1] * view.getYscale(); // radius scaled in y
													// direction
		if (radius > DrawConic.HUGE_RADIUS || yradius > DrawConic.HUGE_RADIUS) {
			App.debug("ellipse fallback");
			// ellipse drawing is handling those cases better
			updateEllipse();
			return;
		}

		if (firstCircle) {
			firstCircle = false;
			arc = AwtFactory.prototype.newArc2D();
			if (ellipse == null)
				ellipse = AwtFactory.prototype.newEllipse2DDouble();
		}

		int i = -1; // bugfix

		// if circle is very big, draw arc: this is very important
		// for graphical continuity

		// BIG RADIUS: larger than screen diagonal
		int BIG_RADIUS = view.getWidth() + view.getHeight(); // > view's
																// diagonal
		if (radius < BIG_RADIUS && yradius < BIG_RADIUS) {
			circle = ellipse;
			arcFiller = null;
			// calc screen coords of midpoint
			Coords M;
			if (isPreview) // midpoint has been calculated in view coords
				M = conic.getMidpoint3D().getInhomCoords();
			else {
				M = view.getCoordsForView(conic.getMidpoint3D());
				if (!Kernel.isZero(M.getZ())) {// check if in view
					isVisible = false;
					return;
				}
				// check if eigen vec are in view
				for (int j = 0; j < 2; j++) {
					Coords ev = view.getCoordsForView(conic.getEigenvec3D(j));
					if (!Kernel.isZero(ev.getZ())) {// check if in view
						isVisible = false;
						return;
					}
				}
			}
			mx = M.getX() * view.getXscale() + view.getxZero();
			my = -M.getY() * view.getYscale() + view.getyZero();
			ellipse.setFrame(mx - radius, my - yradius, 2.0 * radius,
					2.0 * yradius);
		} else {
			// special case: really big circle
			// draw arc according to midpoint position
			// of the arc
			Coords M = view.getCoordsForView(conic.getMidpoint3D());
			if (!Kernel.isZero(M.getZ())) {// check if in view
				isVisible = false;
				return;
			}
			// check if eigen vec are in view
			for (int j = 0; j < 2; j++) {
				Coords ev = view.getCoordsForView(conic.getEigenvec3D(j));
				if (!Kernel.isZero(ev.getZ())) {// check if in view
					isVisible = false;
					return;
				}
			}
			mx = M.getX() * view.getXscale() + view.getxZero();
			my = -M.getY() * view.getYscale() + view.getyZero();

			angSt = Double.NaN;
			// left
			if (mx < 0.0) {
				// top
				if (my < 0.0) {
					angSt = -Math.acos(-mx / radius);
					angEnd = -Math.asin(-my / yradius);
					i = 0;
				}
				// bottom
				else if (my > view.getHeight()) {
					angSt = Math.asin((my - view.getHeight()) / yradius);
					angEnd = Math.acos(-mx / radius);
					i = 2;
				}
				// middle
				else {
					angSt = -Math.asin((view.getHeight() - my) / yradius);
					angEnd = Math.asin(my / yradius);
					i = 1;
				}
			}
			// right
			else if (mx > view.getWidth()) {
				// top
				if (my < 0.0) {
					angSt = Math.PI + Math.asin(-my / yradius);
					angEnd = Math.PI
							+ Math.acos((mx - view.getWidth()) / radius);
					i = 6;
				}
				// bottom
				else if (my > view.getHeight()) {
					angSt = Math.PI
							- Math.acos((mx - view.getWidth()) / radius);
					angEnd = Math.PI
							- Math.asin((my - view.getHeight()) / yradius);
					i = 4;
				}
				// middle
				else {
					angSt = Math.PI - Math.asin(my / yradius);
					angEnd = Math.PI
							+ Math.asin((view.getHeight() - my) / yradius);
					i = 5;
				}
			}
			// top middle
			else if (my < 0.0) {
				angSt = Math.PI + Math.acos(mx / radius);
				angEnd = 2 * Math.PI
						- Math.acos((view.getWidth() - mx) / radius);
				i = 7;
			}
			// bottom middle
			else if (my > view.getHeight()) {
				angSt = Math.acos((view.getWidth() - mx) / radius);
				angEnd = Math.PI - Math.acos(mx / radius);
				i = 3;
			}
			// center on screen
			else {
				// huge circle with center on screen: use screen rectangle
				// instead of circle for possible filling
				if (radius < BIG_RADIUS || yradius < BIG_RADIUS) {
					updateEllipse();
					return;
				}
				shape = circle = AwtFactory.prototype.newRectangle(-1, -1,
						view.getWidth() + 2, view.getHeight() + 2);

				arcFiller = null;
				xLabel = -100;
				yLabel = -100;
				return;
			}

			if (Double.isNaN(angSt) || Double.isNaN(angEnd)) {
				// to ensure drawing ...
				angSt = 0.0d;
				angEnd = 2 * Math.PI;
				arcFiller = null;
				fullAngle = true;
			}

			// set arc
			circle = arc;
			arc.setArc(mx - radius, my - yradius, 2.0 * radius, 2.0 * yradius,
					Math.toDegrees(angSt), Math.toDegrees(angEnd - angSt),
					GArc2D.OPEN);

			// set general path for filling the arc to screen borders
			if ((conic.getAlphaValue() > 0.0f || conic.isHatchingEnabled())
					&& !fullAngle) {
				if (gp == null)
					gp = new GeneralPathClipped(view);
				else
					gp.reset();
				org.geogebra.common.awt.GPoint2D sp = arc.getStartPoint();
				org.geogebra.common.awt.GPoint2D ep = arc.getEndPoint();

				switch (i) { // case number
				case 0: // left top
					gp.moveTo(0, 0);
					gp.lineTo(sp.getX(), sp.getY());
					gp.lineTo(ep.getX(), ep.getY());
					break;

				case 1: // left middle
					gp.moveTo(0, view.getHeight());
					gp.lineTo(sp.getX(), sp.getY());
					gp.lineTo(ep.getX(), ep.getY());
					gp.lineTo(0, 0);
					break;

				case 2: // left bottom
					gp.moveTo(0, view.getHeight());
					gp.lineTo(sp.getX(), sp.getY());
					gp.lineTo(ep.getX(), ep.getY());
					break;

				case 3: // middle bottom
					gp.moveTo(view.getWidth(), view.getHeight());
					gp.lineTo(sp.getX(), sp.getY());
					gp.lineTo(ep.getX(), ep.getY());
					gp.lineTo(0, view.getHeight());
					break;

				case 4: // right bottom
					gp.moveTo(view.getWidth(), view.getHeight());
					gp.lineTo(sp.getX(), sp.getY());
					gp.lineTo(ep.getX(), ep.getY());
					break;

				case 5: // right middle
					gp.moveTo(view.getWidth(), 0);
					gp.lineTo(sp.getX(), sp.getY());
					gp.lineTo(ep.getX(), ep.getY());
					gp.lineTo(view.getWidth(), view.getHeight());
					break;

				case 6: // right top
					gp.moveTo(view.getWidth(), 0);
					gp.lineTo(sp.getX(), sp.getY());
					gp.lineTo(ep.getX(), ep.getY());
					break;

				case 7: // top middle
					gp.moveTo(0, 0);
					gp.lineTo(sp.getX(), sp.getY());
					gp.lineTo(ep.getX(), ep.getY());
					gp.lineTo(view.getWidth(), 0);
					break;

				default:
					gp = null;
				}
				// gp.
				arcFiller = gp;
			}
		}
		shape = circle;

		// set label position
		xLabel = (int) (mx - radius / 2.0);
		yLabel = (int) (my - yradius * 0.85) + 20;
	}

	protected Coords[] ev;

	/**
	 * Update in case this draws an ellipse
	 */
	protected void updateEllipse() {
		setShape(null);
		// check for huge pixel radius
		double xRadius = halfAxes[0] * view.getXscale();
		double yRadius = halfAxes[1] * view.getYscale();
		if (xRadius > DrawConic.HUGE_RADIUS || yRadius > DrawConic.HUGE_RADIUS) {
			isVisible = false;
			return;
		}

		// check if in view
		Coords M;
		if (isPreview) { // midpoint has been calculated in view coords
			M = conic.getMidpoint3D().getInhomCoords();
		} else {
			M = view.getCoordsForView(conic.getMidpoint3D());
			if (!Kernel.isZero(M.getZ())) {// check if in view
				isVisible = false;
				return;
			}
		}

		if (ev == null) {
			ev = new Coords[2];
		}
		if (isPreview) { // calculations were in view coords
			for (int j = 0; j < 2; j++) {
				ev[j] = conic.getEigenvec(j);
			}
		} else {
			for (int j = 0; j < 2; j++) {
				ev[j] = view.getCoordsForView(conic.getEigenvec3D(j));
				if (!Kernel.isZero(ev[j].getZ())) {// check if in view
					isVisible = false;
					return;
				}
			}
		}

		if (firstEllipse) {
			firstEllipse = false;
			if (ellipse == null)
				ellipse = AwtFactory.prototype.newEllipse2DDouble();
		}

		// set transform
		transform.setTransform(view.getCoordTransform());
		transform.concatenate(view.getCompanion().getTransform(conic, M, ev));

		// set ellipse
		ellipse.setFrameFromCenter(0, 0, halfAxes[0], halfAxes[1]);

		// BIG RADIUS: larger than screen diagonal
		int BIG_RADIUS = view.getWidth() + view.getHeight(); // > view's
																// diagonal
		if (xRadius < BIG_RADIUS && yRadius < BIG_RADIUS) {
			shape = transform.createTransformedShape(ellipse);
		} else {
			// clip big arc at screen
			// shape=ClipShape.clipToRect(shape,ellipse, transform, new
			// Rectangle(-1,
			// -1, view.getWidth() + 2, view.getHeight() + 2));
			shape = ClipShape.clipToRect(ellipse, transform,
					AwtFactory.prototype.newRectangle(-1, -1,
							view.getWidth() + 2, view.getHeight() + 2));

		}
		// set label coords
		labelCoords[0] = -halfAxes[0] / 2.0d;
		labelCoords[1] = halfAxes[1] * 0.85d - 20.0 / view.getYscale();
		transform.transform(labelCoords, 0, labelCoords, 0, 1);
		xLabel = (int) labelCoords[0];
		yLabel = (int) labelCoords[1];
	}

	/**
	 * draw only one edge for the hyperbola section
	 */
	protected void updateHyperbolaEdge() {
		// only used in DrawConicSection
		isVisible = false;
	}

	protected void updateHyperbola() {

		// check if in view
		Coords M;
		if (isPreview) { // midpoint has been calculated in view coords
			M = conic.getMidpoint3D().getInhomCoords();
		} else {
			M = view.getCoordsForView(conic.getMidpoint3D());
			if (!Kernel.isZero(M.getZ())) {// check if in view
				isVisible = false;
				return;
			}
		}
		if (ev == null) {
			ev = new Coords[2];
		}
		if (isPreview) { // calculations were in view coords
			for (int j = 0; j < 2; j++) {
				ev[j] = conic.getEigenvec(j);
			}
		} else {
			for (int j = 0; j < 2; j++) {
				ev[j] = view.getCoordsForView(conic.getEigenvec3D(j));
				if (!Kernel.isZero(ev[j].getZ())) {// check if in view
					isVisible = false;
					return;
				}
			}
		}

		updateHyperbolaResetPaths();

		a = halfAxes[0];
		b = halfAxes[1];

		updateHyperbolaX0();

		// init step width
		if (x0 <= a) { // hyperbola is not visible on screen
			isVisible = false;
			return;
		}

		// set number of plot points according to size of x0
		// add ten points per screen width
		n = PLOT_POINTS
				+ (int) (Math.abs(x0 - a) / (view.getXmax() - view.getXmin()))
				* 10;
		// n < 0 might result from huge real
		if (points != n && n > 0) {
			points = Math.min(n, MAX_PLOT_POINTS);
		}

		// hyperbola is visible on screen
		step = Math.sqrt((x0 - a) / (x0 + a)) / (points - 1);

		// build Polyline of parametric hyperbola
		// hyp(t) = 1/(1-t^2) {a(1+t^2), 2bt}, 0 <= t < 1
		// this represents the first quadrant's wing of a hypberola
		/*
		 * hypRight.addPoint(points - 1, a, 0); hypLeft.addPoint(points - 1, -a,
		 * 0);
		 */
		updateHyperbolaAddPoint(points - 1, a, 0);

		t = step;
		int i = 1;
		index0 = points; // points ... 2*points - 2
		index1 = points - 2; // points-2 ... 0
		while (index1 >= 0) {
			tsq = t * t;
			denom = 1.0 - tsq;
			// calc coords of first quadrant
			x = (a * (1.0 + tsq) / denom);
			y = (2.0 * b * t / denom);

			// first and second quadrants
			updateHyperbolaAddPoint(index0, x, y);
			// third and fourth quadrants
			updateHyperbolaAddPoint(index1, x, -y);

			/*
			 * // first quadrant hypRight.addPoint(index0, x, y); // second
			 * quadrant hypLeft.addPoint(index0, -x, y); // third quadrant
			 * hypLeft.addPoint(index1, -x, -y); // fourth quadrant
			 * hypRight.addPoint(index1, x, -y);
			 */

			index0++;
			index1--;
			i++;
			t = i * step;
		}

		updateHyperbolaClosePaths();

		// set transform for Graphics2D
		transform.setTransform(view.getCoordTransform());
		transform.concatenate(view.getCompanion().getTransform(conic, M, ev));

		updateHyperboalSetTransformToPaths();

		updateHyperbolaLabelCoords();
		transform.transform(labelCoords, 0, labelCoords, 0, 1);
		xLabel = (int) labelCoords[0];
		yLabel = (int) labelCoords[1];

		updateHyperbolaSetShape();
	}

	/** set label coords */
	protected void updateHyperbolaLabelCoords() {

		labelCoords[0] = 2.0 * a;
		// point on curve: y = b * sqrt(3) minus 20 pixels
		labelCoords[1] = b * 1.7 - 20.0 / view.getYscale();
	}

	/**
	 * reset paths for hyperbola
	 */
	protected void updateHyperbolaResetPaths() {

		if (firstHyperbola) {
			firstHyperbola = false;
			points = PLOT_POINTS;
			hypRight = new GeneralPathClipped(view); // right wing
			hypLeft = new GeneralPathClipped(view); // left wing
		} else {
			hypRight.reset();
			hypLeft.reset();
		}
	}

	/**
	 * updates hyperbola x maximum value
	 */
	protected void updateHyperbolaX0() {
		// draw hyperbola wing from x=a to x=x0
		// the drawn hyperbola must be larger than the screen
		// get max distance from midpoint to screen edge
		x0 = Math.max(
				Math.max(Math.abs(midpoint.getX() - view.getXmin()),
						Math.abs(midpoint.getX() - view.getXmax())),
				Math.max(Math.abs(midpoint.getY() - view.getYmin()),
						Math.abs(midpoint.getY() - view.getYmax())));
		// ensure that rotated hyperbola is fully on screen:
		x0 *= 1.5;
	}

	/**
	 * add point to paths for hyperbola
	 * 
	 * @param index
	 *            index for the point
	 * @param x1
	 *            x coord
	 * @param y1
	 *            y coord
	 */
	protected void updateHyperbolaAddPoint(int index, double x1, double y1) {
		hypRight.addPoint(index, x1, y1);
		hypLeft.addPoint(index, -x1, y1);

	}

	/** build general paths of hyperbola wings and transform them */
	protected void updateHyperboalSetTransformToPaths() {

		hypLeft.transform(transform);
		hypRight.transform(transform);
	}

	/**
	 * close hyperbola branchs
	 */
	protected void updateHyperbolaClosePaths() {

		// we have drawn the hyperbola from x=a to x=x0
		// ensure correct filling by adding points at (2*x0, y)
		if (conic.getAlphaValue() > 0.0f || conic.isHatchingEnabled()) {
			hypRight.lineTo(Float.MAX_VALUE, y);
			hypRight.lineTo(Float.MAX_VALUE, -y);
			hypLeft.lineTo(-Float.MAX_VALUE, y);
			hypLeft.lineTo(-Float.MAX_VALUE, -y);
		}
	}

	/**
	 * set shape for hyperbola
	 */
	protected void updateHyperbolaSetShape() {
		setShape(AwtFactory.prototype.newArea(hypLeft));
		// geogebra.awt.Area.getAWTArea(super.getShape()).add(new
		// Area(geogebra.awt.GenericShape.getAwtShape(hypRight)));
		super.getShape().add(AwtFactory.prototype.newArea(hypRight));
	}

	/**
	 * draw only one edge for the parabola section
	 */
	protected void updateParabolaEdge() {
		// only used for conic section
		isVisible = false;
	}

	protected void updateParabola() {
		if (conic.p > DrawConic.HUGE_RADIUS) {
			isVisible = false;
			return;
		}

		// check if in view
		Coords M;
		if (isPreview) { // midpoint has been calculated in view coords
			M = conic.getMidpoint3D().getInhomCoords();
		} else {
			M = view.getCoordsForView(conic.getMidpoint3D());
			if (!Kernel.isZero(M.getZ())) {// check if in view
				isVisible = false;
				return;
			}
		}
		if (ev == null) {
			ev = new Coords[2];
		}
		if (isPreview) { // calculations were in view coords
			for (int j = 0; j < 2; j++) {
				ev[j] = conic.getEigenvec(j);
			}
		} else {
			for (int j = 0; j < 2; j++) {
				ev[j] = view.getCoordsForView(conic.getEigenvec3D(j));
				if (!Kernel.isZero(ev[j].getZ())) {// check if in view
					isVisible = false;
					return;
				}
			}
		}

		if (firstParabola) {
			firstParabola = false;
			parabola = AwtFactory.prototype.newGeneralPath();
		}

		updateParabolaX0Y0();

		// set transform
		transform.setTransform(view.getCoordTransform());
		transform.concatenate(view.getCompanion().getTransform(conic, M, ev));

		// setCurve(P0, P1, P2)
		// parabola.setCurve(x0, y0, -x0, 0.0, x0, -y0);
		// shape = transform.createTransformedShape(parabola);
		parpoints[0] = x0;
		parpoints[1] = y0;

		parpoints[2] = -x0 / 3;
		parpoints[3] = y0 / 3;

		parpoints[4] = -x0 / 3;
		parpoints[5] = -y0 / 3;

		parpoints[6] = x0;
		parpoints[7] = -y0;
		transform.transform(parpoints, 0, parpoints, 0, 4);

		updateParabolaPath();

		shape = parabola;

		updateParabolaLabelCoords();

		transform.transform(labelCoords, 0, labelCoords, 0, 1);
		xLabel = (int) labelCoords[0];
		yLabel = (int) labelCoords[1];
	}

	/**
	 * update label coords for parabola
	 */
	protected void updateParabolaLabelCoords() {
		// set label coords
		labelCoords[0] = 2 * conic.p;
		// y = 2p minus 20 pixels
		labelCoords[1] = labelCoords[0] - 20.0 / view.getYscale();

	}

	/** calc control points coords of parabola y^2 = 2 p x */
	protected void updateParabolaX0Y0() {

		x0 = Math.max(Math.abs(vertex.getX() - view.getXmin()),
				Math.abs(vertex.getX() - view.getXmax()));
		x0 = Math.max(x0, Math.abs(vertex.getY() - view.getYmin()));
		x0 = Math.max(x0, Math.abs(vertex.getY() - view.getYmax()));

		/*
		 * x0 *= 2.0d; // y^2 = 2px y0 = Math.sqrt(2*c.p*x0);
		 */

		// avoid sqrt by choosing x = k*p with
		// i = 2*k is quadratic number
		// make parabola big enough: k*p >= 2*x0 -> 2*k >= 4*x0/p
		x0 = 4 * x0 / conic.p;

		// changed these to doubles, see #654 y=x^2+100000x+1
		double i = 4;
		k2 = 16;

		while (k2 < x0) {
			i += 2;
			k2 = i * i;
		}
		x0 = k2 / 2 * conic.p; // x = k*p
		y0 = i * conic.p; // y = sqrt(2k p^2) = i p
	}

	/**
	 * create path for parabola
	 */
	protected void updateParabolaPath() {
		parabola.reset();
		parabola.moveTo((float) parpoints[0], (float) parpoints[1]);
		parabola.curveTo((float) parpoints[2], (float) parpoints[3],
				(float) parpoints[4], (float) parpoints[5],
				(float) parpoints[6], (float) parpoints[7]);
	}

	@Override
	final public void draw(org.geogebra.common.awt.GGraphics2D g2) {
		if (!isVisible)
			return;
		g2.setColor(getObjectColor());
		switch (type) {
		case GeoConicNDConstants.CONIC_SINGLE_POINT:
			int pointType;
			if ((conic == geo && conic.isInverseFill())
					|| geo.isInverseFill() != conic.isInverseFill()) {
				fill(g2, getShape(), false);
				pointType = EuclidianStyleConstants.POINT_STYLE_CIRCLE;
			} else {
				pointType = EuclidianStyleConstants.POINT_STYLE_DOT;
			}
			if (!ignoreSingularities) {
				drawPoint.setPointStyle(pointType);
				drawPoint.draw(g2);
			}
			break;

		case GeoConicNDConstants.CONIC_INTERSECTING_LINES:
		case GeoConicNDConstants.CONIC_DOUBLE_LINE:
		case GeoConicNDConstants.CONIC_PARALLEL_LINES:
			drawLines(g2);
			break;

		case GeoConicNDConstants.CONIC_LINE:
			drawLines[0].draw(g2);
			break;
		case GeoConicNDConstants.CONIC_EMPTY:
			if (conic.isInverseFill()) {
				fill(g2, getShape(), false);
			}
			break;
		case GeoConicNDConstants.CONIC_CIRCLE:
		case GeoConicNDConstants.CONIC_ELLIPSE:
		case GeoConicNDConstants.CONIC_PARABOLA:

			if (conic.isInverseFill()) {
				fill(g2, getShape(), false);
			} else {
				fill(g2, shape, false); // fill using default/hatching/image as
										// appropriate
			}
			if (arcFiller != null)
				fill(g2, arcFiller, true); // fill using default/hatching/image
											// as appropriate

			if (geo.doHighlighting()) {
				g2.setStroke(selStroke);
				g2.setColor(geo.getSelColor());
				g2.draw(shape);
			}

			g2.setStroke(objStroke);
			g2.setColor(getObjectColor());
			if (geo.getLineThickness() > 0) {
				g2.draw(shape);
			}
			if (labelVisible) {
				g2.setFont(view.getFontConic());
				g2.setColor(geo.getLabelColor());
				drawLabel(g2);
			}
			break;

		case GeoConicNDConstants.CONIC_HYPERBOLA:
			drawHyperbola(g2);
			break;
		}
	}

	/**
	 * draw lines
	 * 
	 * @param g2
	 *            graphic context
	 */
	protected void drawLines(org.geogebra.common.awt.GGraphics2D g2) {
		if (geo.getLineThickness() > 0) {
			drawLines[0].draw(g2);
			drawLines[1].draw(g2);
		}
		if (conic.isInverseFill()) {
			fill(g2, getShape(), false);
		} else
			fill(g2, shape, false);
	}

	/**
	 * draw hyperbola
	 * 
	 * @param g2
	 *            graphic context
	 */
	protected void drawHyperbola(org.geogebra.common.awt.GGraphics2D g2) {
		if (conic.isInverseFill()) {
			org.geogebra.common.awt.GArea a1 = AwtFactory.prototype
					.newArea(hypLeft);
			org.geogebra.common.awt.GArea a2 = AwtFactory.prototype
					.newArea(hypRight);
			org.geogebra.common.awt.GArea complement = AwtFactory.prototype
					.newArea(view.getBoundingPath());
			complement.subtract(a1);
			complement.subtract(a2);
			fill(g2, complement, false);
		} else {
			if (hypLeftOnScreen)
				fill(g2, hypLeft, true);
			if (hypRightOnScreen)
				fill(g2, hypRight, true);
		}

		if (geo.doHighlighting()) {
			g2.setStroke(selStroke);
			g2.setColor(geo.getSelColor());

			if (hypLeftOnScreen)
				g2.drawWithValueStrokePure(hypLeft);
			if (hypRightOnScreen)
				g2.drawWithValueStrokePure(hypRight);
		}
		g2.setStroke(objStroke);
		g2.setColor(getObjectColor());
		if (geo.getLineThickness() > 0) {
			if (hypLeftOnScreen)
				g2.drawWithValueStrokePure(hypLeft);
			if (hypRightOnScreen)
				g2.drawWithValueStrokePure(hypRight);
		}

		if (labelVisible) {
			g2.setFont(view.getFontConic());
			g2.setColor(geo.getLabelColor());
			drawLabel(g2);
		}
	}

	/**
	 * Returns the bounding box of this Drawable in screen coordinates.
	 * 
	 * @return null when this Drawable is infinite or undefined
	 */
	@Override
	final public org.geogebra.common.awt.GRectangle getBounds() {
		if (!geo.isDefined() || !geo.isEuclidianVisible())
			return null;

		switch (type) {
		case GeoConicNDConstants.CONIC_SINGLE_POINT:
			return drawPoint.getBounds();

		case GeoConicNDConstants.CONIC_CIRCLE:
		case GeoConicNDConstants.CONIC_ELLIPSE:
			return shape.getBounds();
		case GeoConicNDConstants.CONIC_PARABOLA:
		case GeoConicNDConstants.CONIC_HYPERBOLA:
			// might need another formula for flat hyperbolae, max() prevents
			// flattening of xx-yy=1000
			double focX = Math.max(
					Math.abs(conic.linearEccentricity
							* conic.eigenvec[0].getX()),
					Math.abs(conic.linearEccentricity
							* conic.eigenvec[0].getY()));
			int xmin = view.toScreenCoordX(midpoint.getX()
 - focX);
			int xmax = view.toScreenCoordX(midpoint.getX()
 + focX);
			int ymin = view.toScreenCoordY(midpoint.getY()
 - focX);
			int ymax = view.toScreenCoordY(midpoint.getY()
 + focX);

			return AwtFactory.prototype.newRectangle(xmin, ymax, xmax - xmin,
					ymin - ymax);
		default:
			return null;
		}
	}

	@Override
	final public void drawTrace(GGraphics2D g2) {
		g2.setColor(getObjectColor());
		switch (type) {
		case GeoConicNDConstants.CONIC_SINGLE_POINT:
			drawPoint.drawTrace(g2);
			break;

		case GeoConicNDConstants.CONIC_INTERSECTING_LINES:
		case GeoConicNDConstants.CONIC_DOUBLE_LINE:
		case GeoConicNDConstants.CONIC_PARALLEL_LINES:
			drawLines[0].drawTrace(g2);
			drawLines[1].drawTrace(g2);
			break;

		case GeoConicNDConstants.CONIC_LINE:
			drawLines[0].drawTrace(g2);
			break;

		case GeoConicNDConstants.CONIC_CIRCLE:
		case GeoConicNDConstants.CONIC_ELLIPSE:
		case GeoConicNDConstants.CONIC_PARABOLA:
			g2.setStroke(objStroke);
			g2.setColor(getObjectColor());
			g2.draw(shape);
			break;

		case GeoConicNDConstants.CONIC_HYPERBOLA:
			g2.setStroke(objStroke);
			g2.setColor(getObjectColor());
			g2.draw(hypLeft);
			g2.draw(hypRight);
			break;
		}
	}

	/**
	 * 
	 * @return true if it has to check it's on filling
	 */
	protected boolean checkIsOnFilling() {
		return isFilled() && type != GeoConicNDConstants.CONIC_SINGLE_POINT
				&& type != GeoConicNDConstants.CONIC_DOUBLE_LINE;
	}

	@Override
	final public boolean hit(int hitX, int hitY, int hitThreshold) {
		if (!isVisible)
			return false;
		// set a flag that says if the point is on the filling
		boolean isOnFilling = false;
		if (checkIsOnFilling()) {
			double realX = view.toRealWorldCoordX(hitX);
			double realY = view.toRealWorldCoordY(hitY);
			double x3 = view.toRealWorldCoordX(3) - view.toRealWorldCoordX(0);
			double y3 = view.toRealWorldCoordY(3) - view.toRealWorldCoordY(0);
			int insideNeigbors = (conic.isInRegion(realX, realY) ? 1 : 0)
					+ (conic.isInRegion(realX - x3, realY - y3) ? 1 : 0)
					+ (conic.isInRegion(realX + x3, realY - y3) ? 1 : 0)
					+ (conic.isInRegion(realX - x3, realY + y3) ? 1 : 0)
					+ (conic.isInRegion(realX + x3, realY + y3) ? 1 : 0);
			if (conic.isInverseFill())
				isOnFilling = (insideNeigbors < 5);
			else
				isOnFilling = (insideNeigbors > 0);
		}
		// set a flag to say if point is on the boundary
		boolean isOnBoundary = false;
		switch (type) {
		case GeoConicNDConstants.CONIC_SINGLE_POINT:
			isOnBoundary = drawPoint.hit(hitX, hitY, hitThreshold);
			break;
		case GeoConicNDConstants.CONIC_INTERSECTING_LINES:
		case GeoConicNDConstants.CONIC_DOUBLE_LINE:
		case GeoConicNDConstants.CONIC_PARALLEL_LINES:
			isOnBoundary = hitLines(hitX, hitY, hitThreshold);
			break;
		case GeoConicNDConstants.CONIC_LINE:
			isOnBoundary = drawLines[0].hit(hitX, hitY, hitThreshold);
			break;
		case GeoConicNDConstants.CONIC_CIRCLE:
		case GeoConicNDConstants.CONIC_PARABOLA:
			if (strokedShape == null) {
				strokedShape = objStroke.createStrokedShape(shape);
			}
			isOnBoundary = strokedShape.intersects(hitX - hitThreshold, hitY
					- hitThreshold, 2 * hitThreshold, 2 * hitThreshold);
			break;

		case GeoConicNDConstants.CONIC_ELLIPSE:
			isOnBoundary = hitEllipse(hitX, hitY, hitThreshold);
			break;

		case GeoConicNDConstants.CONIC_HYPERBOLA:
			isOnBoundary = hitHyperbola(hitX, hitY, hitThreshold);
			break;
		}

		// Application.debug("isOnFilling="+isOnFilling+"\nisOnBoundary="+isOnBoundary);
		if (isOnFilling) {
			if (isOnBoundary) {
				conic.setLastHitType(HitType.ON_BOUNDARY);
				return true;
			}
			conic.setLastHitType(HitType.ON_FILLING);
			return true;
		}
		if (isOnBoundary) {
			conic.setLastHitType(HitType.ON_BOUNDARY);
			return true;
		}
		conic.setLastHitType(HitType.NONE);
		return false;
	}

	/**
	 * Says if the coords hit lines
	 * 
	 * @param hitX
	 *            x coord for hit
	 * @param hitY
	 *            y coord for hit
	 * @param hitThreshold
	 *            acceptable distance from line
	 * @return true if lines are hit
	 */
	public boolean hitLines(int hitX, int hitY, int hitThreshold) {
		return drawLines[0].hit(hitX, hitY, hitThreshold)
				|| drawLines[1].hit(hitX, hitY, hitThreshold);
	}

	/**
	 * Says if the coords hit hyperbola
	 * 
	 * @param hitX
	 *            x coord for hit
	 * @param hitY
	 *            y coord for hit
	 * @return true if lines are hitted
	 * @param hitThreshold
	 *            acceptable distance from line
	 */
	public boolean hitHyperbola(int hitX, int hitY, int hitThreshold) {
		if (strokedShape == null) {
			strokedShape = objStroke.createStrokedShape(hypLeft);
			strokedShape2 = objStroke.createStrokedShape(hypRight);
		}
		return strokedShape.intersects(hitX - hitThreshold,
				hitY - hitThreshold, 2 * hitThreshold, 2 * hitThreshold)
				|| strokedShape2.intersects(hitX - hitThreshold, hitY
						- hitThreshold, 2 * hitThreshold, 2 * hitThreshold);

	}

	/**
	 * Says if the coords hit ellipse
	 * 
	 * @param hitX
	 *            x coord for hit
	 * @param hitY
	 *            y coord for hit
	 * @return true if lines are hitted
	 * @param hitThreshold
	 *            acceptable distance from line
	 */
	public boolean hitEllipse(int hitX, int hitY, int hitThreshold) {
		if (strokedShape == null) {
			strokedShape = objStroke.createStrokedShape(shape);
		}
		return strokedShape.intersects(hitX - hitThreshold,
				hitY - hitThreshold, 2 * hitThreshold, 2 * hitThreshold);
	}

	@Override
	final public boolean isInside(org.geogebra.common.awt.GRectangle rect) {
		switch (type) {
		case GeoConicNDConstants.CONIC_SINGLE_POINT:
			return drawPoint.isInside(rect);

		case GeoConicNDConstants.CONIC_CIRCLE:
		case GeoConicNDConstants.CONIC_ELLIPSE:
			return rect != null && shape != null
					&& rect.contains(shape.getBounds());
		}

		return false;
	}

	@Override
	public boolean intersectsRectangle(GRectangle rect) {
		if (type == GeoConicNDConstants.CONIC_SINGLE_POINT) {
			return drawPoint.intersectsRectangle(rect);
		}
		if (type == GeoConicNDConstants.CONIC_DOUBLE_LINE) {
			return drawLines[0].intersectsRectangle(rect)
					|| drawLines[1].intersectsRectangle(rect);
		}
		if (isFilled()) {
			return super.intersectsRectangle(rect);
		}
		if (shape != null) {
			return shape.intersects(rect) && !shape.contains(rect);
		}
		if (super.getShape() != null) {
			return super.getShape().intersects(rect)
					&& !super.getShape().contains(rect);
		}
		return false;
	}

	@Override
	public GeoElement getGeoElement() {
		return geo;
	}

	@Override
	public void setGeoElement(GeoElement geo) {
		this.geo = geo;
		if (drawLines != null)
			for (int i = 0; i < 2 && drawLines[i] != null; i++)
				drawLines[i].setGeoElement(geo);
	}

	private void initPreview() {
		// init the conic for preview
		Construction cons = previewTempPoints[0].getConstruction();
		isPreview = true;

		switch (previewMode) {
		case EuclidianConstants.MODE_CIRCLE_TWO_POINTS:
			AlgoCircleTwoPoints algo = new AlgoCircleTwoPoints(cons,
					previewTempPoints[0], previewTempPoints[1]);
			cons.removeFromConstructionList(algo);
			initConic(algo.getCircle());
			break;

		case EuclidianConstants.MODE_CIRCLE_POINT_RADIUS:
			Coords p = view.getCoordsForView(prevPoints.get(0)
					.getInhomCoordsInD3());
			previewTempPoints[0].setCoords(p.projectInfDim(), false);

			MyDouble distance = new MyDouble(cons.getKernel(),
					previewTempPoints[1].distance(previewTempPoints[0]));
			AlgoCirclePointRadius algoCircleRadius = new AlgoCirclePointRadius(
					cons,
					previewTempPoints[0], distance);
			cons.removeFromConstructionList(algoCircleRadius);
			initConic(algoCircleRadius.getCircle());
			break;

		case EuclidianConstants.MODE_CONIC_FIVE_POINTS:
			GeoPoint[] pts = { previewTempPoints[0], previewTempPoints[1],
					previewTempPoints[2], previewTempPoints[3],
					previewTempPoints[4] };
			AlgoConicFivePoints algo0 = new AlgoConicFivePoints(cons, pts);
			cons.removeFromConstructionList(algo0);
			initConic(algo0.getConic());
			break;

		case EuclidianConstants.MODE_CIRCLE_THREE_POINTS:
			AlgoCircleThreePoints algo2 = new AlgoCircleThreePoints(cons,
					previewTempPoints[0], previewTempPoints[1],
					previewTempPoints[2]);
			cons.removeFromConstructionList(algo2);
			initConic(algo2.getCircle());
			break;

		case EuclidianConstants.MODE_ELLIPSE_THREE_POINTS:
			AlgoEllipseHyperbolaFociPoint algo3 = new AlgoEllipseHyperbolaFociPoint(
					cons, previewTempPoints[0], previewTempPoints[1],
					previewTempPoints[2], GeoConicNDConstants.CONIC_ELLIPSE);
			cons.removeFromConstructionList(algo3);
			initConic(algo3.getConic());
			break;

		case EuclidianConstants.MODE_HYPERBOLA_THREE_POINTS:
			AlgoEllipseHyperbolaFociPoint algo4 = new AlgoEllipseHyperbolaFociPoint(
					cons, previewTempPoints[0], previewTempPoints[1],
					previewTempPoints[2], GeoConicNDConstants.CONIC_HYPERBOLA);
			cons.removeFromConstructionList(algo4);
			initConic(algo4.getConic());
			break;

		case EuclidianConstants.MODE_COMPASSES:
			AlgoCirclePointRadius algo5 = new AlgoCirclePointRadius(cons,
					previewTempPoints[0], previewTempRadius);
			cons.removeFromConstructionList(algo5);
			initConic(algo5.getCircle());
			break;

		case EuclidianConstants.MODE_PARABOLA:
			AlgoParabolaPointLine algo6 = new AlgoParabolaPointLine(cons,
					previewTempPoints[0], previewTempLine);
			cons.removeFromConstructionList(algo6);
			initConic(algo6.getParabola());
			break;

		default:
			App.debug("unknown conic type");
		}

		if (conic != null)
			conic.setLabelVisible(false);
	}

	// preview of circle with midpoint through a second point
	final public void updatePreview() {

		switch (previewMode) {
		case EuclidianConstants.MODE_COMPASSES:
			// compass: set radius of preview circle
			// two points or one segment selected to define radius
			isVisible = conic != null
					&& (prevPoints.size() == 2 || prevSegments.size() == 1 || prevConics
							.size() == 1);
			if (isVisible) {
				if (prevPoints.size() == 2) {
					GeoPointND p1 = prevPoints.get(0);
					GeoPointND p2 = prevPoints.get(1);
					previewTempRadius.setValue(p1.distance(p2));
				} else if (prevSegments.size() == 1) {
					GeoSegmentND seg = prevSegments.get(0);
					previewTempRadius.setValue(seg.getLength());
				} else if (prevConics.size() == 1) {
					GeoConicND prevCircle = prevConics.get(0);
					previewTempRadius.setValue(prevCircle.getCircleRadius());
				}
				previewTempRadius.updateCascade();
			}
			break;

		case EuclidianConstants.MODE_PARABOLA:

			isVisible = prevLines.size() == 1;

			if (prevLines.size() > 0) {
				GeoLineND lND = prevLines.get(0);
				Coords equation = lND.getCartesianEquationVector(view
						.getMatrix());
				previewTempLine.setCoords(equation.getX(), equation.getY(),
						equation.getZ());
			}

			if (prevPoints.size() > 0) {
				Coords p = view.getCoordsForView(prevPoints.get(0)
						.getInhomCoordsInD3());
				// Application.debug("p["+i+"]=\n"+p);
				previewTempPoints[0].setCoords(p.projectInfDim(), true);

				previewTempPoints[0].updateCascade();
			}

			break;

		case EuclidianConstants.MODE_CIRCLE_POINT_RADIUS:
			isVisible = conic != null && prevPoints.size() == neededPrevPoints;
			if (isVisible) {
				Coords p = view.getCoordsForView(prevPoints.get(0)
						.getInhomCoordsInD3());
				previewTempPoints[0].setCoords(p.projectInfDim(), false);

				Construction cons = previewTempPoints[0].getConstruction();
				MyDouble distance = new MyDouble(cons.getKernel(),
						previewTempPoints[1].distance(previewTempPoints[0]));
				AlgoCirclePointRadius algoCircleRadius = new AlgoCirclePointRadius(
						cons, previewTempPoints[0], distance);
				cons.removeFromConstructionList(algoCircleRadius);
				initConic(algoCircleRadius.getCircle());
				this.conic.updateCascade();
			}
			break;

		default:
			// all other conic preview modes: use points to define preview conic
			isVisible = conic != null && prevPoints.size() == neededPrevPoints;
			if (isVisible) {
				for (int i = 0; i < prevPoints.size(); i++) {
					Coords p = view.getCoordsForView(prevPoints.get(i)
							.getInhomCoordsInD3());
					// App.debug("p["+i+"]=\n"+p);
					previewTempPoints[i].setCoords(p.projectInfDim(), false);
				}
				previewTempPoints[0].updateCascade();
			}

		}

	}

	final public void updateMousePos(double xRW, double yRW) {
		if (isVisible) {
			// double xRW = view.toRealWorldCoordX(x);
			// double yRW = view.toRealWorldCoordY(y);
			previewTempPoints[previewTempPoints.length - 1].setCoords(xRW, yRW,
					1.0);
			previewTempPoints[previewTempPoints.length - 1].updateCascade();
			update();
		}
	}

	final public void drawPreview(org.geogebra.common.awt.GGraphics2D g2) {
		draw(g2);
	}

	public void disposePreview() {
		if (conic != null) {
			conic.remove();
		}
	}

	/**
	 * Returns the conic to be draw (might not be equal to geo, if this is part
	 * of bigger geo)
	 * 
	 * @return conic
	 */
	public GeoConicND getConic() {
		return conic;
	}

	/**
	 * @param ignore
	 *            to avoid drawing single point if part of ineq
	 */
	public void setIgnoreSingularities(boolean ignore) {
		this.ignoreSingularities = ignore;

	}
}
