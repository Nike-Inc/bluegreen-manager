package com.nike.tools.bgm.client.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rds.AmazonRDSClient;

/**
 * Makes client obj for communicating with AWS.
 */
@Component
public class AWSClientFactory
{
  @Value("${bluegreen.aws.access.key.id}")
  private String awsAccessKeyId;

  @Value("${bluegreen.aws.secret.access.key}")
  private String awsSecretAccessKey;

  @Value("${bluegreen.aws.region}")
  private String awsRegionName;

  /**
   * Constructs an AmazonRDSClient and sets the region.
   */
  public AmazonRDSClient makeRegionalRDSClient()
  {
    AWSCredentials credentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
    AmazonRDSClient rds = new AmazonRDSClient(credentials);
    Regions region = Regions.valueOf(awsRegionName);
    rds.setRegion(region);
    return rds;
  }

  //Test purposes only
  void setAwsAccessKeyId(String awsAccessKeyId)
  {
    this.awsAccessKeyId = awsAccessKeyId;
  }

  //Test purposes only
  void setAwsSecretAccessKey(String awsSecretAccessKey)
  {
    this.awsSecretAccessKey = awsSecretAccessKey;
  }

  //Test purposes only
  void setAwsRegionName(String awsRegionName)
  {
    this.awsRegionName = awsRegionName;
  }
}
