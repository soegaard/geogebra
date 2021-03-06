package org.geogebra.common.geogebra3D.euclidian3D.openGL;

import org.geogebra.common.geogebra3D.euclidian3D.openGL.ManagerShaders.TypeElement;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.Matrix.Coords;
import org.geogebra.common.kernel.Matrix.Coords3;
import org.geogebra.common.util.debug.Log;

public class PlotterSurfaceElements extends PlotterSurface {

	final private static boolean DEBUG = false;

	final private static void debug(String s) {
		if (DEBUG) {
			Log.debug(s);
		}
	}

	private int latitudeMin;
	private int latitude;
	private int latitudeMax;
	private int latitudeMaxTop;
	private int latitudeMaxBottom;
	
	private interface DrawSphereEllipsoid {
		public void drawNCr(Coords normal);

		public void drawNCrm(Coords normal);
	}

	private class DrawSphere implements DrawSphereEllipsoid {

		private PlotterSurface surface;
		private Coords center;
		private double radius;

		public DrawSphere() {
		}

		public void set(PlotterSurface surface, Coords center, double radius) {
			this.surface = surface;
			this.center = center;
			this.radius = radius;
		}

		public void drawNCr(Coords normal) {
			surface.drawNCr(normal, center, radius);
		}

		public void drawNCrm(Coords normal) {
			surface.drawNCrm(normal, center, radius);
		}
	}

	private class DrawEllipsoid implements DrawSphereEllipsoid {

		private PlotterSurface surface;
		private Coords center;
		private Coords ev0, ev1, ev2;
		private double r0, r1, r2;

		private Coords c = Coords.createInhomCoorsInD3();
		private Coords n = new Coords(4);
		private Coords tmpCoords = new Coords(4);

		public DrawEllipsoid() {
		}

		public void set(PlotterSurface surface, Coords center, Coords ev0,
				Coords ev1, Coords ev2, double r0, double r1, double r2) {
			this.surface = surface;
			this.center = center;
			this.ev0 = ev0;
			this.ev1 = ev1;
			this.ev2 = ev2;
			this.r0 = r0;
			this.r1 = r1;
			this.r2 = r2;
		}

		public void drawNCr(Coords normal) {
			c.setValues(center, 3);
			tmpCoords.setMul(ev0, r0 * normal.getX());
			c.addInside(tmpCoords);
			tmpCoords.setMul(ev1, r1 * normal.getY());
			c.addInside(tmpCoords);
			tmpCoords.setMul(ev2, r2 * normal.getZ());
			c.addInside(tmpCoords);

			n.setMul(ev0, r1 * r2 * normal.getX());
			tmpCoords.setMul(ev1, r0 * r2 * normal.getY());
			n.addInside(tmpCoords);
			tmpCoords.setMul(ev2, r0 * r1 * normal.getZ());
			n.addInside(tmpCoords);
			n.normalize();

			surface.drawNV(n, c);
		}

		public void drawNCrm(Coords normal) {
			c.setValues(center, 3);
			tmpCoords.setMul(ev0, r0 * normal.getX());
			c.addInside(tmpCoords);
			tmpCoords.setMul(ev1, r1 * normal.getY());
			c.addInside(tmpCoords);
			tmpCoords.setMul(ev2, -r2 * normal.getZ());
			c.addInside(tmpCoords);

			n.setMul(ev0, r1 * r2 * normal.getX());
			tmpCoords.setMul(ev1, r0 * r2 * normal.getY());
			n.addInside(tmpCoords);
			tmpCoords.setMul(ev2, -r0 * r1 * normal.getZ());
			n.addInside(tmpCoords);
			n.normalize();

			surface.drawNV(n, c);
		}
	}

	public PlotterSurfaceElements(Manager manager) {
		super(manager);
	}

	@Override
	public void drawSphere(Coords center, double radius, int longitude,
			double longitudeStart, int longitudeLength) {

		setLatitudeLongitudeMinMax(center, radius, longitude);

		if (drawSphere == null) {
			drawSphere = new DrawSphere();
		}
		drawSphere.set(this, center, radius);

		drawSphereEllipsoidNV(drawSphere, longitude, longitudeStart,
				longitudeLength);

		setSphereEllipsoidIndices(longitude, longitudeLength);
	}

