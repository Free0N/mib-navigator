package org.mibNavigator;

/**
 * MIB Navigator
 *
 * Copyright (C) 2005, Matt Hamilton <matthew.hamilton@washburn.edu>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

import org.mibNavigator.contextmenu.TextContextMenuListener;
import org.mibNavigator.contextmenu.ListContextMenuListener;
import org.mibNavigator.contextmenu.ListContextMenu;
import org.mibNavigator.contextmenu.TextContextMenu;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import org.mibNavigator.libmib.mibtree.MibTreeBuilder;
import org.mibNavigator.libmib.mibtree.MibTreeNode;
import org.mibNavigator.libmib.oid.MibObjectType;

/**
 *  MibBrowser creates the graphical interface for the MIB Navigator application. 
 */
public class MibBrowser implements ActionListener, TreeSelectionListener, ListSelectionListener, GetRequestListener
{
    private JPanel browserPanel;
    
    private JTree mibTree;
    private DefaultTreeModel mibModel;
    private MibTreeBuilder treeBuilder;
    private JScrollPane mibTreeScroll;
    
    private OidInfoViewer oidViewer;

    private JLabel oidNameLabel, oidNumberLabel, addressLabel, communityLabel, portLabel, timeoutLabel, oidInputLabel;
    private JTextField oidNameField, oidNumberField, resolvedAddrField, communityField, portField, timeoutField, oidInputField; 
    private JComboBox addressBox;
    
    private JLabel resultsLabel;
    private JScrollPane resultsScroll;
    private JList resultsList;

    private JButton getButton;
    
    private Color backgroundColor;

    private StringBuilder oidNameBuff;
    private StringBuilder oidNumBuff;
    
    public static final File DEFAULT_MIB_DIR = new File("." + File.separator + "mibs"); //base MIB directory (has been tested on Linux and Windows)
    
    private GetRequestWorker snmpGetWorker = null;
    
    private static final String GET_START_LABEL = "Get Data";
    private static final String GET_STOP_LABEL = "Stop";
        

    /**
     * Creates a new MibBrowser that uses the given MibTreeBuilder to manage
     * its MIB tree.
     * 
     * @param newBuilder the MibTreeBuilder used to manage the MIB tree
     * 
     * @throws IllegalArgumentException if newBuilder is null
     */
    public MibBrowser(MibTreeBuilder newBuilder)
    {
        if(newBuilder == null)
            throw new IllegalArgumentException("MIB tree builder cannot be null.");
        
        treeBuilder = newBuilder;
        
        oidNameBuff = new StringBuilder();
        oidNumBuff = new StringBuilder();

        initializeComponents();
        layoutComponents(); 
    }
    

