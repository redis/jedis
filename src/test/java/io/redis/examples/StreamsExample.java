//EXAMPLE: stream_tutorial
//HIDE_START
package io.redis.examples;

import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.UnifiedJedis;
//HIDE_END

//REMOVE_START
import org.junit.Test;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.*;

import java.util.*;

import static org.junit.Assert.assertEquals;
//REMOVE_END

public class StreamsExample {

  @Test
  public void run(){

    //HIDE_START
    UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");
    //HIDE_END

    //REMOVE_START
    jedis.del("race:france", "race:italy", "race:usa");
    //REMOVE_END

    // STEP_START xadd
    StreamEntryID res1 = jedis.xadd("race:france",new HashMap<String,String>(){{put("rider","Castilla");put("speed","30.2");put("position","1");put("location_id","1");}} , XAddParams.xAddParams());

    System.out.println(res1); // >>> 1701760582225-0

    StreamEntryID res2 = jedis.xadd("race:france",new HashMap<String,String>(){{put("rider","Norem");put("speed","28.8");put("position","3");put("location_id","1");}} , XAddParams.xAddParams());

    System.out.println(res2); // >>> 1701760582225-1

    StreamEntryID res3 = jedis.xadd("race:france",new HashMap<String,String>(){{put("rider","Prickett");put("speed","29.7");put("position","2");put("location_id","1");}} , XAddParams.xAddParams());

    System.out.println(res3); // >>> 1701760582226-0
    //STEP_END

    //REMOVE_START
    assertEquals(jedis.xlen("race:france"),3);
    //REMOVE_END

    //STEP_START xrange
    List<StreamEntry> res4 = jedis.xrange("race:france","1701760582225-0","+",2);

    System.out.println(res4); // >>> [1701760841292-0 {rider=Castilla, speed=30.2, location_id=1, position=1}, 1701760841292-1 {rider=Norem, speed=28.8, location_id=1, position=3}]
    //STEP_END

    //STEP_START xread_block
    List<Map.Entry<String, List<StreamEntry>>> res5= jedis.xread(XReadParams.xReadParams().block(300).count(100),new HashMap<String,StreamEntryID>(){{put("race:france",new StreamEntryID());}});
    System.out.println(
      res5
    ); // >>> [race:france=[1701761996660-0 {rider=Castilla, speed=30.2, location_id=1, position=1}, 1701761996661-0 {rider=Norem, speed=28.8, location_id=1, position=3}, 1701761996661-1 {rider=Prickett, speed=29.7, location_id=1, position=2}]]
    //STEP_END

    //STEP_START xadd_2
    StreamEntryID res6 = jedis.xadd("race:france",new HashMap<String,String>(){{put("rider","Castilla");put("speed","29.9");put("position","2");put("location_id","1");}} , XAddParams.xAddParams());
    System.out.println(res6); // >>> 1701762285679-0
    //STEP_END

    //STEP_START xlen
    long res7 = jedis.xlen("race:france");
    System.out.println(res7); // >>> 4
    //STEP_END

    //STEP_START xadd_id
    StreamEntryID res8 = jedis.xadd("race:usa", new HashMap<String,String>(){{put("racer","Castilla");}},XAddParams.xAddParams().id("0-1"));
    System.out.println(res8); // >>> 0-1

    StreamEntryID res9 = jedis.xadd("race:usa", new HashMap<String,String>(){{put("racer","Norem");}},XAddParams.xAddParams().id("0-2"));
    System.out.println(res9); // >>> 0-2
    //STEP_END

    //STEP_START xadd_bad_id
    try {
      StreamEntryID res10 = jedis.xadd("race:usa", new HashMap<String,String>(){{put("racer","Prickett");}},XAddParams.xAddParams().id("0-1"));
      System.out.println(res10); // >>> 0-1
    }
    catch (JedisDataException e){
      System.out.println(e); // >>> ERR The ID specified in XADD is equal or smaller than the target stream top item
    }
    //STEP_END

    //STEP_START xadd_7
    StreamEntryID res11 = jedis.xadd("race:usa", new HashMap<String,String>(){{put("racer","Norem");}},XAddParams.xAddParams().id("0-*"));
    System.out.println(res11);
    //STEP_END

    //STEP_START xrange_all
    List<StreamEntry> res12 = jedis.xrange("race:france","-","+");
    System.out.println(
      res12
    ); // >>> [1701764734160-0 {rider=Castilla, speed=30.2, location_id=1, position=1}, 1701764734160-1 {rider=Norem, speed=28.8, location_id=1, position=3}, 1701764734161-0 {rider=Prickett, speed=29.7, location_id=1, position=2}, 1701764734162-0 {rider=Castilla, speed=29.9, location_id=1, position=2}]
    //STEP_END

    //STEP_START xrange_time
    List<StreamEntry> res13 = jedis.xrange("race:france",String.valueOf(System.currentTimeMillis()-1000),String.valueOf(System.currentTimeMillis()+1000));
    System.out.println(
      res13
    ); // >>> [1701764734160-0 {rider=Castilla, speed=30.2, location_id=1, position=1}, 1701764734160-1 {rider=Norem, speed=28.8, location_id=1, position=3}, 1701764734161-0 {rider=Prickett, speed=29.7, location_id=1, position=2}, 1701764734162-0 {rider=Castilla, speed=29.9, location_id=1, position=2}]
    //STEP_END

    //STEP_START xrange_step_1
    List<StreamEntry> res14 = jedis.xrange("race:france","-","+",2);
    System.out.println(res14); // >>> [1701764887638-0 {rider=Castilla, speed=30.2, location_id=1, position=1}, 1701764887638-1 {rider=Norem, speed=28.8, location_id=1, position=3}]
    //STEP_END

    //STEP_START xrange_step_2
    List<StreamEntry> res15 = jedis.xrange("race:france",String.valueOf(System.currentTimeMillis()-1000)+"-0","+",2);
    System.out.println(res15); // >>> [1701764887638-0 {rider=Castilla, speed=30.2, location_id=1, position=1}, 1701764887638-1 {rider=Norem, speed=28.8, location_id=1, position=3}]
    //STEP_END

    //STEP_START xrange_empty
    List<StreamEntry> res16 = jedis.xrange("race:france",String.valueOf(System.currentTimeMillis()+1000)+"-0","+",2);
    System.out.println(res16); // >>> []
    // STEP_END

    //STEP_START xrevrange
    List<StreamEntry> res17 = jedis.xrevrange("race:france","+","-",1);
    System.out.println(res17); // >>> [1701765218592-0 {rider=Castilla, speed=29.9, location_id=1, position=2}]
    //STEP_END

    //STEP_START xread
    List<Map.Entry<String, List<StreamEntry>>> res18= jedis.xread(XReadParams.xReadParams().count(2),new HashMap<String,StreamEntryID>(){{put("race:france",new StreamEntryID());}});
    System.out.println(
      res18
    ); // >>> [race:france=[1701765384638-0 {rider=Castilla, speed=30.2, location_id=1, position=1}, 1701765384638-1 {rider=Norem, speed=28.8, location_id=1, position=3}]]
    //STEP_END

    //STEP_START xgroup_create
    String res19 = jedis.xgroupCreate("race:france","france_riders",StreamEntryID.LAST_ENTRY,false);
    System.out.println(res19); // >>> OK
    //STEP_END

    //STEP_START xgroup_create_mkstream
    String res20 = jedis.xgroupCreate("race:italy","italy_riders",StreamEntryID.LAST_ENTRY,true);
    System.out.println(res20); // >>> OK
    //STEP_END

    //STEP_START xgroup_read
    StreamEntryID id1 = jedis.xadd("race:italy", new HashMap<String,String>(){{put("rider","Castilaa");}},XAddParams.xAddParams());
    StreamEntryID id2 = jedis.xadd("race:italy", new HashMap<String,String>(){{put("rider","Royce");}},XAddParams.xAddParams());
    StreamEntryID id3 = jedis.xadd("race:italy", new HashMap<String,String>(){{put("rider","Sam-Bodden");}},XAddParams.xAddParams());
    StreamEntryID id4 = jedis.xadd("race:italy", new HashMap<String,String>(){{put("rider","Prickett");}},XAddParams.xAddParams());
    StreamEntryID id5 = jedis.xadd("race:italy", new HashMap<String,String>(){{put("rider","Norem");}},XAddParams.xAddParams());

    List<Map.Entry<String, List<StreamEntry>>> res21 = jedis.xreadGroup("italy_riders","Alice", XReadGroupParams.xReadGroupParams().count(1),new HashMap<String,StreamEntryID>(){{put("race:italy",StreamEntryID.UNRECEIVED_ENTRY);}});
    System.out.println(res21); // >>> [race:italy=[1701766299006-0 {rider=Castilaa}]]
    //STEP_END

    //STEP_START xgroup_read_id
    List<Map.Entry<String, List<StreamEntry>>> res22 = jedis.xreadGroup("italy_riders","Alice", XReadGroupParams.xReadGroupParams().count(1),new HashMap<String,StreamEntryID>(){{put("race:italy",new StreamEntryID());}});
    System.out.println(res22); // >>> [race:italy=[1701766299006-0 {rider=Castilaa}]]
    //STEP_END

    //STEP_START xack
    long res23 = jedis.xack("race:italy","italy_riders",id1);
    System.out.println(res23); // >>> 1

    List<Map.Entry<String, List<StreamEntry>>> res24 = jedis.xreadGroup("italy_riders","Alice", XReadGroupParams.xReadGroupParams().count(1),new HashMap<String,StreamEntryID>(){{put("race:italy",new StreamEntryID());}});
    System.out.println(res24); // >>> [race:italy=[]]
    //STEP_END

    //STEP_START xgroup_read_bob
    List<Map.Entry<String, List<StreamEntry>>> res25 = jedis.xreadGroup("italy_riders","Bob", XReadGroupParams.xReadGroupParams().count(2),new HashMap<String,StreamEntryID>(){{put("race:italy",StreamEntryID.UNRECEIVED_ENTRY);}});
    System.out.println(res25); // >>> [race:italy=[1701767632261-1 {rider=Royce}, 1701767632262-0 {rider=Sam-Bodden}]]
    //STEP_END

    //STEP_START xpending
    StreamPendingSummary res26 = jedis.xpending("race:italy","italy_riders");
    System.out.println(res26.getConsumerMessageCount()); // >>> {Bob=2}
    //STEP_END

    //STEP_START xpending_plus_minus
    List<StreamPendingEntry> res27 = jedis.xpending("race:italy","italy_riders",XPendingParams.xPendingParams().start(StreamEntryID.MINIMUM_ID).end(StreamEntryID.MAXIMUM_ID).count(10));
    System.out.println(res27); // >>> [1701768567412-1 Bob idle:0 times:1, 1701768567412-2 Bob idle:0 times:1]
    //STEP_END

    //STEP_START xrange_pending
    List<StreamEntry> res28 = jedis.xrange("race:italy",id2.toString(),id2.toString());
    System.out.println(res28); // >>> [1701768744819-1 {rider=Royce}]
    //STEP_END

    //STEP_START xclaim
    List<StreamEntry> res29 = jedis.xclaim("race:italy","italy_riders","Alice", 0L, XClaimParams.xClaimParams().time(60000),id2);
    System.out.println(res29); // >>> [1701769004195-1 {rider=Royce}]
    //STEP_END

    //STEP_START xautoclaim
    Map.Entry<StreamEntryID, List<StreamEntry>> res30 = jedis.xautoclaim("race:italy","italy_riders","Alice",1L,new StreamEntryID("0-0"),XAutoClaimParams.xAutoClaimParams().count(1));
    System.out.println(res30); // >>> [1701769266831-2=[1701769266831-1 {rider=Royce}]
    //STEP_END

    //STEP_START xautoclaim_cursor
    Map.Entry<StreamEntryID, List<StreamEntry>> res31 = jedis.xautoclaim("race:italy","italy_riders","Alice",1L,new StreamEntryID(id2.toString()),XAutoClaimParams.xAutoClaimParams().count(1));
    System.out.println(res31); // >>> [0-0=[1701769605847-2 {rider=Sam-Bodden}]
    //STEP_END

    //STEP_START xinfo
    StreamInfo res32 = jedis.xinfoStream("race:italy");
    System.out.println(
      res32.getStreamInfo()
    ); // >>> {radix-tree-keys=1, radix-tree-nodes=2, entries-added=5, length=5, groups=1, max-deleted-entry-id=0-0, first-entry=1701769637612-0 {rider=Castilaa}, last-generated-id=1701769637612-4, last-entry=1701769637612-4 {rider=Norem}, recorded-first-entry-id=1701769637612-0}
    //STEP_END

    //STEP_START xinfo_groups
    List<StreamGroupInfo> res33 = jedis.xinfoGroups("race:italy");
    for (StreamGroupInfo a : res33){
      System.out.println(
        a.getGroupInfo()
      ); // >>> {last-delivered-id=1701770253659-0, lag=2, pending=2, name=italy_riders, consumers=2, entries-read=3}
    }
    //STEP_END

    //STEP_START xinfo_consumers
    List<StreamConsumersInfo> res34 = jedis.xinfoConsumers("race:italy","italy_riders");
    for (StreamConsumerInfo a : res34){
      System.out.println(
        a.getConsumerInfo()
      ); // {inactive=1, idle=1, pending=1, name=Alice} , {inactive=3, idle=3, pending=1, name=Bob}
    }
    //STEP_END

    //STEP_START maxlen
    jedis.xadd("race:italy", new HashMap<String,String>(){{put("rider","Jones");}},XAddParams.xAddParams().maxLen(10));
    jedis.xadd("race:italy", new HashMap<String,String>(){{put("rider","Wood");}},XAddParams.xAddParams().maxLen(10));
    jedis.xadd("race:italy", new HashMap<String,String>(){{put("rider","Henshaw");}},XAddParams.xAddParams().maxLen(10));
    long res35 = jedis.xlen("race:italy");
    System.out.println(res35); // >>> 8

    List<StreamEntry> res36 = jedis.xrange("race:italy","-","+");
    System.out.println(res36); // >>> [1701771219852-0 {rider=Castilaa}, 1701771219852-1 {rider=Royce}, 1701771219853-0 {rider=Sam-Bodden}, 1701771219853-1 {rider=Prickett}, 1701771219853-2 {rider=Norem}, 1701771219858-0 {rider=Jones}, 1701771219858-1 {rider=Wood}, 1701771219859-0 {rider=Henshaw}]

    StreamEntryID id6 = jedis.xadd("race:italy", new HashMap<String,String>(){{put("rider","Smith");}},XAddParams.xAddParams().maxLen(2));

    List<StreamEntry> res37 = jedis.xrange("race:italy","-","+");
    System.out.println(res37); // >>> [1701771067332-1 {rider=Henshaw}, 1701771067332-2 {rider=Smith}]
    //STEP_END

    //STEP_START xtrim
    long res38 = jedis.xtrim("race:italy",XTrimParams.xTrimParams().maxLen(10).exactTrimming());
    System.out.println(res38); /// >>> 0
    //STEP_END

    //STEP_START xtrim2
    long res39 = jedis.xtrim("race:italy",XTrimParams.xTrimParams().maxLen(10));
    System.out.println(res39); /// >>> 0
    //STEP_END

    //STEP_START xdel
    List<StreamEntry> res40 = jedis.xrange("race:italy","-","+");
    System.out.println(res40); // >>> [1701771356428-2 {rider=Henshaw}, 1701771356429-0 {rider=Smith}]

    long res41 = jedis.xdel("race:italy",id6);
    System.out.println(res41); // >>> 1

    List<StreamEntry> res42 = jedis.xrange("race:italy","-","+");
    System.out.println(res42); // >>> [1701771517639-1 {rider=Henshaw}]
    //STEP_END
  }

}
