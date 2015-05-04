package com.nike.tools.bgm.client.aws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

import static com.nike.tools.bgm.client.aws.Ec2InstanceFilter.PRIVATE_IP_ADDRESS;

/**
 * Sends commands to Amazon EC2.
 * <p/>
 * All methods here communicate with Amazon and use a StopWatch.
 */
public class Ec2Client
{
  private static final Logger LOGGER = LoggerFactory.getLogger(Ec2Client.class);

  /**
   * Synchronous client, requests will block til done.
   */
  private AmazonEC2Client awsEc2Client;

  public Ec2Client(AmazonEC2Client awsEc2Client)
  {
    this.awsEc2Client = awsEc2Client;
  }

  /**
   * Uses the 'ec2 describe instances' command to lookup the ec2 instance by its private ip address.
   */
  public Instance describeInstanceByPrivateIPAddress(String privateIpAddress)
  {
    LOGGER.debug("describeInstances(filter privateIpAddress: " + privateIpAddress + ")");
    if (StringUtils.isBlank(privateIpAddress))
    {
      throw new IllegalArgumentException("Blank privateIpAddress");
    }
    StopWatch stopWatch = new StopWatch();
    try
    {
      stopWatch.start();
      List<Filter> filters = new ArrayList<Filter>();
      filters.add(new Filter(PRIVATE_IP_ADDRESS.toString(), Arrays.asList(privateIpAddress)));
      DescribeInstancesRequest request = new DescribeInstancesRequest();
      request.setFilters(filters);
      DescribeInstancesResult result = awsEc2Client.describeInstances(request);
      final String context = "privateIpAddress " + privateIpAddress;
      if (result == null || CollectionUtils.isEmpty(result.getReservations()))
      {
        throw new RuntimeException("EC2 cannot find reservation with " + context);
      }
      else if (result.getReservations().size() > 1)
      {
        LOGGER.warn("Expected 1 reservation with " + context + ", found " + result.getReservations().size());
      }
      return getFirstInstance(result.getReservations().get(0), context);
    }
    finally
    {
      stopWatch.stop();
      LOGGER.debug("describeInstances time elapsed: " + stopWatch);
    }
  }

  /**
   * Returns the first ec2 instance from a reservation.  Throws if no instances, warns if multiple instances.
   * <p/>
   *
   * @param context For error messages, if needed.
   */
  private Instance getFirstInstance(Reservation reservation, String context)
  {
    if (reservation == null || CollectionUtils.isEmpty(reservation.getInstances()))
    {
      throw new RuntimeException("EC2 cannot find instance with " + context);
    }
    else if (reservation.getInstances().size() > 1)
    {
      LOGGER.warn("Expected 1 instance with " + context + ", found " + reservation.getInstances().size());
    }
    return reservation.getInstances().get(0);
  }

}
