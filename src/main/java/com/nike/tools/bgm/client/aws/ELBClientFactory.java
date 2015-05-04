package com.nike.tools.bgm.client.aws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Simple factory class that simply constructs an ELBClient.
 * <p/>
 * Pulling this into its own class makes the client classes more testable.
 */
@Component
public class ELBClientFactory
{
  @Autowired
  private AWSClientFactory awsClientFactory;

  public ELBClient create()
  {
    return new ELBClient(awsClientFactory.makeRegionalELBClient());
  }
}
