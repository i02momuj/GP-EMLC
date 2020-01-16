package g3pkemlc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Date;
import java.util.List;
import java.util.Arrays;
import java.util.Comparator;

import net.sf.jclec.IFitness;
import net.sf.jclec.IConfigure;
import net.sf.jclec.IIndividual;
import net.sf.jclec.AlgorithmEvent;
import net.sf.jclec.IAlgorithmListener;

import net.sf.jclec.algorithm.PopulationAlgorithm;
import net.sf.jclec.fitness.SimpleValueFitness;
import net.sf.jclec.stringtree.StringTreeIndividual;
import net.sf.jclec.util.IndividualStatistics;

import org.apache.commons.configuration.Configuration;

import org.apache.commons.lang.builder.EqualsBuilder;

import g3pkemlc.utils.TreeUtils;
import g3pkemlc.utils.Utils;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.Evaluation;
import mulan.evaluation.measure.Measure;

/**
 * This class is a listener for PopulationAlgorithms, that performs a report of 
 * the actual population. This report consists on ...
 * 
 * @author Sebastian Ventura
 */

public class Listener implements IAlgorithmListener, IConfigure 
{
	/////////////////////////////////////////////////////////////////
	// --------------------------------------- Serialization constant
	/////////////////////////////////////////////////////////////////
	
	/** Generated by Eclipse */
	
	private static final long serialVersionUID = -6866004037911080430L;

	/////////////////////////////////////////////////////////////////
	// --------------------------------------------------- Properties
	/////////////////////////////////////////////////////////////////

	/** Name of the report*/
	
	private String reportTitle;
		
	/** Report frequency */
	
	private int reportFrequency;
	
	/** Show report on console? */
	
	private boolean reportOnConsole; 

	/** Write report on file? */

	private boolean reportOnFile; 
	
	/** Save all population individuals? */
	
	private boolean saveCompletePopulation;
	
	/** Report frequency for testing */
	
	private int testReportFrequency;
	
	Utils utils = new Utils();
	
	/** File to store the number of leaves of the best individual at each generation */
    String bestLeavesFilename = "reports/bestLeaves.csv";
    
    /** File to store the maximum depth of best individual at each generation */
    String bestMaxDepthFilename = "reports/bestMaxDepth.csv";
    
    /** File to store the best tree at the end of the algorithm */
    String bestTreeFilename = "reports/bestTree.csv";
    
    /** File to store the fitness of the best individual at each generation */
    String bestFilename = "reports/bestFitness.csv";
    
    /** File to store the average fitness of the individuals at each generation */
    String avgFilename = "reports/avgFitness.csv";
    
    /** File to store the average fitness of the individuals at each generation */
    String medianFilename = "reports/medianFitness.csv";
    
    /** File to store the worst fitness of the individuals at each generation */
    String worstFilename = "reports/worstFitness.csv";
    
    /** File to store evaluation over test set of final ensemble */
    String classificationReportFilename = "reports/testResults.csv";
    
    /** File to store evaluation over test set of given generation ensemble */
    String iterTestReportFilename = "reports/iter_testResults.csv";
	
	private long time_start;
	
	/////////////////////////////////////////////////////////////////
	// ------------------------------------------- Internal variables
	/////////////////////////////////////////////////////////////////

	/** Report file */
	
	private File reportFile;

	/** Report file writer */
	
	private FileWriter reportFileWriter;
	
	/** Directory for saving complete populations */
	
	private File reportDirectory;

	/////////////////////////////////////////////////////////////////
	// ------------------------------------------------- Constructors
	/////////////////////////////////////////////////////////////////	
	
	public Listener() 
	{
		super();
	}

	/////////////////////////////////////////////////////////////////
	// ----------------------------------------------- Public methods
	/////////////////////////////////////////////////////////////////	
	
	// Setting and getting properties
	
	public final String getReportTitle() 
	{
		return reportTitle;
	}

	public final void setReportTitle(String reportTitle) 
	{
		this.reportTitle = reportTitle;
	}

	public final int getReportFrequency() 
	{
		return reportFrequency;
	}

	public final void setReportFrequency(int reportFrequency) 
	{
		this.reportFrequency = reportFrequency;
	}

	public boolean isReportOnCconsole() {
		return reportOnConsole;
	}

