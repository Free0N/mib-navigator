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

import java.io.*;

/**
 *  Class representing a general string of octets.
 */
public class SNMPOctetString extends SNMPObject
{
    protected byte[] data;
    protected SNMPBERType tag = SNMPBERType.SNMP_OCTETSTRING;


    /**
     *  Create a zero-length octet string.
     */
    public SNMPOctetString()
    {
        data = new byte[0];
    }


    /**
     *  Create an octet string from the bytes of the supplied String.
     */
    public SNMPOctetString(String stringData)
    {
        this.data = stringData.getBytes();
    }


    /**
     *  Create an octet string from the supplied byte array. The array may be either
     *  user-supplied, or part of a retrieved BER encoding. Note that the BER encoding
     *  of the data of an octet string is just the raw bytes.
     */
    public SNMPOctetString(byte[] enc)
    {
        extractFromBEREncoding(enc);
    }


    /**
     *  Return the array of raw bytes.
     */
    public Object getValue()
    {
        return data;
    }


    /**
     *  Used to set the value from a byte array.
     *  
     *  @throws SNMPBadValueException Indicates an incorrect object type supplied.
     */
    public void setValue(Object data)
        throws SNMPBadValueException
    {
        if (data instanceof byte[])
            this.data = (byte[])data;
        else if (data instanceof String)
            this.data = ((String)data).getBytes();
        else
            throw new SNMPBadValueException(" Octet String: bad object supplied to set value ");
    }


    /**
     *  Returns the BER encoding for the octet string. Note the the "value" part of the
     *  BER type,length,value triple is just the sequence of raw bytes.
     */
    protected byte[] getBEREncoding()
    {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();

        // calculate encoding for length of data
        byte[] len = SNMPBERCodec.encodeLength(data.length);

        // encode T,L,V info
        outBytes.write(tag.getByte());
        outBytes.write(len, 0, len.length);
        outBytes.write(data, 0, data.length);

        return outBytes.toByteArray();
    }


    protected void extractFromBEREncoding(byte[] enc)
    {
        data = new byte[enc.length];

        // copy data
        for (int i = 0; i < enc.length; i++)
            data[i] = enc[i];
    }


    /**
     *  Checks the embedded arrays for equality.
     */
    public boolean equals(Object other)
    {
        // false if other is null
        if (other == null)
            return false;

        // check first to see that they're both of the same class
        if (!this.getClass().equals(other.getClass()))
            return false;

        SNMPOctetString otherSNMPObject = (SNMPOctetString)other;

        // see if their embedded arrays are equal
        if (java.util.Arrays.equals((byte[])this.getValue(),(byte[])otherSNMPObject.getValue()))
            return true;
        else
            return false;
    }



    /**
     *  Generates a hash value so SNMP Octet String subclasses can be used in Hashtables.
     */
    public int hashCode()
    {
        int hash = 0;

        // generate a hashcode from the embedded array
        for (int i = 0; i < data.length; i++)
        {
            hash += data[i];
            hash += (hash << 10);
            hash ^= (hash >> 6);
        }

        hash += (hash << 3);
        hash ^= (hash >> 11);
        hash += (hash << 15);

        return hash;
    }


    /**
     *  Returns a String constructed from the raw bytes. If the bytes contain a printable String, 
     *  a printable String will be returned.  Otherwise, a hex String representation of the data
     *  will be returned.
     */
    public String toString()
    {
        //Added by Matt Hamilton on July 13, 2005
        //The following code tries to format the Octet String appropriately
        //depending on its contents.  This is to ensure that actual character strings
        //are displayed properly, while binary data is shown as Hex Strings.
        
        String returnString = "";
        
        if(this.isPrintable())
        {
            returnString = new String(data);

            int nullLocation = returnString.indexOf('\0');

            //truncate at first null character if the string doesn't start with null
            if(nullLocation > 0)
                returnString = returnString.substring(0, nullLocation);
        }
        else
        {
            //Display non-printable character data as a hex string.  
            //This converts the raw bytes to a presentable format.
            returnString = this.toHexString().toUpperCase();
        }

		return returnString;
    }
    
    
    /**
     * Added by Matt Hamilton on 8/9/05.
     * Checks whether the bytes in the OctetString can be displayed or printed as a
     * valid sequence of Unicode characters.  Due to the internal byte array format 
     * of the OctetString, each byte is checked individually and independently.  
     * Thus, only those characters which will fit into a single byte (8 bits) will 
     * display properly since 2 or more byte character codes will not be detected.
     * As a result, only Unicode Basic Latin and Latin-1 character sets will appear 
     * correctly. In general, control codes are considered non-printable with the 
     * exception of whitespace.
     * 
     * Note:  I just noticed while searching that someone else's SNMP implementation has 
     * a method for this functionality with the same name as mine.  I did not rip them off, 
     * I swear.  From what's given in their doc, the implementation seems very similar, too.
     * 
     * @return true if every byte in the OctetString is a printable character
     */
    public boolean isPrintable()
    {
        int maxLength = data.length;
        
        //if(data.length == 1 && data[data.length - 1] == 0)      //special case for single null bytes so that the null hex string is returned
            //return false;
        //else 
        if(data.length > 0 && data[data.length - 1] == 0)  //if the last byte is null (0x00), exclude that byte from the check
            maxLength = maxLength - 1; 

        
        //Valid, printable, 8-bit Unicode characters should be detected properly.  However,
        //characters with codes greater than those supported by Unicode Basic Latin and Latin-1 
        //(up to 0x00FF) will not display properly since bytes are individually checked. Thus each character
        //displayed will really only be those that can fit into 1 byte, while Java supports 16 bit Unicode 
        //which uses 2 bytes.
        //See: <a href="http://www.unicode.org/charts/">Unicode Charts</a>
        //
        //Data should display as hex strings, unless the data manages to be all non-control values, then
        //it will display characters whether it makes sense or not.
        for(int i = 0; i < maxLength; i++)
        {
            /*char c = (char)data[i];
            if(Character.isIdentifierIgnorable(c) || Character.isISOControl(c))
                return false;*/
            
            if(Character.isIdentifierIgnorable(data[i]) || Character.isISOControl(data[i]))
                return false;
        }
        
        return true;
    }


    private String hexByte(byte b)
    {
        int pos = b;
        if (pos < 0)
            pos += 256;
        String returnString = new String();
        returnString += Integer.toHexString(pos/16);
        returnString += Integer.toHexString(pos%16);
        return returnString;
    }


    /**
     *  Returns a space-separated hex string corresponding to the raw bytes.
     */
    public String toHexString()
    {
        StringBuffer returnStringBuffer = new StringBuffer();

        //for (int i = 0; i < data.length; i++)
        for (byte octet : data)
            returnStringBuffer.append(this.hexByte(octet) + " ");

        return returnStringBuffer.toString();
    }

}