	private void setLatitudeLongitudeMinMax(Coords center, double radius,
			int longitude) {

		manager.startGeometry(Manager.Type.TRIANGLES);

		// set texture to (0,0)
		manager.setDummyTexture();

		latitude = longitude / 4;

		// check which parts are visible (latitudes)
		Coords o = manager.getView3D().getCenter();
		double frustumRadius = manager.getView3D().getFrustumRadius();

		double z = center.getZ();
		double zMin = o.getZ() - frustumRadius;
		double zMax = o.getZ() + frustumRadius;

		latitudeMaxTop = 0;
		latitudeMaxTop = latitude;
		if (Kernel.isGreater(z + radius, zMax)) {
			double angle = Math.asin((zMax - z) / radius);
			latitudeMaxTop = (int) (latitude * 2 * angle / Math.PI) + 2;
		}

		latitudeMaxBottom = 0;
		latitudeMaxBottom = latitude;
		if (Kernel.isGreater(zMin, z - radius)) {
			double angle = Math.asin((z - zMin) / radius);
			latitudeMaxBottom = (int) (latitude * 2 * angle / Math.PI) + 2;
		}

		// debug(latitudeMaxBottom+","+latitudeMaxTop);

		latitudeMax = Math.max(latitudeMaxTop, latitudeMaxBottom);
		if (latitudeMax > latitude) {
			latitudeMax = latitude;
		}

		latitudeMin = 0; // start on equator
		if (latitudeMaxTop < 0) { // start below equator
			latitudeMin = -latitudeMaxTop;
		} else if (latitudeMaxBottom < 0) { // start above equator
			latitudeMin = -latitudeMaxBottom;
		}

	}

	private Coords n;

	private DrawSphere drawSphere;


	private void drawSphereEllipsoidNV(DrawSphereEllipsoid dse, int longitude,
			double longitudeStart, int longitudeLength) {

		// start drawing
		if (n == null) {
			n = new Coords(4);
		}

		double[] cosSinV = new double[2];

		debug("longitude = " + longitude + " , longitudeLength = "
				+ longitudeLength);

		short lastStartIndex, lastLength, currentStartIndex, currentLength, lastBoth, both;
		boolean drawTop, drawBottom;
		int vi, nextJump, next;
		short shift;

		// ///////////////
		// draw vertices

		// first latitude
		cosSin(latitudeMin, latitude, cosSinV);
		for (int ui = 0; ui < longitudeLength; ui++) {
			sphericalCoords(ui, longitude, longitudeStart, cosSinV, n);
			dse.drawNCr(n);
		}
		
		arrayIndex = 0;

		lastStartIndex = 0;
		lastLength = (short) longitudeLength;
		currentStartIndex = lastStartIndex;
		currentLength = (short) longitudeLength;
		
		// both = 1 if only drawing up or down, both = 2 if drawing both
		drawTop = true;
		drawBottom = true;
		lastBoth = 1;
		both = 1;

		vi = latitudeMin + 1;
		nextJump = (int) (latitude / Math.PI);
		debug("latitude : " + latitude + " , latitude-nextJump : "
				+ (latitude - nextJump));
		next = 0;
		shift = 1;

		while (next < latitudeMax) {
			
			next = Math.min(latitudeMax, latitude - nextJump);
			debug("latitude : " + latitude + " , latitudeMin : "
					+ latitudeMin + " , next : " + next + " , latitudeMax : "
					+ latitudeMax);

			// until next jump
			while (vi < next) {

				drawTop = vi < latitudeMaxTop;
				drawBottom = vi < latitudeMaxBottom;

				cosSin(vi, latitude, cosSinV);
				for (int ui = 0; ui < longitudeLength; ui += shift) {
					sphericalCoords(ui, longitude, longitudeStart, cosSinV, n);
					if (drawTop) {// top vertices
						dse.drawNCr(n);
					}
					if (drawBottom) {// bottom vertices
						dse.drawNCrm(n);
					}
				}


				lastBoth = both;
				both = 0;
				if (drawTop) {// top vertices
					both++;
				}
				if (drawBottom) {// bottom vertices
					both++;
				}

				debug("vi : " + vi + " -- both : " + both);

				lastStartIndex = currentStartIndex;
				lastLength = currentLength;
				currentStartIndex += lastLength * lastBoth;


				if (drawTop) {// top triangles
					if (longitudeLength == longitude) {
						arrayIndex += 6 * lastLength;
					} else {
						arrayIndex += 6 * (lastLength - 1);
					}

				}

				if (drawBottom) {// bottom triangles
					if (longitudeLength == longitude) {
						arrayIndex += 6 * lastLength;
					} else {
						arrayIndex += 6 * (lastLength - 1);
					}
				}

				vi++;
			}

			// jump
			if (next > latitudeMin && next < latitudeMax) {

				shift *= 2;
				cosSin(vi, latitude, cosSinV);
				for (int ui = 0; ui < longitudeLength; ui += shift) {
					sphericalCoords(ui, longitude, longitudeStart, cosSinV, n);
					if (drawTop) {// top vertices
						dse.drawNCr(n);
					}
					if (drawBottom) {// bottom vertices
						dse.drawNCrm(n);
					}

				}

				lastBoth = both;

				lastStartIndex = currentStartIndex;
				lastLength = currentLength;
				currentStartIndex += lastLength * lastBoth;
				currentLength /= 2;

				if (drawTop) {// top triangles
					if (longitudeLength == longitude) {
						arrayIndex += 9 * currentLength;
					} else {
						arrayIndex += 9 * (currentLength - 1);
					}
				}

				if (drawBottom) {// bottom triangles
					if (longitudeLength == longitude) {
						arrayIndex += 9 * currentLength;
					} else {
						arrayIndex += 9 * (currentLength - 1);
					}
				}

				vi++;

				nextJump /= 2;
			}


		}

		lastBoth = both;

		lastStartIndex = currentStartIndex;
		lastLength = currentLength;
		currentStartIndex += lastLength * lastBoth;


		// north pole
		if (latitudeMax == latitude) {

			if (drawTop) {

				dse.drawNCr(Coords.VZ);
				
				if (longitudeLength == longitude) {
					arrayIndex += 3 * lastLength;
				} else {
					arrayIndex += 3 * (lastLength - 1);
				}

			}

			// south pole
			if (drawBottom) {

				dse.drawNCrm(Coords.VZ);

				if (longitudeLength == longitude) {
					arrayIndex += 3 * lastLength;
				} else {
					arrayIndex += 3 * (lastLength - 1);
				}

			}
		}

		debug("==== arrayIndex (1) = " + arrayIndex);

	}

