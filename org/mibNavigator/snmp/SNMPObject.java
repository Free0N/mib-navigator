/*
 * SNMP Package
 *
 * Copyright (C) 2004, Jonathan Sevy <jsevy@mcs.drexel.edu>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package org.mibNavigator.snmp;


/** 
 *  Abstract base class of all SNMP data type classes.
 */
public abstract class SNMPObject
{
    
    /** 
     *  Must return a Java object appropriate to represent the value/data contained
     *  in the SNMP object
     */
    public abstract Object getValue();
    
    
    /** 
     *  Must set the value of the SNMP object when supplied with an appropriate
     *  Java object containing an appropriate value.
     */
    public abstract void setValue(Object o)
        throws SNMPBadValueException;
    
    
    /** 
     *  Should return an appropriate human-readable representation of the stored value.
     */  
    public abstract String toString();
    
    
    /** 
     *  Must return the BER byte encoding (type, length, value) of the SNMP object.
     */  
    protected abstract byte[] getBEREncoding();
    
    
    /**
     *  Compares two SNMPObject subclass objects by checking their values for equality.
     */
    public boolean equals(Object other)
    {
        // false if other is null
        if (other == null)
            return false;
        
        // check first to see that they're both of the same class
        if (!this.getClass().equals(other.getClass()))
            return false;
        
        SNMPObject otherSNMPObject = (SNMPObject)other;  
         
        // now see if their embedded values are equal
        if (this.getValue().equals(otherSNMPObject.getValue()))
            return true;
        else
            return false;
    }
    
    
    /**
     *  Generates a hash value so SNMP objects can be used in Hashtables.
     */
    public int hashCode()
    {
        // just use hashcode value of embedded value by default
        if (this.getValue() != null)
            return this.getValue().hashCode();
        else
            return 0;
    }
   
}