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
package msi.gama.lang.utils;

import static msi.gama.common.interfaces.IKeyword.*;
import static msi.gaml.expressions.GamlExpressionFactory.*;
import java.io.*;
import java.text.*;
import java.util.*;
import msi.gama.common.interfaces.*;
import msi.gama.common.util.StringUtils;
import msi.gama.lang.gaml.gaml.*;
import msi.gama.lang.gaml.gaml.util.GamlSwitch;
import msi.gama.lang.gaml.resource.GamlResource;
import msi.gama.precompiler.IUnits;
import msi.gama.runtime.GAMA;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.*;
import msi.gaml.compilation.AbstractGamlAdditions;
import msi.gaml.descriptions.*;
import msi.gaml.expressions.*;
import msi.gaml.statements.DrawStatement;
import msi.gaml.types.*;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.xtext.resource.*;

/**
 * The Class ExpressionParser.
 */

public class GamlExpressionCompiler implements IExpressionCompiler<Expression> {

	public IVarExpression each_expr;
	final static NumberFormat nf = NumberFormat.getInstance(Locale.US);
	private IExpression world = null;

	/*
	 * The context (IDescription) in which the parser operates. If none is given, the global context
	 * of the current simulation is returned (via simulation.getModel().getDescription()) if it is
	 * available. Otherwise, only simple expressions (that contain constants) can be parsed.
	 */
	private IDescription context;

	private final IExpressionFactory factory;

	static {
		IExpressionCompiler.OPERATORS.put(MY, new GamaMap());
	}

	public GamlExpressionCompiler() {
		factory = GAMA.getExpressionFactory();
	}

	@Override
	public IExpression compile(final IExpressionDescription s, final IDescription parsingContext) {
		if ( s.isConstant() ) { return s.getExpression(); }
		EObject e = getEObjectOf(s);
		IDescription previous = setContext(parsingContext);
		IExpression result = compile(e);
		setContext(previous);
		return result;
	}

	private IExpression compile(final EObject s) {
		if ( s == null ) {
			// No error, since the null expressions come from previous (more focused)
			// errors and not from the parser itself.
			return null;
		}
		IExpression expr = compiler.doSwitch(s);
		EGaml.setGamlDescription(s, expr);
		return expr;
	}

	// KEEP
	private IExpression species(final String name) {
		return factory.createConst(name, Types.get(IType.SPECIES), getSpeciesContext(name)
			.getType());
	}

	private IExpression skill(final String name) {
		return factory.createConst(name, Types.get(IType.STRING));
	}

	// KEEP
	private IExpression unary(final String op, final Expression e) {
		if ( op == null ) { return null; }
		IExpression expr = compile(e);
		if ( expr == null ) { return null; }
		if ( op.equals(MY) ) {
			IDescription desc = getContext().getDescriptionDeclaringVar(MYSELF);
			if ( desc != null ) {
				// We are in a remote context, so 'my' refers to the calling agent
				IExpression myself = desc.getVarExpr(MYSELF);
				IDescription species = getSpeciesContext(myself.getType().getSpeciesName());
				IExpression var = species.getVarExpr(EGaml.getKeyOf(e));
				return factory.createOperator(_DOT, desc, myself, var);
			}
			// Otherwise, we ignore 'my' since it refers to 'self'
			return expr;
		}
		// The unary "unit" operator should let the value of its child pass through
		if ( op.equals("�") ) { return expr; }
		if ( isSpeciesName(op) ) { return factory.createOperator(AS, context, expr, species(op)); }
		if ( isSkillName(op) ) { return factory.createOperator(AS, context, expr, skill(op)); }
		return factory.createOperator(op, context, expr);
	}