	private void setSphereEllipsoidIndices(int longitude, int longitudeLength) {


		// ///////////////
		// set indices
		arrayI = manager.getCurrentGeometryIndices(arrayIndex);


		arrayIndex = 0;

		short lastStartIndex = 0;
		short lastLength = (short) longitudeLength;
		short currentStartIndex = lastStartIndex;
		short currentLength = (short) longitudeLength;

		// both = 1 if only drawing up or down, both = 2 if drawing both
		boolean drawTop = true;
		boolean drawBottom = true;
		short lastBoth = 1;
		short both = 1;

		int vi = latitudeMin + 1;
		int nextJump = (int) (latitude / Math.PI);
		debug("latitude : " + latitude + " , latitude-nextJump : "
				+ (latitude - nextJump));
		int next = 0;

		while (next < latitudeMax) {

			next = Math.min(latitudeMax, latitude - nextJump);
			debug("latitude : " + latitude + " , latitudeMin : "
					+ latitudeMin + " , next : " + next + " , latitudeMax : "
					+ latitudeMax);

			// until next jump
			while (vi < next) {

				drawTop = vi < latitudeMaxTop;
				drawBottom = vi < latitudeMaxBottom;


				lastBoth = both;
				both = 0;
				if (drawTop) {// top vertices
					both++;
				}
				if (drawBottom) {// bottom vertices
					both++;
				}

				debug("vi : " + vi + " -- both : " + both);

				lastStartIndex = currentStartIndex;
				lastLength = currentLength;
				currentStartIndex += lastLength * lastBoth;

				if (drawTop) {// top triangles
					short currentIndex = currentStartIndex;
					short lastIndex;
					for (lastIndex = lastStartIndex; lastIndex < currentStartIndex
							- lastBoth; lastIndex += lastBoth) {

						arrayI.put(lastIndex);
						arrayIndex++;
						arrayI.put((short) (lastIndex + lastBoth));
						arrayIndex++;
						arrayI.put(currentIndex);
						arrayIndex++;

						arrayI.put(currentIndex);
						arrayIndex++;
						arrayI.put((short) (lastIndex + lastBoth));
						arrayIndex++;
						arrayI.put((short) (currentIndex + both));
						arrayIndex++;

						currentIndex += both;
					}

					if (longitudeLength == longitude) {
						arrayI.put(lastIndex);
						arrayIndex++;
						arrayI.put(lastStartIndex);
						arrayIndex++;
						arrayI.put(currentIndex);
						arrayIndex++;

						arrayI.put(currentIndex);
						arrayIndex++;
						arrayI.put(lastStartIndex);
						arrayIndex++;
						arrayI.put(currentStartIndex);
						arrayIndex++;
					}

				}

				// shift to draw also bottom
				if (lastBoth == 2) {
					lastStartIndex += 1;
				}
				if (both == 2) {
					currentStartIndex += 1;
				}

				if (drawBottom) {// bottom triangles
					short currentIndex = currentStartIndex;
					short lastIndex;
					for (lastIndex = lastStartIndex; lastIndex < currentStartIndex
							- both; lastIndex += lastBoth) {
						arrayI.put(lastIndex);
						arrayIndex++;
						arrayI.put(currentIndex);
						arrayIndex++;
						arrayI.put((short) (lastIndex + lastBoth));
						arrayIndex++;

						arrayI.put(currentIndex);
						arrayIndex++;
						arrayI.put((short) (currentIndex + both));
						arrayIndex++;
						arrayI.put((short) (lastIndex + lastBoth));
						arrayIndex++;

						currentIndex += both;
					}

					if (longitudeLength == longitude) {
						arrayI.put(lastIndex);
						arrayIndex++;
						arrayI.put(currentIndex);
						arrayIndex++;
						arrayI.put(lastStartIndex);
						arrayIndex++;

						arrayI.put(currentIndex);
						arrayIndex++;
						arrayI.put(currentStartIndex);
						arrayIndex++;
						arrayI.put(lastStartIndex);
						arrayIndex++;
					}
				}

				// shift back
				if (lastBoth == 2) {
					lastStartIndex -= 1;
				}
				if (both == 2) {
					currentStartIndex -= 1;
				}
				vi++;
			}

			// jump
			if (next > latitudeMin && next < latitudeMax) {

				lastBoth = both;

				lastStartIndex = currentStartIndex;
				lastLength = currentLength;
				currentStartIndex += lastLength * lastBoth;
				currentLength /= 2;

				if (drawTop) {// top triangles
					short currentIndex = currentStartIndex;
					short lastIndex;
					for (lastIndex = lastStartIndex; lastIndex < currentStartIndex
							- 2 * lastBoth; lastIndex += 2 * lastBoth) {

						arrayI.put(lastIndex);
						arrayIndex++;
						arrayI.put((short) (lastIndex + lastBoth));
						arrayIndex++;
						arrayI.put(currentIndex);
						arrayIndex++;

						arrayI.put((short) (lastIndex + lastBoth));
						arrayIndex++;
						arrayI.put((short) (lastIndex + 2 * lastBoth));
						arrayIndex++;
						arrayI.put((short) (currentIndex + both));
						arrayIndex++;

						arrayI.put((short) (lastIndex + lastBoth));
						arrayIndex++;
						arrayI.put((short) (currentIndex + both));
						arrayIndex++;
						arrayI.put(currentIndex);
						arrayIndex++;

						currentIndex += both;

					}

					if (longitudeLength == longitude) {
						// close the parallel
						arrayI.put(lastIndex);
						arrayIndex++;
						arrayI.put((short) (lastIndex + lastBoth));
						arrayIndex++;
						arrayI.put(currentIndex);
						arrayIndex++;

						arrayI.put((short) (lastIndex + lastBoth));
						arrayIndex++;
						arrayI.put(lastStartIndex);
						arrayIndex++;
						arrayI.put(currentStartIndex);
						arrayIndex++;

						arrayI.put((short) (lastIndex + lastBoth));
						arrayIndex++;
						arrayI.put(currentStartIndex);
						arrayIndex++;
						arrayI.put(currentIndex);
						arrayIndex++;
					}

					// shift for maybe draw bottom
					lastStartIndex += 1;
					currentStartIndex += 1;

				}

				if (drawBottom) {// bottom triangles
					short currentIndex = currentStartIndex;
					short lastIndex;
					for (lastIndex = lastStartIndex; lastIndex < currentStartIndex
							- 2 * lastBoth; lastIndex += 2 * lastBoth) {
						arrayI.put(lastIndex);
						arrayIndex++;
						arrayI.put(currentIndex);
						arrayIndex++;
						arrayI.put((short) (lastIndex + lastBoth));
						arrayIndex++;

						arrayI.put((short) (lastIndex + lastBoth));
						arrayIndex++;
						arrayI.put((short) (currentIndex + both));
						arrayIndex++;
						arrayI.put((short) (lastIndex + 2 * lastBoth));
						arrayIndex++;

						arrayI.put((short) (lastIndex + lastBoth));
						arrayIndex++;
						arrayI.put(currentIndex);
						arrayIndex++;
						arrayI.put((short) (currentIndex + both));
						arrayIndex++;

						currentIndex += both;

					}

					if (longitudeLength == longitude) {
						// close the parallel
						arrayI.put(lastIndex);
						arrayIndex++;
						arrayI.put(currentIndex);
						arrayIndex++;
						arrayI.put((short) (lastIndex + lastBoth));
						arrayIndex++;

						arrayI.put((short) (lastIndex + lastBoth));
						arrayIndex++;
						arrayI.put(currentStartIndex);
						arrayIndex++;
						arrayI.put(lastStartIndex);
						arrayIndex++;

						arrayI.put((short) (lastIndex + lastBoth));
						arrayIndex++;
						arrayI.put(currentIndex);
						arrayIndex++;
						arrayI.put(currentStartIndex);
						arrayIndex++;
					}

				}

				if (drawTop) {
					// shift back
					lastStartIndex -= 1;
					currentStartIndex -= 1;
				}

				vi++;

				nextJump /= 2;
			}

		}

		lastBoth = both;

		lastStartIndex = currentStartIndex;
		lastLength = currentLength;
		currentStartIndex += lastLength * lastBoth;

		// north pole
		if (latitudeMax == latitude) {

			if (drawTop) {


				short lastIndex;
				for (lastIndex = lastStartIndex; lastIndex < currentStartIndex
						- lastBoth; lastIndex += lastBoth) {
					arrayI.put(lastIndex);
					arrayIndex++;
					arrayI.put((short) (lastIndex + lastBoth));
					arrayIndex++;
					arrayI.put(currentStartIndex);
					arrayIndex++;
				}

				if (longitudeLength == longitude) {
					// close the parallel
					arrayI.put(lastIndex);
					arrayIndex++;
					arrayI.put(lastStartIndex);
					arrayIndex++;
					arrayI.put(currentStartIndex);
					arrayIndex++;
				}

				// shift for maybe south pole
				lastStartIndex += 1;
				currentStartIndex += 1;
			}

			// south pole
			if (drawBottom) {


				short lastIndex;
				for (lastIndex = lastStartIndex; lastIndex < currentStartIndex
						- lastBoth; lastIndex += lastBoth) {
					arrayI.put(lastIndex);
					arrayIndex++;
					arrayI.put(currentStartIndex);
					arrayIndex++;
					arrayI.put((short) (lastIndex + lastBoth));
					arrayIndex++;
				}

				if (longitudeLength == longitude) {
					// close the parallel
					arrayI.put(lastIndex);
					arrayIndex++;
					arrayI.put(currentStartIndex);
					arrayIndex++;
					arrayI.put(lastStartIndex);
					arrayIndex++;
				}

			}
		}

		debug("==== arrayIndex (2) = " + arrayIndex);

		arrayI.rewind();

		manager.endGeometry(arrayIndex,
				TypeElement.SURFACE);

	}

