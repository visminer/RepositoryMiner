package org.repositoryminer.scm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.repositoryminer.exceptions.ErrorMessage;
import org.repositoryminer.exceptions.VisMinerAPIException;
import org.repositoryminer.model.Commit;
import org.repositoryminer.model.Contributor;
import org.repositoryminer.model.Diff;
import org.repositoryminer.model.DiffType;
import org.repositoryminer.model.Reference;
import org.repositoryminer.model.ReferenceType;
import org.repositoryminer.utility.HashHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see SCM This class supports git repositories.
 */
public class GitSCM implements SCM {

	private static final String RM_BRANCH = "RM-c1b31321c0";
	private static final Logger LOGGER = LoggerFactory.getLogger(GitSCM.class);

	private Repository repository;
	private Git git;
	private RevWalk revWalk;
	private TreeWalk treeWalk;
	private DiffFormatter diffFormatter;
	private String absolutePath;
	private String repoId;

	@Override
	public void open(String repositoryPath, int binaryThreshold) {
		FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
		try {
			repository = repositoryBuilder.setGitDir(new java.io.File(repositoryPath)).readEnvironment().findGitDir()
					.build();
		} catch (IOException e) {
			throw new VisMinerAPIException(ErrorMessage.GIT_REPOSITORY_IOERROR.toString(), e);
		}

		git = new Git(repository);
		revWalk = new RevWalk(repository);
		treeWalk = new TreeWalk(repository);

		diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
		diffFormatter.setRepository(repository);
		diffFormatter.setContext(0);
		diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
		diffFormatter.setDetectRenames(true);

		if (binaryThreshold != 0)
			diffFormatter.setBinaryFileThreshold(binaryThreshold);

		absolutePath = repository.getWorkTree().getAbsolutePath().replace("\\", "/");
		repoId = HashHandler.SHA1(absolutePath);
	}

	@Override
	public String getAbsolutePath() {
		return absolutePath;
	}

	@Override
	public List<Reference> getReferences() {
		List<Reference> refs = new ArrayList<Reference>();

		Iterable<Ref> branches = null;
		try {
			branches = git.branchList().call();
		} catch (GitAPIException e) {
			errorHandler(ErrorMessage.GIT_BRANCH_LIST_ERROR.toString(), e);
		}

		for (Ref b : branches) {

			if (b.getName().equals("HEAD"))
				continue;

			int i = b.getName().lastIndexOf("/") + 1;
			String id = HashHandler.SHA1(absolutePath + "/" + b.getName());
			Reference r = new Reference(id, repoId, b.getName().substring(i), b.getName(), ReferenceType.BRANCH);
			refs.add(r);

		}

		Iterable<Ref> tags = null;
		try {
			tags = git.tagList().call();
		} catch (GitAPIException e) {
			errorHandler(ErrorMessage.GIT_TAG_LIST_ERROR.toString(), e);
		}

		for (Ref t : tags) {

			int i = t.getName().lastIndexOf("/") + 1;
			String id = HashHandler.SHA1(absolutePath + "/" + t.getName());
			Reference r = new Reference(id, repoId, t.getName().substring(i), t.getName(), ReferenceType.TAG);
			refs.add(r);

		}

		return refs;
	}

	@Override
	public List<Commit> getCommits(int skip, int maxCount) {
		Iterable<RevCommit> revCommits = null;
		try {
			if (maxCount != 0)
				revCommits = git.log().all().setSkip(skip).setMaxCount(maxCount).call();
			else
				revCommits = git.log().all().call();
		} catch (GitAPIException | IOException e) {
			errorHandler(ErrorMessage.GIT_LOG_COMMIT_ERROR.toString(), e);
		}

		List<Commit> commits = new ArrayList<Commit>();

		for (RevCommit revCommit : revCommits) {

			PersonIdent author = revCommit.getAuthorIdent();
			PersonIdent committer = revCommit.getCommitterIdent();

			Contributor myAuthor = new Contributor(author.getName(), author.getEmailAddress());

			Contributor myCommitter = new Contributor(committer.getName(), committer.getEmailAddress());

			List<String> parents = new ArrayList<String>();
			for (RevCommit parent : revCommit.getParents())
				parents.add(parent.getName());

			List<Diff> changes = null;
			try {
				changes = getCommitedFiles(revCommit.getName());
			} catch (IOException e) {
				errorHandler(ErrorMessage.GIT_RETRIEVE_CHANGES_ERROR.toString(), e);
			}

			Commit c = new Commit(revCommit.getName(), revCommit.getFullMessage(), author.getWhen(),
					committer.getWhen(), repoId, parents, myAuthor, myCommitter, changes);
			commits.add(c);

		}
		return commits;
	}

	@Override
	public List<String> getReferenceCommits(String fullName, ReferenceType type) {
		if (type == ReferenceType.BRANCH)
			return getCommitsFromBranch(fullName);
		else
			return getCommitsFromTag(fullName);
	}

	@Override
	public byte[] getData(String commit, String filePath) {
		revWalk.reset();
		ObjectReader reader = repository.newObjectReader();
		try {

			RevCommit revCommit = revWalk.parseCommit(ObjectId.fromString(commit));
			RevTree tree = revCommit.getTree();
			TreeWalk treeWalk = TreeWalk.forPath(reader, filePath, tree);

			ObjectId objId = null;
			byte[] bytes = null;

			if (treeWalk != null && (objId = treeWalk.getObjectId(0)) != null) {
				bytes = reader.open(objId).getBytes();
			}

			reader.close();
			return bytes;

		} catch (IOException | LargeObjectException e) {
			errorHandler(ErrorMessage.GIT_RETRIEVE_DATA_ERROR.toString(), e);
			return null;
		}
	}

