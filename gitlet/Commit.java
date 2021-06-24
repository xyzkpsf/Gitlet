package gitlet;

import java.io.File;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** Commit class for Gitlet, the object to perform commit.
 *  @author Xiaoyi Zhu
 */

public class Commit implements Serializable {

    /** Make a commit withe the given information.
     * @param message commit message.
     * @param parent first parent.
     * @param parent2 second parent.
     * @param stagingMap staging area.
     * @param init whether it is init.
     * @param isMerge whether it is from merge.
     * @param branchName branch name.
     * @param rmList remove list. */
    public Commit(String message, Commit parent, Commit parent2,
                  HashMap<String, Blobs> stagingMap, boolean init,
                  boolean isMerge, String branchName, List<String> rmList) {
        if (init) {
            _timeS = "Wed Dec 31 16:00:00 1969 -0800";
            _parentId = null;
            _allFiles = new HashMap<>();
        } else {
            _time = ZonedDateTime.now();
            _timeS = _time.format(DateTimeFormatter.ofPattern
                            ("EEE MMM d HH:mm:ss yyyy xxxx"));
            _parentId = parent.getSha();
            if (isMerge) {
                _parentId2 = parent2.getSha();
            }
            _allFiles = copyAllFiles(parent);
            update(_allFiles, stagingMap, rmList);
        }
        _message = message;
        _fromMerge = isMerge;
        _sha = getSha();
        _branchName = branchName;
    }

    /** init a Commit.
     * @return the new commit object. */
    static Commit init() {
        return new Commit("initial commit", null, null, null, true,
                false, "master", null);
    }

    /** Return a Blobs according to the file name.
     @param fileName file name.
     @return a Blobs if there is on, null if not. */
    public Blobs getBlobs(String fileName) {
        if (_allFiles.containsKey(fileName)) {
            return _allFiles.get(fileName);
        }
        return null;
    }

    /** Return the hash of this commit, form by the string of all files name
     * + time + parent + message.
     * @return The sha-1 String. */
    public String getSha() {
        String allfile = "";
        for (Map.Entry<String, Blobs> entry : _allFiles.entrySet()) {
            String key = entry.getKey();
            allfile += key;
        }
        if (_parentId == null) {
            return Utils.sha1(_timeS, _message, allfile);
        }
        String parentToString = _parentId;
        if (_fromMerge) {
            parentToString += _parentId2;
        }
        return Utils.sha1(_timeS, _message, parentToString, allfile);
    }

    /** Return the branch of this commit.
     * @return the branch name of this. */
    public String getBranchName() {
        return _branchName;
    }

    /** Return a list of all the files name in this commit.
     * @return a list of all the files name in this commit. */
    public LinkedList<String> getAllFilesList() {
        LinkedList<String> result = new LinkedList<String>();
        for (Map.Entry<String, Blobs> entry : _allFiles.entrySet()) {
            String key = entry.getKey();
            result.add(key);
        }
        return result;
    }

    /** Copy the _allFiles from parent.
     * @param parent the parent.
     * @return a hashmap from its (first) parent. */
    public HashMap<String, Blobs> copyAllFiles(Commit parent) {
        HashMap<String, Blobs> result = new HashMap<String, Blobs>();
        for (Map.Entry<String, Blobs> entry : parent._allFiles.entrySet()) {
            String key = entry.getKey();
            Blobs b = entry.getValue();
            result.put(key, b);
        }
        return result;
    }

    /** Update the map storing all the files with its names in this commit
     * from the staging list.
     * @param  filemap old file map from parent.
     * @param  stagingMap map from stage area.
     * @param removeList remove list. */
    public void update(HashMap<String, Blobs> filemap,
                       HashMap<String, Blobs> stagingMap,
                       List<String> removeList) {
        if (!stagingMap.isEmpty()) {
            for (Map.Entry<String, Blobs> entry : stagingMap.entrySet()) {
                String filename = entry.getKey();
                Blobs stagingB = entry.getValue();
                if (!filemap.containsKey(filename)) {
                    filemap.put(filename, stagingB);
                } else {
                    Blobs commitB = (Blobs) filemap.get(filename);
                    if (!commitB.getsha().equals(stagingB.getsha())) {
                        filemap.replace(filename, stagingB);
                    }
                }
                File toDelete = new File(".gitlet/staging/"
                        + stagingB.getsha());
                toDelete.delete();
            }
        }
        if (!removeList.isEmpty()) {
            while (!removeList.isEmpty()) {
                filemap.remove(removeList.remove(0));
            }
        }
    }

    /** Return commit's parent's sha-1.
     * @return parent's sha-1. */
    public String getParentId() {
        return _parentId;
    }

    /** Return commit's second parent's sha-1.
     * @return parent2 's sha-1*/
    public String getParentId2() {
        return _parentId2;
    }

    /** check whether this commit is tracking a file.
     * @param fileName the file to be checked.
     * @return a boolean indicates whether it is tracking. */
    public boolean tracking(String fileName) {
        return _allFiles.containsKey(fileName);
    }

    /** Return commit's time.
     * @return time. */
    public String getTime() {
        return _timeS;
    }

    /** Return commit's message.
     * @return  commit message. */
    public String getMessage() {
        return _message;
    }

    /** Return commit's hashmap of files.
     * @return the hashmap of all files. */
    public HashMap<String, Blobs> getFileMap() {
        return _allFiles;
    }

    /** Return both parent id if from merge.
     * @return  special string. */
    public String getMergeLogString() {
        return getParentId().substring(0, 7) + " "
                + getParentId2().substring(0, 7);
    }

    /** Return boolean about merge.
     * @return is from merge or not. */
    public boolean fromMerge() {
        return _fromMerge;
    }

    /** The time of this commit. */
    private ZonedDateTime _time;
    /** The String pf time of this commit. */
    private String _timeS;
    /** The parent(main) of this commit. */
    private String _parentId;
    /** The parent(secondary) of this commit(from merge).
     * it is null by default. */
    private String _parentId2;
    /** The boolean records whether this is from a merge. */
    private boolean _fromMerge;
    /** The String holds the sha-1 of this commit. */
    private String _sha;
    /** The String holds the commit message. */
    private String _message;
    /** The String holds the branch made started by this commit. */
    private String _branchName;
    /** The Hashmap stores all the file on this commit, saved by file name. */
    private HashMap<String, Blobs> _allFiles;

}

