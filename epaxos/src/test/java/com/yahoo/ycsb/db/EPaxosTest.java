package com.yahoo.ycsb.db;

import com.yahoo.ycsb.DBException;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Properties;

@Test
public class EPaxosTest {

  EPaxosClient client = new EPaxosClient();

  @Test(enabled =false)
  public void base(){
    try {
      client.setProperties(new Properties());
      client.init();
    } catch (DBException e) {
      e.printStackTrace();
    }
    client.insert("t","k", Collections.EMPTY_MAP);
    client.read("t","k", Collections.EMPTY_SET,Collections.EMPTY_MAP);
  }


}