	private IExpression binary(final String op, final IExpression left, final Expression e2) {
		if ( left == null ) { return null; }
		// if the operator is "as", the right-hand expression should be a casting type
		if ( AS.equals(op) ) {
			String type = EGaml.getKeyOf(e2);
			if ( isSpeciesName(type) ) { return factory.createOperator(op, context, left,
				species(type)); }
			if ( isSkillName(type) ) { return factory.createOperator(AS_SKILL, context, left,
				skill(type)); }
			// allows expression such as 'agent as dead'. Should we allow them ?
			return factory.createOperator(type, context, left);
		}
		// if the operator is "is", the right-hand expression should be a type
		if ( IS.equals(op) ) {
			String type = EGaml.getKeyOf(e2);
			if ( isTypeName(type) ) { return factory.createOperator(op, context, left,
				factory.createConst(type, Types.get(IType.STRING))); }
			if ( isSkillName(type) ) { return factory.createOperator(IS_SKILL, context, left,
				factory.createConst(type, Types.get(IType.STRING))); }
			getContext().flagError(
				"'is' must be followed by a type, species or skill name. " + type +
					" is neither of these.", IGamlIssue.NOT_A_TYPE, e2, type);
			return null;
		}

		// we verify and compile apart the calls to actions as operators
		TypeDescription sd = getContext().getSpeciesDescription(left.getType().getSpeciesName());
		if ( sd != null ) {
			StatementDescription cd = sd.getAction(op);
			if ( cd != null ) {
				if ( !(e2 instanceof Array) && !(e2 instanceof Parameters) ) {
					context.flagError(
						"Arguments must be provided as [a1::v1, a2::v2] or (a1:v1, a2:v2) ; ",
						IGamlIssue.UNKNOWN_ARGUMENT, e2);
					return null;
				}
				List<Expression> parameters =
					EGaml.getExprsOf(e2 instanceof Array ? ((Array) e2).getExprs()
						: ((Parameters) e2).getParams());
				IExpression right = compileArguments(cd, parameters);
				return factory.createAction(op, context, left, right);
			}
		}

		// if the operator is an iterator, we must initialize "each"
		if ( ITERATORS.contains(op) ) {
			IType t = left.getContentType();
			each_expr = new EachExpression(EACH, t, t);
		}
		// we can now safely compile the right-hand expression
		IExpression right = compile(e2);
		// and return the binary expression
		return factory.createOperator(op, context, left, right);

	}

	// KEEP
	private IExpression binary(final String op, final Expression e1, final Expression right) {
		// if the expression is " var of agents ", we must compile it apart
		if ( OF.equals(op) ) { return compileFieldExpr(right, e1); }
		// we can now safely compile the left-hand expression
		IExpression left = compile(e1);
		return binary(op, left, right);
	}

	private TypeDescription getSpeciesContext(final String e) {
		return getContext().getSpeciesDescription(e);
	}

	private boolean isSpeciesName(final String s) {
		return getContext().getModelDescription().hasSpeciesDescription(s);
	}

	private boolean isSkillName(final String s) {
		return AbstractGamlAdditions.getSkillClasses().containsKey(s);
	}

	private boolean isTypeName(final String s) {
		return getContext().getModelDescription().getTypeNamed(s) != null;
	}

	private IExpression compileFieldExpr(final Expression leftExpr, final Expression fieldExpr) {
		String var = EGaml.getKeyOf(fieldExpr);
		IExpression target = compile(leftExpr);
		if ( target == null ) { return null; }
		IType type = target.getType();
		TypeDescription desc = getSpeciesContext(type.getSpeciesName());
		if ( desc == null ) {
			TypeFieldExpression expr = (TypeFieldExpression) type.getGetter(var);
			if ( expr == null ) {
				// New: "." can now be used to access containers elements. But not for long :)
				if ( Types.get(IType.CONTAINER).isAssignableFrom(type) ) {
					// We have an instance of the use of "." as "at" or "@"
					return binary(IKeyword.AT, target, fieldExpr);
				}
				context.flagError("Field " + var + " unknown for " + target.toGaml() + " of type " +
					type, IGamlIssue.UNKNOWN_FIELD, leftExpr, var, type.toString());
				return null;
			}
			expr = expr.copyWith(target);
			EGaml.setGamlDescription(fieldExpr, expr);
			return expr;
		}
		IVarExpression expr = (IVarExpression) desc.getVarExpr(var);
		if ( expr == null ) {
			context.flagError("Unknown variable :" + var + " in " + desc.getName(),
				IGamlIssue.UNKNOWN_VAR, leftExpr, var, desc.getName());
		}
		EGaml.setGamlDescription(fieldExpr, expr);
		return factory.createOperator(_DOT, context, target, expr);

	}

	// KEEP

