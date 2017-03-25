/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package input;

import core.DTNHost;
import core.Message;
import core.World;

/**
 * External event for creating a message.
 */
public class MessageCreateEvent extends MessageEvent {
	private int size;
	private int responseSize;
	private double lat;
	private double lon;
	private String timeStamp;
	private String type;
	private boolean extraParams = false;


	/**
	 * Creates a message creation event with a optional response request
	 * @param from The creator of the message
	 * @param to Where the message is destined to
	 * @param id ID of the message
	 * @param size Size of the message
	 * @param responseSize Size of the requested response message or 0 if
	 * no response is requested
	 * @param time Time, when the message is created
	 */
	public MessageCreateEvent(int from, int to, String id, int size,
			int responseSize, double time) {
		super(from,to, id, time);
		this.size = size;
		this.responseSize = responseSize;
	}

	// Added Disarm
	public MessageCreateEvent(int from, int to, String id, int size,
							  int responseSize, double time, double lat, double lon, String timeStamp, String type) {
		super(from,to, id, time);
		this.size = size;
		this.responseSize = responseSize;
		this.lat = lat;
		this.lon = lon;
		this.timeStamp = timeStamp;
		this.type = type;
		this.extraParams = true;
	}


	/**
	 * Creates the message this event represents.
	 */
	@Override
	public void processEvent(World world) {
		DTNHost to = world.getNodeByAddress(this.toAddr);
		DTNHost from = world.getNodeByAddress(this.fromAddr);

		// Added Disarm
		if(extraParams == true)
		{
			Message m = new Message(from, to, this.id, this.size);
			m.addProperty("Lat",this.lat);
			m.addProperty("Lon",this.lon);
			m.addProperty("TimeStamp",this.timeStamp);
			m.addProperty("Type",this.type);

			m.setResponseSize(this.responseSize);
			from.createNewMessage(m);
		}
		else
		{
			Message m = new Message(from, to, this.id, this.size);
			m.setResponseSize(this.responseSize);
			from.createNewMessage(m);

		}

	}

	@Override
	public String toString() {
		return super.toString() + " [" + fromAddr + "->" + toAddr + "] " +
		"size:" + size + " CREATE";
	}
}
