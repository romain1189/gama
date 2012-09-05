/*
 * GAMA - V1.4 http://gama-platform.googlecode.com
 * 
 * (c) 2007-2011 UMI 209 UMMISCO IRD/UPMC & Partners (see below)
 * 
 * Developers :
 * 
 * - Alexis Drogoul, UMI 209 UMMISCO, IRD/UPMC (Kernel, Metamodel, GAML), 2007-2012
 * - Vo Duc An, UMI 209 UMMISCO, IRD/UPMC (SWT, multi-level architecture), 2008-2012
 * - Patrick Taillandier, UMR 6228 IDEES, CNRS/Univ. Rouen (Batch, GeoTools & JTS), 2009-2012
 * - Beno�t Gaudou, UMR 5505 IRIT, CNRS/Univ. Toulouse 1 (Documentation, Tests), 2010-2012
 * - Phan Huy Cuong, DREAM team, Univ. Can Tho (XText-based GAML), 2012
 * - Pierrick Koch, UMI 209 UMMISCO, IRD/UPMC (XText-based GAML), 2010-2011
 * - Romain Lavaud, UMI 209 UMMISCO, IRD/UPMC (RCP environment), 2010
 * - Francois Sempe, UMI 209 UMMISCO, IRD/UPMC (EMF model, Batch), 2007-2009
 * - Edouard Amouroux, UMI 209 UMMISCO, IRD/UPMC (C++ initial porting), 2007-2008
 * - Chu Thanh Quang, UMI 209 UMMISCO, IRD/UPMC (OpenMap integration), 2007-2008
 */
package msi.gama.common.interfaces;

import java.awt.Color;
import java.awt.image.BufferedImage;
import msi.gama.metamodel.shape.IShape;
import msi.gama.outputs.IDisplayOutput;

/**
 * Written by drogoul Modified on 26 nov. 2009
 * 
 * @todo Description
 * 
 */
public interface IDisplaySurface {

	public static final double SELECTION_SIZE = 20; // pixels
	public static final int MAX_SIZE = 4000; // pixels

	BufferedImage getImage();

	void dispose();

	void updateDisplay();

	void forceUpdateDisplay(); // toggling off synchronization

	int[] computeBoundsFrom(int width, int height);

	boolean resizeImage(int width, int height);

	void outputChanged(final double env_width, final double env_height, final IDisplayOutput output);

	void zoomIn();

	void zoomOut();

	void zoomFit();

	/**
	 * Switch between 2D and 3D view (Only with Opengl view)
	 */
	void toggleView();
	
	/**
	 * Activate the picking mode
	 */
	void togglePicking();

	ILayerManager getManager();

	void fireSelectionChanged(Object a);

	void focusOn(IShape geometry, ILayer display);

	boolean canBeUpdated();

	void canBeUpdated(boolean ok);

	void setBackgroundColor(Color background);

	void setPaused(boolean b);

	public boolean isPaused();

	public void setQualityRendering(boolean quality);

	void setSynchronized(boolean checked);

	void setAutoSave(boolean autosave, int x, int y);

	void setSnapshotFileName(String string);

	void snapshot();

	/**
	 * @param swtNavigationPanel
	 *            FIXME Create an interface for the navigqtion panel
	 */
	void setNavigator(Object swtNavigationPanel);

	/**
	 * @return the width of the panel
	 */
	int getWidth();

	/**
	 * @return the height of the panel
	 */
	int getHeight();

	/**
	 * @return the width of the image (bitmap)
	 */
	int getImageWidth();

	/**
	 * @return the height of the image (bitmap)
	 */
	int getImageHeight();

	/**
	 * Sets the origin (top left corner) of the image in the panel
	 * @param i
	 * @param j
	 */
	void setOrigin(int i, int j);

	/**
	 * Returns the x coordinate of the origin (top left corner of the image in the panel)
	 * @return
	 */
	int getOriginX();

	/**
	 * Returns the y coordinate of the origin (top left corner of the image in the panel)
	 * @return
	 */

	int getOriginY();

	/**
	 * Post-constructor that initializes the surface
	 * @param w
	 * @param h
	 * @param layerDisplayOutput
	 */
	void initialize(double w, double h, IDisplayOutput layerDisplayOutput);

	/**
	 * 
	 * @return an Array of size 3 containing the red, green and blue components
	 */
	int[] getHighlightColor();

	void setHighlightColor(int[] rgb);

	void addShapeFile();

}
