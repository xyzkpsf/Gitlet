package gitlet;

import java.io.File;
import java.util.LinkedList;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.nio.file.Files;
import java.util.Map;
import java.nio.file.Paths;
import java.util.Queue;
import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayDeque;


/** The Repo class used to monitor the whole repo file, which initializes
 * and keeps tracking files.
 *  @author Xiaoyi Zhu
 */
public class Repo implements Serializable {

    /** Initialize a Repo, when type init in command. */
    public Repo() {
        try {
            if (!Files.exists(Paths.get(".gitlet"))) {
                Files.createDirectories(Paths.get(".gitlet"));
                Files.createDirectories(Paths.get(".gitlet/commits"));
                Files.createDirectories(Paths.get(".gitlet/staging"));
                Commit newCommit = Commit.init();
                _head = newCommit.getBranchName();
                _allCommit = new HashMap<>();
                _allBranch = new HashMap<>();
                _allStaging = new HashMap<>();
                _allUntrack = new LinkedList<>();
                _untrackedFile = new LinkedList<>();
                _modiList = new LinkedList<>();
                _allCommit.put(newCommit.getSha(), newCommit);
                _allBranch.put(newCommit.getBranchName(), newCommit.getSha());
                File newFile = new File(".gitlet/commits/"
                        + newCommit.getSha());
                Utils.writeObject(newFile, newCommit);
            }
        } catch (IOException e) {
            Utils.message("A Gitlet version-control system already exists "
                    + "in the current directory.");
            exit0();
        }
    }

    /** Check the all the files in repo first, get the list of untrack files.
     * Then check the fine to be added, operate according to the conditions.
     * @param fileName the name of file to be added. */
    public void add(String fileName) {
        File newFile = new File(fileName);
        if (!newFile.exists()) {
            Utils.message("File does not exist.");
            exit0();
        }
        Blobs b = new Blobs(fileName);
        Blobs bInCommit = getHead().getBlobs(fileName);
        if (bInCommit == null) {
            if (_allStaging.isEmpty() || !_allStaging.containsKey(fileName)) {
                _allStaging.put(fileName, b);
            } else {
                _allStaging.replace(fileName, b);
            }
            File blobsFile = new File(".gitlet/staging/" + b.getsha());
            Utils.writeObject(blobsFile, b);
        } else {
            if (bInCommit.getsha().equals(b.getsha())) {
                _allStaging.remove(fileName);
            } else {
                _allStaging.put(fileName, b);
                File blobsFile = new File(".gitlet/staging/" + b.getsha());
                Utils.writeObject(blobsFile, b);
            }
        }
        _allUntrack.remove(fileName);
    }

    /** make a new commit, update all the variables.
     * @param message message. */
    public void commit(String message) {
        if (message == null || message.equals("")) {
            Utils.message("Please enter a commit message.");
            exit0();
        }
        if (_allStaging.isEmpty() && _allUntrack.isEmpty()) {
            Utils.message("No changes added to the commit.");
            exit0();
        }
        Commit parent = getHead();
        Commit parent2 = getCommitFromId(_parentId2);
        Commit newCommit = new Commit(message, parent, parent2, _allStaging,
                false, _isMerge, _head, _allUntrack);
        _head = newCommit.getBranchName();
        _allStaging.clear();
        _allUntrack.clear();
        _allCommit.put(newCommit.getSha(), newCommit);
        String newCommitId = newCommit.getSha();
        _isMerge = false;
        _allBranch.replace(newCommit.getBranchName(), newCommitId);
        File commitFile = new File(".gitlet/commits/" + newCommitId);
        Utils.writeObject(commitFile, newCommit);
    }

