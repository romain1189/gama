/*********************************************************************************************
 * 
 *
 * 'ISpatialIndex.java', in plugin 'msi.gama.core', is part of the source code of the 
 * GAMA modeling and simulation platform.
 * (c) 2007-2014 UMI 209 UMMISCO IRD/UPMC & Partners
 * 
 * Visit https://code.google.com/p/gama-platform/ for license information and developers contact.
 * 
 * 
 **********************************************************************************************/
package msi.gama.metamodel.topology;

import java.awt.Graphics2D;
import java.util.Collection;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.metamodel.shape.IShape;
import msi.gama.metamodel.topology.filter.IAgentFilter;
import msi.gama.runtime.IScope;
import msi.gaml.species.ISpecies;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Written by drogoul Modified on 23 f�vr. 2011
 * 
 * @todo Description
 * 
 */
public interface ISpatialIndex {

	// public final static Envelope ENVELOPE = new Envelope();

	public abstract void insert(IAgent agent);

	public abstract void remove(final IShape previous, final IAgent agent);

	public abstract IAgent firstAtDistance(IScope scope, final IShape source, final double dist, final IAgentFilter f);

	public abstract Collection<IAgent> allInEnvelope(IScope scope, final IShape source, final Envelope envelope,
		final IAgentFilter f, boolean contained);

	public abstract void drawOn(Graphics2D g2, int width, int height);

	Collection<IAgent> allAtDistance(IScope scope, IShape source, double dist, IAgentFilter f);

	public interface Compound extends ISpatialIndex {

		public abstract void add(ISpatialIndex index, ISpecies species);

		public abstract void dispose();
	}

}