/**
 * libmib - Java SNMP Management Information Base Library
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

package libmib.mibtree;

import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import libmib.oid.MibObjectIdentifier;
import libmib.oid.MibObjectType;
import snmp.SNMPBadValueException;

/**
 * This extension of DefaultMutableTreeNode includes some MIB specific methods
 * for searching, as well as a more specific constructor for using a MibObjectType as the node's
 * user object.
 */
public class MibTreeNode extends DefaultMutableTreeNode 
{
    public static final boolean MATCH_NEAREST_PATH = true;
    public static final boolean MATCH_EXACT_PATH = false;
    
    /**
     * Constructs a new default MibTreeNode.
     */
    public MibTreeNode() 
    {
        super();
    }
    
    /**
     * Constructs a new MibTreeNode using an object that inherits from MibObjectIdentifier as the user object.
     * 
     * @param userMIBObject the MibObjectIdentifier to set as the user object of the node
     */
    public MibTreeNode(MibObjectIdentifier userMIBObject) 
    {
        super(userMIBObject);
    }
    
    /**
     * Searches for a node by it's name starting the search
     * at this node.  A breadth first enumeration is used.
     * 
     * @param nodeName the name of the node to search for
     * @return the node if it is found
     */
    public MibTreeNode getNodeByName(String nodeName)
    {
        //find node by name
        boolean found = false;
        MibTreeNode node = null;

        Enumeration e = this.breadthFirstEnumeration();


        while(e.hasMoreElements() && !found)
        {
            node = (MibTreeNode)e.nextElement();

            if(String.valueOf(node).equalsIgnoreCase(nodeName))
                found = true;
        }

        if(found)
            return node;
        else
            return null;
    }
    
    
    /**
     * Searches for a node by its OID string path starting at this node.
     * The search goes through the children at each successive node until the
     * correct OID has been constructed.  
     * NOTE: the OID will be constructed STARTING at this node, 
     * and thus this should usually be used with the root node of a tree since 
     * local searches aren't useful with whole OIDs.  However, it could be useful with 
     * partial OIDs.
     * 
     * @param oid the OID path string of the node to search for
     * @param matchType a flag indicating whether the nearest node should be returned
     * even though the exact node was not found.
     * For example, if 1.3.6.1.2.1.1.1.0 was searched for, than 1.3.6.1.2.1.1.1 (system.sysDescr)
     * will be returned.
     * @return the node if it is found or null if it is not
     * @throws SNMPBadValueException 
     */
    public MibTreeNode getNodeByOid(String oid, boolean matchType) throws NumberFormatException
    {    
        String[] oidArray = oid.split("\\.");

        int curElement = 0;
        int treeDepth = 0;

        boolean oidFound = false;
        boolean indexFound = false;
        
        StringBuffer constructedOID = new StringBuffer();
        MibTreeNode curNode = null;
        MibTreeNode foundNode = null;
        MibObjectType curOID = null;

        Enumeration children = this.children();

        try
        {
            while( (treeDepth < oidArray.length) && !oidFound )
            {
                indexFound = false;
                while(children.hasMoreElements() && !indexFound)
                {
                    curNode = (MibTreeNode)children.nextElement();
                    curOID = (MibObjectType)curNode.getUserObject();
                    //System.out.print("\n\n" + curNode);
    
                    //compare the current object's id to the desired next number in the OID
                    if(curOID.getId() == Integer.parseInt(oidArray[curElement]))
                    {
                        indexFound = true;
                        constructedOID.append("." + curOID.getId());
                        
                        foundNode = curNode;
                        curElement++;
    
                        //System.out.print(" " + constructedOID.toString() + " " + foundNode);
                    }
    
                    //System.out.print("\n" + found);
                }
    
                int dotIndex = constructedOID.indexOf("."); //just to be on the safe side, instead of just assuming the first '.' is at index 1
                if(constructedOID.toString().substring(dotIndex + 1).equals(oid))
                    oidFound = true;
                
                //System.out.print("\n" + constructedOID.toString().substring(dotIndex + 1));
                
                //This ensures that if all children of a node have been checked and no index match was found,
                //then the search has gone as deep as it should go, and thus it should break out
                if(!indexFound)
                    break; 
                
                children = curNode.children();
    
                treeDepth++;
            }
    
            if(oidFound || matchType == MibTreeNode.MATCH_NEAREST_PATH)
                return foundNode;
            else
                return null;
        }
        catch(NumberFormatException e)
        {
            throw new NumberFormatException("Object Identifier: " + oid + " is invalid.");
        }
            
            
    }
    
    /**
     * Returns the full path name from the root of the tree to this node
     * as a String.  This basically simplifies what is returned from
     * a node's getPath method.
     * 
     * @return a String containing the full path name for this node,
     * for example: iso.org.dod
     */
    public String getOidNamePath()
    {
        StringBuffer fullPathname = new StringBuffer();
        TreeNode[] pathFromRoot = this.getPath();
        
        fullPathname.append(pathFromRoot[1]); //ignore first node (generic root)
        for(int i = 2; i < pathFromRoot.length; i++)
        {
            fullPathname.append("." + pathFromRoot[i].toString());
        }

        return fullPathname.toString();
    }
    
    /**
     * Returns the full path number from the root of the tree to this node
     * as a String.
     * 
     * @return a string such as "1.3.6.1.1" representing a node's path
     * from the root.
     */
    public String getOidNumberPath()
    {
        StringBuffer fullNumberPath = new StringBuffer();
        TreeNode[] pathFromRoot = this.getPath();
        
        fullNumberPath.append(((MibObjectType)((MibTreeNode)pathFromRoot[1]).getUserObject()).getId()); //ignore first node (generic root)
        for(int i = 2; i < pathFromRoot.length; i++)
        {
            fullNumberPath.append("." + ((MibObjectType)((MibTreeNode)pathFromRoot[i]).getUserObject()).getId());
        }

        return fullNumberPath.toString();
    }
    
    /**
     * Returns both the OID name and number paths.  This returns what
     * getOIDNamePath and getOIDNumberPath do, but in a String array
     * together.
     * 
     * @return a String array with both the full OID name and number
     * paths of this node.  The first element is the number and the second element
     * is the name.
     */
    public String[] getOidPaths()
    {
        StringBuffer fullNumberPath = new StringBuffer();
        StringBuffer fullNamePath = new StringBuffer();
        
        TreeNode[] pathFromRoot = this.getPath();
        
        fullNumberPath.append(((MibObjectType)((MibTreeNode)pathFromRoot[1]).getUserObject()).getId()); //ignore first node (generic root)
        fullNamePath.append(pathFromRoot[1]); //ignore first node (generic root)
        
        for(int i = 2; i < pathFromRoot.length; i++)
        {
            fullNumberPath.append("." + ((MibObjectType)((MibTreeNode)pathFromRoot[i]).getUserObject()).getId());
            fullNamePath.append("." + pathFromRoot[i].toString());
        }

        String[] paths = new String[2];
        paths[0] = fullNumberPath.toString();
        paths[1] = fullNamePath.toString();
        
        return paths;
    }
    
}