    /** remove a file.
     * @param fileName the file to be rm. */
    public void rm(String fileName) {
        if (!_allStaging.containsKey(fileName)
                && !getHead().tracking(fileName)) {
            Utils.message("No reason to remove the file.");
            exit0();
        }
        File inStagFolder = null;
        File rmFile = new File(fileName);
        if (rmFile.exists()) {
            Blobs rmBlob = new Blobs(fileName);
            inStagFolder = new File(".gitlet/staging/" + rmBlob.getsha());
        }
        if (getHead().tracking(fileName)) {
            rmFile.delete();
            _allUntrack.add(fileName);
        }
        if (_allStaging.containsKey(fileName)) {
            _allStaging.remove(fileName);
            if (inStagFolder != null) {
                inStagFolder.delete();
            }
            updateList();
        }
    }

    /**Print the log.
     * @param head the head commit. */
    public void log(Commit head) {
        Commit curr = head;
        while (curr != null) {
            printLog(curr);
            curr = _allCommit.get(curr.getParentId());
        }
    }

    /** Print the global log. */
    public void goballog() {
        for (Map.Entry<String, Commit> entry : _allCommit.entrySet()) {
            Commit curr = entry.getValue();
            printLog(curr);
        }
    }

    /** Helper method to print log, avoiding repeat.
     * @param curr current commit. */
    public void printLog(Commit curr) {
        System.out.println("===");
        System.out.println("commit " + curr.getSha());
        if (curr.fromMerge()) {
            System.out.println("Merge: " + curr.getMergeLogString());
        }
        System.out.println("Date: " + curr.getTime());
        System.out.println(curr.getMessage());
        System.out.println();
    }

