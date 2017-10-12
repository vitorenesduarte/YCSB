package com.yahoo.ycsb.db;

import com.yahoo.ycsb.DBException;
import org.testng.annotations.Test;

import java.util.Collections;

@Test
public class EPaxosTest {

  EPaxosClient client = new EPaxosClient();

  @Test
  public void base(){
    try {
      client.init();
    } catch (DBException e) {
      e.printStackTrace();
    }
    client.insert("t","k", Collections.EMPTY_MAP);
    client.read("t","k", Collections.EMPTY_SET,Collections.EMPTY_MAP);
  }


}