	private IExpression compileArguments(final StatementDescription action,
		final List<Expression> words) {
		if ( action == null ) {
			context.flagError("Action cannot be determined", IGamlIssue.UNKNOWN_ACTION);
			return null;
		}
		final GamaList list = new GamaList();
		for ( int i = 0, n = words.size(); i < n; i++ ) {
			Expression e = words.get(i);
			if ( !(e instanceof Pair) && !(e instanceof ArgumentPair) && !(e instanceof Parameter) ) {
				context.flagError(
					"Arguments must be provided as [a1::v1, a2::v2] or (a1:v1, a2:v2) ; ",
					IGamlIssue.UNKNOWN_ARGUMENT, e, action.getArgNames().toArray(new String[] {}));
				return null;
			}
			String arg = null;
			if ( e instanceof ArgumentPair ) {
				arg = EGaml.getKey.caseArgumentPair((ArgumentPair) e);
			} else {
				arg = EGaml.getKeyOf(e.getLeft());
			}
			if ( !action.containsArg(arg) ) {
				context.flagError(
					"Argument " + arg + " is not allowed for action " + action.getName(),
					IGamlIssue.UNKNOWN_ARGUMENT, e, action.getArgNames().toArray(new String[] {}));
				return null;
			}
			// We modify the expression in line to replace the arg by a string terminal
			list.add(binary("::", factory.createConst(arg, Types.get(IType.STRING)), e.getRight()));
		}
		return factory.createMap(list);
	}

	// KEEP
	private IExpression getWorldExpr() {
		if ( world == null ) {
			IType tt = getContext().getModelDescription().getWorldSpecies().getType();
			world =
				factory.createVar(WORLD_AGENT_NAME, tt, tt, true, IVarExpression.WORLD,
					context.getModelDescription());
		}
		return world;
	}

	private IDescription setContext(final IDescription context) {
		IDescription previous = context;
		this.context = context;
		return previous;
	}

	private IDescription getContext() {
		if ( context == null ) { return GAMA.getModelContext(); }
		return context;
	}

	/**
	 * @see msi.gaml.expressions.IExpressionParser#parseArguments(msi.gaml.descriptions.ExpressionDescription,
	 *      msi.gaml.descriptions.IDescription)
	 */
	@Override
	public Map<String, IExpressionDescription> parseArguments(final IExpressionDescription s,
		final IDescription command) {
		EObject o = getEObjectOf(s);
		if ( o == null ) { return Collections.EMPTY_MAP; }
		if ( !(o instanceof Array) && !(o instanceof Parameters) ) {
			command.flagError(
				"Arguments must be provided as [a1::v1, a2::v2] or (a1:v1, a2:v2) ; ", WITH);
			return Collections.EMPTY_MAP;
		}
		List<Expression> parameters =
			EGaml.getExprsOf(o instanceof Array ? ((Array) o).getExprs() : ((Parameters) o)
				.getParams());
		Map<String, IExpressionDescription> argMap = new HashMap();
		for ( Expression exp : parameters ) {
			if ( !(exp instanceof Pair) && !(exp instanceof ArgumentPair) &&
				!(exp instanceof Parameter) ) {
				command.flagError(
					"Arguments must be provided as [a1::v1, a2::v2] or (a1:v1, a2:v2) ; ", WITH);
				return argMap;
			}

			String arg = null;
			if ( exp instanceof ArgumentPair || exp instanceof Parameter ) {
				arg = EGaml.getKeyOf(exp);
			} else {
				arg = EGaml.getKeyOf(exp.getLeft());
			}
			IExpressionDescription ed = new EcoreBasedExpressionDescription(exp.getRight());
			// EGaml.setGamlDescription(exp.getRight(), ed);
			argMap.put(arg, ed);
		}
		return argMap;
	}

	// /**
	// * @see
	// msi.gaml.expressions.IExpressionParser#parseLiteralArray(msi.gaml.descriptions.ExpressionDescription)
	// */
	// @Override
	// public Set<String> parseLiteralArray(final IExpressionDescription s,
	// final IDescription context, final boolean skills) {
	// String type = skills ? "skill" : "attribute";
	// EObject target = getEObjectOf(s);
	// if ( target == null ) { return Collections.EMPTY_SET; }
	// if ( !(target instanceof Array) ) {
	// if ( target instanceof VariableRef ) {
	// String skillName = EGaml.getKeyOf(target);
	// context.flagWarning(type +
	// "s should be provided as a list of identifiers, for instance [" + skillName +
	// "]", IGamlIssue.AS_ARRAY, target, skillName);
	// if ( skills && !isSkillName(skillName) ) {
	// context.flagError("Unknown " + type + " " + skillName,
	// IGamlIssue.UNKNOWN_SKILL, target);
	// }
	// return new HashSet(Arrays.asList(skillName));
	// }
	// if ( target instanceof Expression ) {
	// context.flagError(
	// "Impossible to recognize valid " + type + "s in " + EGaml.toString(target), skills
	// ? IGamlIssue.UNKNOWN_SKILL : IGamlIssue.UNKNOWN_VAR, target);
	// } else {
	// context.flagError(type + "s should be provided as a list of identifiers.",
	// IGamlIssue.UNKNOWN_SKILL, target);
	// }
	// return Collections.EMPTY_SET;
	// }
	// Set<String> result = new HashSet();
	// Array array = (Array) target;
	// for ( Expression expr : EGaml.getExprsOf(array.getExprs()) ) {
	// String name = EGaml.getKeyOf(expr);
	// if ( skills && !isSkillName(name) ) {
	// context.flagError("Unknown " + type + " " + name, skills ? IGamlIssue.UNKNOWN_SKILL
	// : IGamlIssue.UNKNOWN_VAR, expr);
	// } else {
	// result.add(name);
	// }
	// }
	// return result;
	//
	// }