    /** Find a commit that has the given message.
     * @param message the message from a commit. */
    public void find(String message) {
        boolean found = false;
        for (Map.Entry<String, Commit> entry : _allCommit.entrySet()) {
            String key = entry.getKey();
            Commit com = entry.getValue();
            if (com.getMessage().equals(message)) {
                System.out.println(key);
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    /** Print the status. */
    public void status() {
        LinkedList<String> content = new LinkedList<>();
        for (Map.Entry<String, String> entry : _allBranch.entrySet()) {
            String key = entry.getKey();
            content.add(key);
        }
        printContent("Branches", content);
        content.clear();
        for (Map.Entry<String, Blobs> entry : _allStaging.entrySet()) {
            String key = entry.getKey();
            content.add(key);
        }
        printContent("Staged Files", content);
        content.clear();
        content.addAll(_allUntrack);
        printContent("Removed Files", content);
        updateList();
        printContent("Modifications Not Staged For Commit", _modiList);
        printContent("Untracked Files", _untrackedFile);
    }

    /** Helper for the status() that print the format.
     * @param  header the header.
     * @param  content list of the content. */
    public void printContent(String header, LinkedList<String> content) {
        System.out.println("=== " + header + " ===");
        if (content != null) {
            Collections.sort(content);
            for (String s : content) {
                if (s.equals(_head)) {
                    s = "*" + s;
                }
                System.out.println(s);
            }
        }
        System.out.println();
    }

    /**Create a new branch with the head commit, link with the branchName
     *  as the key.
     * @param branchName name of the new branch. */
    public void branch(String branchName) {
        if (_allBranch.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            exit0();
        }
        _allBranch.put(branchName, getHead().getSha());
    }

    /**Checkout the file from a given commit to current working directory.
     * @param fileName name of the file needed to checkout.
     * @param com The commit used to retrieve the file. */
    public void checkout1(String fileName, Commit com) {
        if (!com.getAllFilesList().contains(fileName)) {
            System.out.println("File does not exist in that commit.");
            exit0();
        }
        Blobs b = com.getBlobs(fileName);
        File f = new File(fileName);
        Utils.writeContents(f, (Object) b.getContent());
        _allStaging.remove(fileName);
    }

    /**Checkout the file from the given commit to current working directory.
     * @param commitID ID of the given commit.
     * @param fileName name of the new file need to change. */
    public void checkout2(String commitID, String fileName) {
        Commit com = getCommitFromId(commitID);
        if (com == null) {
            System.out.println("No commit with that id exists.");
            exit0();
        }
        checkout1(fileName, com);
    }

    /**Checkout to the given branch.
     * @param branch name of the given branch. */
    public void checkout3(String branch) {
        if (!_allBranch.containsKey(branch)) {
            System.out.println("No such branch exists.");
            exit0();
        }
        if (getHead().getSha().equals(_allBranch.get(branch))
                && branch.equals(_head)) {
            System.out.println("No need to checkout the current branch.");
            exit0();
        }
        Commit checkOutCommit = getCommit(_allBranch.get(branch));
        checkOutCommit(checkOutCommit);
        _head = branch;
    }

    /**Checkout by the given commit.
     * @param com the given commit. */
    public void checkOutCommit(Commit com) {
        LinkedList<String> oldList = getHead().getAllFilesList();
        LinkedList<String> newList = com.getAllFilesList();

        checkUntracked(null, com, oldList, newList);

        for (Object s : newList) {
            checkout1((String) s, com);
        }
        for (Object s : oldList) {
            if (!newList.contains(s)) {
                File f = new File((String) s);
                f.delete();
            }
        }
        _allStaging.clear();
    }

    /** Checkout by a commit id.
     * @param commitId the given commit. */
    public void reset(String commitId) {
        Commit givenCom = getCommitFromId(commitId);
        if (givenCom == null) {
            System.out.println("No commit with that id exists.");
            exit0();
        }
        checkOutCommit(givenCom);
        _allBranch.replace(_head, givenCom.getSha());
    }

    /** Delete a branch if possible.
     * @param branchName the branch to be deleted. */
    public void rmBranch(String branchName) {
        if (!_allBranch.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            exit0();
        }
        if (branchName.equals(_head)) {
            System.out.println("Cannot remove the current branch.");
            exit0();
        }
        _allBranch.remove(branchName);
    }

    /** This method would be called to search for (shortened) ID.
     * and return the matched commit if there is one.
     * @param id the shorten commit sha1 id.
     * @return the matched commit, null if not. */
    public Commit getCommitFromId(String id) {
        if (id != null) {
            File commitFile = new File(".gitlet/commits");
            File[] commitArray = commitFile.listFiles();
            if (commitArray != null && commitArray.length != 0) {
                for (File f : commitArray) {
                    if (f.getName().contains(id)) {
                        return _allCommit.get(f.getName());
                    }
                }
            }
        }
        return null;
    }

    /**Return the head commit.
     * @return return the head commit. */
    public Commit getHead() {
        return _allCommit.get(_allBranch.get(_head));
    }

    /** Return commit by its has-1.
     * @param has1 the has-1 code of the commit.
     * @return the commit with the according has code. */
    public Commit getCommit(String has1) {
        return _allCommit.get(has1);
    }

    /** Implement the merge command.
     * @param branchName the name of the given branch. */
    public void merge(String branchName) {
        mergeCheck(branchName);
        Commit branchCommit = getCommitFromId(_allBranch.get(branchName));
        Commit split = bfsFindSplit(branchCommit);
        HashMap<String, Blobs> splitFiles = split.getFileMap();
        LinkedList<String> hList =
                new LinkedList<String>(getHead().getAllFilesList());
        LinkedList<String> bList =
                new LinkedList<String>(branchCommit.getAllFilesList());
        checkUntracked(split, branchCommit, hList, bList);
        if (isParent(_allBranch.get(branchName), getHead())) {
            System.out.println("Given branch is an ancestor of "
                    + "the current branch.");
            exit0();
        }
        if (split.getSha().equals(getHead().getSha())) {
            reset(branchCommit.getSha());
            System.out.println("Current branch fast-forwarded.");
            exit0();
        }
        boolean conflict = mergeForLoop(splitFiles, hList, bList, branchCommit);
        if (!hList.isEmpty() && !bList.isEmpty()) {
            for (String fileName : hList) {
                if (bList.contains(fileName) && !getHead().getBlobs(fileName)
                        .getContentString().equals(branchCommit
                                .getBlobs(fileName).getContentString())) {
                    mergeConflict(fileName, getHead().getBlobs(fileName),
                            branchCommit.getBlobs(fileName));
                    conflict = true;
                    hList.remove(fileName);
                    bList.remove(fileName);
                }
            }
        }
        if (!bList.isEmpty()) {
            for (String fileName : bList) {
                if ((hList.isEmpty() || (!hList.contains(fileName)))
                        && !splitFiles.containsKey(fileName)) {
                    checkout1(fileName, branchCommit);
                    _allStaging.put(fileName, branchCommit.getBlobs(fileName));
                }
            }
        }
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
        _parentId2 = branchCommit.getSha();
        _isMerge = true;
        commit("Merged " + branchName + " into "
                + getHead().getBranchName() + ".");
    }

    /** A big for loop used to implement merge.
     * @param splitFiles fileMap from split point.
     * @param hList file list from head.
     * @param bList file list from branch.
     * @param branchCommit branch commit.
     * @return boolean whether there is a merge conflict. */
    public boolean mergeForLoop(HashMap<String, Blobs> splitFiles,
                                LinkedList<String> hList,
                                LinkedList<String> bList, Commit branchCommit) {
        boolean conflict = false;
        for (Map.Entry<String, Blobs> entry : splitFiles.entrySet()) {
            String fileName = entry.getKey();
            Blobs bSplit = entry.getValue();
            String sContent = bSplit.getContentString();
            if (hList.contains(fileName)) {
                Blobs hBlob = getHead().getBlobs(fileName);
                String hContent = hBlob.getContentString();
                if (bList.contains(fileName)) {
                    Blobs bBlob = branchCommit.getBlobs(fileName);
                    String bContent = bBlob.getContentString();
                    if (sContent.equals(hContent)
                            && !sContent.equals(bContent)) {
                        checkout1(fileName, branchCommit);
                        add(fileName);
                    }
                    if (!sContent.equals(hContent) && !sContent.equals(bContent)
                            && !hContent.equals(bContent)) {
                        mergeConflict(fileName, hBlob, bBlob);
                        conflict = true;
                    }
                    bList.remove(fileName);
                } else {
                    if (sContent.equals(hContent)) {
                        rm(fileName);
                    } else {
                        mergeConflict(fileName, hBlob, null);
                        conflict = true;
                    }
                }
                hList.remove(fileName);
            } else {
                if (bList.contains(fileName)
                        && !branchCommit.getBlobs(fileName).getContentString()
                        .equals(sContent)) {
                    mergeConflict(fileName, null,
                            branchCommit.getBlobs(fileName));
                    conflict = true;
                    bList.remove(fileName);
                }
            }
        }
        return conflict;
    }

    /** Helper to check failure case of merge.
     * @param branchName branch name. */
    public void mergeCheck(String branchName) {
        if (_head.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            exit0();
        }
        if (!_allStaging.isEmpty() || !_allUntrack.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            exit0();
        }
        if (!_allBranch.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            exit0();
        }
    }

    /** Helper to find and return a split point.
     * @param branchCommit branch commit.
     * @return the split point. */
    public Commit getSplit(Commit branchCommit) {
        HashMap<Integer, LinkedList<String>> parentMap = new HashMap<>();
        getParentMap(1, getHead(), parentMap);
        int minDis = Integer.MAX_VALUE;
        String result = "";
        for (Map.Entry<Integer, LinkedList<String>> entry
                : parentMap.entrySet()) {
            int dis = entry.getKey();
            LinkedList<String> l = entry.getValue();
            if (!l.isEmpty()) {
                for (String s : l) {
                    if (isParent(s, branchCommit) && dis < minDis) {
                        minDis = dis;
                        result = s;
                    }
                }
            }
        }
        return getCommitFromId(result);
    }

    /** A recursive Helper to get a map of parent Id and distance from head.
     * @param dis The distance from head.
     * @param curr The current Commit to find parent.
     * @param map the map to store the possible parents with same distance. */
    public void getParentMap(int dis, Commit curr, HashMap<Integer,
            LinkedList<String>> map) {
        if (curr.getParentId() != null) {
            if (!map.containsKey(dis)) {
                LinkedList<String> l = new LinkedList<>();
                l.add(curr.getParentId());
                map.put(dis, l);
            }
            map.get(dis).add(curr.getParentId());
            getParentMap(dis + 1, getCommitFromId(curr.getParentId()), map);
        }
        if (curr.getParentId2() != null) {
            if (!map.containsKey(dis)) {
                LinkedList<String> l = new LinkedList<>();
                l.add(curr.getParentId2());
                map.put(dis, l);
            }
            map.get(dis).add(curr.getParentId2());
            getParentMap(dis + 1, getCommitFromId(curr.getParentId2()), map);
        }
    }


    /** Helper to find and return a split point.
     * @param branchCommit branch commit.
     * @return the split point. */
    public Commit getSplitPoint(Commit branchCommit) {
        ArrayList<String> l = findParent();
        while (!l.isEmpty()) {
            if (isParent(l.get(0), branchCommit)) {
                return getCommitFromId(l.get(0));
            }
            l.remove(0);
        }
        return null;
    }

    /** Use BSF idea to find and return a split point.
     * @param branchCommit branch commit.
     * @return the split point if there is one. */
    public Commit bfsFindSplit(Commit branchCommit) {
        Queue<String> q = new ArrayDeque<>();
        LinkedList<String> l = new LinkedList<>();
        q.add(getHead().getSha());
        while (!q.isEmpty()) {
            String thisQ = q.poll();
            l.add(thisQ);
            Commit curr = getCommitFromId(thisQ);
            String parent1 = curr.getParentId();
            String parent2 = curr.getParentId2();
            if (parent1 != null) {
                q.add(parent1);
            }
            if (parent2 != null) {
                q.add(parent2);
            }
        }
        while (!l.isEmpty()) {
            String currId = l.remove(0);
            if (isParent(currId, branchCommit)) {
                return getCommitFromId(currId);
            }
        }
        return null;
    }



    /** Find all the parent according to distance.
     * @return a list with parent. */
    public ArrayList<String> findParent() {
        ArrayList<String> parentList = new ArrayList<>();
        Commit curr = getHead();
        findParentHelper(curr, parentList);
        return parentList;
    }

    /** Find all the parent according to distance.
     * @param com the current commit.
     * @param l the result list. */
    public void findParentHelper(Commit com, ArrayList<String> l) {
        String pId1 = com.getParentId();
        String pId2 = com.getParentId2();
        if (pId1 != null) {
            l.add(pId1);
            findParentHelper(getCommitFromId(pId1), l);
        }
        if (pId2 != null) {
            l.add(pId2);
            findParentHelper(getCommitFromId(pId2), l);
        }
    }

    /** Helper to check whether a commit is a parent of another commit.
     * @param pId The parent's ID to be checked.
     * @param child the child commit to be checked.
     * @return true if is. */
    public boolean isParent(String pId, Commit child) {
        Commit curr = child;
        while (curr != null) {
            String p1 = curr.getParentId();
            String p2 = curr.getParentId2();
            if (p1 != null) {
                if (p1.equals(pId)) {
                    return true;
                }
            }
            if (p2 != null) {
                if (p2.equals(pId)) {
                    return true;
                }
            }
            curr = _allCommit.get(curr.getParentId());
        }
        return false;
    }


    /** Handle merge conflict.
     * @param fileName name of the file to deal with.
     * @param bHead the blob of the file from head.
     * @param bBranch the blob of the file from given branch. */
    public void mergeConflict(String fileName, Blobs bHead, Blobs bBranch) {
        String content = "<<<<<<< HEAD\n";
        if (bHead != null) {
            content += bHead.getContentString();
        }
        content += "=======\n";
        if (bBranch != null) {
            content += bBranch.getContentString();
        }
        content += ">>>>>>>\n";
        File f = new File(fileName);
        Utils.writeContents(f, content);
        add(fileName);
    }

    /** Check for untracked files before reset, checkout and merge.
     * @param splitPoint only exists when merge, otherwise null.
     * @param branchCommit the given commit.
     * @param hList the file list from head.
     * @param bList the file list from the given branch. */
    public void checkUntracked(Commit splitPoint, Commit branchCommit,
                               LinkedList<String> hList,
                               LinkedList<String> bList) {
        for (String fileName : bList) {
            if (!hList.contains(fileName)) {
                File newFile = new File(fileName);
                if (newFile.exists()) {
                    String content = Utils.readContentsAsString(newFile);
                    String contentInBranch = branchCommit.getBlobs(fileName)
                            .getContentString();
                    if (!content.equals(contentInBranch)) {
                        System.out.println("There is an untracked file in the"
                                + " way; delete it or add it first.");
                        exit0();
                    }
                }
            }
        }
    }

    /** update untraked file when needed. */
    public void updateList() {
        String dir = System.getProperty("user.dir");
        File file = new File(dir);
        File[] allFileList = file.listFiles(File::isFile);
        LinkedList<String> fileList = new LinkedList<>();
        _untrackedFile.clear();
        _modiList.clear();
        for (File f : allFileList) {
            String fileName = f.getName();
            fileList.add(fileName);
            if (f.isFile() && !f.isHidden() && !getHead().tracking(fileName)
                    && !_allStaging.containsKey(fileName)) {
                _untrackedFile.add(fileName);
            }
            String content = Utils.readContentsAsString(f);
            if (getHead().tracking(fileName)
                    && !_allStaging.containsKey(fileName)) {
                if (!getHead().getBlobs(fileName).getContentString()
                        .equals(content)) {
                    String s = fileName + " (modified)";
                    _modiList.add(s);
                }
            }
            if (_allStaging.containsKey(fileName)) {
                if (!_allStaging.get(fileName).getContentString()
                        .equals(content)) {
                    String s = fileName + " (modified)";
                    _modiList.add(s);
                }
            }
        }
        for (Map.Entry<String, Blobs> entry : _allStaging.entrySet()) {
            String fileName = entry.getKey();
            if (!fileList.contains(fileName)
                    && !_allUntrack.contains(fileName)) {
                String s = fileName + " (deleted)";
                _modiList.add(s);
            }
        }
        LinkedList<String> trackedList = getHead().getAllFilesList();
        for (String fileName : trackedList) {
            if (!fileList.contains(fileName)
                    && !_allUntrack.contains(fileName)) {
                String s = fileName + " (deleted)";
                _modiList.add(s);
            }
        }

    }

    /** Exit with 0, avoid typing. */
    public void exit0() {
        System.exit(0);
    }

    /** String storing the branch name of the Head. */
    private String _head;
    /** Second parentId of commit if merge. */
    private String _parentId2;
    /** Flag to indicate a merge. */
    private boolean _isMerge;
    /** hash-map stores all the commits with its sha-1 code. */
    private HashMap<String, Commit> _allCommit;
    /** hash-map stores all the branch's head with its sha-1 code. */
    private HashMap<String, String> _allBranch;
    /** hash-map stores all the files in staged with its file name. */
    private HashMap<String, Blobs> _allStaging;
    /** List contains all the untrack files. */
    private LinkedList<String> _allUntrack;
    /** List contains all the untrack files slightly differenct from the one
     * above. */
    private LinkedList<String> _untrackedFile;
    /** List contains all the files that modified not staged. */
    private LinkedList<String> _modiList;

}

