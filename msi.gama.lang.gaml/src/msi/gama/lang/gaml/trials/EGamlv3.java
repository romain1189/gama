/**
 * Created by drogoul, 7 f�vr. 2012
 * 
 */
package msi.gama.lang.gaml.xtext_grammarV3;

import msi.gama.common.interfaces.IKeyword;
import msi.gama.common.util.StringUtils;
import msi.gama.lang.gaml.gaml.*;
import msi.gama.lang.gaml.gaml.util.GamlSwitch;
import msi.gaml.descriptions.IGamlDescription;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

/**
 * The class EGaml. A bunch of utilities to work with the various GAML statements and expressions.
 * 
 * @author drogoul
 * @since 7 f�vr. 2012
 * 
 */
public class EGamlv3 {

	public static IGamlDescription getGamlDescription(final EObject object) {
		if ( object == null ) { return null; }
		for ( Adapter o : object.eAdapters() ) {
			if ( o instanceof IGamlDescription ) { return (IGamlDescription) o; }
		}
		return null;
	}

	public static <T> T getGamlDescription(final EObject object, final Class<T> preciseClass) {
		if ( object == null ) { return null; }
		for ( int i = 0, n = object.eAdapters().size(); i < n; i++ ) {
			Adapter a = object.eAdapters().get(i);
			if ( preciseClass.isAssignableFrom(a.getClass()) ) { return (T) a; }

		}
		return null;
	}

	public static void setGamlDescription(final EObject object, final IGamlDescription description) {
		if ( description == null ) { return; }
		IGamlDescription existing = getGamlDescription(object, description.getClass());
		if ( existing != null ) {
			object.eAdapters().remove(existing);
		}
		object.eAdapters().add(description);
	}

	public static void unsetGamlDescription(final EObject object, final IGamlDescription description) {
		if ( object == null ) { return; }
		object.eAdapters().remove(description);
	}

	public static final GamlSwitch<String> getKey = new GamlSwitch<String>() {

		@Override
		public String caseExpression(final Expression object) {
			return object.getOp();
		}

		@Override
		public String caseArgPairExpr(final ArgPairExpr object) {
			String s = object.getArg();
			if ( s.endsWith(":") ) {
				s = s.replace(':', ' ');
			}
			return s.trim();
		}

		@Override
		public String caseParameter(final Parameter object) {
			String s = getKeyOf(object.getLeft());
			if ( s.endsWith(":") ) {
				s = s.replace(':', ' ');
			}
			return s.trim();
		}

		@Override
		public String caseModel(final Model object) {
			return IKeyword.MODEL;
		}

		@Override
		public String caseStatement(final Statement object) {
			return object.getKey();
		}

		// @Override
		// public String caseFacetRef(final FacetRef object) {
		// return object.getRef();
		// }

		@Override
		// public String caseFacetExpr(final FacetExpr object) {
		public String caseFacet(final Facet object) {
			String s = object.getKey();
			if ( s.endsWith(":") ) {
				s = s.replace(':', ' ');
			}
			return s.trim();
			// String ref = object.getKey();
			// return ref == null ? null : caseFacetRef(ref);

		}

		// @Override
		// public String caseNameFacetExpr(final NameFacetExpr object) {
		// return IKeyword.NAME;
		// }
		//
		// @Override
		// public String caseReturnsFacetExpr(final ReturnsFacetExpr object) {
		// return IKeyword.RETURNS;
		// }
		//
		// @Override
		// public String caseActionFacetExpr(final ActionFacetExpr object) {
		// return IKeyword.ACTION;
		// }
		//
		// @Override
		// public String caseFunctionFacetExpr(final FunctionFacetExpr object) {
		// return IKeyword.FUNCTION;
		// }

		@Override
		public String caseFunction(final Function object) {
			return object.getOp();
		}

		@Override
		public String caseVariableRef(final VariableRef object) {
			return NodeModelUtils.getTokenText(NodeModelUtils.getNode(object));
		}

		@Override
		public String caseUnitName(final UnitName object) {
			return object.getOp();
		}

		@Override
		public String caseStringLiteral(final StringLiteral object) {
			return object.getValue();
		}

		@Override
		public String doSwitch(final EObject f) {
			if ( f == null ) { return null; }
			return super.doSwitch(f);
		}

	};

	public static String getNameOf(final Statement s) {
		Body body = s.getBody();
		if ( body == null ) { return null; }
		return body.getName();
	}

	public static Expression getExprOf(final Statement s) {
		Body body = s.getBody();
		if ( body == null ) { return null; }
		return body.getExpr();
	}

	public static Parameters getParamsOf(final Statement s) {
		Body body = s.getBody();
		if ( body == null ) { return null; }
		return body.getParams();
	}

	public static ActionArguments getArgsOf(final Statement s) {
		Body body = s.getBody();
		if ( body == null ) { return null; }
		return body.getArgs();
	}

