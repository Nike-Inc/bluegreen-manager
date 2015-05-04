package com.nike.tools.bgm.client.aws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Simple factory class that simply constructs an EC2Client.
 * <p/>
 * Pulling this into its own class makes the client classes more testable.
 */
@Component
public class EC2ClientFactory
{
  @Autowired
  private AWSClientFactory awsClientFactory;

  public EC2Client create()
  {
    return new EC2Client(awsClientFactory.makeRegionalEC2Client());
  }
}
