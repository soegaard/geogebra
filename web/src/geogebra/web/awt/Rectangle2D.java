package geogebra.web.awt;

import geogebra.common.awt.Rectangle;

public class Rectangle2D extends geogebra.common.awt.Rectangle2D {
	
	private geogebra.web.kernel.gawt.Rectangle2D impl;


	public Rectangle2D() {
		impl = new geogebra.web.kernel.gawt.Rectangle2D.Double();
	}
	
	public Rectangle2D(geogebra.web.kernel.gawt.Rectangle2D bounds2d) {
		impl = bounds2d;
	}
	@Override
	public double getY() {
		return impl.getY();
	}

	@Override
	public double getX() {
		return impl.getX();
	}

	@Override
	public double getWidth() {
		return impl.getWidth();
	}

	@Override
	public double getHeight() {
		return impl.getHeight();
	}

	@Override
	public void setRect(double x, double y, double width, double height) {
		impl.setRect(x, y, width, height);
	}

	@Override
	public void setFrame(double x, double y, double width, double height) {
		impl.setFrame(x, y, width, height);
	}

	@Override
	public boolean intersects(double minX, double minY, double lengthX,
	        double lengthY) {
		return impl.intersects(minX, minY, lengthX, lengthY);
	}

	@Override
	public boolean intersects(Rectangle r) {
		return impl.intersects(r.getX(), r.getY(), r.getHeight(), r.getWidth());
	}

}