	public static Expression getFunctionOf(final Statement stm) {
		// FIXME Tenir compte des cas "type var function: expression;" et "type var {return
		// expression;}"
		Expression expr = stm.getFunction();
		// if ( expr == null && stm.getBlock() != null ) {
		// expr = stm.getBlock().getFunction();
		// }
		return expr;
	}

	public static Expression getValueOf(final Statement s) {
		if ( s instanceof AssignmentStatement ) { return ((AssignmentStatement) s).getValue(); }
		if ( s instanceof Equation ) { return ((Equation) s).getValue(); }
		return null;
	}

	public static EList<Facet> getFacetsOf(final Statement s) {
		return s.getFacets();
	}

	public static String getKeyOf(final EObject f) {
		return getKey.doSwitch(f);
	}

	public static GamlFactory getFactory() {
		return (GamlFactory) GamlPackage.eINSTANCE.getEFactoryInstance();
	}

	public static StringLiteral createTerminal(final String id) {
		if ( id != null ) {
			StringLiteral expr = getFactory().createStringLiteral();
			expr.setValue(id);
			return expr;
		}
		return null;
	}

	public static BooleanLiteral createTerminal(final boolean b) {
		BooleanLiteral expr = getFactory().createBooleanLiteral();
		expr.setValue(b ? IKeyword.TRUE : IKeyword.FALSE);
		return expr;
	}

	final static StringBuilder serializer = new StringBuilder(100);

	public static String toString(final EObject expr) {
		if ( expr == null ) { return null; }
		if ( expr instanceof Statement ) {
			return getNameOf((Statement) expr);
			/*
			 * } else if ( expr instanceof DefinitionFacetExpr ) { return ((DefinitionFacetExpr)
			 * expr)
			 * .getName(); }
			 */
		} else if ( expr instanceof Facet ) { return ((Facet) expr).getName(); }

		if ( !(expr instanceof Expression) ) { return expr.toString(); }
		serializer.setLength(0);
		serialize((Expression) expr);
		return serializer.toString();
	}

	private static void serialize(final Expression expr) {
		if ( expr == null ) {
			return;
		} else if ( expr instanceof TernExp ) {
			serializer.append("(");
			serialize(expr.getLeft());
			serializer.append(")").append(expr.getOp()).append("(");
			serialize(expr.getRight());
			serializer.append(")").append(":");
			serialize(((TernExp) expr).getIfFalse());
		} else if ( expr instanceof StringLiteral ) {
			serializer.append(StringUtils.toGamlString(((StringLiteral) expr).getValue()));
		} else if ( expr instanceof TerminalExpression ) {
			serializer.append(((TerminalExpression) expr).getValue());
		} else if ( expr instanceof Point ) {
			serializer.append("{").append("(");
			serialize(expr.getLeft());
			serializer.append(")").append(expr.getOp()).append("(");
			serialize(expr.getRight());
			serializer.append(")");
			if ( ((Point) expr).getZ() != null ) {
				serializer.append(',').append("(");
				serialize(((Point) expr).getZ());
				serializer.append(")");
			}
			serializer.append("}");
		} else if ( expr instanceof Array ) {
			array(((Array) expr).getExprs(), false);
		} else if ( expr instanceof VariableRef ) {
			serializer.append(getKeyOf(expr));
		} else if ( expr instanceof GamlUnaryExpr ) {
			serializer.append(expr.getOp()).append("(");
			serialize(expr.getRight());
			serializer.append(")");
		} else if ( expr instanceof Function ) {
			function((Function) expr);
		}
		// else if ( expr instanceof FunctionRef ) {
		// function((FunctionRef) expr);
		// }
		else {
			serializer.append("(");
			serialize(expr.getLeft());
			serializer.append(")").append(expr.getOp()).append("(");
			serialize(expr.getRight());
			serializer.append(")");
		}
	}

	private static void function(final Function expr) {
		EList<Expression> args = expr.getArgs();
		// String opName = EGaml.getKeyOf(expr.getLeft());
		String opName = expr.getOp();
		if ( args.size() == 1 ) {
			serializer.append(opName).append("(");
			serialize(args.get(0));
			serializer.append(")");
		} else if ( args.size() == 2 ) {
			serializer.append("(");
			serialize(args.get(0));
			serializer.append(")").append(opName).append("(");
			serialize(args.get(1));
			serializer.append(")");
		} else {
			serializer.append("(");
			serialize(args.get(0));
			serializer.append(")").append(opName);
			array(args, true);
		}
	}

	private static void array(final EList<Expression> args, final boolean arguments) {
		// if arguments is true, parses the list to transform it into a map of args
		// (starting at 1); Experimental right now
		serializer.append("[");
		int size = args.size();
		for ( int i = 0; i < size; i++ ) {
			Expression e = args.get(i);
			if ( arguments ) {
				serializer.append("arg").append(i).append("::");
			}
			serialize(e);
			if ( i < size - 1 ) {
				serializer.append(",");
			}
		}
		serializer.append("]");
	}

}