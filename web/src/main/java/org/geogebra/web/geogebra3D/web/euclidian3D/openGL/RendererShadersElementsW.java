package org.geogebra.web.geogebra3D.web.euclidian3D.openGL;

import org.geogebra.common.geogebra3D.euclidian3D.openGL.Manager;
import org.geogebra.common.geogebra3D.euclidian3D.openGL.ManagerShadersBindBuffers;
import org.geogebra.common.main.App;
import org.geogebra.web.geogebra3D.web.euclidian3D.EuclidianView3DW;

import com.googlecode.gwtgl.binding.WebGLRenderingContext;

/**
 * renderer using shaders and drawElements()
 * 
 * @author mathieu
 *
 */
public class RendererShadersElementsW extends RendererW {

	/**
	 * constructor
	 * 
	 * @param view
	 *            3D view
	 */
	public RendererShadersElementsW(EuclidianView3DW view) {
		super(view);
	}

	@Override
	protected Manager createManager() {
		App.debug("========== createManager");
		return new ManagerShadersBindBuffers(this, view3D);
	}

	@Override
	public void draw(Manager.Type type, int length) {
		glContext.drawElements(getGLType(type), length,
				WebGLRenderingContext.UNSIGNED_SHORT, 0);
	}
}