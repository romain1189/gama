package msi.gama.lang.gaml.ui.decorators;

import msi.gama.gui.swt.*;
import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.internal.ide.IMarkerImageProvider;

public class GamlMarkerImageProvider implements IMarkerImageProvider {

	public GamlMarkerImageProvider() {}

	/**
	 * Returns the relative path for the image
	 * to be used for displaying an marker in the workbench.
	 * This path is relative to the plugin location
	 * 
	 * Returns <code>null</code> if there is no appropriate image.
	 * 
	 * @param marker The marker to get an image path for.
	 * 
	 */
	@Override
	public String getImagePath(final IMarker marker) {
		GamaIcon icon = getImage(marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING));
		if ( icon == null ) { return null; }
		String iconPath = "/icons/full/";
		return iconPath + icon.getCode() + ".png";
	}

	public static GamaIcon getImage(final String description) {
		if ( description.contains("Errors") ) {
			return getImage(IMarker.SEVERITY_ERROR);
		} else if ( description.contains("Warnings") ) {
			return getImage(IMarker.SEVERITY_WARNING);
		} else if ( description.contains("Infos") ) { return getImage(IMarker.SEVERITY_INFO); }
		return null;
	}

	public static GamaIcon getImage(final int severity) {
		switch (severity) {
			case IMarker.SEVERITY_ERROR:
				return GamaIcons.create("marker.error2");
			case IMarker.SEVERITY_WARNING:
				return GamaIcons.create("marker.warning2");
			case IMarker.SEVERITY_INFO:
				return GamaIcons.create("marker.info2");
		}
		return null;
	}

}
