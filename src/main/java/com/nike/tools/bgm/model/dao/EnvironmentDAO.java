package com.nike.tools.bgm.model.dao;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.nike.tools.bgm.model.domain.Environment;

@Repository
public class EnvironmentDAO
{
  @PersistenceContext
  private EntityManager entityManager;

  public List<Environment> findAll()
  {
    return entityManager.createQuery("SELECT OBJECT(e) FROM " + Environment.class.getSimpleName() + " as e").getResultList();
  }
}
