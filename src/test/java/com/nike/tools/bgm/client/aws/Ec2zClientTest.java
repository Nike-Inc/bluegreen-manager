package com.nike.tools.bgm.client.aws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests error-handling capabilities when mock aws returns unexpected results.
 */
public class Ec2zClientTest
{
  private static final String PRIVATE_IP_ADDRESS = "10.111.222.111";
  private static final String INSTANCE_ID = "i-123456";
  private static final String ANOTHER_INSTANCE_ID = "i-234567";

  private AmazonEC2Client mockEC2Client = mock(AmazonEC2Client.class);
  private Ec2zClient ec2Client = new Ec2zClient(mockEC2Client);

  /**
   * Fail case: describe request gets result with empty list of reservations.
   */
  @Test(expected = RuntimeException.class)
  public void testDescribeInstanceByPrivateIPAddress_CantFindReservation()
  {
    setupMock(makeDescribeInstancesResult(false, null));
    ec2Client.describeInstanceByPrivateIPAddress(PRIVATE_IP_ADDRESS);
  }

  /**
   * Warn case: describe request gets result with too many reservations.  Returns instance from first one.
   */
  @Test
  public void testDescribeInstanceByPrivateIPAddress_FoundTooManyReservations()
  {
    setupMock(makeDescribeInstancesResult(false, INSTANCE_ID, ANOTHER_INSTANCE_ID));
    ec2Client.describeInstanceByPrivateIPAddress(PRIVATE_IP_ADDRESS);
  }

  /**
   * Fail case: describe request gets result with one reservation having zero instances inside.
   */
  @Test(expected = RuntimeException.class)
  public void testDescribeInstanceByPrivateIPAddress_CantFindInstance()
  {
    setupMock(makeDescribeInstancesResult(true, null));
    ec2Client.describeInstanceByPrivateIPAddress(PRIVATE_IP_ADDRESS);
  }

  /**
   * Warn case: describe request gets result with one reservation having too many instances inside.
   * Returns first instance.
   */
  @Test
  public void testDescribeInstanceByPrivateIPAddress_FoundTooManyInstances()
  {
    setupMock(makeDescribeInstancesResult(true, INSTANCE_ID, ANOTHER_INSTANCE_ID));
    ec2Client.describeInstanceByPrivateIPAddress(PRIVATE_IP_ADDRESS);
  }

  /**
   * Pass case: describe request gets result with exactly one reservation and one instance inside.
   */
  @Test
  public void testDescribeInstanceByPrivateIPAddress_Pass()
  {
    setupMock(makeDescribeInstancesResult(false, INSTANCE_ID));
    ec2Client.describeInstanceByPrivateIPAddress(PRIVATE_IP_ADDRESS);
  }

  /**
   * Sets up the mock ec2 client to return a fakeResult for the describe-instances call.
   */
  private void setupMock(DescribeInstancesResult fakeResult)
  {
    when(mockEC2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(fakeResult);
  }

  /**
   * Test helper - makes describe result with one or more named instances.
   *
   * @param oneBigReservation If true, puts all the instanceIds in a single reservation.
   *                          Otherwise makes one reservation per instanceId.
   */
  private DescribeInstancesResult makeDescribeInstancesResult(boolean oneBigReservation, String... instanceIds)
  {
    DescribeInstancesResult result = new DescribeInstancesResult();
    List<Reservation> reservations = new ArrayList<Reservation>();
    if (oneBigReservation)
    {
      Reservation reservation = new Reservation();
      List<Instance> instances = new ArrayList<Instance>();
      for (String instanceId : instanceIds)
      {
        Instance instance = new Instance();
        instance.setInstanceId(instanceId);
        instances.add(instance);
      }
      reservation.setInstances(instances);
      reservations.add(reservation);
    }
    else if (ArrayUtils.isNotEmpty(instanceIds))
    {
      for (String instanceId : instanceIds)
      {
        Reservation reservation = new Reservation();
        Instance instance = new Instance();
        instance.setInstanceId(instanceId);
        reservation.setInstances(Arrays.asList(instance));
        reservations.add(reservation);
      }
    }
    result.setReservations(reservations);
    return result;
  }
}