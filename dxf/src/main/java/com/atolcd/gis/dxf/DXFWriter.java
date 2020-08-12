package com.atolcd.gis.dxf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Envelope;

import fr.michaelm.jump.drivers.dxf.DxfENTITY;
import fr.michaelm.jump.drivers.dxf.DxfFile;
import fr.michaelm.jump.drivers.dxf.DxfXDATA;

public class DXFWriter {
	
	private String dxfFileName;
	private List<Layer> layers;

	public DXFWriter(String fileName) {

		int pointIndex = fileName.lastIndexOf('.');
	    if (pointIndex > 0) {
	    	fileName =  fileName.substring(0,pointIndex);
	    }
	    
	    this.dxfFileName = fileName.concat(".dxf");
		this.layers = new ArrayList<Layer>();

	}
	
	public void addLayer(Layer layer){
		this.layers.add(layer);
	}
	
	public void write(int precision, boolean writeXData) throws IOException{

		List<String> layerNames = new ArrayList<String>();
		Envelope extent = null;
		List<DxfENTITY> entities =  new ArrayList<DxfENTITY>();
		
		for(Layer layer : this.layers){
			
			//Alimente la liste des layers
			layerNames.add(layer.getName());
			
			//Calcul de l'extent globale
			for (Entity entity:layer.getEntities()){
				
				if(extent != null){
					extent.expandToInclude(entity.getGeometry().getEnvelopeInternal());
				}else{
					extent = entity.getGeometry().getEnvelopeInternal();
				}
				
				
				//Conversion Entity -> DxfEntity
				DxfENTITY dxfENTITY = new DxfENTITY(layer.getName(), entity.getGeometry());
				
				for(ExtendedData extendedData : entity.getExtendedData()){
					
					int code;
					
					if(extendedData.getType().equals(String.class)){
						code = DxfXDATA.GROUPCODE_XDATA_STRING;
						
					}else if(extendedData.getType().equals(Double.class)){
						code = DxfXDATA.GROUPCODE_XDATA_REAL;
						
					}else if(extendedData.getType().equals(Integer.class)){
						code = DxfXDATA.GROUPCODE_XDATA_INTEGER;
						
					}else if(extendedData.getType().equals(Long.class)){
						code = DxfXDATA.GROUPCODE_XDATA_LONG;
						
					}else{
						code = DxfXDATA.GROUPCODE_XDATA_STRING;
					}

					dxfENTITY.addXData(new DxfXDATA(extendedData.getName(), code, extendedData.getValue()));
					
				}
				
				entities.add(dxfENTITY);
				
			}
			
		}

		DxfFile.write(this.dxfFileName, layerNames, extent, entities ,precision, writeXData);

	}

}