	private DrawEllipsoid drawEllipsoid;

	@Override
	public void drawEllipsoid(Coords center, Coords ev0, Coords ev1,
			Coords ev2, double r0, double r1, double r2, int longitude) {

		double r = Math.max(r0, Math.max(r1, r2));
		setLatitudeLongitudeMinMax(center, r, longitude);

		if (drawEllipsoid == null) {
			drawEllipsoid = new DrawEllipsoid();
		}
		drawEllipsoid.set(this, center, ev0, ev1, ev2, r0, r1, r2);

		drawSphereEllipsoidNV(drawEllipsoid, longitude, 0, longitude);

		setSphereEllipsoidIndices(longitude, longitude);
	}

	private int arrayIndex = 0;
	private GLBufferIndices arrayI;

	@Override
	public void startTriangles(int size) {

		manager.startGeometry(Manager.Type.TRIANGLES);

		manager.setDummyTexture();

		arrayIndex = 0;

		arrayI = manager.getCurrentGeometryIndices(size);

	}

	@Override
	public void vertexDirect(Coords3 p) {
		manager.vertex(p.getXf(), p.getYf(), p.getZf());
	}

	@Override
	public void normalDirect(Coords3 n) {
		manager.normal(n.getXf(), n.getYf(), n.getZf());
	}

	@Override
	public void endGeometryDirect() {
		arrayI.rewind();
		manager.endGeometry(arrayIndex, TypeElement.SURFACE);
	}

	/**
	 * @param id
	 *            vertex normal id
	 */
	public void drawIndex(int id) {
		arrayI.put((short) id);
		arrayIndex++;
	}
}
