package com.nike.tools.bgm.client.aws;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.regions.Regions;
import com.amazonaws.regions.ServiceAbbreviations;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
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
    awsClientFactory.getRegionConstant();
  }

  /**
   * Tests that we can make an elb client.
   */
  @Test
  public void testMakeRegionalEC2Client()
  {
    AmazonEC2Client ec2 = awsClientFactory.makeRegionalEC2Client();
    assertEquals(ServiceAbbreviations.EC2, ec2.getServiceName());
  }

  /**
   * Tests that we can make an elb client.
   */
  @Test
  public void testMakeRegionalELBClient()
  {
    AmazonElasticLoadBalancingClient elb = awsClientFactory.makeRegionalELBClient();
    assertEquals(ServiceAbbreviations.ElasticLoadbalancing, elb.getServiceName());
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