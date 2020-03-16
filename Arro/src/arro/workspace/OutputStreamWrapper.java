package arro.workspace;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;

public class OutputStreamWrapper {
    
    private ZipOutputStream zos;
    private OutputStream bos;
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private int nrOfLines = 0;
    private String name = "";
    private boolean zip = false;

    public OutputStreamWrapper(OutputStream out) {
        if(zip) {
            zos = new ZipOutputStream(out);
        }
        else {
            bos = out;
//            try {
//                bos = new BufferedOutputStream(new FileOutputStream("/Users/gerard/Documents/output-text.txt"));
//            } catch (FileNotFoundException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
        }        
    }

    /**
     * Give the subfile a name. As filename inside zip-file.
     * 
     * @param e
     * @throws IOException
     */
    public void putNextEntry(FileEntry e) throws IOException {
        if(zip) {
            zos.putNextEntry(e.getZipEntry());
        }
        else {
            if(!name.isEmpty()) {
                name = new String("\n########## " + nrOfLines + " - " + name + " ##########\n");
                
                bos.write(name.getBytes());
                bos.write(baos.toByteArray(), 0, baos.size());
                baos.reset();
                nrOfLines = 0;
            }
            
            name = e.getName();
        }
    }
    
    public synchronized void write(byte[] b, int off, int len)
        throws IOException
    {
        if(zip) {
            zos.write(b, off, len);
        }
        else {
            for(int i = 0; i < len; i++) {
                if(b[i] == '\n') {
                    nrOfLines++;
                }
            }
            baos.write(b, off, len);
        }
        
    }
    
    public void close() throws IOException {
        if(zip) {
            zos.close();
        }
        else {
            name = new String("\n########## " + nrOfLines + " - " + name + " ##########\n");
            
            bos.write(name.getBytes());
            bos.write(baos.toByteArray(), 0, baos.size());
            
            bos.close();
        }                
    }
}
