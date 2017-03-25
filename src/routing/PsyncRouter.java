/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import core.Connection;
import core.Message;
import core.Settings;
import core.SimClock;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Psync message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class PsyncRouter extends ActiveRouter {

    HashMap<Message,Double> msgImportanceMap;
    public static final String psyncNS = "PsyncRouter";
    private String TEST_TIME = "testTime";
    String testTime;
    Date testBeginTime;


    /**
     * Constructor. Creates a new message router based on the settings in
     * the given Settings object.
     * @param s The settings object
     */
    public PsyncRouter(Settings s) {
        super(s);

        // Added Test time
        Settings psyncSettings = new Settings(psyncNS);
        testTime = psyncSettings.getSetting(TEST_TIME);

        try {
            testBeginTime = new SimpleDateFormat("yyyyMMddHHmmss").parse(testTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        this.msgImportanceMap = new HashMap<Message, Double>();
        //TODO: read&use Psync router specific settings (if any)
    }

    /**
     * Copy constructor.
     * @param r The router prototype where setting values are copied from
     */
    protected PsyncRouter(PsyncRouter r) {
        super(r);

        // Added Test time
        this.testTime = r.testTime;

        try {
            testBeginTime = new SimpleDateFormat("yyyyMMddHHmmss").parse(testTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        this.msgImportanceMap = new HashMap<Message, Double>();
        //TODO: copy Psync settings here (if any)
    }

    @Override
    public void update() {
        super.update();
        if (isTransferring() || !canStartTransfer()) {
            return; // transferring, don't try other connections yet
        }
//
//        // Try first the messages that can be delivered to final recipient
//        if (exchangeDeliverableMessages() != null) {
//            return; // started a transfer, don't try others (yet)
//        }

        // then try any/all message to any/all connection
        this.tryAllMessagesToAllConnectionsByImportance();
    }


    protected Connection tryAllMessagesToAllConnectionsByImportance(){
        List<Connection> connections = getConnections();
        if (connections.size() == 0 || this.getNrofMessages() == 0) {
            return null;
        }

        List<Message> messages =
                new ArrayList<Message>(this.getMessageCollection());
        //this.sortByQueueMode(messages);

        for(Message msg : messages){
//            System.out.println(msg.getProperty("Lat"));
            // Set initial importance
            msgImportanceMap.put(msg, 0.0);
        }

        msgImportanceMap = calculateAllImportances();

        // Sort according to importance
        Collections.sort(messages, new FilePriorityComparator());

        return tryMessagesToConnections(messages, connections);
    }

    public String getTileXYString(double lat, double lon, int zoom){

        int xtile = (int)Math.floor( (lon + 180) / 360 * (1<<zoom) ) ;
        int ytile = (int)Math.floor( (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<zoom) ) ;
        if (xtile < 0)
            xtile=0;
        if (xtile >= (1<<zoom))
            xtile=((1<<zoom)-1);
        if (ytile < 0)
            ytile=0;
        if (ytile >= (1<<zoom))
            ytile=((1<<zoom)-1);
        return("" + zoom + "-" + xtile + "-" + ytile + ".topojson");
    }

    public HashMap<Message,Double>  calculateAllImportances(){

        HashMap<Message,Double> newMsgImportanceMap = new HashMap<Message,Double>();

        HashMap<Message,String> msgTileTypeStrMap = new HashMap<Message,String>();

        // Fij
        HashMap<String, Integer> tileTypeCounts = new HashMap<String, Integer>();

        // Total messeges N
        int totalCount = 0;

        // Increment tile-type counts (calculate Fij)
        for(Map.Entry<Message, Double> entry : msgImportanceMap.entrySet()){

            String typeStr = entry.getKey().getProperty("Type").toString();

            double lat = (double)entry.getKey().getProperty("Lat");
            double lon = (double)entry.getKey().getProperty("Lon");
            String tileStr = getTileXYString(lat, lon, 15);

            String tileTypeStr = tileStr + typeStr;

            if(tileTypeCounts.get(tileTypeStr) == null){
                tileTypeCounts.put(tileTypeStr, 0);
            }
            tileTypeCounts.put(tileTypeStr, (tileTypeCounts.get(tileTypeStr) + 1));
            msgTileTypeStrMap.put(entry.getKey(), tileTypeStr);
            totalCount += 1;
        }


        // Calculate Importance
        for(Map.Entry<Message, Double> entry : msgImportanceMap.entrySet()){
            String tileTypeStr = msgTileTypeStrMap.get(entry.getKey());
            int Fij = tileTypeCounts.get(tileTypeStr);
            System.out.println("Fij: " + Fij);
            double Pij = (double)Fij/(double)totalCount;

            // calculate -log(Pij)
            double logPij = (double)(-1) * (Math.log(Pij) / Math.log((double)2));

            // eliminate -0
            if(logPij == 0){
                logPij = 0;
            }
            Date originDateTime=null, currentDateTime=null;
            // calculate e^(t2-t1)
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            try {
                originDateTime = dateFormat.parse(entry.getKey().getProperty("TimeStamp").toString());
                currentDateTime = new Date ((long) (testBeginTime.getTime()  + (SimClock.getTime() * 1000)));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            double ageInSeconds = (currentDateTime.getTime() - originDateTime.getTime())/1000.0;
            double ageInHours = ageInSeconds / (double) 3600.0;

            // Calculate Final Importance
            double importanceValue = logPij * Math.pow(2.71828, ((double)(-1.0) * ageInHours));
            System.out.println("Host: " + this.getHost().getAddress() +  ", Message : " + entry.getKey().getId() +", Importance : " + importanceValue + ", Age :" + ageInSeconds);

            newMsgImportanceMap.put(entry.getKey(), logPij);
        }



        return newMsgImportanceMap;
    }


    @Override
    public PsyncRouter replicate() {
        return new PsyncRouter(this);
    }

    class FilePriorityComparator implements Comparator<Message>{
        @Override
        public int compare(Message x, Message y){
            if(msgImportanceMap.get(x) > msgImportanceMap.get(y) ){
                return -1;
            }
            else{
                return 1;
            }
        }
    }

}
