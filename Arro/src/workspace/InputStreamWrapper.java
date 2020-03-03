package workspace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import util.Logger;

public class InputStreamWrapper {
    private ZipInputStream zis;
    private ByteArrayInputStream baos = null;
    private LineNumberReader reader;
    private final boolean zip = false;

    public InputStreamWrapper(InputStream in) {
        if(zip) {
            zis = new ZipInputStream(in);
        }
        else {
            reader = new LineNumberReader(new InputStreamReader(in));
//            try {
//                bis = new BufferedInputStream(new FileInputStream("/Users/gerard/Documents/output-text.txt"));
//            } catch (FileNotFoundException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }

        }
        
    }

    public FileEntry getNextEntry() throws IOException {
        if(zip) {
            ZipEntry ne = zis.getNextEntry();
            if(ne == null) {
                return null;
            }
            else {
                return new FileEntry(ne);
            }
        }
        else {
            try {
                // Read block size, such as "########## 7 - META ##########"
                reader.readLine(); // skip
                String line = reader.readLine();
                if(line == null) {
                    return null;
                }
                int pos1 = line.indexOf(" ") + 1;
                int pos2 = line.indexOf(" ", pos1);
                int pos3 = pos2 + 3;
                int pos4 = line.indexOf(" ", pos3);
                String name = line.substring(pos3, pos4);
                int nrOfLines = Integer.parseInt(line.substring(pos1, pos2));
                
                String buf = "";
                while(nrOfLines > 0) {
                    buf += reader.readLine();
                    nrOfLines--;
                }
                Logger.out.trace(Logger.WS, buf);

                baos = new ByteArrayInputStream(buf.getBytes());

                return new FileEntry(name);
            } catch (IOException e) {
                return null;
            }
        }
    }

    public void close() throws IOException {
        if(zip) {
            zis.close();
        }
    }

    /**
     * Read all bytes from this FileEntry
     * 
     * @param b
     * @return
     * @throws IOException
     */
    public int read(byte b[]) throws IOException {
        if(zip) {
            return zis.read(b);
        }
        else {
            return baos.read(b);
        }
    }
}
