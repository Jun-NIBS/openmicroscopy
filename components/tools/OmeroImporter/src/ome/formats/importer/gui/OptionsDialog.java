/*
 * ome.formats.importer.gui.OptionsDialog
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2005 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *------------------------------------------------------------------------------
 */

package ome.formats.importer.gui;


import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;

import layout.TableLayout;
import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.ImportConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Priority;

/**
 * @author "Brian W. Loranger"
 */
@SuppressWarnings("serial")
public class OptionsDialog extends JDialog implements ActionListener
{
    private GuiCommonElements       gui;

    private Integer                 dialogHeight = 300;
    private Integer                 dialogWidth = 374;

    private JTabbedPane tabbedPane;
    

    private JPanel mainPanel;
    private JPanel debugOptionsPanel;
    private JPanel  fileChooserPanel;
    private JPanel  singlePanePanel;
    private JPanel  triplePanePanel;

    private JRadioButton singlePaneBtn;
    private JRadioButton triplePaneBtn;
    
    public static final String SINGLE_PANE_IMAGE = "gfx/single_pane_fileChooser.png";
    public static final String TRIPLE_PANE_IMAGE = "gfx/triple_pane_fileChooser.png";
    
    private JButton             cancelBtn;
    private JButton             okBtn;

    public boolean    cancelled = true;

    /** Logger for this class. */
    private static Log          log     = LogFactory.getLog(OptionsDialog.class);
    
    final String allDescription = "This level of debugging turns on all logging. Use this option if you want to see all messages, " +
    		"but be mindful of the fact this can produce some very large log files.";

    final String debugDescription = "This level of debugging is useful when debugging the importer, and is primarily of interest " +
    		" to developers. This can also produce very large files.";

    final String errorDescription = "This level of debugging captures 'minor' bugs which are unlikely to cause the importer to " +
    		"fail or fatally crash. This is the default setting.";
    
    final String fatalDescription = "This level of debugging only captures 'fatal' bugs which are likely to cause the importer to " +
    		"crash and fail.";

    final String infoDescription = "This level of debugging captures messages provided by the developer. Most metadata " +
    		"messages are provided to the log file through this level of debugging information.";
    
    final String offDescription = "This option turns off all debugging information.";
    
    final DebugItem[] debugItems = {
            new DebugItem("All", Priority.ALL_INT, allDescription),
            new DebugItem("Debug",Priority.DEBUG_INT, debugDescription),
            new DebugItem("Error (Default)", Priority.ERROR_INT, errorDescription),
            new DebugItem("Fatal", Priority.FATAL_INT, fatalDescription),
            new DebugItem("Info", Priority.INFO_INT, infoDescription),
            new DebugItem("Off", Priority.OFF_INT, offDescription)            
            };
    
    public OMEROMetadataStoreClient store;
    
    Component owner;
    
    boolean debug = false;

    private JComboBox dBox;

    private JTextPane descriptionText;