	@Override
	public void checkout(String hash) {
		makeCheckout("master", false);
		makeCheckout(hash, true);
	}

	@Override
	public void close() {
		diffFormatter.close();
		treeWalk.close();
		revWalk.close();
		git.close();
		repository.close();
	}

	@Override
	public void reset() {
		try {

			if (!git.status().call().isClean())
				git.reset().setMode(ResetType.HARD).call();

			makeCheckout("master", false);
			git.branchDelete().setBranchNames(RM_BRANCH).setForce(true).call();

		} catch (NoWorkTreeException | GitAPIException e) {
			errorHandler(ErrorMessage.GIT_RESET_ERROR.toString(), e);
		}
	}

	private void errorHandler(String errorMessage, Throwable e) {
		close();
		LOGGER.error(errorMessage, e);
		throw new VisMinerAPIException(errorMessage, e);
	}

	private List<Diff> getCommitedFiles(String commit) throws IOException {
		RevCommit revCommit = revWalk.parseCommit(ObjectId.fromString(commit));
		AnyObjectId currentCommit = repository.resolve(commit);
		AnyObjectId oldCommit = revCommit.getParentCount() > 0 ? repository.resolve(revCommit.getParent(0).getName())
				: null;

		List<DiffEntry> diffs = null;
		diffs = diffFormatter.scan(oldCommit, currentCommit);

		List<Diff> changes = new ArrayList<Diff>();
		for (DiffEntry entry : diffs) {

			RevCommit parentCommit = oldCommit == null ? null
					: revWalk.parseCommit(ObjectId.fromString(oldCommit.getName()));

			String path = null; // file path of the current commit
			String oldPath = null; // file path of the previous commit

			DiffType type = null;

			switch (entry.getChangeType()) {

			case ADD:
				path = absolutePath + "/" + entry.getNewPath();
				type = DiffType.ADD;
				break;

			case COPY:
				path = absolutePath + "/" + entry.getNewPath();
				oldPath = absolutePath + "/" + entry.getOldPath();
				type = DiffType.COPY;
				break;

			case DELETE:
				oldPath = absolutePath + "/" + entry.getOldPath();
				type = DiffType.DELETE;
				break;

			case MODIFY:
				path = absolutePath + "/" + entry.getNewPath();
				type = DiffType.MODIFY;
				break;

			case RENAME:
				path = absolutePath + "/" + entry.getNewPath();
				oldPath = absolutePath + "/" + entry.getOldPath();
				type = DiffType.RENAME;
				break;

			}

			int[] lines = { 0, 0 };
			if (type == DiffType.ADD || type == DiffType.MODIFY) {
				lines = getLinesAddedAndDeleted(path, parentCommit, revCommit);
			}

			Diff change = new Diff(path, oldPath, path!=null ? HashHandler.SHA1(path) : path, lines[0], lines[1], type);
			changes.add(change);
		}
		return changes;
	}

	private int[] getLinesAddedAndDeleted(String path, RevCommit oldCommit, RevCommit currentCommit)
			throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		DiffFormatter formatter = new DiffFormatter(output);
		formatter.setRepository(repository);
		formatter.setContext(0);

		formatter.setPathFilter(PathFilter.create(path));
		formatter.format(oldCommit, currentCommit);

		Scanner scanner = new Scanner(output.toString());
		int added = 0, removed = 0;

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (line.startsWith("+") && !line.startsWith("+++"))
				added++;
			else if (line.startsWith("-") && !line.startsWith("---"))
				removed++;
		}

		output.close();
		formatter.close();
		scanner.close();

		int[] vet = { added, removed };
		return vet;
	}

	private void makeCheckout(String hash, boolean create) {
		try {
			if (create)
				Runtime.getRuntime().exec("git checkout -f -b " + RM_BRANCH + " " + hash, null,
						new java.io.File(absolutePath));
			else
				Runtime.getRuntime().exec("git checkout -f " + hash, null, new java.io.File(absolutePath));
		} catch (IOException e) {
			errorHandler(ErrorMessage.GIT_CHECKOUT_ERROR.toString(), e);
		}
	}

	private List<String> getCommitsFromTag(String refName) {
		try {
			List<Ref> call = git.tagList().call();
			List<String> commits = new ArrayList<String>();

			for (Ref ref : call) {
				if (ref.getName().equals(refName)) {
					LogCommand log = git.log();
					Ref peeledRef = repository.peel(ref);

					if (peeledRef.getPeeledObjectId() != null) {
						log.add(peeledRef.getPeeledObjectId());
					} else {
						log.add(ref.getObjectId());
					}

					for (RevCommit c : log.call())
						commits.add(c.getName());
					break;
				}
			}
			return commits;
		} catch (GitAPIException | IncorrectObjectTypeException | MissingObjectException e) {
			errorHandler(ErrorMessage.GIT_BRANCH_COMMITS_ERROR.toString(), e);
			return null;
		}
	}

	private List<String> getCommitsFromBranch(String refName) {
		Iterable<RevCommit> revCommits = null;
		try {
			revCommits = git.log().add(repository.resolve(refName)).call();
		} catch (RevisionSyntaxException | GitAPIException | IOException e) {
			errorHandler(ErrorMessage.GIT_TAG_COMMITS_ERROR.toString(), e);
		}

		List<String> commits = new ArrayList<String>();
		for (RevCommit c : revCommits)
			commits.add(c.getName());
		return commits;
	}

}