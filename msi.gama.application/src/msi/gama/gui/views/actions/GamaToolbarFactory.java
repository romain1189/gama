/**
 * Created by drogoul, 19 janv. 2012
 * 
 */
package msi.gama.gui.views.actions;

import msi.gama.common.util.GuiUtils;
import msi.gama.gui.views.GamaViewPart;
import org.eclipse.jface.action.*;
import org.eclipse.ui.IViewSite;

/**
 * The class ViewActionFactory.
 * 
 * @author drogoul
 * @since 19 janv. 2012
 * 
 */
public class GamaToolbarFactory implements IGamaViewActions {

	private static IContributionItem createContributionItem(final GamaViewPart view, final int code) {
		switch (code) {
			case SEP:
				return new Separator();
			case SNAP:
				return new SnapshotItem(view);
			case ZOOM_IN:
				return new ZoomInItem(view);
			case ZOOM_LEVEL:
				return new ZoomIndicatorItem(view);
			case ZOOM_OUT:
				return new ZoomOutItem(view);
			case ZOOM_FIT:
				return new ZoomFitItem(view);
			case FOCUS:
				return new DisplayedAgentsMenu(view);
			case NEW_MONITOR:
				return new NewMonitorItem(view);
			case LAYERS:
				return new LayersItem(view);
			case REFRESH:
				return new FrequencyItem(view);
			case SAVE:
				return new SaveItem(view);
			case REVERT:
				return new RevertItem(view);
			case PAUSE:
				return new PauseItem(view);
			case SYNC:
				return new SynchronizeItem(view);
			case RENDERING:
				return new RenderingItem(view);
			case HIGHLIGHT_COLOR:
				return new HighlightColorItem(view);
			case CAMERA:
				return new CameraItem(view);
			case ARCBALL:
				return new ArcBallItem(view);
			case TRIANGULATION:
				return new TriangulationItem(view);
			case SPLITLAYER:
				return new SplitLayerItem(view);
			case ROTATION:
				return new RotationItem(view);
			case SWITCHCAMERA:
				return new SwitchCameraItem(view);
			case OVERLAY:
				return new OverlayItem(view);
			case OPENGL:
				return new OpenGLItem(view);
			case CLEAR:
				return new ClearItem(view);

		}
		return null;
	}

	public static void buildToolbar(final GamaViewPart view, final Integer ... codes) {
		IToolBarManager manager = ((IViewSite) view.getSite()).getActionBars().getToolBarManager();
		for ( Integer i : codes ) {
			IContributionItem item = createContributionItem(view, i);
			if ( item != null ) {
				manager.add(item);
			} else {
				GuiUtils.debug("Item id " + i + " cannot be created for view " + view.getPartName());
			}
		}
		manager.update(true);
	}

}
