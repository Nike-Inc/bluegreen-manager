package bluegreen.manager.jobs;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import bluegreen.manager.model.domain.TaskHistory;
import bluegreen.manager.model.domain.TaskStatus;
import bluegreen.manager.utils.DateHelper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class SkipRemarkHelperTest
{
  private static Date START_TIME = DateHelper.tzstringToDate("2015-01-01 12:30:00 -0800");
  private static Timestamp START_TIMESTAMP = new Timestamp(START_TIME.getTime());

  private TaskStatus priorStatus;
  private boolean force;
  private boolean expectSkip;
  private SkipRemarkHelper skipRemarkHelper;
  private TaskHistory priorTaskHistory;

  public SkipRemarkHelperTest(TaskStatus priorStatus, boolean force, boolean expectSkip)
  {
    this.priorStatus = priorStatus;
    this.force = force;
    this.expectSkip = expectSkip;
    this.skipRemarkHelper = new SkipRemarkHelper();

    this.priorTaskHistory = new TaskHistory();
    this.priorTaskHistory.setStartTime(START_TIMESTAMP);
    this.priorTaskHistory.setStatus(priorStatus);
  }

  @Parameters
  public static Collection<Object[]> goodData()
  {
    return Arrays.asList(new Object[][] {
        //No force
        {
            TaskStatus.SKIPPED, false, true
        },
        {
            TaskStatus.DONE, false, true
        },
        {
            TaskStatus.PROCESSING, false, false
        },
        {
            TaskStatus.ERROR, false, false
        },
        //Yes force
        {
            TaskStatus.SKIPPED, true, false
        },
        {
            TaskStatus.DONE, true, false
        },
        {
            TaskStatus.PROCESSING, true, false
        },
        {
            TaskStatus.ERROR, true, false
        },
    });
  }

  /**
   * Tests that 'make' produces the right value of 'skip'.
   * <p/>
   * Also sneaks in a test of useRemark.
   */
  @Test
  public void testMake()
  {
    SkipRemark skipRemark = skipRemarkHelper.make(priorStatus, force);
    assertEquals(expectSkip, skipRemark.isSkip());
    assertTrue(skipRemarkHelper.useRemark(skipRemark, priorTaskHistory).contains(priorStatus.toString()));
  }
}
