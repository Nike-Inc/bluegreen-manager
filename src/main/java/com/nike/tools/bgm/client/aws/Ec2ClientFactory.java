package com.nike.tools.bgm.client.aws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Simple factory class that simply constructs an EC2Client.
 * <p/>
 * Pulling this into its own class makes the client classes more testable.
 */
@Component
public class Ec2ClientFactory
{
  @Autowired
  private AwsClientFactory awsClientFactory;

  public Ec2Client create()
  {
    return new Ec2Client(awsClientFactory.makeRegionalEC2Client());
  }
}