	GamlSwitch<IExpression> compiler = new GamlSwitch<IExpression>() {

		@Override
		public IExpression caseActionDefinition(ActionDefinition object) {
			return super.caseActionDefinition(object);
		}

		@Override
		public IExpression caseUnitFakeDefinition(UnitFakeDefinition object) {
			return super.caseUnitFakeDefinition(object);
		}

		@Override
		public IExpression caseTypeFakeDefinition(TypeFakeDefinition object) {
			return super.caseTypeFakeDefinition(object);
		}

		@Override
		public IExpression caseVarFakeDefinition(VarFakeDefinition object) {
			return super.caseVarFakeDefinition(object);
		}

		@Override
		public IExpression caseSkillRef(SkillRef object) {
			return skill(EGaml.getKey.caseSkillRef(object));
		}

		@Override
		public IExpression caseActionRef(ActionRef object) {
			return factory.createConst(EGaml.getKey.caseActionRef(object), Types.get(IType.STRING));
		}

		@Override
		public IExpression caseExpression(final Expression object) {
			// in the general case, we try to return a binary expression
			return binary(EGaml.getKeyOf(object), object.getLeft(), object.getRight());
		}

		@Override
		public IExpression caseVariableRef(final VariableRef object) {
			String s = EGaml.getKey.caseVariableRef(object);
			if ( s == null ) { return caseVarDefinition(object.getRef()); }
			return caseVar(s, object);
		}

		@Override
		public IExpression caseTypeRef(final TypeRef object) {
			String s = EGaml.getKeyOf(object);
			if ( s == null ) {
				// we delegate to the referenced GamlVarRef
				return caseTypeDefinition(object.getRef());
			}
			return caseVar(s, object);
		}

		@Override
		public IExpression caseUnitName(final UnitName object) {
			String s = EGaml.getKeyOf(object);
			if ( IUnits.UNITS.containsKey(s) ) { return factory.createUnitExpr(s, context); }
			// If it is a unit, we return its float value
			context.flagError(s + " is not a unit name.", IGamlIssue.NOT_A_UNIT, object,
				(String[]) null);
			return null;
		}

		public IExpression caseVar(final String s, final EObject object) {
			if ( s == null ) {
				getContext().flagError("Unknown variable", IGamlIssue.UNKNOWN_VAR, object);
				return null;
			}

			// HACK
			if ( s.equals(USER_LOCATION) ) { return factory.createVar(USER_LOCATION,
				Types.get(IType.POINT), Types.NO_TYPE, true, IVarExpression.TEMP, context); }

			// HACK
			if ( s.equals(EACH) ) { return each_expr; }
			if ( s.equals(NULL) ) { return new ConstantExpression(null, Types.NO_TYPE,
				Types.NO_TYPE); }
			// We try to find a species name from the name
			IDescription temp_sd = null;
			if ( context != null ) {
				temp_sd = getContext().getSpeciesDescription(s);
			}
			if ( temp_sd != null ) { return factory.createConst(s, Types.get(IType.SPECIES),
				temp_sd.getType()); }
			if ( s.equals(SELF) ) {
				temp_sd = getContext().getSpeciesContext();
				if ( temp_sd == null ) {
					context.flagError("Unable to determine the species of self",
						IGamlIssue.GENERAL, object);
					return null;
				}
				IType tt = temp_sd.getType();
				return factory.createVar(SELF, tt, tt, true, IVarExpression.SELF, null);
			}
			if ( s.equalsIgnoreCase(WORLD_AGENT_NAME) ) { return getWorldExpr(); }
			if ( isSkillName(s) ) { return skill(s); }

			// By default, we try to find a variable

			temp_sd = context == null ? null : context.getDescriptionDeclaringVar(s);
			if ( temp_sd != null ) { return temp_sd.getVarExpr(s); }
			if ( context != null ) {

				// Short circuiting the use of keyword in "draw ..." to ensure backward
				// compatibility while providing a useful warning.

				if ( context.getKeyword().equals(DRAW) ) {
					if ( DrawStatement.SHAPES.keySet().contains(s) ) {
						context.flagWarning("The symbol " + s +
							" is not used anymore in draw. Please use geometries instead, e.g. '" +
							s + "(size)'", IGamlIssue.UNKNOWN_KEYWORD, object, s);
					}
					return factory.createConst(s + "__deprecated", Types.get(IType.STRING));
				}

				context.flagError("The variable " + s +
					" has not been previously defined. Check its name or declare it",
					IGamlIssue.UNKNOWN_VAR, object, s);
				// return factory.createVar(s, Types.NO_TYPE, Types.NO_TYPE, true,
				// IVarExpression.TEMP, context);
			}
			return null;

		}

		@Override
		public IExpression caseVarDefinition(final VarDefinition object) {
			return skill(object.getName());
		}

		@Override
		public IExpression caseTypeDefinition(final TypeDefinition object) {
			return caseVar(object.getName(), object);
		}

		@Override
		public IExpression caseSkillFakeDefinition(final SkillFakeDefinition object) {
			return caseVar(object.getName(), object);
		}

		@Override
		public IExpression caseReservedLiteral(final ReservedLiteral object) {
			return caseVar(EGaml.getKeyOf(object), object);
		}

		@Override
		public IExpression caseIf(final If object) {

			IExpression ifFalse = compile(object.getIfFalse());
			IExpression alt =
				factory.createOperator(":", context, compile(object.getRight()), ifFalse);
			return factory.createOperator("?", context, compile(object.getLeft()), alt);
		}

		@Override
		public IExpression caseArgumentPair(final ArgumentPair object) {
			return binary("::", caseVar(EGaml.getKeyOf(object), object), object.getRight());
		}

		@Override
		public IExpression casePair(final Pair object) {
			return binary(EGaml.getKeyOf(object), object.getLeft(), object.getRight());
		}

		@Override
		public IExpression caseBinary(final Binary object) {
			return binary(EGaml.getKeyOf(object), object.getLeft(), object.getRight());
		}

		@Override
		public IExpression caseUnit(final Unit object) {
			// We simply return a multiplication, since the right member (the "unit") will be
			// translated into its float value
			return binary("*", object.getLeft(), object.getRight());
		}

		@Override
		public IExpression caseUnary(final Unary object) {
			return unary(EGaml.getKeyOf(object), object.getRight());
		}

		@Override
		public IExpression caseDot(final Dot object) {
			return compileFieldExpr(object.getLeft(), object.getRight());
		}

		@Override
		public IExpression caseAccess(final Access object) {
			List<? extends Expression> list = EGaml.getExprsOf(object.getArgs());
			List<IExpression> result = new ArrayList();
			for ( int i = 0, n = list.size(); i < n; i++ ) {
				Expression eExpr = list.get(i);
				IExpression e = compile(eExpr);
				result.add(e);
			}
			IExpression container = compile(object.getLeft());
			IExpression indices = factory.createList(result);
			return factory.createOperator("internal_at", context, container, indices);
		}

		@Override
		public IExpression caseArray(final Array object) {
			List<? extends Expression> list = EGaml.getExprsOf(object.getExprs());
			List<IExpression> result = new ArrayList();
			boolean allPairs = true;
			for ( int i = 0, n = list.size(); i < n; i++ ) {
				Expression eExpr = list.get(i);
				allPairs = allPairs && eExpr instanceof Pair;
				IExpression e = compile(eExpr);
				result.add(e);
			}
			if ( allPairs && !list.isEmpty() ) { return factory.createMap(result); }
			return factory.createList(result);
		}

		@Override
		public IExpression casePoint(final Point object) {
			IExpression point2d =
				binary(IExpressionCompiler.INTERNAL_POINT, object.getLeft(), object.getRight());
			Expression z = object.getZ();
			return z == null ? point2d : binary(IExpressionCompiler.INTERNAL_Z, point2d, z);
		}

		@Override
		public IExpression caseParameters(final Parameters object) {
			IList<IExpression> list = new GamaList();
			for ( Expression p : EGaml.getExprsOf(object.getParams()) ) {
				list.add(binary("::",
					factory.createConst(EGaml.getKeyOf(p.getLeft()), Types.get(IType.STRING)),
					p.getRight()));
			}
			return factory.createMap(list);
		}

		@Override
		public IExpression caseFunction(final Function object) {
			String op = EGaml.getKeyOf(object);
			List<Expression> args = EGaml.getExprsOf(object.getArgs());
			int size = args.size();
			if ( size == 1 ) { return unary(op, args.get(0)); }
			if ( size == 2 ) { return binary(op, args.get(0), args.get(1)); }
			IExpression[] compiledArgs = new IExpression[size];
			for ( int i = 0; i < size; i++ ) {
				compiledArgs[i] = compile(args.get(i));
			}
			return factory.createOperator(op, context, compiledArgs);
		}

		@Override
		public IExpression caseIntLiteral(final IntLiteral object) {
			try {
				Integer val = Integer.parseInt(EGaml.getKeyOf(object), 10);
				return factory.createConst(val, Types.get(IType.INT));
			} catch (NumberFormatException e) {
				context.flagError("Malformed integer: " + EGaml.getKeyOf(object),
					IGamlIssue.UNKNOWN_NUMBER, object);
				return null;
			}
		}

		@Override
		public IExpression caseDoubleLiteral(final DoubleLiteral object) {
			try {
				String s = EGaml.getKeyOf(object);
				if ( s == null ) { return null; }
				Number val = nf.parse(s);
				return factory.createConst(val.doubleValue(), Types.get(IType.FLOAT));
			} catch (ParseException e) {
				context.flagError("Malformed float: " + EGaml.getKeyOf(object),
					IGamlIssue.UNKNOWN_NUMBER, object);
				return null;
			}

		}

		@Override
		public IExpression caseColorLiteral(final ColorLiteral object) {
			try {
				Integer val = Integer.parseInt(EGaml.getKeyOf(object).substring(1), 16);
				return factory.createConst(val, Types.get(IType.INT));
			} catch (NumberFormatException e) {
				context.flagError("Malformed integer: " + EGaml.getKeyOf(object),
					IGamlIssue.UNKNOWN_NUMBER, object);
				return null;
			}
		}

		@Override
		public IExpression caseStringLiteral(final StringLiteral object) {
			return factory.createConst(StringUtils.unescapeJava(EGaml.getKeyOf(object)),
				Types.get(IType.STRING));
		}

		@Override
		public IExpression caseBooleanLiteral(final BooleanLiteral object) {
			String s = EGaml.getKeyOf(object);
			if ( s == null ) { return null; }
			return s.equalsIgnoreCase(TRUE) ? TRUE_EXPR : FALSE_EXPR;
		}

		@Override
		public IExpression defaultCase(final EObject object) {
			// if ( object instanceof Expression ) {
			// getContext().flagError("Unrecognized expression " + EGaml.toString(object),
			// IGamlIssue.GENERAL, object);
			// } else {}
			getContext().flagError("Cannot compile: " + object, IGamlIssue.GENERAL, object);
			return null;
		}

	};

