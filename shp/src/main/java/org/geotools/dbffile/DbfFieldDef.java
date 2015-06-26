package org.geotools.dbffile;

import java.io.IOException;
import java.nio.charset.Charset;

import com.vividsolutions.jump.io.EndianDataInputStream;

/**
 * class to hold infomation about the fields in the file
 */
public class DbfFieldDef implements DbfConsts {
    static final boolean DEBUG = false;
    public StringBuffer fieldname = new StringBuffer(DBF_NAMELEN);
    public char fieldtype;
    public int fieldstart;
    public int fieldlen;
    public int fieldnumdec;

    public DbfFieldDef() { /* do nothing */
    }

    public DbfFieldDef(String fieldname, char fieldtype, int fieldlen, int fieldnumdec) {
        this.fieldname = new StringBuffer(fieldname);
        this.fieldname.setLength(DBF_NAMELEN);
        this.fieldtype = fieldtype;
        this.fieldlen = fieldlen;
        this.fieldnumdec = fieldnumdec;
    }

    public String toString() {
        return new String("" + fieldname + " " + fieldtype + " " + fieldlen + "." + fieldnumdec);
    }

    // [Matthias Scholz 04.Sept.2010] Charset changes
    /**
     * Sets up the Dbf field definition. For compatibilty reasons, this method
     * is is now a wrapper for the changed/new one with Charset functions.
     *
     * @see #setup(int pos, EndianDataInputStream dFile, Charset charset)
     * 
     * @param pos
     * @param dFile
     * @throws IOException
     */
    public void setup(int pos, EndianDataInputStream dFile) throws IOException {
        setup(pos, dFile, Charset.defaultCharset());
    }

    /**
     * Sets up the Dbf field definition with a specified Charset for the
     * fieldnames.
     *
     * @param pos
     * @param dFile
     * @param charset
     * @throws IOException
     */
    public void setup(int pos, EndianDataInputStream dFile, Charset charset) throws IOException {

        // two byte character modification thanks to Hisaji ONO
        byte[] strbuf = new byte[DBF_NAMELEN]; // <---- byte array buffer for
                                               // storing string's byte data
        int j = -1;
        int term = -1;
        for (int i = 0; i < DBF_NAMELEN; i++) {
            byte b = dFile.readByteLE();
            if (b == 0) {
                if (term == -1)
                    term = j;
                continue;
            }
            j++;
            strbuf[j] = b; // <---- read string's byte data
        }
        if (term == -1)
            term = j;
        String name = new String(strbuf, 0, term + 1, charset.name());

        fieldname.append(name.trim()); // <- append byte array to String Buffer

        if (DEBUG)
            System.out.println("Fieldname " + fieldname);
        fieldtype = (char) dFile.readUnsignedByteLE();
        fieldstart = pos;
        dFile.skipBytes(4);
        switch (fieldtype) {
        case 'C':
        case 'c':
        case 'D':
        case 'L':
        case 'M':
        case 'G':
            fieldlen = (int) dFile.readUnsignedByteLE();
            fieldnumdec = (int) dFile.readUnsignedByteLE();
            fieldnumdec = 0;
            break;
        case 'N':
        case 'n':
        case 'F':
        case 'f':
            fieldlen = (int) dFile.readUnsignedByteLE();
            fieldnumdec = (int) dFile.readUnsignedByteLE();
            break;
        default:
            System.out.println("Help - wrong field type: " + fieldtype);
        }
        if (DEBUG)
            System.out.println("Fieldtype " + fieldtype + " width " + fieldlen + "." + fieldnumdec);

        dFile.skipBytes(14);

    }
}
