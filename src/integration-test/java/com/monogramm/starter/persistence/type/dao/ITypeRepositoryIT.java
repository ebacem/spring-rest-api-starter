/*
 * Creation by madmath03 the 2017-09-11.
 */

package com.monogramm.starter.persistence.type.dao;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import com.monogramm.starter.config.data.InitialDataLoader;
import com.monogramm.starter.persistence.AbstractGenericRepositoryIT;
import com.monogramm.starter.persistence.type.entity.Type;
import com.monogramm.starter.persistence.type.exception.TypeNotFoundException;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@link ITypeRepository} Integration Test.
 * 
 * @author madmath03
 */
public class ITypeRepositoryIT extends AbstractGenericRepositoryIT<Type, ITypeRepository> {

  private static final String DISPLAYNAME = "Foo";

  @Autowired
  private InitialDataLoader initialDataLoader;

  @Override
  protected Type buildTestEntity() {
    return Type.builder(DISPLAYNAME).build();
  }

  /**
   * Test method for {@link ITypeRepository#findAll()}.
   */
  @Override
  @Test
  public void testFindAll() {
    int expectedSize = 0;
    // ...plus the permissions created at application initialization
    if (initialDataLoader.getTypes() != null) {
      expectedSize += initialDataLoader.getTypes().size();
    }

    final List<Type> actual = getRepository().findAll();

    assertNotNull(actual);
    assertEquals(expectedSize, actual.size());
  }

  /**
   * Test method for
   * {@link ITypeRepository#findAllContainingNameIgnoreCase(java.lang.String)}.
   */
  @Test
  public void testFindAllContainingNameIgnoreCase() {
    final List<Type> models = new ArrayList<>();

    final List<Type> actual = getRepository().findAllContainingNameIgnoreCase(DISPLAYNAME);

    assertThat(actual, is(models));
  }

  /**
   * Test method for {@link ITypeRepository#findByNameIgnoreCase(String)}.
   * 
   * @throws TypeNotFoundException if the type is not found.
   */
  @Test
  public void testFindByNameIgnoreCase() {
    final Type model = this.buildTestEntity();
    model.setName(model.getName().toUpperCase());
    getRepository().add(model);

    final Type actual = getRepository().findByNameIgnoreCase(DISPLAYNAME);

    assertThat(actual, is(model));
  }

  /**
   * Test method for {@link ITypeRepository#findByNameIgnoreCase(java.lang.String)}.
   * 
   * @throws TypeNotFoundException if the type is not found.
   */
  @Test
  public void testFindByNameIgnoreCaseNoResult() {
    assertNull(getRepository().findByNameIgnoreCase(null));
  }

  /**
   * Test method for {@link ITypeRepository#findByNameIgnoreCase(java.lang.String)}.
   * 
   * @throws TypeNotFoundException if the type is not found.
   */
  @Test
  public void testFindByNameIgnoreCaseNonUnique() {
    getRepository().add(Type.builder(DISPLAYNAME + "1").build());
    getRepository().add(Type.builder(DISPLAYNAME + "2").build());

    assertNull(getRepository().findByNameIgnoreCase(DISPLAYNAME));
  }

  /**
   * Test method for {@link ITypeRepository#findByNameIgnoreCase(java.lang.String)}.
   * 
   * @throws TypeNotFoundException if the type is not found.
   */
  @Test
  public void testFindByNameIgnoreCaseNotFound() {
    assertNull(getRepository().findByNameIgnoreCase(null));
  }

  /**
   * Test method for {@link ITypeRepository#exists(java.util.UUID, java.lang.String)}.
   */
  @Test
  public void testExistsUUIDString() {
    final boolean expected = true;
    final Type model = this.buildTestEntity();
    final List<Type> models = new ArrayList<>(1);
    models.add(model);
    getRepository().save(models);

    final boolean actual = getRepository().exists(model.getId(), model.getName());

    assertThat(actual, is(expected));
  }

  /**
   * Test method for {@link ITypeRepository#exists(java.util.UUID, java.lang.String)}.
   */
  @Test
  public void testExistsUUIDStringNotFound() {
    final boolean expected = false;

    final boolean actual = getRepository().exists(RANDOM_ID, DISPLAYNAME);

    assertThat(actual, is(expected));
  }

}
