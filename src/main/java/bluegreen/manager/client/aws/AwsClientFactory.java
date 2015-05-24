package bluegreen.manager.client.aws;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.rds.AmazonRDSClient;

/**
 * Makes client objects for communicating with AWS.
 */
@Component
public class AwsClientFactory
{
  @Value("${bluegreen.aws.access.key.id}")
  private String awsAccessKeyId;

  @Value("${bluegreen.aws.secret.access.key}")
  private String awsSecretAccessKey;

  @Value("${bluegreen.aws.region}")
  private String awsRegionName;

  private Region awsRegion;

  /**
   * Converts the configured region name to an aws region enum constant.
   */
  @PostConstruct
  public void getRegionConstant()
  {
    awsRegion = Region.getRegion(Regions.valueOf(awsRegionName));
  }

  /**
   * Constructs an AmazonEC2Client and sets the region.
   */
  public AmazonEC2Client makeRegionalEc2Client()
  {
    AWSCredentials credentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
    AmazonEC2Client ec2 = new AmazonEC2Client(credentials);
    ec2.setRegion(awsRegion);
    return ec2;
  }

  /**
   * Constructs an AmazonElasticLoadBalancingClient and sets the region.
   */
  public AmazonElasticLoadBalancingClient makeRegionalElbClient()
  {
    AWSCredentials credentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
    AmazonElasticLoadBalancingClient elb = new AmazonElasticLoadBalancingClient(credentials);
    elb.setRegion(awsRegion);
    return elb;
  }

  /**
   * Constructs an AmazonRDSClient and sets the region.
   */
  public AmazonRDSClient makeRegionalRdsClient()
  {
    AWSCredentials credentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
    AmazonRDSClient rds = new AmazonRDSClient(credentials);
    rds.setRegion(awsRegion);
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
