package com.nike.tools.bgm.client.aws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Simple factory class that simply constructs an RDSClient.
 * <p/>
 * Pulling this into its own class makes the client classes more testable.
 */
@Component
public class RdsClientFactory
{
  @Autowired
  private AwsClientFactory awsClientFactory;

  public RdsClient create()
  {
    return new RdsClient(awsClientFactory.makeRegionalRdsClient());
  }
}
