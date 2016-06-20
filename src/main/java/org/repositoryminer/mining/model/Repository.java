package org.repositoryminer.mining.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.repositoryminer.mining.RepositoryExplorer;
import org.repositoryminer.scm.ReferenceType;
import org.repositoryminer.scm.SCMType;

public class Repository {

	private String id;
	private String name;
	private String description;
	private String path;
	private SCMType scm;
	private List<Commit> commits;
	private List<Reference> branches;
	private List<Reference> tags;
	private List<Contributor> contributors;
	private Map<String, String> workingDirectory;
	private int currentCommit;
	private Reference currentReference;

	public void mine() throws IOException {
		RepositoryExplorer.mineRepository(this);
	}
	
	public Document getMetrics(String file, String commit) {
		return RepositoryExplorer.getMetricMeasures(file, commit);
	}
	
	public Document getCodeSmells(String file, String commit) {
		return RepositoryExplorer.getCodeSmellsMeasures(file, commit);
	}
	
	public Document getTechnicalDebtss(String file, String commit) {
		return RepositoryExplorer.getTechnicalDebtsMeasures(file, commit);
	}
	
	public Document getMeasures(String file, String commit) {
		return RepositoryExplorer.getAllMeasures(file, commit);
	}
	
	public void lastCommit() {
		currentCommit = commits.size() - 1;
		RepositoryExplorer.mineCurrentCommit(this);
	}

	public void firstCommit() {
		currentCommit = 0;
		RepositoryExplorer.mineCurrentCommit(this);
	}

	public boolean previousCommit() {
		if (currentCommit == 0) {
			return false;
		}
		currentCommit -= 1;
		RepositoryExplorer.mineCurrentCommit(this);
		return true;
	}

	public boolean nextCommit() {
		if (currentCommit == commits.size()-1) {
			return false;
		}
		currentCommit += 1;
		RepositoryExplorer.mineCurrentCommit(this);
		return true;
	}

	public void setCurrentReference(ReferenceType type, String name) {
		List<Reference> refs = null;
		switch (type) {
			case BRANCH: refs = branches; break;
			case TAG: refs = tags; break;
		}
		
		for (Reference r : refs) {
			if (r.getName().equals(name)) {
				setCurrentReference(r);;
				break;
			}
		}
	}
	
	public List<Commit> getCommitsFromAuthor(Contributor contributor) {
		List<Commit> commitsAux = new ArrayList<Commit>();
		for (Commit c : commits) {
			if (c.getAuthor().equals(contributor)) {
				commitsAux.add(c);
			}
		}
		return commitsAux;
	}
	
	public List<Commit> getCommitsFromCommitter(Contributor contributor) {
		List<Commit> commitsAux = new ArrayList<Commit>();
		for (Commit c : commits) {
			if (c.getCommitter().equals(contributor)) {
				commitsAux.add(c);
			}
		}
		return commitsAux;
	}

	public List<Contributor> getCommittersUntilCommit(Commit commit) {
		int index = commits.indexOf(commit);
		if (index == -1) {
			return new ArrayList<Contributor>();
		}
		
		Set<Contributor> contribsSet = new HashSet<Contributor>();
		for (int i = 0; i <= index; i++) {
			contribsSet.add(commits.get(i).getCommitter());
		}
		return new ArrayList<Contributor>(contribsSet);
	}
	
	public List<Contributor> getAuthorsUntilCommit(Commit commit) {
		int index = commits.indexOf(commit);
		if (index == -1) {
			return new ArrayList<Contributor>();
		}
		
		Set<Contributor> contribsSet = new HashSet<Contributor>();
		for (int i = 0; i <= index; i++) {
			contribsSet.add(commits.get(i).getAuthor());
		}
		return new ArrayList<Contributor>(contribsSet);
	}
	
	/**
	 * @param currentCommit
	 *            the currentCommit to set
	 */
	public boolean setCurrentCommit(Commit currentCommit) {
		if (commits != null) {
			int i = commits.indexOf(currentCommit);
			if (i >= 0) {
				this.currentCommit = i;
				return true;
			}
			return false;
		}
		return false;
	}

	/**
	 * @param currentReference
	 *            the currentReference to set
	 */
	public void setCurrentReference(Reference currentReference) {
		this.currentReference = currentReference;
		RepositoryExplorer.mineCurrentReference(this);
	}

	@SuppressWarnings("unchecked")
	public static Map<String, String> parseWorkingDirectory(Document doc) {
		Map<String, String> wd = new HashMap<String, String>();
		for (Document d : (List<Document>) doc.get("files")) {
			wd.put(d.getString("file"), d.getString("checkout"));
		}
		return wd;
	}

	@SuppressWarnings("unchecked")
	public static void initRepository(Document doc, Repository repository) {
		repository.id = doc.getString("_id");
		repository.name = doc.getString("name");
		repository.description = doc.getString("description");
		repository.path = doc.getString("path");
		repository.scm = SCMType.valueOf(doc.getString("scm"));
		repository.contributors = Contributor.parseDocuments((List<Document>) doc.get("contributors"));
	}

	public Repository(String path) {
		this.path = path;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description
	 *            the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path
	 *            the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @return the scm
	 */
	public SCMType getScm() {
		return scm;
	}

	/**
	 * @param scm
	 *            the scm to set
	 */
	public void setScm(SCMType scm) {
		this.scm = scm;
	}

	/**
	 * @return the commits
	 */
	public List<Commit> getCommits() {
		return commits;
	}

	/**
	 * @param commits
	 *            the commits to set
	 */
	public void setCommits(List<Commit> commits) {
		this.commits = commits;
	}

	/**
	 * @return the branches
	 */
	public List<Reference> getBranches() {
		return branches;
	}

	/**
	 * @param branches
	 *            the branches to set
	 */
	public void setBranches(List<Reference> branches) {
		this.branches = branches;
	}

	/**
	 * @return the tags
	 */
	public List<Reference> getTags() {
		return tags;
	}

	/**
	 * @param tags
	 *            the tags to set
	 */
	public void setTags(List<Reference> tags) {
		this.tags = tags;
	}

	/**
	 * @return the contributors
	 */
	public List<Contributor> getContributors() {
		return contributors;
	}

	/**
	 * @param contributors
	 *            the contributors to set
	 */
	public void setContributors(List<Contributor> contributors) {
		this.contributors = contributors;
	}

	/**
	 * @return the workingDirectory
	 */
	public Map<String, String> getWorkingDirectory() {
		return workingDirectory;
	}

	/**
	 * @param workingDirectory
	 *            the workingDirectory to set
	 */
	public void setWorkingDirectory(Map<String, String> workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	/**
	 * @return the currentCommit
	 */
	public Commit getCurrentCommit() {
		return commits.get(currentCommit);
	}

	/**
	 * @return the currentReference
	 */
	public Reference getCurrentReference() {
		return currentReference;
	}

}
