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

import java.io.InterruptedIOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import libmib.mibtree.MibTreeNode;
import libmib.oid.MibObjectType;
import snmp.SNMPBadValueException;
import snmp.SNMPGetException;
import snmp.SNMPInteger;
import snmp.SNMPObject;
import snmp.SNMPObjectIdentifier;
import snmp.SNMPSequence;
import snmp.SNMPVarBindList;
import snmp.SNMPv1Communicator;

/**
 *  This class retrieves OID values using SNMP GetNextRequests for a given host.  It executes
 *  in a separate thread so that the user interface of the application that uses this class does not freeze. 
 *  It extends the Java <code>SwingWorker</code> abstract class that is recommended for use in updating Swing components
 *  from other threads.<br><br>
 *  
 *  The retrieval process begins with a specified base OID and stops when the next OID no longer 
 *  begins with this base OID, when the user interrupts the process, or an exception occurs.<br><br>
 *  
 *  <code>GetRequestWorker</code> notifies other objects of incoming data and state changes through a simplified 
 *  form of the event listener pattern commonly used by many other Java classes.  As is usually the case with this 
 *  pattern, the listening class must implement a listener interface, in this case <code>GetRequestListener</code>, 
 *  in order to receive data from the<code>GetRequestWorker</code>.  Finally, the 3 kinds of data events generated by 
 *  <code>GetRequestWorker</code> are so disjoint that they may seem to warrant 3 separate listener interfaces, but for simplicity
 *  a single listener interface is used even though no single type of event is generated.
 */
public class GetRequestWorker extends SwingWorker 
{
    private EventListenerList requestListeners = new EventListenerList();
    
    private final String communityString;
    private final String oidInputString;
    private final String addressString;
    private int hostPort;
    private int hostTimeout;
    
    private final MibTreeNode root;
    
    private static final int SNMP_VERSION = 0;
    private static final int DEFAULT_TIMEOUT = 4000;
    private static final String STD_PREFIX = "iso.org.dod.internet.mgmt.mib-2.";
    private static final String ENT_PREFIX = "iso.org.dod.internet.private.enterprises.";
    
    
    /**
     * Initializes the worker with all necessary values.
     * 
     * @param commString the SNMP community string for the target host device
     * @param oidString the starting OID for the GetRequest
     * @param address the <code>String</code> representation of the target host device's IP address
     * @param rootNode the <code>MibTreeNode</code> root of the MIB tree
     */
    public GetRequestWorker(final String commString, final String oidString, 
            final String address, final MibTreeNode rootNode)
    {
        if(rootNode == null)
            throw new IllegalArgumentException("Root node cannot be null.");
        
        addressString = address;
        communityString = commString;
        oidInputString = oidString;
        root = rootNode;
        
        hostPort = SNMPv1Communicator.DEFAULT_SNMP_PORT;
        hostTimeout = DEFAULT_TIMEOUT;
    }
    
    public void setPort(int newPort)
    {
        hostPort = newPort;
    }
    
    public void setTimeout(int newTimeout)
    {
        hostTimeout = newTimeout;
    }
    
    /**
     * Adds a new GetRequestListener to the worker so that it is notified 
     * of worker events.  This method is not safe once the Worker's start
     * method has been called.
     * 
     * @param newListener the GetRequestListener that will receive data
     */
    public void addGetRequestListener(GetRequestListener newListener) 
    {
        requestListeners.add(GetRequestListener.class, newListener);
    }


    public Object construct() 
    {
        return this.doGetRequest();
    }
    

    public void finished()
    {
        this.fireRequestTerminationEvent(this.get().toString());
    }
    
