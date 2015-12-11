package br.edu.ufba.softvis.visminer.persistence.dao;

import java.util.List;

import br.edu.ufba.softvis.visminer.model.database.SoftwareUnitDB;

/**
 * Software_unit table DAO interface.
 */
public interface SoftwareUnitDAO extends DAO<SoftwareUnitDB, Integer>{

  /**
   * @param uid
   * @return Software unit by uid.
   */
  public SoftwareUnitDB findByUid(String uid);

  /**
   * @param repositoryId
   * @param commitId
   * @return List of software units by repository
   */
  public List<SoftwareUnitDB> findByRepository(int repositoryId, int commitId);

}