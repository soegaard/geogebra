/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

package org.geogebra.common.kernel.algos;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;

import org.geogebra.common.euclidian.EuclidianConstants;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.FixedPathRegionAlgo;
import org.geogebra.common.kernel.LocusEquation;
import org.geogebra.common.kernel.Path;
import org.geogebra.common.kernel.PathNormalizer;
import org.geogebra.common.kernel.PathParameter;
import org.geogebra.common.kernel.RestrictionAlgoForLocusEquation;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.arithmetic.NumberValue;
import org.geogebra.common.kernel.commands.Commands;
import org.geogebra.common.kernel.geos.GeoConic;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.geos.GeoSegment;
import org.geogebra.common.kernel.kernelND.GeoPointND;
import org.geogebra.common.kernel.prover.NoSymbolicParametersException;
import org.geogebra.common.kernel.prover.polynomial.Polynomial;
import org.geogebra.common.kernel.prover.polynomial.Variable;

public class AlgoPointOnPath extends AlgoElement implements FixedPathRegionAlgo,
		SymbolicParametersAlgo, SymbolicParametersBotanaAlgo,
		RestrictionAlgoForLocusEquation {

	private Path path; // input
	protected GeoPointND P; // output
	private NumberValue param;
	private Polynomial[] polynomials;
	private Polynomial[] botanaPolynomials;
	private Variable variable;
	private Variable[] botanaVars;

	public AlgoPointOnPath(Construction cons, String label, Path path,
			double x, double y) {

		this(cons, label, path, x, y, 0);

	}

	public AlgoPointOnPath(Construction cons, String label, Path path,
			double x, double y, double z) {

		this(cons, path, x, y, z, true);

		P.setLabel(label);
	}

	/**
	 * @author Tam
	 * 
	 *         for special cases of e.g. AlgoIntersectLineConic
	 */
	private void addIncidence() {
		P.addIncidence((GeoElement) path, false);

	}

	public AlgoPointOnPath(Construction cons, String label, Path path,
			double x, double y, NumberValue param) {

		this(cons, label, path, x, y, 0, param);

	}

	public AlgoPointOnPath(Construction cons, String label, Path path,
			double x, double y, double z, NumberValue param) {

		this(cons, path, x, y, z, param);
		P.setLabel(label);
	}

	public AlgoPointOnPath(Construction cons, Path path, double x, double y,
			NumberValue param) {

		this(cons, path, x, y, 0, param);

	}

	public AlgoPointOnPath(Construction cons, Path path, double x, double y,
			double z, NumberValue param) {
		super(cons);
		this.path = path;

		// create point on path and compute current location
		createPoint(path, x, y, z);

		this.param = param;

		setInputOutput(); // for AlgoElement
		compute();
		addIncidence();
	}

	public AlgoPointOnPath(Construction cons, Path path, double x, double y) {

		this(cons, path, x, y, 0, true);

	}

	public AlgoPointOnPath(Construction cons, Path path, double x, double y,
			double z, boolean addIncidence) {
		super(cons, addIncidence);
		this.path = path;

		// create point on path and compute current location
		createPoint(path, x, y, z);

		setInputOutput(); // for AlgoElement
		if (addIncidence) {
			addIncidence();
		} else {
			P.setEuclidianVisible(false);
		}
	}

	protected void createPoint(Path path, double x, double y, double z) {

		P = new GeoPoint(cons);
		P.setPath(path);
		P.setCoords(x, y, 1.0);

	}

	@Override
	public Commands getClassName() {
		return Commands.Point;
	}

	@Override
	public int getRelatedModeID() {
		return EuclidianConstants.MODE_POINT;
	}

	// for AlgoElement
	@Override
	protected void setInputOutput() {
		if (param == null) {
			input = new GeoElement[1];
			input[0] = path.toGeoElement();
		} else {
			input = new GeoElement[2];
			input[0] = path.toGeoElement();
			input[1] = param.toGeoElement();
		}
		setOutputLength(1);
		setOutput(0, (GeoElement) P);
		setDependencies(); // done by AlgoElement
	}

	public GeoPointND getP() {
		return P;
	}

	public Path getPath() {
		return path;
	}

	@Override
	public final void compute() {
		if (param != null) {
			PathParameter pp = P.getPathParameter();
			// Application.debug(param.getDouble()+" "+path.getMinParameter()+" "+path.getMaxParameter());
			pp.setT(PathNormalizer.toParentPathParameter(param.getDouble(),
					path.getMinParameter(), path.getMaxParameter()));
			// Application.debug(pp.t);
		}
		if (input[0].isDefined()) {
			path.pathChanged(P);
			P.updateCoords();
		} else {
			P.setUndefined();
		}
	}

	@Override
	final public String toString(StringTemplate tpl) {
		// Michael Borcherds 2008-03-30
		// simplified to allow better Chinese translation
		return getLoc().getPlain("PointOnA", input[0].getLabel(tpl));
	}

	public boolean isChangeable(GeoElement out) {
		return param == null;
	}

	public SymbolicParameters getSymbolicParameters() {
		return new SymbolicParameters(this);
	}

	public void getFreeVariables(HashSet<Variable> variables)
			throws NoSymbolicParametersException {
		if (input[0] instanceof GeoSegment) {
			throw new NoSymbolicParametersException();
		}
		if (input[0] instanceof GeoLine) {
			((SymbolicParametersAlgo) input[0]).getFreeVariables(variables);
			if (variable == null) {
				variable = new Variable((GeoElement) P);
			}
			variables.add(variable);
			return;
		}
		throw new NoSymbolicParametersException();
	}

	public int[] getDegrees() throws NoSymbolicParametersException {
		if (input[0] instanceof GeoSegment) {
			throw new NoSymbolicParametersException();
		}
		if (input[0] instanceof GeoLine) {
			int[] degreesLine = ((SymbolicParametersAlgo) input[0])
					.getDegrees();

			int[] result = new int[3];
			result[0] = degreesLine[2] + 1;
			result[1] = degreesLine[2] + 1;
			result[2] = Math.max(degreesLine[0] + 1, degreesLine[1] + 1);
			return result;
		}
		throw new NoSymbolicParametersException();
	}

	public BigInteger[] getExactCoordinates(HashMap<Variable, BigInteger> values)
			throws NoSymbolicParametersException {
		if (input[0] instanceof GeoSegment) {
			throw new NoSymbolicParametersException();
		}
		if (input[0] instanceof GeoLine && variable != null) {
			BigInteger[] exactCoordinates = new BigInteger[3];
			BigInteger[] line = ((SymbolicParametersAlgo) input[0])
					.getExactCoordinates(values);
			exactCoordinates[0] = line[2].multiply(values.get(variable));
			exactCoordinates[1] = line[2].multiply(BigInteger.ONE
					.subtract(values.get(variable)));
			exactCoordinates[2] = line[0].multiply(
					values.get(variable).negate()).add(
					line[1].multiply(values.get(variable).subtract(
							BigInteger.ONE)));
			return exactCoordinates;
		}
		return null;
	}

	public Polynomial[] getPolynomials() throws NoSymbolicParametersException {
		if (polynomials != null) {
			return polynomials;
		}
		if (input[0] instanceof GeoSegment) {
			throw new NoSymbolicParametersException();
		}
		if (path instanceof GeoLine) {
			if (variable == null) {
				variable = new Variable((GeoElement) P);
			}
			polynomials = new Polynomial[3];
			Polynomial[] line = ((SymbolicParametersAlgo) input[0])
					.getPolynomials();
			polynomials[0] = line[2].multiply(new Polynomial(variable));
			polynomials[1] = line[2].multiply((new Polynomial(1))
					.subtract(new Polynomial(variable)));
			polynomials[2] = line[0].multiply(
					(new Polynomial(variable)).negate()).add(
					line[1].multiply((new Polynomial(variable))
							.subtract(new Polynomial(1))));
			return polynomials;

		}
		throw new NoSymbolicParametersException();
	}

	public Polynomial[] getBotanaPolynomials(GeoElement geo)
			throws NoSymbolicParametersException {
		if (botanaPolynomials != null) {
			return botanaPolynomials;
		}

		if (input[0] != null && input[0] instanceof GeoLine) {
			if (botanaVars == null) {
				botanaVars = new Variable[2];
				botanaVars[0] = new Variable(true);
				botanaVars[1] = new Variable();
			}
			Variable[] fv = ((SymbolicParametersBotanaAlgo) input[0])
					.getBotanaVars(input[0]); // 4 variables
			botanaPolynomials = new Polynomial[1];
			botanaPolynomials[0] = Polynomial.collinear(fv[0], fv[1], fv[2],
					fv[3], botanaVars[0], botanaVars[1]);
			return botanaPolynomials;
		}

		if (input[0] != null && input[0] instanceof GeoConic) {
			if (((GeoConic) input[0]).isCircle()) {
				if (botanaVars == null) {
					botanaVars = new Variable[2];
					botanaVars[0] = new Variable(true);
					botanaVars[1] = new Variable();
				}
				Variable[] fv = ((SymbolicParametersBotanaAlgo) input[0])
						.getBotanaVars(input[0]); // 4 variables
				botanaPolynomials = new Polynomial[1];
				// If this new point is D, and ABC is already a triangle with
				// the circumcenter O,
				// then here we must claim that e.g. AO=OD:
				botanaPolynomials[0] = Polynomial.equidistant(fv[2], fv[3],
						fv[0], fv[1], botanaVars[0], botanaVars[1]);
				return botanaPolynomials;
			}
			if (((GeoConic) input[0]).isParabola()) {
				if (botanaVars == null) {
					botanaVars = new Variable[4];
					// point P on parabola
					botanaVars[0] = new Variable(true);
					botanaVars[1] = new Variable();
					// T- projection of P on AB
					botanaVars[2] = new Variable();
					botanaVars[3] = new Variable();
				}
				Variable[] vparabola = ((SymbolicParametersBotanaAlgo) input[0])
						.getBotanaVars(input[0]);
				botanaPolynomials = new Polynomial[3];

				// FP = PT
				botanaPolynomials[0] = Polynomial.equidistant(vparabola[8],
						vparabola[9], botanaVars[0], botanaVars[1],
						botanaVars[2], botanaVars[3]);

				// A,T,B collinear
				botanaPolynomials[1] = Polynomial.collinear(vparabola[4],
						vparabola[5], botanaVars[2], botanaVars[3],
						vparabola[6], vparabola[7]);

				// PT orthogonal AB
				botanaPolynomials[2] = Polynomial.perpendicular(botanaVars[0],
						botanaVars[1], botanaVars[2], botanaVars[3],
						vparabola[4], vparabola[5], vparabola[6], vparabola[7]);

				return botanaPolynomials;
			}
			if (((GeoConic) input[0]).isEllipse()
					|| ((GeoConic) input[0]).isHyperbola()) {
				if (botanaVars == null) {
					botanaVars = new Variable[4];
					// P - point on ellipse/hyperbola
					botanaVars[0] = new Variable(true);
					botanaVars[1] = new Variable();
					// distances between point on ellipse/hyperbola
					// and foci points
					botanaVars[2] = new Variable();
					botanaVars[3] = new Variable();
				}

				Variable[] vellipse = ((SymbolicParametersBotanaAlgo) input[0])
						.getBotanaVars(input[0]);

				botanaPolynomials = new Polynomial[3];

				Polynomial e_1 = new Polynomial(botanaVars[2]);
				Polynomial e_2 = new Polynomial(botanaVars[3]);
				Polynomial d1 = new Polynomial(vellipse[2]);
				Polynomial d2 = new Polynomial(vellipse[3]);
				
				// d1+d2 = e1'+e2'
				botanaPolynomials[0] = d1.add(d2).subtract(e_1).subtract(e_2);

				// e1'^2=Polynomial.sqrDistance(a1,a2,p1,p2)
				botanaPolynomials[1] = Polynomial.sqrDistance(botanaVars[0],
						botanaVars[1], vellipse[6], vellipse[7]).subtract(
						e_1.multiply(e_1));

				// e2'^2=Polynomial.sqrDistance(b1,b2,p1,p2)
				botanaPolynomials[2] = Polynomial.sqrDistance(botanaVars[0],
						botanaVars[1], vellipse[8], vellipse[9]).subtract(
						e_2.multiply(e_2));

				return botanaPolynomials;

			}
		}

		throw new NoSymbolicParametersException();
	}

	public Variable[] getBotanaVars(GeoElement geo) {
		return botanaVars;
	}

	@Override
	public boolean isLocusEquable() {
		return true;
	}

	public EquationElementInterface buildEquationElementForGeo(GeoElement geo,
			EquationScopeInterface scope) {
		return LocusEquation.eqnPointOnPath(geo, this, scope);
	}
}
