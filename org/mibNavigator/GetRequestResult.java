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

public class GetRequestResult 
{
    private final String displayOID;
    private final String numericOID;
    private final String oidValue;
    
    public GetRequestResult(final String displayOID, final String numericOID, final String value)
    {
        this.displayOID = displayOID;
        this.numericOID = numericOID;
        oidValue = value;
    }
    
    public String getOIDName()
    {
        return displayOID;
    }
    
    public String getOIDNumber()
    {
        return numericOID;
    }
    
    public String getOIDValue()
    {
        return oidValue;
    }
    
	@Override
    public String toString()
    {
        return displayOID + ": " + oidValue;
    }
    
    
    
}