    OptionsDialog(GuiCommonElements gui, JFrame owner, String title, boolean modal)
    {
        super(owner);
        
        this.owner = owner;

        setLocation(200, 200);
        setTitle(title);
        setModal(modal);
        setResizable(false);
        setSize(new Dimension(dialogWidth, dialogHeight));
        setLocationRelativeTo(owner);

        tabbedPane = new JTabbedPane();
        tabbedPane.setOpaque(false); // content panes must be opaque

        this.gui = gui;

        /////////////////////// START MAIN PANEL ////////////////////////

        // Set up the main pane
        double mainPanelTable[][] =
        {{TableLayout.FILL, 120, 5, 120, TableLayout.FILL}, // columns
        {TableLayout.FILL, 5, 30}}; // rows     
        
        mainPanel = gui.addMainPanel(this, mainPanelTable, 10, 10, 10, 10, debug);
        
        // Buttons at the bottom of the form

        cancelBtn = gui.addButton(mainPanel, "Cancel", 'L',
                "Cancel", "1, 2, f, c", debug);
        cancelBtn.addActionListener(this);

        okBtn = gui.addButton(mainPanel, "OK", 'Q',
                "Import", "3, 2, f, c", debug);
        okBtn.addActionListener(this);

        this.getRootPane().setDefaultButton(okBtn);
        gui.enterPressesWhenFocused(okBtn);
        
        mainPanel.add(tabbedPane, "0,0,4,0");
        
        /////////////////////// START DEBUG OPTIONS PANEL ////////////////////////
        
        double debugOptionTable[][] =
            {{TableLayout.FILL}, // columns
            {10,TableLayout.PREFERRED,20,30,15,TableLayout.FILL}}; // rows
        
        debugOptionsPanel = gui.addMainPanel(tabbedPane, debugOptionTable, 0, 10, 10, 10, debug);
        
        String message = "Choose the level of detail for your log file's data.";
        gui.addTextPane(debugOptionsPanel, message, "0, 1, 0, 0", debug);
        dBox = gui.addComboBox(debugOptionsPanel, "Debug Level: ", debugItems, 'D',
                "Choose the level of detail for your log file's data.", 95, "0,3,f,c", debug);
        dBox.addActionListener(this);
        
        String description = ((DebugItem) dBox.getSelectedItem()).getDescription();
        descriptionText = gui.addTextPane(debugOptionsPanel, description, "0, 5", debug);
        final Font textFieldFont = (Font)UIManager.get("TextField.font");
        final Font font = new Font(textFieldFont.getFamily(), Font.ITALIC, textFieldFont.getSize());
        descriptionText.setFont(font);
        
        /////////////////////// START FILECHOOSER PANEL ////////////////////////
        
        // Set up the import panel for tPane, quit, and send buttons
        
        double fileChooserTable[][] =
            {{TableLayout.FILL, 120, 5, 120, TableLayout.FILL}, // columns
            {TableLayout.PREFERRED,15,TableLayout.FILL,10}}; // rows

        fileChooserPanel = gui.addMainPanel(tabbedPane, fileChooserTable, 0,10,0,10, debug);

        message = "Switch between single pane view and triple pane view. " +
        		"You will need to reboot the importer before your changes will take effect.";
        gui.addTextPane(fileChooserPanel, message, "0, 0, 4, 0", debug);
        
        // Set up single pane table
        double singlePaneTable[][] =
        {{24, 5, TableLayout.FILL}, // columns
                {TableLayout.FILL}}; // rows

        // Panel containing the single pane layout

        singlePanePanel = gui.addMainPanel(fileChooserPanel, singlePaneTable, 0, 0, 0, 0, debug);
        
        singlePaneBtn = gui.addRadioButton(singlePanePanel, 
                null, 'u', 
                null, "0,0", debug);
        
        gui.addImagePanel(singlePanePanel, SINGLE_PANE_IMAGE, "2,0", debug);
        
        fileChooserPanel.add(singlePanePanel, "0, 2, 1, 2");

        // Set up triple pane table
        double triplePaneTable[][] =
        {{24, 5, TableLayout.FILL}, // columns
                {TableLayout.FILL}}; // rows
        
        // Panel containing the triple pane layout

        triplePanePanel = gui.addMainPanel(fileChooserPanel, triplePaneTable, 0, 0, 0, 0, debug);

        triplePaneBtn = gui.addRadioButton(triplePanePanel, 
                null, 'u', 
                null, "0,0", debug);
        
        
        gui.addImagePanel(triplePanePanel, TRIPLE_PANE_IMAGE, "2,0", debug);     
        
        fileChooserPanel.add(triplePanePanel, "3, 2, 4, 2");
        
        ButtonGroup group = new ButtonGroup();
        group.add(singlePaneBtn);
        group.add(triplePaneBtn);
        
        log.debug("Using Quaqua?" + gui.config.getUseQuaqua());
        if (gui.config.getUseQuaqua() == true)
        {
            triplePaneBtn.setSelected(true);
            singlePaneBtn.setSelected(false);
        } 
        else
        {
            triplePaneBtn.setSelected(false);
            singlePaneBtn.setSelected(true);
        }
    
        /////////////////////// START TABBED PANE ////////////////////////
        
        if (gui.getIsMac())
            tabbedPane.addTab("FileChooser Settings", null, fileChooserPanel, "FileChooser Settings");
        
        tabbedPane.addTab("Debug Settings", null, debugOptionsPanel, "Debug Settings");
        //this.add(mainPanel);
        
        this.add(mainPanel);
        
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == cancelBtn)
        {
            cancelled = true;
            this.dispose();
        }
        if (e.getSource() == okBtn && this.isDisplayable())
        {
            if (singlePaneBtn.isSelected())
                gui.config.setUseQuaqua(false);
            else
                gui.config.setUseQuaqua(true);
            this.dispose();
            gui.restartNotice(owner, null);
        }
        
        if (e.getSource() == dBox)
        {
            descriptionText.setText(((DebugItem) dBox.getSelectedItem()).getDescription());
            this.repaint();
        }
    }

    public static void main (String[] args) {

        String laf = UIManager.getSystemLookAndFeelClassName() ;
        //laf = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
        //laf = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
        //laf = "javax.swing.plaf.metal.MetalLookAndFeel";
        //laf = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";

        try {
            UIManager.setLookAndFeel(laf);
        } catch (Exception e) 
        { System.err.println(laf + " not supported."); }

        ImportConfig config = new ImportConfig(null);
        
        OptionsDialog dialog = new OptionsDialog(new GuiCommonElements(config), null, "Options Dialog", true);
        if (dialog != null) System.exit(0);
    }
}

class DebugItem
{
    private String text;
    private int level;
    private String description;

    public DebugItem(final String text, final int level, final String description)
    {
        this.text = text;
        this.level = level;
        this.description = description;
    }

    @Override
    public String toString()
    {
        return text;
    }

    public int getLevel()
    {
        return level;
    }
    
    public String getDescription()
    {
        return description;
    }
}