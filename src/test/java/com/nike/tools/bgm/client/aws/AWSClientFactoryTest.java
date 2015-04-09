package com.nike.tools.bgm.client.aws;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.regions.Regions;
import com.amazonaws.regions.ServiceAbbreviations;
import com.amazonaws.services.rds.AmazonRDSClient;

import static org.junit.Assert.assertEquals;

public class AWSClientFactoryTest
{
  private static final String KEY_ID = "my-access-key-id";
  private static final String SECRET_KEY = "123456789";
  private static final String REGION_NAME = Regions.US_WEST_2.toString();

  private AWSClientFactory awsClientFactory = new AWSClientFactory();

  @Before
  public void setUp()
  {
    awsClientFactory.setAwsAccessKeyId(KEY_ID);
    awsClientFactory.setAwsSecretAccessKey(SECRET_KEY);
    awsClientFactory.setAwsRegionName(REGION_NAME);
  }

  /**
   * Tests that we can make an rds client.
   */
  @Test
  public void testMakeRegionalRDSClient()
  {
    AmazonRDSClient rds = awsClientFactory.makeRegionalRDSClient();
    assertEquals(ServiceAbbreviations.RDS, rds.getServiceName());
  }
}