	public final void setReportOnCconsole(boolean reportOnCconsole) 
	{
		this.reportOnConsole = reportOnCconsole;
	}

	public final boolean isReportOnFile() 
	{
		return reportOnFile;
	}

	public final void setReportOnFile(boolean reportOnFile) 
	{
		this.reportOnFile = reportOnFile;
	}

	public final boolean isSaveCompletePopulation() 
	{
		return saveCompletePopulation;
	}

	public final void setSaveCompletePopulation(boolean saveCompletePopulation) 
	{
		this.saveCompletePopulation = saveCompletePopulation;
	}

	// IConfigure interface
	
	@Override
	public void configure(Configuration settings) 
	{
		time_start = System.currentTimeMillis();
		// Set report title (default "untitled")
		String reportTitle = settings.getString("report-title", "untitled");
		setReportTitle(reportTitle);
		// Set report frequency (default 10 generations)
		int reportFrequency = settings.getInt("report-frequency", 10); 
		setReportFrequency(reportFrequency);
		// Set console report (default on)
		boolean reportOnConsole = settings.getBoolean("report-on-console", false);
		setReportOnCconsole(reportOnConsole);
		// Set file report (default off)
		boolean reportOnFile = settings.getBoolean("report-on-file", false);
		setReportOnFile(reportOnFile);
		// Set save individuals (default false)
		boolean saveCompletePopulation = settings.getBoolean("save-complete-population", false);
		setSaveCompletePopulation(saveCompletePopulation);	
		
		testReportFrequency = settings.getInt("test-report-frequency", Integer.MAX_VALUE); 
	}

	// IAlgorithmListener interface
	
	@Override
	public void algorithmStarted(AlgorithmEvent event) 
	{		
		// Create report title for this instance
		String dateString = 
			new Date(System.currentTimeMillis()).toString().replace(':','.');
		String actualReportTitle = reportTitle+dateString;
		// If save complete population create a directory for storing
		// individual population files 
		if (saveCompletePopulation) {
			reportDirectory = new File(actualReportTitle);
			if (!reportDirectory.mkdir()) {
				throw new RuntimeException("Error creating report directory");
			}
		}
		// If report is stored in a text file, create report file
		if (reportOnFile) {
			reportFile = new File(actualReportTitle+".report.txt");
			try {
				reportFileWriter = new FileWriter(reportFile);
				reportFileWriter.flush();
				reportFileWriter.write(dateString+"\n");
			} 
			catch (IOException e) {
				e.printStackTrace();
			}			
		}
		// Do an iteration report
		doIterationReport((PopulationAlgorithm) event.getAlgorithm(), true);
	}

	@Override
	public void iterationCompleted(AlgorithmEvent event)
	{
		doIterationReport((PopulationAlgorithm) event.getAlgorithm(), false);
	}

