package bluegreen.manager.model.dao;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Query;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bare minimum test, since the entityManager is mocked.
 */
@RunWith(MockitoJUnitRunner.class)
public class GenericDAOTest
{
  private static final String FAKE_NAME = "fakeName";

  @InjectMocks
  private FakeEntityDAO fakeEntityDAO;

  @Mock
  private EntityManager mockEntityManager;

  @Mock
  private Query mockQuery;

  @Before
  public void setUp()
  {
    when(mockEntityManager.createQuery(anyString())).thenReturn(mockQuery);
  }

  @Test
  public void testPersist()
  {
    FakeEntity fakeEntity = new FakeEntity();
    fakeEntity.setName(FAKE_NAME);
    fakeEntityDAO.persist(fakeEntity);
    verify(mockEntityManager).persist(fakeEntity);
  }

  @Test
  public void testMerge()
  {
    FakeEntity fakeEntity = new FakeEntity();
    fakeEntityDAO.merge(fakeEntity);
    verify(mockEntityManager).merge(fakeEntity);
  }

  @Test
  public void testRefresh()
  {
    FakeEntity fakeEntity = new FakeEntity();
    fakeEntityDAO.refresh(fakeEntity);
    verify(mockEntityManager).refresh(fakeEntity);
  }

  @Test
  public void testContains()
  {
    FakeEntity fakeEntity = new FakeEntity();
    fakeEntityDAO.contains(fakeEntity);
    verify(mockEntityManager).contains(fakeEntity);
  }

  @Entity
  private static class FakeEntity
  {
    @Id
    private long id;

    private String name;

    public long getId()
    {
      return id;
    }

    public void setId(long id)
    {
      this.id = id;
    }

    public String getName()
    {
      return name;
    }

    public void setName(String name)
    {
      this.name = name;
    }
  }

  private static class FakeEntityDAO extends GenericDAO<FakeEntity>
  {
    //Just take the inherited methods
  }
}