    /**
     * Retrieves SNMP values via GetNextRequests.
     * GetRequestListeners are updated with the results through operations wrapped 
     * in Runnables and executed using invokeLater when appropriate in order to 
     * update GUI components in the Event Dispatch Thread.
     * 
     * @return an Object that is the final result produced by the thread.
     * In this case, the method will return an empty String on successful
     * execution, and a String error message when an exception occurs.
     */
    private Object doGetRequest() 
    {
        String baseOid = oidInputString;

        //Create local copies for data integrity during the GetRequest process since these fields can be set after construction.
        int port = hostPort;
        int timeout = hostTimeout;
        
        try
        {
            //Get the IP Address and attempt to resolve it; if the address is valid, update the interface.
            //Unfortunately, pressing the Stop button during address resolution/lookup will have no immediate
            //effect since these calls cannot be interrupted. However, they also cannot be done outside of this thread
            //because an exception in address resolution that indicates an invalid address should stop the GET process.
            InetAddress address = InetAddress.getByName(addressString);
            String resolvedAddr = address.getCanonicalHostName();
            this.fireAddressResolvedEvent(addressString, resolvedAddr);  //this will occur if the host address is valid
             
            //Establish a new SNMPv1 interface with the given data.
            SNMPv1Communicator snmpInterface = new SNMPv1Communicator(SNMP_VERSION, address, communityString);
            snmpInterface.setSocketTimeout(timeout);
            snmpInterface.setPort(port);

            SNMPVarBindList newVarBinds;
            SNMPObjectIdentifier receivedOid;
            SNMPObject snmpValue;
            SNMPSequence pair;

            String nextOid = baseOid;

            //Retrieve all values until the next OID does not start with the base OID (walk the tree)
            //or the thread is interrupted by the user pressing the stop button.
            while(nextOid.startsWith(baseOid))
            {
                //Instead of checking at the while condition like normal Threads,
                //check for an interrupt here so that an exception can be thrown.
                if(Thread.interrupted()) 
                    throw new InterruptedException();

                newVarBinds = snmpInterface.getNextMIBEntry(nextOid);
 
                //Extract OID information from the VarBindList.
                pair = (SNMPSequence)newVarBinds.getSNMPObjectAt(0);
                
                receivedOid = (SNMPObjectIdentifier)pair.getSNMPObjectAt(0);
                nextOid = receivedOid.toString();

                //This check stops the last OID, which will not start 
                //with with the base OID, from being displayed.
                if(nextOid.startsWith(baseOid)) 
                {
                    //NOTE: the remaining interactions with any Swing components in this thread are all
                    //with data models, and none of them are updated.  From what I've read,
                    //the single thread rule applies to UPDATING Swing VISUAL components.
                    
                    MibTreeNode curNode = root.getNodeByOid(nextOid, true);
                    
                    String displayOid = nextOid;
                    
                    //If the OID or the nearest OID was found in the tree, resolve and format the OID for display.
                    if(curNode != null)
                        displayOid = this.formatDisplayOid(curNode, nextOid);

                    //Extract the returned value from the VarBindList and convert it to a String.
                    snmpValue = pair.getSNMPObjectAt(1);
                    String snmpValueString = snmpValue.toString();                     
                    
                    //There is a potential problem here because the closest node is returned if the exact
                    //match is not found.  However, it seems inefficient to do another search with the
                    //option to return the closest node set to false.
                    if(curNode != null && (snmpValue instanceof SNMPInteger))
                    {
                        MibObjectType curObj = (MibObjectType)curNode.getUserObject();
                        if(curObj.hasValueList())
                        {
                            int intValue = ((BigInteger)snmpValue.getValue()).intValue();
                            
                            String valueName = curObj.getSyntax().matchValueName(intValue);
                            
                            //valueName will be empty if either the value wasn't found or for some reason the name was "".
                            //Either way, the number is more informative than an empty String in this case.
                            if(!valueName.equals(""))       
                                snmpValueString = valueName; 
                        }
                    }  

                    GetRequestResult curItem = new GetRequestResult(displayOid, nextOid, snmpValueString);
                    this.fireResultReceivedEvent(curItem);
                }

                //attempt to slow this sucker down a bit so it doesn't swamp the agent device
                //-- don't know if this is actually a good idea
                //Thread.sleep(150);
            }
            
            return ""; //successful execution and normal termination
        }
        catch(InterruptedException e)
        {
            return "";  
        }
        catch(SNMPBadValueException | SNMPGetException e)
        {
            return e.getMessage();
        }
        catch(SocketTimeoutException e)
        {
            return "No response from host:  " + e.getMessage();
        }
        catch(InterruptedIOException e)
        {
            return "Interrupted during retrieval:  " + e.getMessage();
        }
        catch(UnknownHostException e) 
        {
            return "Unknown host: " + e.getMessage();
        }
        catch(Exception e) //not recommended, but exceptional circumstances will likely always prevent successful execution of the GetRequest process
        {
            return "Exception during retrieval:  " + e.getMessage();
        }
    }
    
    
    /**
     * Replaces a portion of a numerical oid with its equivalent named OID
     * as found in a MibTree and trims the beginning path.
     * For example: 1.3.6.1.2.1.1.1.0 will be converted to system.sysDescr.0
     * 
     * @return a resolved and formatted display OID String
     */
    private String formatDisplayOid(MibTreeNode node, String oidString)
    {
        // Get the full name and number paths of the node.
        String[] paths = node.getOidPaths(); 
        String oidNumberPath = paths[0];
        String oidNamePath = paths[1];

        if(oidString.startsWith(oidNumberPath)) //make sure the OID number pattern isn't matched elsewhere in a really long OID
        {
            oidString = oidString.replaceFirst(oidNumberPath, oidNamePath);

            //This is a bit of a hack since I'm trying to replicate the way GetIf displays
            //OID names during a GET.
            //All it does is chop off the beginning parts of the OID paths to improve
            //display.
            if(oidString.contains(STD_PREFIX))
                oidString = oidString.substring(oidString.indexOf(STD_PREFIX) + STD_PREFIX.length());
            else if(oidString.contains(ENT_PREFIX))
                oidString = oidString.substring(oidString.indexOf(ENT_PREFIX) + ENT_PREFIX.length());
        }
        
        return oidString;
    }
    
    

