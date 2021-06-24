package gitlet;

import java.io.File;
import java.io.Serializable;

/** Blob class for Gitlet, the object to hold information of the files.
 *  @author Xiaoyi Zhu
 */
public class Blobs implements Serializable {

    /** Initializes a Blobs with the given file name.
     * @param name the fine name. */
    Blobs(String name) {
        File newFile = new File(name);
        _fileName = name;
        _content = Utils.readContents(newFile);
        _contentString = Utils.readContentsAsString(newFile);
        String temp = "";
        temp = _fileName + _contentString;
        _sha1 = Utils.sha1(temp);
    }

    /**Return content in String. */
    public String getContentString() {
        return _contentString;
    }

    /**Return content in byte[]. */
    public byte[] getContent() {
        return _content;
    }

    /** Return sha-1. */
    public String getsha() {
        return _sha1;
    }


    /** The String of the file name. */
    private String _fileName;
    /** The byte array to hold content. */
    private byte[] _content;
    /** The String to hold content. */
    private String _contentString;
    /** The String to hold hashcode of the whole blob. */
    private String _sha1;


}
