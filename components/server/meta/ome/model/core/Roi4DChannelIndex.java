package ome.model.core;

import ome.util.BaseModelUtils;


import java.util.*;




/**
 * Roi4DChannelIndex generated by hbm2java
 */
public class
Roi4DChannelIndex extends ome.model.core.Roi4D
implements java.io.Serializable ,
ome.api.OMEModel
, ome.NewModel 
{

    // Fields    

     private Integer indexC;


    // Constructors

    /** default constructor */
    public Roi4DChannelIndex() {
    }
    
   
    
    

    // Property accessors

    /**
     * 
     */
    public Integer getIndexC() {
        return this.indexC;
    }
    
    public void setIndexC(Integer indexC) {
        this.indexC = indexC;
    }





	/** utility methods. Container may re-assign this. */	
	protected static BaseModelUtils _utils = 
		new BaseModelUtils();
	public BaseModelUtils getUtils(){
		return _utils;
	}
	public void setUtils(BaseModelUtils utils){
		_utils = utils;
	}



}
