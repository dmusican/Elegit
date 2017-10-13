package elegit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import elegit.exceptions.MissingRepoException;
import elegit.exceptions.TagNameExistsException;
import elegit.models.CommitHelper;
import elegit.models.TagHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidTagNameException;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevTag;

/**
 * Model that holds all the tags for a repoHelper object
 */
public class TagModel {
    private RepoHelper repoHelper;
    private List<TagHelper> upToDateTags;
    private List<TagHelper> unpushedTags;
    private List<String> tagsWithUnpushedCommits;
    private Map<String, TagHelper> tagIdMap;

    static final Logger logger = LogManager.getLogger();

    /**
     * Initializes internal lists
     *
     * @param repoHelper the repoHelper this tag model holds tags for
     * @throws GitAPIException
     * @throws IOException
     */
    public TagModel(RepoHelper repoHelper) throws GitAPIException, IOException {
        this.repoHelper = repoHelper;
        unpushedTags = new ArrayList<>();
        tagsWithUnpushedCommits = new ArrayList<>();
        tagIdMap = new HashMap<>();
        upToDateTags = this.getAllLocalTags();
    }

    /**
     * Looks through all the tags and checks that they are added to commit helpers
     *
     * @return true if there were changes, false if not
     * @throws IOException
     * @throws GitAPIException
     */
    public boolean updateTags() throws IOException, GitAPIException {
        List<String> oldTagNames = getAllTagNames();
        Map<String, Ref> tagMap = this.repoHelper.getRepo().getTags();
        int oldSize = oldTagNames.size();
        for (String s : tagMap.keySet()) {
            if (oldTagNames.contains(s)) {
                //Check if the tag is annotated or not, find the commit name accordingly
                String commitName;
                if (tagMap.get(s).getPeeledObjectId() != null)
                    commitName = tagMap.get(s).getPeeledObjectId().getName();
                else commitName = tagMap.get(s).getObjectId().getName();
                // Re add the tag if it isn't there

                if (!this.repoHelper.getCommit(commitName).hasTag(s)) {
                    this.repoHelper.getCommit(commitName).addTag(this.tagIdMap.get(s));
                }

                oldTagNames.remove(s);
                if (tagsWithUnpushedCommits.contains(s)) {
                    tagsWithUnpushedCommits.remove(s);
                }
            } else {
                Ref r = tagMap.get(s);
                makeTagHelper(r, s);
            }
        }
        if (oldTagNames.size() > 0) { //There are tags that were deleted, so we remove them
            for (String s : oldTagNames) {
                this.repoHelper.getCommit(this.tagIdMap.get(s).getCommitId()).removeTag(s);
                this.unpushedTags.remove(s);
                this.upToDateTags.remove(tagIdMap.get(s));
                tagsWithUnpushedCommits.remove(s);
                this.tagIdMap.remove(s);
            }
        }
        return !(oldSize == getAllTagNames().size() && oldTagNames.size() == 0);
    }

    /**
     * Tags a commit
     *
     * @param tagName the name for the tag.
     * @param commitName the id of the commit to apply this tag to
     * @throws GitAPIException if the 'git tag' call fails.
     */
    public void tag(String tagName, String commitName) throws GitAPIException, MissingRepoException,
            IOException, TagNameExistsException, InvalidTagNameException {
        logger.info("Attempting tag");
        if (!repoHelper.exists()) throw new MissingRepoException();
        Git git = new Git(this.repoHelper.getRepo());
        // This creates a lightweight tag
        // TODO: add support for annotated tags?
        CommitHelper c = repoHelper.getCommit(commitName);
        if (c.hasTag(tagName))
            throw new TagNameExistsException();
        Ref r = git.tag().setName(tagName).setObjectId(c.getCommit()).setAnnotated(false).call();
        git.close();
        TagHelper t = makeTagHelper(r, tagName);
        this.unpushedTags.add(t);
    }

