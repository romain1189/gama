package msi.gama.jogl.scene;

import java.awt.Color;
import java.awt.image.BufferedImage;
import msi.gama.jogl.utils.JOGLAWTGLRenderer;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.metamodel.shape.GamaPoint;
import msi.gama.runtime.*;
import msi.gama.runtime.GAMA.InScope;

public class ImageObject extends AbstractObject {

	public BufferedImage image;
	public IAgent agent;
	public double z_layer;
	public int layerId;
	public double x;
	public double y;
	public double z;
	public double width;
	public double height;
	public Double angle = 0d;
	public boolean isDynamic;
	public MyTexture texture;
	public String name;

	public ImageObject(final BufferedImage image, final IAgent agent, final double z_layer, final int layerId,
		final double x, final double y, final double z, final Double alpha, final double width, final double height,
		final Double angle, final GamaPoint offset, final GamaPoint scale, final boolean isDynamic,
		final MyTexture texture, final String name) {
		super(null, offset, scale, alpha);
		setZ_fighting_id((double) layerId);
		this.image = image;
		this.agent = agent;
		this.z_layer = z_layer;
		this.x = x;
		this.y = y;
		this.z = z;
		this.width = width;
		this.height = height;
		this.angle = angle;
		this.isDynamic = isDynamic;
		this.texture = texture;
		this.layerId = layerId;
		this.name = name;
	}

	@Override
	public void unpick() {
		picked = false;
	}

	public void pick() {
		picked = true;
	}

	@Override
	public Color getColor() {
		if ( picked ) { return pickedColor; }
		return super.getColor();
	}

	@Override
	public void draw(final ObjectDrawer drawer, final boolean picking) {
		if ( picking ) {
			JOGLAWTGLRenderer renderer = drawer.renderer;
			renderer.gl.glPushMatrix();
			renderer.gl.glLoadName(pickingIndex);
			if ( renderer.pickedObjectIndex == pickingIndex ) {
				if ( agent != null ) {
					renderer.setPicking(false);
					pick();
					renderer.currentPickedObject = this;
					// The picked image is the grid
					if ( this.name != null ) {
						final GamaPoint pickedPoint =
							renderer.getIntWorldPointFromWindowPoint(renderer.camera.getLastMousePressedPosition());
						IAgent ag = GAMA.run(new InScope<IAgent>() {

							@Override
							public IAgent run(final IScope scope) {
								return agent.getPopulationFor(name).getAgent(scope,
									new GamaPoint(pickedPoint.x, -pickedPoint.y));
							}
						});
						renderer.displaySurface.selectAgents(ag, layerId - 1);
					} else {
						renderer.displaySurface.selectAgents(agent, layerId - 1);
					}
				}
			}
			super.draw(drawer, picking);
			renderer.gl.glPopMatrix();
		} else {
			super.draw(drawer, picking);
		}
	}

}