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

package msi.gama.lang.gaml.ui.quickfix;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.ui.editor.model.edit.*;
import org.eclipse.xtext.ui.editor.quickfix.*;
import org.eclipse.xtext.validation.Issue;

public class GamlQuickfixProvider extends DefaultQuickfixProvider {

	public static final String QF_NOTFACETOFKEY = "NOTFACETOFKEY";
	public static final String QF_UNKNOWNFACET = "UNKNOWNFACET";
	public static final String QF_KEYHASNOFACET = "KEYHASNOFACET";
	public static final String QF_NOTKEYOFCONTEXT = "NOTKEYOFCONTEXT";
	public static final String QF_NOTKEYOFMODEL = "NOTKEYOFMODEL";
	public static final String QF_INVALIDSETVAR = "INVALIDSETVAR";
	public static final String QF_BADEXPRESSION = "QF_BADEXPRESSION";

	// @Fix(MyJavaValidator.INVALID_NAME)
	// public void capitalizeName(final Issue issue, IssueResolutionAcceptor acceptor) {
	// acceptor.accept(issue, "Capitalize name", "Capitalize the name.", "upcase.png", new
	// IModification() {
	// public void apply(IModificationContext context) throws BadLocationException {
	// IXtextDocument xtextDocument = context.getXtextDocument();
	// String firstLetter = xtextDocument.get(issue.getOffset(), 1);
	// xtextDocument.replace(issue.getOffset(), 1, firstLetter.toUpperCase());
	// }
	// });
	// }
	public void removeIssue(final String label, final Issue issue,
		final IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, label, "", "", new IModification() {

			@Override
			public void apply(final IModificationContext context) throws BadLocationException {
				IXtextDocument xtextDocument = context.getXtextDocument();
				xtextDocument.replace(issue.getOffset(), issue.getLength(), "");
			}
		});
	}

	@Fix(QF_NOTKEYOFMODEL)
	public void fixKeyOfModel(final Issue issue, final IssueResolutionAcceptor acceptor) {
		removeIssue("Remove this keyword", issue, acceptor);
	}

	@Fix(QF_NOTKEYOFCONTEXT)
	public void fixKeyOfContext(final Issue issue, final IssueResolutionAcceptor acceptor) {
		removeIssue("Remove this keyword", issue, acceptor);
	}

	@Fix(QF_NOTFACETOFKEY)
	public void fixFacetOfKey(final Issue issue, final IssueResolutionAcceptor acceptor) {
		removeIssue("Remove this facet", issue, acceptor);
	}

}