    /**
     * Helper method to make a tagHelper given a ref and a name of the tag. Also adds the
     * tag helper to the tagIdMap
     *
     * @param r       the ref to make a tagHelper for. This can be a peeled or unpeeled tag
     * @param tagName the name of the tag
     * @return a tagHelper object with the information stored
     * @throws IOException
     * @throws GitAPIException
     */
    private TagHelper makeTagHelper(Ref r, String tagName) throws IOException, GitAPIException {
        String commitName;
        boolean isAnnotated = false;

        //Check if the tag is annotated or not, find the commit name accordingly
        if (r.getPeeledObjectId() != null) {
            commitName = r.getPeeledObjectId().getName();
            isAnnotated = true;
        } else commitName = r.getObjectId().getName();

        // Find the commit helper associated with the commit name
        CommitHelper c = this.repoHelper.getCommit(commitName);
        TagHelper t;

        // If the commit that this tag points to isn't in the commitIdMap,
        // then that commit has not yet been pushed, so warn the user
        if (c == null) {
            this.tagsWithUnpushedCommits.add(tagName);
            return null;
        } else if (this.tagsWithUnpushedCommits.contains(tagName)) {
            this.tagsWithUnpushedCommits.remove(tagName);
        }

        // If it's not an annotated tag, we make a lightweight tag helper
        if (!isAnnotated) {
            t = new TagHelper(tagName, c);
            c.addTag(t);
        }
        // Otherwise, the tag has a message and all the stuff a commit has
        else {
            ObjectReader objectReader = repoHelper.getRepo().newObjectReader();
            ObjectLoader objectLoader = objectReader.open(r.getObjectId());
            RevTag tag = RevTag.parse(objectLoader.getBytes());
            objectReader.close();
            t = new TagHelper(tag, c);
            c.addTag(t);
        }
        if (!tagIdMap.containsKey(tagName)) {
            tagIdMap.put(tagName, t);
        }
        return t;
    }

    /**
     * Deletes a given tag
     *
     * @param tagName the name of the tag to delete
     * @throws MissingRepoException
     * @throws GitAPIException
     */
    public void deleteTag(String tagName) throws MissingRepoException, GitAPIException {
        TagHelper tagToRemove = tagIdMap.get(tagName);

        if (!repoHelper.exists()) throw new MissingRepoException();
        // should this Git instance be class-level?
        Git git = new Git(this.repoHelper.getRepo());
        // git tag -d
        git.tagDelete().setTags(tagToRemove.getRefName()).call();
        git.close();

        tagToRemove.getCommit().removeTag(tagName);
        this.upToDateTags.remove(tagToRemove);
        this.tagIdMap.remove(tagName);
    }

    /* ************************ GETTERS ************************ */
    public TagHelper getTag(String tagName) { return tagIdMap.get(tagName); }

    public List<String> getAllTagNames() { return new ArrayList<>(tagIdMap.keySet()); }

    /**
     * Constructs a list of all local tags found by parsing the tag refs from the repo
     * then wrapping them into a TagHelper with the appropriate commit
     *
     * @return a list of TagHelpers for all the tags
     * @throws IOException
     * @throws GitAPIException
     */
    public List<TagHelper> getAllLocalTags() throws IOException, GitAPIException {
        Map<String, Ref> tagMap = this.repoHelper.getRepo().getTags();
        List<TagHelper> tags = new ArrayList<>();
        for (String s : tagMap.keySet()) {
            Ref r = tagMap.get(s);
            tags.add(makeTagHelper(r, s));
        }
        return tags;
    }

    public List<TagHelper> getAllTags() {
        return new ArrayList<>(tagIdMap.values());
    }

    /**
     * Returns a map of commits and associated tags
     * @return a map with commits as they key and a list of tag helpers associated with it
     */
    public Map<CommitHelper, List<TagHelper>> getTagCommitMap(){
        Map<CommitHelper, List<TagHelper>> commitTagMap = new HashMap<>();

        List<TagHelper> tags = this.getAllTags();

        for(TagHelper tag : tags){
            CommitHelper head = tag.getCommit();
            if(commitTagMap.containsKey(head)){
                commitTagMap.get(head).add(tag);
            }else{
                commitTagMap.put(head, Stream.of(tag).collect(Collectors.toList()));
            }
        }
        return commitTagMap;
    }
}