	private static GamlResource getFreshResource() {
		if ( resource == null ) {
			XtextResourceSet rs = EGaml.getInstance(XtextResourceSet.class);
			rs.setClasspathURIContext(EcoreBasedExpressionDescription.class);
			IResourceFactory resourceFactory = EGaml.getInstance(IResourceFactory.class);
			URI uri = URI.createURI("dummy.gaml");
			resource = (GamlResource) resourceFactory.createResource(uri);
			rs.getResources().add(resource);
		} else {
			resource.unload();
		}
		return resource;
	}

	private static GamlResource resource;

	private static EObject getEObjectOf(final IExpressionDescription s) {
		EObject o = s.getTarget();
		if ( o == null && s instanceof StringBasedExpressionDescription ) {
			o = getEObjectOf(s.toString());
		}
		return o;
	}

	private static EObject getEObjectOf(final String string) throws GamaRuntimeException {
		String s = "dummy <- " + string;
		GamlResource resource = getFreshResource();
		InputStream is = new ByteArrayInputStream(s.getBytes());
		try {
			resource.load(is, null);
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}

		if ( resource.getErrors().isEmpty() ) {
			EObject e = resource.getContents().get(0);
			if ( e instanceof StringEvaluator ) { return ((StringEvaluator) e).getExpr(); }
		} else {
			Diagnostic d = resource.getErrors().get(0);
			throw new GamaRuntimeException(d.getMessage());
		}
		return null;

	}

}