	@Override
	public void algorithmFinished(AlgorithmEvent event) 
	{
		long time = System.currentTimeMillis() - time_start;
		
		// Do last generation report
		doIterationReport((PopulationAlgorithm) event.getAlgorithm(), true);
		
		BufferedWriter bestLeavesWriter = null;
		BufferedWriter bestMaxDepthWriter = null;
		BufferedWriter bestTreeWriter = null;
		BufferedWriter bestWriter = null;
		BufferedWriter medianWriter = null;
		BufferedWriter avgWriter = null;
		BufferedWriter worstWriter = null;
		BufferedWriter classificationReportWriter = null;
		
		try {
			bestLeavesWriter = new BufferedWriter(new FileWriter(bestLeavesFilename, true));
			bestLeavesWriter.write("\n");
			bestLeavesWriter.close();
			
			bestMaxDepthWriter = new BufferedWriter(new FileWriter(bestMaxDepthFilename, true));
			bestMaxDepthWriter.write("\n");
			bestMaxDepthWriter.close();
			
			// Fitness comparator, inhabitants, and best individual
			Comparator<IFitness> comparator = ((PopulationAlgorithm) event.getAlgorithm()).getEvaluator().getComparator();
			List<IIndividual> inhabitants = ((PopulationAlgorithm) event.getAlgorithm()).getInhabitants();
			IIndividual best = IndividualStatistics.bestIndividual(inhabitants, comparator);
			String bestGen = ((StringTreeIndividual)best).getGenotype();
			bestTreeWriter = new BufferedWriter(new FileWriter(bestTreeFilename, true));
			int [] votesPerLabel = TreeUtils.votesPerLabel(bestGen, ((Alg)event.getAlgorithm()).klabelsets, ((Alg)event.getAlgorithm()).fullTrainData.getNumLabels());
			double avgVotes = TreeUtils.avgVotes(votesPerLabel);
			bestTreeWriter.write(bestGen + " " + Arrays.toString(votesPerLabel) + "; " + avgVotes + "; " + utils.getLeaves(bestGen).size() + ";\n");
			bestTreeWriter.close();
			
			bestWriter = new BufferedWriter(new FileWriter(bestFilename, true));
			bestWriter.write("\n");
			bestWriter.close();
			
			medianWriter = new BufferedWriter(new FileWriter(medianFilename, true));
			medianWriter.write("\n");
			medianWriter.close();
			
			avgWriter = new BufferedWriter(new FileWriter(avgFilename, true));
			avgWriter.write("\n");
			avgWriter.close();
			
			worstWriter = new BufferedWriter(new FileWriter(worstFilename, true));
			worstWriter.write("\n");
			worstWriter.close();
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		EMLC ensemble = ((Alg)event.getAlgorithm()).getEnsemble();
		MultiLabelInstances testData = ((Alg)event.getAlgorithm()).getTestData();
		List<Measure> measures = utils.prepareMeasures(testData);
		Evaluation results;
		try {
			results = new Evaluation(measures, testData);
			mulan.evaluation.Evaluator eval = new mulan.evaluation.Evaluator();
			
			ensemble.resetSeed(((Alg)event.getAlgorithm()).getSeed());
			results = eval.evaluate(ensemble, testData, measures);
			
			//If the file didnt exist, print the header
			boolean printHeader = false;
			if(!utils.fileExist(classificationReportFilename)) {
				printHeader = true;
			}
			
			classificationReportWriter = new BufferedWriter(new FileWriter(classificationReportFilename, true));
			if(printHeader) {
				classificationReportWriter.write(" ; ");
				for(int i=0; i<results.getMeasures().size(); i++) {
					classificationReportWriter.write(results.getMeasures().get(i).getName() + "; ");
				}
				classificationReportWriter.write("time(ms); generations\n");
			}
			
			classificationReportWriter.write(testData.getDataSet().relationName() + "_" + ((Alg)event.getAlgorithm()).getSeed() + "; " + results.toCSV().replace(",", ".") + time + ";" + ((Alg)event.getAlgorithm()).getGeneration() + ";\n");
			classificationReportWriter.close();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		((Alg)event.getAlgorithm()).clear();
		
		// Close report file if necessary
		if (reportOnFile  && reportFile != null) {
			try {
				reportFileWriter.close();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void algorithmTerminated(AlgorithmEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	// java.lang.Object methods

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof Listener) {
			Listener cother = (Listener) other;
			EqualsBuilder eb = new EqualsBuilder();
			// reportTitle
			eb.append(reportTitle, cother.reportTitle);
			// reportFrequency
			eb.append(reportFrequency, cother.reportFrequency);
			// reportOnConsole
			eb.append(reportOnConsole, cother.reportOnConsole);
			// reportOnFile
			eb.append(reportOnFile, cother.reportOnFile);
			// saveCompletePopulation
			eb.append(saveCompletePopulation, cother.saveCompletePopulation);			
			return eb.isEquals();
		}
		else {
			return false;
		}
	}

	protected void doIterationReport(PopulationAlgorithm algorithm, boolean force)
	{
		// Fitness comparator
		Comparator<IFitness> comparator = algorithm.getEvaluator().getComparator();
		// Population individuals
		List<IIndividual> inhabitants = algorithm.getInhabitants();
		// Actual generation
		int generation = algorithm.getGeneration();
		
		// Check if this is correct generation
		if (force || generation%reportFrequency == 0) {
			// Do population report
			StringBuffer sb = new StringBuffer("Generation " + generation + " Report\n");
			// Best individual
			IIndividual best = IndividualStatistics.bestIndividual(inhabitants, comparator);		
			sb.append("Best individual: "+best+ ((SimpleValueFitness)best.getFitness()).getValue() +"\n");
			// Worst individual
			IIndividual worst = IndividualStatistics.worstIndividual(inhabitants, comparator);
			sb.append("Worst individual: "+worst+ ((SimpleValueFitness)worst.getFitness()).getValue() + "\n");
			// Median individual
			IIndividual median = IndividualStatistics.medianIndividual(inhabitants, comparator);
			sb.append("Median individual: "+median+  ((SimpleValueFitness)median.getFitness()).getValue() + "\n");		
			// Average fitness and fitness variance
			double [] avgvar = IndividualStatistics.averageFitnessAndFitnessVariance(inhabitants);
			sb.append("Average fitness = " + avgvar[0]+"\n");
			sb.append("Fitness variance = "+ avgvar[1]+"\n");
			
			// Write report string to the standard output (if necessary) 
			if (reportOnConsole) {
				System.out.println(sb.toString());
			}
			
			// Write string to the report file (if necessary) 
			if (reportOnFile) {
				try {
					reportFileWriter.write(sb.toString());
					reportFileWriter.flush();
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			BufferedWriter bestLeavesWriter = null;
			BufferedWriter bestMaxDepthWriter = null;
			BufferedWriter bestWriter = null;
			BufferedWriter medianWriter = null;
			BufferedWriter avgWriter = null;
			BufferedWriter worstWriter = null;
			
			try {
				bestLeavesWriter = new BufferedWriter(new FileWriter(bestLeavesFilename, true));
				bestLeavesWriter.write(utils.countLeaves(((StringTreeIndividual)best).getGenotype()) + "; ");
				bestLeavesWriter.close();
				
				bestMaxDepthWriter = new BufferedWriter(new FileWriter(bestMaxDepthFilename, true));
				bestMaxDepthWriter.write(utils.calculateTreeMaxDepth(((StringTreeIndividual)best).getGenotype()) + "; ");
				bestMaxDepthWriter.close();
				
				bestWriter = new BufferedWriter(new FileWriter(bestFilename, true));
				bestWriter.write(((SimpleValueFitness)best.getFitness()).getValue() + "; ");
				bestWriter.close();
				
				medianWriter = new BufferedWriter(new FileWriter(medianFilename, true));
				medianWriter.write(((SimpleValueFitness)median.getFitness()).getValue() + "; ");
				medianWriter.close();
				
				avgWriter = new BufferedWriter(new FileWriter(avgFilename, true));
				avgWriter.write(avgvar[0] + "; ");
				avgWriter.close();
				
				worstWriter = new BufferedWriter(new FileWriter(worstFilename, true));
				worstWriter.write(((SimpleValueFitness)worst.getFitness()).getValue() + "; ");
				worstWriter.close();
			} catch(Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		// Check if test report should be done
        if(generation % testReportFrequency == 0 && generation > 0) {
        	Alg alg = (Alg)algorithm;
            
            BufferedWriter testIterReportWriter = null;
            
            //Predict over test data and do test report
            EMLC ensemble = ((Alg)algorithm).getEnsemble();
            MultiLabelInstances testData = alg.getTestData();
            List<Measure> measures = utils.prepareMeasures(testData);
            Evaluation results;
            try {
                results = new Evaluation(measures, testData);
                mulan.evaluation.Evaluator eval = new mulan.evaluation.Evaluator();
                        
                ensemble.resetSeed(alg.getSeed());
                results = eval.evaluate(ensemble, testData, measures);
                        
                //If the file didn't exist, print the header
                boolean printHeader = false;
//                if(!Utils.fileExist(iterTestReportFilename)) {
//                    printHeader = true;
//                }
                        
                testIterReportWriter = new BufferedWriter(new FileWriter(iterTestReportFilename, true));
                if(printHeader) {
                    testIterReportWriter.write(" ; ; ");
                    for(int i=0; i<results.getMeasures().size(); i++) {
                        testIterReportWriter.write(results.getMeasures().get(i).getName() + "; ");
                    }
                    testIterReportWriter.write("time(ms);\n");
                }
                        
                testIterReportWriter.write(generation + ";" +testData.getDataSet().relationName() + "_" + alg.getSeed() + "; " + results.toCSV().replace(",", ".") + ";\n");
                testIterReportWriter.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
		
		
	}
}