    /**
     * Initializes the MibBrowser interface components.
     */
    private void initializeComponents()
    {
        browserPanel = new JPanel();

        TextContextMenu textPopMenu = new TextContextMenu();
		TextContextMenuListener textPopListen = new TextContextMenuListener(textPopMenu);

        oidViewer = new OidInfoViewer(textPopListen); 
        
		//oid info
        oidNameLabel = new JLabel("OID Name: ");
        backgroundColor = oidNameLabel.getBackground(); //this is for look and feel purposes
        oidNumberLabel = new JLabel("OID Number: ");

        oidNameField = new JTextField(37);
        oidNameField.setEditable(false);
        oidNameField.setBackground(backgroundColor);
        oidNameField.addMouseListener(textPopListen);

        oidNumberField = new JTextField(37);
        oidNumberField.setEditable(false);
        oidNumberField.setBackground(backgroundColor);
        oidNumberField.addMouseListener(textPopListen);
        
        
		//host info
		addressLabel = new JLabel("IP Address:");
        addressBox = new JComboBox();
        addressBox.setMaximumRowCount(15);
        addressBox.setEditable(true);
        Dimension addressSize = new Dimension(113, 20);
        addressBox.setPreferredSize(addressSize);
        addressBox.setMaximumSize(addressSize);
        
        //Add a context menu to the combo box's text component.
        ComboBoxEditor editor = (ComboBoxEditor)addressBox.getEditor();
        Component comp = editor.getEditorComponent();
        comp.addMouseListener(textPopListen);

        resolvedAddrField = new JTextField(17);
        resolvedAddrField.setEditable(false);
        resolvedAddrField.setHorizontalAlignment(JTextField.CENTER);
        resolvedAddrField.setBackground(backgroundColor);
        resolvedAddrField.addMouseListener(textPopListen);

        communityLabel = new JLabel("Community String:");
        communityField = new JTextField(12);
        communityField.setText("public");
        communityField.setEditable(true);
        communityField.addMouseListener(textPopListen);
        
        portLabel = new JLabel("Port:");
        portField = new JTextField(4);
        portField.setText("161");
        portField.addMouseListener(textPopListen);
        
        timeoutLabel = new JLabel("Timeout:");
        timeoutField = new JTextField(4);
        timeoutField.setText("4000");
        timeoutField.addMouseListener(textPopListen);

        oidInputLabel = new JLabel("OID:");
        oidInputField = new JTextField(21);
        oidInputField.setText("");
        oidInputField.setEditable(true);
        oidInputField.addMouseListener(textPopListen);
        oidInputField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),"treeSearch");
        oidInputField.getActionMap().put("treeSearch", new OidTreeSearchAction());

        ListContextMenu listPopMenu = new ListContextMenu();
        ListContextMenuListener listPopListen = new ListContextMenuListener(listPopMenu);
        
		resultsLabel = new JLabel("Results:");
        resultsList = new JList(new DefaultListModel());
        resultsList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        resultsList.addMouseListener(listPopListen);
        resultsList.addListSelectionListener(this);
        
        resultsScroll = new JScrollPane(resultsList);
		resultsScroll.setPreferredSize(new Dimension(50, 75));

        getButton = new JButton(GET_START_LABEL);
        getButton.setPreferredSize(new Dimension(80, 25));
        getButton.setActionCommand("start get");
        getButton.addActionListener(this);
        getButton.setMnemonic(KeyEvent.VK_G);


        //Configure the mib tree.
        try
        {
            treeBuilder.addMIBDirectory(DEFAULT_MIB_DIR);
        }
        catch(IllegalArgumentException e) //if the default directory doesn't exist
        {
            System.out.print(e.getMessage());
        }
        
        //If the mibs directory wasn't found, this will return a tree model with only default nodes.
        mibModel = (DefaultTreeModel)treeBuilder.getMibTreeModel();

        mibTree = new JTree();
        mibTree.setModel(mibModel); 
        mibTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        mibTree.addTreeSelectionListener(this);
        mibTree.setRootVisible(false);
                
        DefaultTreeCellRenderer cellRenderer = new DefaultTreeCellRenderer();
        cellRenderer.setHorizontalAlignment(DefaultTreeCellRenderer.CENTER);
        
        //Don't display any node icons.
        cellRenderer.setLeafIcon(null);
        cellRenderer.setOpenIcon(null);
        cellRenderer.setClosedIcon(null);
        
        cellRenderer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        mibTree.setCellRenderer(cellRenderer);
        mibTree.setShowsRootHandles(true);
        mibTree.setSelectionRow(0); //automatically select the first visible node

		//Add the tree to a scroll pane.
		mibTreeScroll = new JScrollPane(mibTree);
		mibTreeScroll.setPreferredSize(new Dimension(400, 180));
    }


    /**
     * Lays out and arranges the interface components.
     */
    private void layoutComponents()
    {
		//set up the layout
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints cons = new GridBagConstraints();
		Insets ins = new Insets(2, 2, 2, 0);
		cons.insets = ins;
		//cons.gridwidth = 1;
		//cons.gridheight = 1;

		//TREE PANEL
		JPanel treePanel = new JPanel();
		treePanel.setLayout(layout);

		cons.gridx = 0;
		cons.gridy = 0;
		cons.weightx = .5;
		cons.weighty = .5;
		cons.fill = GridBagConstraints.BOTH;
		cons.anchor = GridBagConstraints.FIRST_LINE_START;
		layout.setConstraints(mibTreeScroll, cons);
		treePanel.add(mibTreeScroll);


		//TREE AND OID DETAILS CONTAINING PANEL
		JPanel topPanel = new JPanel();
		topPanel.setLayout(layout);

		cons.gridx = 0;
		cons.gridy = 0;
        cons.weightx = .65;
        cons.weighty = .65;
		cons.fill = GridBagConstraints.BOTH;
		layout.setConstraints(treePanel, cons);
		topPanel.add(treePanel);

		cons.gridx = 1;
		cons.gridy = 0;
        cons.weightx = .35;
        cons.weighty = .35;
		layout.setConstraints(oidViewer.getPanel(), cons);
		topPanel.add(oidViewer.getPanel());


		//OID NUMBER AND NAME PANEL
		JPanel oidPanel = new JPanel();
		oidPanel.setLayout(layout);

		ins.set(2, 0, 2, 0);
		cons.weightx = 0;
		cons.weighty = 0;
		cons.anchor = GridBagConstraints.LINE_START;

		cons.fill = GridBagConstraints.NONE;
		cons.gridx = 0;
		cons.gridy = 0;
		layout.setConstraints(oidNameLabel, cons);
		oidPanel.add(oidNameLabel);

		cons.fill = GridBagConstraints.HORIZONTAL;
		cons.gridx = 1;
		cons.gridy = 0;
		layout.setConstraints(oidNameField, cons);
		oidPanel.add(oidNameField);

		cons.fill = GridBagConstraints.NONE;
		cons.gridx = 0;
		cons.gridy = 1;
		layout.setConstraints(oidNumberLabel, cons);
		oidPanel.add(oidNumberLabel);

		cons.fill = GridBagConstraints.HORIZONTAL;
		cons.gridx = 1;
		cons.gridy = 1;
		layout.setConstraints(oidNumberField, cons);
		oidPanel.add(oidNumberField);
        
        
		//TIMEOUT/PORT PANEL
        JPanel hostSubPanel = new JPanel();
        hostSubPanel.setLayout(layout);
        
        ins.set(2, 2, 2, 2);
        cons.gridx = 0;
        cons.gridy = 0;
        layout.setConstraints(portLabel, cons);
        hostSubPanel.add(portLabel);
        
        cons.gridx = 1;
        cons.gridy = 0;
        layout.setConstraints(portField, cons);
        hostSubPanel.add(portField);
        
        cons.gridx = 2;
        cons.gridy = 0;
        layout.setConstraints(timeoutLabel, cons);
        hostSubPanel.add(timeoutLabel);
        
        cons.gridx = 3;
        cons.gridy = 0;
        layout.setConstraints(timeoutField, cons);
        hostSubPanel.add(timeoutField);


		//HOST DETAILS PANEL
		JPanel hostPanel = new JPanel();
		hostPanel.setLayout(layout);

		cons.weightx = 0;
		cons.weighty = 0;
		cons.anchor = GridBagConstraints.LINE_START;
		cons.fill = GridBagConstraints.NONE;

		cons.gridx = 0;
		cons.gridy = 0;
		layout.setConstraints(addressLabel, cons);
		hostPanel.add(addressLabel);

		cons.gridx = 1;
		cons.gridy = 0;
		layout.setConstraints(addressBox, cons);
		hostPanel.add(addressBox);

        cons.gridx = 2;
        cons.gridy = 0;
        layout.setConstraints(resolvedAddrField, cons);
        hostPanel.add(resolvedAddrField);

		cons.gridx = 0;
		cons.gridy = 1;
		layout.setConstraints(communityLabel, cons);
		hostPanel.add(communityLabel);

		cons.gridx = 1;
		cons.gridy = 1;
		layout.setConstraints(communityField, cons);
		hostPanel.add(communityField);
        
        cons.gridx = 2;
        cons.gridy = 1;
        layout.setConstraints(hostSubPanel, cons);
        hostPanel.add(hostSubPanel);
        

		//OID NUMBER/NAME AND HOST DETAILS CONTAINER PANEL
		JPanel topMidPanel = new JPanel();
		topMidPanel.setLayout(layout);

		cons.gridx = 0;
		cons.gridy = 0;
		layout.setConstraints(oidPanel, cons);
		topMidPanel.add(oidPanel);

		cons.gridx = 1;
		cons.gridy = 0;
		layout.setConstraints(hostPanel, cons);
		topMidPanel.add(hostPanel);

		//BUTTONS PANEL
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(layout);

		cons.gridx = 0;
		cons.gridy = 0;
		layout.setConstraints(getButton, cons);
		buttonPanel.add(getButton);

		//OID SEARCH/SELECTION PANEL
		JPanel oidSearchPanel = new JPanel();
		oidSearchPanel.setLayout(layout);

		cons.gridx = 0;
		cons.gridy = 0;
		layout.setConstraints(oidInputLabel, cons);
		oidSearchPanel.add(oidInputLabel);

		cons.gridx = 1;
		cons.gridy = 0;
		layout.setConstraints(oidInputField, cons);
		oidSearchPanel.add(oidInputField);

		//BUTTON AND SEARCH/SELECTION CONTAINING PANEL
		JPanel bottomMidPanel = new JPanel();
		bottomMidPanel.setLayout(layout);

		cons.gridx = 0;
		cons.gridy = 0;
		layout.setConstraints(buttonPanel, cons);
		bottomMidPanel.add(buttonPanel);

		cons.gridx = 1;
		cons.gridy = 0;
		layout.setConstraints(oidSearchPanel, cons);
		bottomMidPanel.add(oidSearchPanel);

		//RESULTS PANEL
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(layout);

		cons.anchor = GridBagConstraints.LINE_START;
		cons.gridx = 0;
		cons.gridy = 0;
		layout.setConstraints(resultsLabel, cons);
		bottomPanel.add(resultsLabel);

		cons.weightx = .75;
		cons.weighty = .75;
		cons.fill = GridBagConstraints.BOTH;
		cons.gridheight = GridBagConstraints.REMAINDER;
		cons.gridx = 0;
		cons.gridy = 1;
		layout.setConstraints(resultsScroll, cons);
		bottomPanel.add(resultsScroll);


		//Configure and add panels to the master browser panel.
        browserPanel.setLayout(layout);
        browserPanel.setBackground(backgroundColor);

		ins.set(2, 0, 2, 0);
		cons.gridheight = 1;
		cons.anchor = GridBagConstraints.LINE_START;
		cons.fill = GridBagConstraints.BOTH;
		cons.gridx = 0;
		cons.gridy = 0;
        cons.weightx = .30;
        cons.weighty = .30;
		layout.setConstraints(topPanel, cons);
        browserPanel.add(topPanel);

		cons.anchor = GridBagConstraints.LINE_START;
		cons.fill = GridBagConstraints.NONE;
		cons.gridx = 0;
		cons.gridy = 1;
        cons.weightx = .0;
        cons.weighty = .0;
		layout.setConstraints(topMidPanel, cons);
        browserPanel.add(topMidPanel);

		cons.anchor = GridBagConstraints.LINE_START;
		cons.gridx = 0;
		cons.gridy = 2;
        cons.weightx = .0;
        cons.weighty = .0;
		layout.setConstraints(bottomMidPanel, cons);
        browserPanel.add(bottomMidPanel);

		cons.anchor = GridBagConstraints.LINE_START;
		cons.fill = GridBagConstraints.BOTH;
		ins.set(3, 0, 3, 0);
		cons.insets = ins;
		cons.weightx = .5;
		cons.weighty = .5;
		cons.gridx = 0;
		cons.gridy = 3;
		layout.setConstraints(bottomPanel, cons);
        browserPanel.add(bottomPanel);
	}
    
    // *** Start of MibBrowser data access methods. ***
    
    /**
     * Gets the internal JPanel created and used by MibBrowser.
     * 
     * @return the internal JPanel of MibBrowser
     */
    public JPanel getBrowserPanel()
    {
        return browserPanel;
    }
    
    
    /**
     * Gets a reference to the MibTreeBuilder used by MibBrowser to manage MIBs in the tree.
     * 
     * @return the MibTreeBuilder used by MibBrowser
     */
    public MibTreeBuilder getMibBuilder()
    {
        return treeBuilder;
    }
    
    
    /**
     * Gets the browser's list of IP addresses.  It returns a copy of the address combo box's 
     * data model in List form.  External modifications to the List after retrieving it from MibBrowser
     * will not change MibBrowser's address combo box.
	 * @return <code>List&ltString&gt</code> list of IP addresses.
     */
    public List<String> getAddresses()
    {
        int ipCount = addressBox.getItemCount();
        List<String> ipList = new ArrayList<>(ipCount);
        
        for(int i = 0; i < ipCount; i++)
            ipList.add((String)addressBox.getItemAt(i));

        return ipList;
    }
    
    /**
     * Sets the browser's list of IP addresses.  The address list is used to create the address
     * combo box's data model.  External modifications to newAddressList after passing it to MibBrowser
     * will not change MibBrowser's address combo box.
	 * @param newAddressList
     */
    public void setAddresses(List<String> newAddressList)
    {
        addressBox.setModel(new DefaultComboBoxModel(newAddressList.toArray()));
    }
    
    // *** End of MibBrowser data access methods. ***
    
    
	/**
	 * ActionListener implementation method: reacts to menu and button actions, which are usually clicks or
	 * the pressing of the Enter key when a button has focus.
     * 
     * @param event the ActionEvent generated when a component's default action occurs
     */
	@Override
	public void actionPerformed(ActionEvent event)
	{
        String actionCommand = event.getActionCommand();

		if(actionCommand.equals("start get"))
		{    
		    //Spawn a new GetRequestWorker thread for retrieving data from a device running an SNMP agent.
            
            if(getButton.getText().equals(GET_START_LABEL))
            {
                //This check is just to avoid even attempting to use an empty OID or IP address field.
                if(!oidInputField.getText().trim().equals("") 
                    && addressBox.getSelectedItem() != null
                    && !(((String)addressBox.getSelectedItem()).trim().equals("")) )   
                {
                    DefaultListModel resultsListModel = (DefaultListModel)resultsList.getModel();
                    
                    try
                    {
                        //Get all necessary values from the user interface before starting the Get process.
                        //This ensures that the user can't affect what values the thread uses once it
                        //has been launched.
                        String communityString = communityField.getText().trim();
                        String addressString = ((String)addressBox.getSelectedItem()).trim();
                        int port = Integer.parseInt(portField.getText().trim());
                        int timeout = Integer.parseInt(timeoutField.getText().trim());
    
                        //try to scroll to the correct OID
                        String oidInputString = trimCharacter(oidInputField.getText().trim(), '.');
                        String oidTreeNumberString = oidNumberField.getText();
                        
                        if(!oidInputString.equals(oidTreeNumberString))
                            setVisibleNodeByOID(oidInputString, MibTreeNode.MATCH_EXACT_PATH);
    
                        resultsListModel.removeAllElements();
                        getButton.setText(GET_STOP_LABEL);
                        
                        //Initialize and start the GetRequest process in a different thread using a SwingWorker.
                        snmpGetWorker = new GetRequestWorker(communityString, oidInputString, addressString,
                                (MibTreeNode)mibModel.getRoot());
                        snmpGetWorker.addGetRequestListener(this);
                        snmpGetWorker.setPort(port);
                        snmpGetWorker.setTimeout(timeout);
                        snmpGetWorker.start(); 
                    }
                    catch(NumberFormatException e)
                    {
                        resultsListModel.removeAllElements();
                        resultsListModel.addElement("Bad numerical input: " + e.getMessage() + "\n");
                    } 
                }
            } //if the button says "Get Data"
            else
            {
                if(snmpGetWorker != null)
                    snmpGetWorker.interrupt();  //stop the Get process
            }
		}
	}
    

	/**
     * TreeSelectionListener implementation method: reacts to tree node selections (ie. user clicks a node)
     * 
     * @param event the TreeSelectionEvent generated by a node selection change
	 */
	@Override
	public void valueChanged(TreeSelectionEvent event)
	{
		TreePath tPath = event.getPath();
		Object[] path = tPath.getPath();
		MibTreeNode curNode;
		MibObjectType curMIBObject;

		//Reset the string buffers; these buffers are continuously
        //reused to avoid always creating new ones.
		oidNameBuff.delete(0, oidNameBuff.length());
		oidNumBuff.delete(0, oidNumBuff.length());

		//Construct the mib object name and oid path strings from the
        //object path array; start at 1 to exclude the root.
		for(int i = 1; i < path.length; i++)
		{
			curNode = (MibTreeNode)path[i];
			curMIBObject = (MibObjectType)curNode.getUserObject();

			//don't put a '.' at the beginning
			if(i > 1)
			{
				oidNameBuff.append(".");
				oidNumBuff.append(".");
			}

			oidNameBuff.append(curMIBObject.getName());
			oidNumBuff.append(String.valueOf(curMIBObject.getId()));
		}

		//set the oid name and number strings
		oidNameField.setText(oidNameBuff.toString());
		oidNumberField.setText(oidNumBuff.toString());

        if(!oidInputField.getText().trim().equals(oidNumberField.getText().trim()))
		    oidInputField.setText(oidNumBuff.toString()); //synch the input field with the tree display

		curNode = (MibTreeNode)tPath.getLastPathComponent();
		curMIBObject = (MibObjectType)curNode.getUserObject();

		//display the selected OID's details
        oidViewer.setMIBObject(curMIBObject);
	}
    
    /**
     * ListSelectionListener implementation method: reacts to changes in the selection on a JList.
     * When the user selects a row or more, the oid of the selected row with the lowest index is retrieved,
     * searched for in the MIB tree, and then displayed.
     * 
     * @param selectEvent
     */
	@Override
    public void valueChanged(ListSelectionEvent selectEvent) 
    {
        //The IsAdjusting check is to make sure the code that occurs on a selection value
        //change does not run twice.
        //The second time valueChanged is called is when a new row has been selected, the first time
        //is when the previous selections are removed.  The second time valueChanged is called,
        //this value is false.
        if(!selectEvent.getValueIsAdjusting())
        {
            JList source = (JList)selectEvent.getSource();
            int selectedIndex = source.getSelectedIndex();
            
            if(selectedIndex > -1)
            {
                Object selectedObject = source.getModel().getElementAt(selectedIndex);
                
                try
                {
                    if(selectedObject instanceof GetRequestResult)
                    {
                        String selectedOID = ((GetRequestResult)selectedObject).getOIDNumber();
                        setVisibleNodeByOID(selectedOID, MibTreeNode.MATCH_NEAREST_PATH);
                    }
                }
                //Catch bad OIDs, though this is very unlikely if the OID
                //is in the results list.
                catch(NumberFormatException e) 
                {
                    //do nothing
                    //They say it is horrible to have empty catch blocks but I do not care when this happens!
                }
            }
        }
        
    }
    

    /**
     * Action for performing a search of the MIB tree based on the OID string
     * in the OID input field.
     */
	private class OidTreeSearchAction extends AbstractAction
	{
		@Override
        public void actionPerformed(ActionEvent event)
        {
            try
            {
                setVisibleNodeByOID(oidInputField.getText().trim(), MibTreeNode.MATCH_EXACT_PATH);
            }
            catch(NumberFormatException e)
            {
                //just do nothing, I may add an "OID not found" message later
            }
        }
	}
    

    /**
     * Searches for a node by a specified OID string such as 1.3.6.1.1, etc., and
     * then sets the selected node in the JTree to that node.
     * Note that in the event that the OID is found in the tree,
     * a TreeSelectionListener valueChanged event will be triggered.
     * 
     * @param oidString the OID path string to search for
     * @param matchType the boolean determining whether to match the closest path or the exact path.
     * This uses MibTreeNode's constants MATCH_NEAREST_PATH or MATCH_EXACT_PATH.
     */
    private void setVisibleNodeByOID(final String oidString, final boolean matchType) throws NumberFormatException
    {
        MibTreeNode root = (MibTreeNode)mibModel.getRoot(); //get the root node to search from the root
        MibTreeNode testNode = root.getNodeByOid(oidString, matchType);

        if(testNode != null)
        {
            TreePath nodePath = new TreePath(testNode.getPath());
            
            mibTree.setSelectionPath(nodePath);
            mibTree.scrollPathToVisible(nodePath);
        }
    }


    /**
     * Removes all leading and trailing instances of a character from a String.
     * 
     * @param untrimmed the String to trim the characters from
     * @param trimChar the character to trim
     * @return the trimmed String
     */
    private String trimCharacter(String untrimmed, char trimChar)
    {
        String trimmed = untrimmed;
        String stringToTrim = String.valueOf(trimChar);
        
        //Trim leading characters.
        while(trimmed.startsWith(stringToTrim))
            trimmed = trimmed.substring(trimmed.indexOf(stringToTrim) + 1);

        //Trim trailing characters.
        while(trimmed.endsWith(stringToTrim))
            trimmed = trimmed.substring(0, trimmed.lastIndexOf(stringToTrim));

        return trimmed;
    }

    
    // *** Start of GetRequestListener implementation methods. ***
    
    /**
     * Displays the resolved name of an IP address next to the input combo box, 
     * and adds the raw address to this combo box if it does not already exist.
	 * @param validAddress
     */
	@Override
    public void hostAddressResolved(String validAddress, String resolvedAddress) 
    {
        //Display the resolved address.
        resolvedAddrField.setText(resolvedAddress);
        resolvedAddrField.setCaretPosition(0);
        
        int listLength = addressBox.getItemCount();
        
        //Do a search to determine if this address already exists in the combo box.
        boolean alreadyExists = false;
        int i = 0;
        while( (i < listLength) && !alreadyExists )
        {
            String currentAddr = (String)addressBox.getItemAt(i);
            if(validAddress.equals(currentAddr))
                alreadyExists = true;
            
            i++;
        }
        
        //If the address doesn't already exist, add it to the beginning of the list.
        if(!alreadyExists)
            addressBox.insertItemAt(validAddress, 0);
    }
    
    
    /**
     * Adds a new GetRequestResult to the resultsList.
     * A GetRequestResult encapsulates data about a single OID and its
     * returned value. Both the partial OID name and full OID number are stored in it.
	 * @param dataResultItem
     * @see GetRequestResult
     */
	@Override
    public void requestResultReceived(GetRequestResult dataResultItem) 
    {
        ((DefaultListModel)resultsList.getModel()).addElement(dataResultItem);
    }
    
    
   /**
    * Adds the termination status of the GetRequestWorker to the results list.  If it terminated
    * successfully, an empty String will be returned.  It also resets the get data button.
	 * @param messageString
    */
	@Override
    public void requestTerminated(String messageString)
    {
        if(!messageString.equals("")) //only add error messages, do nothing when successful
            ((DefaultListModel)resultsList.getModel()).addElement(messageString);
                 
        getButton.setText(GET_START_LABEL);
    }
    
    // *** End of GetRequestListener implementation methods. ***
    
}