    // *** Firing methods for updating the GetRequestListeners ***

    private void fireAddressResolvedEvent(final String addressString, final String resolvedAddress)
    {
        final Object[] listeners = requestListeners.getListenerList();
        
        for (int i = listeners.length - 2; i >= 0; i -= 2) 
        {
            if (listeners[i] == GetRequestListener.class) 
            {
                final GetRequestListener currentListener = (GetRequestListener)listeners[i + 1];
                Runnable doFireAddressResolved = () -> {
					currentListener.hostAddressResolved(addressString, resolvedAddress);
				};
                SwingUtilities.invokeLater(doFireAddressResolved);
            }
        }
    }
    
    private void fireResultReceivedEvent(final GetRequestResult result)
    {
        final Object[] listeners = requestListeners.getListenerList();
        boolean test = SwingUtilities.isEventDispatchThread();
        for (int i = listeners.length - 2; i >= 0; i -= 2) 
        {
            if (listeners[i] == GetRequestListener.class) 
            {
                final GetRequestListener currentListener = (GetRequestListener)listeners[i + 1];
                Runnable doFireResultReceived = () -> {
					currentListener.requestResultReceived(result);
				};
                SwingUtilities.invokeLater(doFireResultReceived);
            }
        }
    }
    
    private void fireRequestTerminationEvent(final String statusMessage)
    {
        final Object[] listeners = requestListeners.getListenerList();

        for (int i = listeners.length - 2; i >= 0; i -= 2) 
        {
            if (listeners[i] == GetRequestListener.class) 
            {
                final GetRequestListener currentListener = (GetRequestListener)listeners[i + 1];
                Runnable doRequestTerminated = () -> {
					currentListener.requestTerminated(statusMessage);
				};
                SwingUtilities.invokeLater(doRequestTerminated);
            }
        }
    }
    
}
