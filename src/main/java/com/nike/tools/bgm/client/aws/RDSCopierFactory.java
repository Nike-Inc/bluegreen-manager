package com.nike.tools.bgm.client.aws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Simple factory class that simply constructs an RDSCopier.
 * <p/>
 * Pulling this into its own class makes the client classes more testable.
 */
@Component
public class RDSCopierFactory
{
  @Autowired
  private AWSClientFactory awsClientFactory;

  public RDSClient create()
  {
    return new RDSClient(awsClientFactory.makeRegionalRDSClient());
  }
}
