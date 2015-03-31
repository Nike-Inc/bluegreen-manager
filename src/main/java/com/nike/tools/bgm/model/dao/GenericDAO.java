package com.nike.tools.bgm.model.dao;

import java.lang.reflect.ParameterizedType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GenericDAO<T>
{
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @PersistenceContext
  protected EntityManager entityManager;

  protected Class<T> entityClass;

  public GenericDAO()
  {
    ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
    this.entityClass = (Class<T>) genericSuperclass.getActualTypeArguments()[0];
  }

  /**
   * Persists a new entity.
   */
  public void persist(T entity)
  {
    entityManager.persist(entity);
  }

  /**
   * Merges a detached entity.
   */
  public <T> T merge(T entity)
  {
    return entityManager.merge(entity);
  }

  /**
   * Refreshes the entity from the database, overwriting any transient change.
   */
  public void refresh(T entity)
  {
    entityManager.refresh(entity);
  }

  /**
   * Returns true if the given entity is managed in the persistence context.
   */
  public boolean contains(T entity)
  {
    return entityManager.contains(entity);
  }
}
