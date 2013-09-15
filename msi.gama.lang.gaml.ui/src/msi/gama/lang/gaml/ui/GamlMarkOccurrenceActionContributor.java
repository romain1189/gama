/**
 * Created by drogoul, 12 sept. 2013
 * 
 */
package msi.gama.lang.gaml.ui;

import msi.gama.common.*;
import msi.gama.common.GamaPreferences.Entry;
import msi.gama.common.GamaPreferences.IPreferenceChange;
import msi.gama.common.util.GuiUtils;
import msi.gaml.types.IType;
import org.eclipse.xtext.ui.editor.occurrences.MarkOccurrenceActionContributor;
import org.eclipse.xtext.ui.editor.preferences.*;
import com.google.inject.Singleton;

/**
 * The class GamlMarkOccurrenceActionContributor.
 * 
 * @author drogoul
 * @since 12 sept. 2013
 * 
 */
@Singleton
public class GamlMarkOccurrenceActionContributor extends MarkOccurrenceActionContributor implements
	IPreferenceStoreInitializer {

	IPreferenceStoreAccess access;

	// Preference here is an instance variable, but only one will be created as this class is a singleton.
	public final Entry<Boolean> EDITOR_MARK_OCCURENCES = GamaPreferences
		.create("editor.mark.occurences", "Mark occurences", true, IType.BOOL).in(GamaPreferences.EDITOR)
		.group("Options").onChange(new IPreferenceChange<Boolean>() {

			@Override
			public boolean valueChange(final Boolean newValue) {
				stateChanged(newValue);
				return true;
			}
		});

	@Override
	protected void stateChanged(final boolean newState) {
		GuiUtils.debug("Mark occurences is now " + newState);
		super.stateChanged(newState);
	}

	@Override
	public void initialize(final IPreferenceStoreAccess preferenceStoreAccess) {
		access = preferenceStoreAccess;
		preferenceStoreAccess.getWritablePreferenceStore().setDefault(getPreferenceKey(),
			EDITOR_MARK_OCCURENCES.getValue());
		preferenceStoreAccess.getWritablePreferenceStore().setValue(getPreferenceKey(),
			EDITOR_MARK_OCCURENCES.getValue());
	}
}