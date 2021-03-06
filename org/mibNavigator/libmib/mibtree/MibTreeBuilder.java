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

package org.mibNavigator.libmib.mibtree;

import java.io.File;

import javax.swing.tree.TreeModel;

import org.mibNavigator.libmib.MibInvalidFormatException;


/**
 * An interface defining the basic methods required for a MIB compiler that uses a 
 * TreeModel structure to store the OID hierarchy.
 */
public interface MibTreeBuilder 
{
    /**
     * Adds all of the MIB files in the specified directory to the MIB tree model.
     * Because potentially more than one MIB is being added, dependencies between MIBs are 
     * taken into account.
     * 
     * @param mibDir the File object representing a directory containing
     *               MIB files to add to the MIB tree.
     */
    public void addMIBDirectory(File mibDir);
    
    
    /**
     * Adds an individual MIB file to the MIB tree.
     * 
     * @param mibFile the File object representing the MIB to add to the MIB tree.
     * 
     * @throws MibInvalidFormatException if the MIB file is not in a valid format
     */
    public void addMIBFile(File mibFile) throws MibInvalidFormatException;
    
    
    /**
     * Retrieves the TreeModel used by the builder.
	 * @return 
     */
    public TreeModel getMibTreeModel();

}
