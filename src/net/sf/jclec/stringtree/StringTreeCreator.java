package net.sf.jclec.stringtree;

import gpemlc.IndividualCreator;
import net.sf.jclec.ISpecies;

import net.sf.jclec.base.AbstractCreator;

/**
 * Creation of BinArrayIndividual (and subclasses).
 *  
 * @author Sebastian Ventura
 */

public class StringTreeCreator extends AbstractCreator 
{
	/////////////////////////////////////////////////////////////////
	// --------------------------------------- Serialization constant
	/////////////////////////////////////////////////////////////////
	
	/** Generated by Eclipse */
	
	private static final long serialVersionUID = -2638928425169895614L;

	/////////////////////////////////////////////////////////////////
	// ------------------------------------------- Internal variables
	/////////////////////////////////////////////////////////////////
	
	// Operation variables
	
	/** Associated species */
	
	protected transient StringTreeSpecies species;
	
	/** Genotype schema */
	
	protected transient String schema;
	
	/**
	 * Max number of children at each node
	 */
	int maxChildren;
	
	/**
	 * Max depth of the tree
	 */
	int maxDepth;
	
	/**
	 * Max value for the leaves
	 */
	int nMax;
	

	/////////////////////////////////////////////////////////////////
	// ------------------------------------------------- Constructors
	/////////////////////////////////////////////////////////////////
	
	public StringTreeCreator() 
	{
		super();
	}

	/////////////////////////////////////////////////////////////////
	// ----------------------------------------------- Public methods
	/////////////////////////////////////////////////////////////////
	
	public void setMaxChildren(int maxChildren) {
		this.maxChildren = maxChildren;
	}
	
	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}
	
	public void setnMax(int nMax) {
		this.nMax = nMax;
	}
	
	
	// java.lang.Object methods
	
	/**
	 * {@inheritDoc}
	 */
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof StringTreeCreator){
			return true;
		}
		else {
			return false;
		}
	}

	/////////////////////////////////////////////////////////////////
	// -------------------------------------------- Protected methods
	/////////////////////////////////////////////////////////////////
	
	// AbstractCreator methods
	
	/**
	 * {@inheritDoc}
	 */
	
	@Override
	protected void prepareCreation() 
	{
		ISpecies spc = context.getSpecies();
		if (spc instanceof StringTreeSpecies) {
			// Sets individual species
			this.species = (StringTreeSpecies) spc;
			// Sets genotype schema
			this.schema = this.species.getGenotypeSchema();
		}
		else {
			throw new IllegalStateException("Illegal species in context");
		}
	}

	/* 
	 * Este mtodo fija el schema que vamos a utilizar para crear los genotipos
	 * de los nuevos individuos. Para ello, asegura que el objeto species que
	 * representa a los individuos de la poblacin es de tipo IBinArraySpecies.
	 * En caso negativo, lanza una excepcin.
	 */

	/**
	 * {@inheritDoc}
	 */
	
	@Override
	protected void createNext() 
	{
		createdBuffer.add(species.createIndividual(createGenotype()));
	}
	
	/* 
	 * Este objeto crea los cromosomas de los individuos consultando su schema. Para 
	 * la posicin i-sima del schema, si el valor de ste es '0' o '1' asignar el
	 * mismo valor presente en el schema. Si el valor es '*' asignar un valor del
	 * conjunto {0,1} elegido al azar.
	 */

	/////////////////////////////////////////////////////////////////
	// ---------------------------------------------- Private methods
	/////////////////////////////////////////////////////////////////
	
	/**
	 * Create a byte [] genotype, filling it randomly
	 */
	private final String createGenotype()
	{
		IndividualCreator creator = new IndividualCreator();
		return creator.create(nMax, maxDepth, maxChildren);
	}
	
	/**
	 * Creates a child in the form (C ... C) including n times the 'C'
	 * 
	 * @param n Number of children
	 * @return String
	 */
	private String child(int n) {
		String child = "(";
		for(int i=0; i<n; i++) {
			child += "C ";
		}
		child = child.substring(0, child.length()-1) + ")";
		
		return child;
	}
	
	/**
	 * Creates a child in the form (C ... C) including the 'C' a random number of times between [2, max]
	 * 
	 * @param maxChildren Max number of children
	 * @return String
	 */
	private String childRandomSize(int maxChildren) {
		int currNChild = randgen.choose(2, maxChildren+1); //between 2 and max, included
		return child(currNChild);
	}
	
	/**
	 * Replace a given position in the string with a new string
	 * 
	 * @param str Original string 
	 * @param pos Position to replace
	 * @param newStr String to insert in position 'pos'
	 * @return Modified string
	 */
	public String replace(String str, int pos, String newStr) {
		return str.substring(0, pos) + newStr + str.substring(pos+1, str.length());
	}
}