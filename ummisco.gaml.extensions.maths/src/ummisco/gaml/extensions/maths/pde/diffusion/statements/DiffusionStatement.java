package ummisco.gaml.extensions.maths.pde.diffusion.statements;

import msi.gama.common.interfaces.IKeyword;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.metamodel.topology.grid.GamaSpatialMatrix.GridPopulation;
import msi.gama.precompiler.GamlAnnotations.facet;
import msi.gama.precompiler.GamlAnnotations.facets;
import msi.gama.precompiler.GamlAnnotations.inside;
import msi.gama.precompiler.GamlAnnotations.symbol;
import msi.gama.precompiler.*;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.matrix.GamaFloatMatrix;
import msi.gama.util.matrix.IMatrix;
import msi.gaml.descriptions.*;
import msi.gaml.statements.AbstractStatementSequence;
import msi.gaml.types.IType;

@facets(value = { @facet(name = "var", type = IType.ID, optional = false),
		@facet(name = "on", type = IType.ID, optional = false),
		@facet(name = "mat_diffu", type = IType.MATRIX, optional = false),
		@facet(name = "mask", type = IType.MATRIX, optional = true),
		@facet(name = "cycle_length", type = IType.INT, optional = true) }, omissible = IKeyword.EQUATION)
@symbol(name = { "diffusion" }, kind = ISymbolKind.SEQUENCE_STATEMENT, with_sequence = true)
// , with_args = true)
@inside(kinds = { ISymbolKind.BEHAVIOR, ISymbolKind.SINGLE_STATEMENT,
		ISymbolKind.SPECIES, ISymbolKind.MODEL })
public class DiffusionStatement extends AbstractStatementSequence {

	double time_initial = 0, time_final = 1;
	int discret = 0;
	double cycle_length = 1;

	public DiffusionStatement(final IDescription desc) {
		super(desc);

		// List<IDescription> statements =
		// desc.getSpeciesContext().getChildren();
		// String eqName = getFacet("diffusion").literalValue();

		// Based on the facets, choose a solver and init it;

	}

	public void doDiffusion(IScope scope, String varName, String speciesName,
			IMatrix mat_diffu) {

		GridPopulation pop = (GridPopulation) scope.getAgentScope()
				.getPopulationFor(speciesName);
		IAgent[] lstAgents = pop.toArray();
		IMatrix mmm = pop.matrixValue(scope);
		int cols = mmm.getCols(scope);
		int rows = mmm.getRows(scope);
		

		IMatrix<Double> mask = new GamaFloatMatrix(scope,cols, rows);
		
		if (getFacet("mask") != null) {
			mask = (IMatrix) getFacet("mask").value(scope);
		}
		

		int kRows=mat_diffu.getCols(scope);
		int kCols=mat_diffu.getRows(scope);
		
		int kCenterX = kRows / 2;
		int kCenterY = kCols / 2;
		GamaFloatMatrix tmp = new GamaFloatMatrix(scope, cols, rows);

		// System.out.println(xcenter+" "+ycenter);
		// find center position of kernel (half of kernel size)
		float sum=0;
		int mm=0, nn=0, ii=0, jj=0;

		for(int i=0; i < rows; ++i)              // rows
		{
		    for(int j=0; j < cols; ++j)          // columns
		    {
		        sum = 0;                     // init to 0 before sum

		        for(int m=0; m < kRows; ++m)     // kernel rows
		        {
		            mm = kRows - 1 - m;      // row index of flipped kernel

		            for(int n=0; n < kCols; ++n) // kernel columns
		            {
		                nn = kCols - 1 - n;  // column index of flipped kernel

		                // index of input signal, used for checking boundary
		                ii = i + (m - kCenterY);
		                jj = j + (n - kCenterX);

		                // ignore input samples which are out of bound
		                if( ii >= 0 && ii < rows && jj >= 0 && jj < cols ){
//		                out[i][j] = out[i][j] + in[ii][jj] * kernel[mm][nn];
		                	double inValue=Double.parseDouble(""+lstAgents[ii * rows + jj].getAttribute(varName));
		                	double currentValue=Double.parseDouble(""+tmp.get(scope, i,j));
		                	double currentMatValue=Double.parseDouble(""+mat_diffu.get(scope, mm,nn));
		                	double newValue=currentValue+ inValue * currentMatValue;
		                	tmp.set(scope, i, j, newValue);
		                }
		            }
		        }
		    }
		}

//		for (int i = 0; i < cols; i++) {
//			for (int j = 0; j < rows; j++) {
//				double currentValue = Double.parseDouble(""
//						+ lstAgents[i * rows + j].getAttribute(varName));
//				if (currentValue <= Double.MAX_VALUE
//						&& currentValue >= - (Double.MAX_VALUE)) { 
////						&& i >= xcenter
////						&& j >= ycenter && i < cols - xcenter
////						&& j < rows - ycenter) {
//
//					for (int u = i - xcenter; u <= i + xcenter; u++) {
//						for (int v = j - ycenter; v <= j + ycenter; v++) {
//							if(u<0 || v<0 || u>=cols || v>=rows){
//								continue;
//							}
//							// System.out.println(u+" "+v+" | "+( u - i +
//							// xcenter)+" "+(v - j + ycenter));
//							double currentMatValue = Double.parseDouble(""
//									+ mat_diffu.get(scope, u - i + xcenter, v
//											- j + ycenter));
//							double currentMask=(Double.parseDouble(""+mask.get(scope,i, j))<-1)?0:1;
////							System.out.println(currentMask);
//							double newValue = tmp.get(scope, u, v)
//									+ currentValue * currentMatValue * currentMask;
//							tmp.set(scope, u, v, newValue);
//						}
//					}
//					// System.out.println();
//				}
//			}
//		}


		for (int i = 0; i < cols; i++) {
			for (int j = 0; j < rows; j++) {
				lstAgents[i * rows + j].setAttribute(varName,
						tmp.get(scope, i, j));
			}
		}

	}

	@Override
	public Object privateExecuteIn(final IScope scope)
			throws GamaRuntimeException {
		int cLen = 1;
		if (getFacet("cycle_length") != null) {
			cLen = Integer.parseInt("" + getFacet("cycle_length").value(scope));
		}

		String varName = (String) getFacet("var").value(scope);
		String speciesName = (String) getFacet("on").value(scope);
		IMatrix mat_diffu = (IMatrix) getFacet("mat_diffu").value(scope);
		
		

		for (int time = 0; time < cLen; time++) {
			doDiffusion(scope, varName, speciesName, mat_diffu);
		}
		// super.privateExecuteIn(scope);
		// if (getFacet("cycle_length") != null) {
		// cycle_length = Double.parseDouble(""
		// + getFacet("cycle_length").value(scope));
		// }

		// System.out.println(tcc);
		return null;
	}
}