package de.yard.threed.osm2graph.viewer.model;


import de.yard.threed.osm2graph.osm.MainGrid;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Observable;

public class Data extends Observable {
	
	private Configuration config = new BaseConfiguration();
	private File osmFile = null;
//	private ConversionFacade.Results conversionResults = null;
	Logger logger = Logger.getLogger(Data.class);
	public MainGrid mainGrid;

	public Configuration getConfig() {
		return config;
	}

	public void setConfig(Configuration config) {
		
		this.config = config;
		
		this.setChanged();
		this.notifyObservers();
		
	}
	
	/**
	 * 
	 */
	/*public void loadOSMData(OSMDataReader reader, boolean failOnLargeBBox,
							Factory<? extends TerrainInterpolator> interpolatorFactory,
							Factory<? extends EleConstraintEnforcer> enforcerFactory,
							ConversionFacade.ProgressListener listener)
					throws IOException, BoundingBoxSizeException {
		
		try {
			logger.debug("loading osm");
			if (reader instanceof StrictOSMFileReader) {
				this.osmFile = ((StrictOSMFileReader)reader).getFile();
			} else {
				this.osmFile = null;
			}
			
			ConversionFacade converter = new ConversionFacade();
			converter.setTerrainEleInterpolatorFactory(interpolatorFactory);
			converter.setEleConstraintEnforcerFactory(enforcerFactory);
			
			converter.addProgressListener(listener);
			
			if (failOnLargeBBox) {
				config.addProperty("maxBoundingBoxDegrees", 1);
			}
			
			logger.debug("creating Representations");
			conversionResults = converter.createRepresentations(
					reader.getData(), null, config, null);
			
		} catch (IOException e) {
			
			osmFile = null;
			conversionResults = null;
			
			throw e;
			
		} catch (BoundingBoxSizeException e) {
			
			osmFile = null;
			conversionResults = null;
			
			throw e;
			
		} finally {
			
			config.clearProperty("maxBoundingBoxDegrees");
			
		}
		
		this.setChanged();
		this.notifyObservers();
		
	}*/
	
	public File getOsmFile() {
		return osmFile;
	}
	
	/*public ConversionFacade.Results getConversionResults() {
		return conversionResults;
	}

	public void setConversionResults(ConversionFacade.Results conversionResults) {
		this.conversionResults = conversionResults;
	}*/

	public void setMainGrid(MainGrid mainGrid) {
		this.mainGrid = mainGrid;
	}
}
