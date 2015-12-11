package br.edu.ufba.softvis.visminer.persistence.dao;

import java.util.Date;
import java.util.List;

import br.edu.ufba.softvis.visminer.model.database.CommitDB;

/**
 * Commit table DAO interface.
 */
public interface CommitDAO extends DAO<CommitDB, Integer> {

  /**
   * @param treeId
   * @return List of commits by tree.
   */
  public List<CommitDB> findByTree(int treeId);

  /**
   * @param repositoryPath
   * @return All commits from a given repository.
   */
  public List<CommitDB> findByRepository(String repositoryPath);

  /**
   * @param repoId
   * @return Commits that does not reference issues.
   */
  public List<CommitDB> findNotRefIssue(int repoId);

  /**
   * @param repositoryId
   * @param until
   * @return All ids from the first commit date until certain commit date.
   */
  public List<Integer> findIdsUntilDate(int repositoryId, Date until);

}