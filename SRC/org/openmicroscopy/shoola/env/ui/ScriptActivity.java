/*
 * org.openmicroscopy.shoola.env.ui.ScriptActivity 
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2010 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package org.openmicroscopy.shoola.env.ui;



//Java imports
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import javax.swing.JFrame;


//Third-party libraries

//Application-internal dependencies
import omero.model.OriginalFile;
import org.openmicroscopy.shoola.env.config.Registry;
import org.openmicroscopy.shoola.env.data.model.DownloadActivityParam;
import org.openmicroscopy.shoola.env.data.model.ScriptObject;
import org.openmicroscopy.shoola.util.ui.filechooser.FileChooser;
import pojos.FileAnnotationData;

/** 
 * Activity to run the specified scripts.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since 3.0-Beta4
 */
public class ScriptActivity	
	extends ActivityComponent
{

	/** Indicates to run the script. */
	public static final int	RUN = 0;
	
	/** Indicates to upload the script. */
	public static final int	UPLOAD = 1;
	
	/** The description of the activity. */
	private static final String		DESCRIPTION_RUN_CREATION = 
								"Running Script";
	
	/** The description of the activity when it is finished. */
	private static final String		DESCRIPTION_RUN_CREATED = 
		"Script Run terminated";
	
	/** The description of the activity. */
	private static final String		DESCRIPTION_UPLOAD_CREATION = 
									"Uploading Script";
	
	/** The description of the activity when finished. */
	private static final String		DESCRIPTION_UPLOAD_CREATED = 
		"Script uploaded";
	
	/** The description of the activity when cancelled. */
	private static final String		DESCRIPTION_UPLOAD_CANCEL = 
		"Upload cancelled";
	
	/** The description of the activity when cancelled. */
	private static final String		DESCRIPTION_RUN_CANCEL = "Run cancelled";
	
	/** The script to run. */
    private ScriptObject	script;
    
    /** One of the constants defined by this class. */
    private int 			index;
    
    /**
     * Creates a new instance.
     * 
     * @param viewer	The viewer this data loader is for.
     *               	Mustn't be <code>null</code>.
     * @param registry	Convenience reference for subclasses.
     * @param script	The script to run.
     * @param activity 	The activity associated to this loader.
     */
	public ScriptActivity(UserNotifier viewer, Registry registry,
			ScriptObject script, int index)
	{
		super(viewer, registry, DESCRIPTION_RUN_CREATION, script.getIcon(), 
				ActivityComponent.ADVANCED);
		switch (index) {
			case UPLOAD:
				type.setText(DESCRIPTION_UPLOAD_CREATION);
				setIndex(ActivityComponent.GENERAL);
				break;
		}
		
		if (script == null)
			throw new IllegalArgumentException("Parameters not valid.");
		this.script = script;
		this.index = index;
	}

	/**
	 * Creates a concrete loader.
	 * @see ActivityComponent#createLoader()
	 */
	protected UserNotifierLoader createLoader()
	{
		switch (index) {
			case UPLOAD:
				loader = new ScriptUploader(viewer,  registry, script, this);
				break;
			case RUN:
				loader = new ScriptRunner(viewer,  registry, script, this);
		}

		return loader;
	}

	/**
	 * Modifies the text of the component. 
	 * @see ActivityComponent#notifyActivityEnd()
	 */
	protected void notifyActivityEnd()
	{
		switch (index) {
			case UPLOAD:
				type.setText(DESCRIPTION_UPLOAD_CREATED);
				break;
			case RUN:
				type.setText(DESCRIPTION_RUN_CREATED);
		}
	}
	
	/**
	 * Modifies the text of the component. 
	 * @see ActivityComponent#notifyActivityCancelled()
	 */
	protected void notifyActivityCancelled()
	{
		switch (index) {
			case UPLOAD:
				type.setText(DESCRIPTION_UPLOAD_CANCEL);
				break;
			case RUN:
				type.setText(DESCRIPTION_RUN_CANCEL);
		}
	}
	
	/** Notifies to download the file. */
	protected void notifyDownload()
	{
		//Check name space.
		if (!(result instanceof FileAnnotationData)) {
			downloadButton.setEnabled(false);
			return;
		}
		final FileAnnotationData data = (FileAnnotationData) result;
		JFrame f = registry.getTaskBar().getFrame();
		FileChooser chooser = new FileChooser(f, FileChooser.SAVE, 
				"Download", "Select where to download the results.", null, 
				true);
		IconManager icons = IconManager.getInstance(registry);
		chooser.setTitleIcon(icons.getIcon(IconManager.DOWNLOAD_48));
		chooser.setSelectedFileFull(data.getFileName());
		chooser.setApproveButtonText("Download");
		chooser.addPropertyChangeListener(new PropertyChangeListener() {
		
			public void propertyChange(PropertyChangeEvent evt) {
				String name = evt.getPropertyName();
				if (FileChooser.APPROVE_SELECTION_PROPERTY.equals(name)) {
					File folder = (File) evt.getNewValue();
					if (data == null) return;
					OriginalFile f = (OriginalFile) data.getContent();
					IconManager icons = IconManager.getInstance(registry);
					DownloadActivityParam activity = 
						new DownloadActivityParam(f,
							folder, icons.getIcon(IconManager.DOWNLOAD_22));
					activity.setLegend(data.getDescription());
					activity.setLegendExtension(
							DownloadActivity.LEGEND_TEXT_CSV);
					viewer.notifyActivity(activity);
				}
			}
		});
		chooser.centerDialog();
	}
	
}
