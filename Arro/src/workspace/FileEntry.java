package workspace;

import java.util.zip.ZipEntry;

public class FileEntry {

    private ZipEntry ze;
    
    public FileEntry(String key) {
        ze = new ZipEntry(key);
    }

    public FileEntry(ZipEntry nextEntry) {
        ze = nextEntry;
    }

    public String getName() {
        return ze.getName();
    }
    
    public ZipEntry getZipEntry() {
        return ze;
    }
}
