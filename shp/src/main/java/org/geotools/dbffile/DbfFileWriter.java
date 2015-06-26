package org.geotools.dbffile;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;

import org.geotools.misc.FormatedString;

import com.vividsolutions.jump.io.EndianDataOutputStream;

/**
 * a class for writing dbf files
 * 
 * @author Ian Turton modified by Micha&euml;l MICHAUD on 3 nov. 2004
 */

public class DbfFileWriter implements DbfConsts {

    private final static boolean DEBUG = false;

    private final static String DBC = "DbFW>";

    int NoFields = 1;

    int NoRecs = 0;

    int recLength = 0;

    DbfFieldDef fields[];

    EndianDataOutputStream ls;

    private boolean header = false;

    private Charset charset = Charset.defaultCharset();

    public DbfFileWriter(String file) throws IOException {
        if (DEBUG)
            System.out.println("---->uk.ac.leeds.ccg.dbffile.DbfFileWriter constructed. Will identify itself as " + DBC);
        ls = new EndianDataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
    }

    public void writeHeader(DbfFieldDef f[], int nrecs) throws IOException {

        NoFields = f.length;
        NoRecs = nrecs;
        fields = new DbfFieldDef[NoFields];
        for (int i = 0; i < NoFields; i++) {
            fields[i] = f[i];
        }
        ls.writeByteLE(3); // ID - dbase III with out memo

        // sort out the date
        Calendar calendar = new GregorianCalendar();
        Date trialTime = new Date();
        calendar.setTime(trialTime);
        ls.writeByteLE(calendar.get(Calendar.YEAR) - DBF_CENTURY);
        ls.writeByteLE(calendar.get(Calendar.MONTH) + 1); // month is 0-indexed
        ls.writeByteLE(calendar.get(Calendar.DAY_OF_MONTH));

        int dataOffset = 32 * NoFields + 32 + 1;
        for (int i = 0; i < NoFields; i++) {
            recLength += fields[i].fieldlen;
        }

        recLength++; // delete flag
        if (DEBUG)
            System.out.println(DBC + "rec length " + recLength);
        ls.writeIntLE(NoRecs);
        ls.writeShortLE(dataOffset); // length of header
        ls.writeShortLE(recLength);

        for (int i = 0; i < 20; i++)
            ls.writeByteLE(0); // 20 bytes of junk!

        // field descriptions
        for (int i = 0; i < NoFields; i++) {
            // patch from Hisaji Ono for Double byte characters
            ls.write(fields[i].fieldname.toString().getBytes(charset.name()), 0, 11); // [Matthias
                                                                                      // Scholz
                                                                                      // 04.Sept.2010]
                                                                                      // Charset
                                                                                      // added
            ls.writeByteLE(fields[i].fieldtype);
            for (int j = 0; j < 4; j++)
                ls.writeByteLE(0); // junk
            ls.writeByteLE(fields[i].fieldlen);
            ls.writeByteLE(fields[i].fieldnumdec);
            for (int j = 0; j < 14; j++)
                ls.writeByteLE(0); // more junk
        }
        ls.writeByteLE(0xd);
        header = true;
    }

    @SuppressWarnings("rawtypes")
    public void writeRecords(Vector[] recs) throws DbfFileException, IOException {
        if (!header) {
            throw (new DbfFileException("Must write header before records"));
        }
        int i = 0;
        try {
            if (DEBUG)
                System.out.println(DBC + ":writeRecords writing " + recs.length + " records");
            for (i = 0; i < recs.length; i++) {
                if (recs[i].size() != NoFields)
                    throw new DbfFileException("wrong number of records in " + i + "th record " + recs[i].size() + " expected " + NoFields);
                writeRecord(recs[i]);
            }
        } catch (DbfFileException e) {
            throw new DbfFileException(DBC + "at rec " + i + "\n" + e);
        }
    }

    @SuppressWarnings({ "unused", "rawtypes" })
    public void writeRecord(Vector rec) throws DbfFileException, IOException {
        if (!header) {
            throw (new DbfFileException(DBC + "Must write header before records"));
        }

        if (rec.size() != NoFields)
            throw new DbfFileException(DBC + "wrong number of fields " + rec.size() + " expected " + NoFields);
        String s;
        ls.writeByteLE(' ');
        int len;
        StringBuffer tmps;
        for (int i = 0; i < NoFields; i++) {
            len = fields[i].fieldlen;
            Object o = rec.elementAt(i);
            switch (fields[i].fieldtype) {
            case 'C':
            case 'c':
            case 'D': // Added by [Jon Aquino]
                // case 'L': moved to the end by mmichaud
            case 'M':
            case 'G':
                // chars
                String ss = (String) o;
                while (ss.length() < fields[i].fieldlen) {
                    // need to fill it with ' ' chars
                    // this should converge quickly
                    ss = ss + "                                                                                                                  ";
                }
                tmps = new StringBuffer(ss);
                tmps.setLength(fields[i].fieldlen);
                // patch from Hisaji Ono for Double byte characters
                ls.write(tmps.toString().getBytes(charset.name()), fields[i].fieldstart, fields[i].fieldlen); // [Matthias
                                                                                                              // Scholz
                                                                                                              // 04.Sept.2010]
                                                                                                              // Charset
                                                                                                              // added
                break;
            case 'N':
            case 'n':
                // int?
                String fs = "";
                if (fields[i].fieldnumdec == 0) {
                    if (o instanceof Integer) {
                        fs = FormatedString.format(((Integer) o).intValue(), fields[i].fieldlen);
                    }
                    // case LONG added by mmichaud on 18 sept. 2004
                    else if (o instanceof Long) {
                        fs = FormatedString.format(((Long) o).toString(), 0, fields[i].fieldlen);
                    } else if (o instanceof java.math.BigDecimal) {
                        fs = FormatedString.format(((BigDecimal) o).toString(), 0, fields[i].fieldlen);
                    } else
                        ;
                    if (fs.length() > fields[i].fieldlen)
                        fs = FormatedString.format(0, fields[i].fieldlen);
                    ls.writeBytesLE(fs);
                    break;
                } else {
                    if (o instanceof Double) {
                        fs = FormatedString.format(((Double) o).toString(), fields[i].fieldnumdec, fields[i].fieldlen);
                    } else if (o instanceof java.math.BigDecimal) {
                        fs = FormatedString.format(((BigDecimal) o).toString(), fields[i].fieldnumdec, fields[i].fieldlen);
                    } else
                        ;
                    if (fs.length() > fields[i].fieldlen)
                        fs = FormatedString.format("0.0", fields[i].fieldnumdec, fields[i].fieldlen);
                    ls.writeBytesLE(fs);
                    break;
                }
            case 'F':
            case 'f':
                // double
                s = ((Double) o).toString();
                String x = FormatedString.format(s, fields[i].fieldnumdec, fields[i].fieldlen);
                ls.writeBytesLE(x);
                break;
            // Case 'logical' added by mmichaud on 18 sept. 2004
            case 'L':
                // boolean
                if (o == null || o.equals("") || o.equals(" "))
                    ls.writeBytesLE(" ");
                else {
                    boolean b = ((Boolean) o).booleanValue();
                    ls.writeBytesLE(b ? "T" : "F");
                }
                break;
            }// switch
        }// fields
    }

    public void close() throws IOException {
        ls.writeByteLE(0x1a); // eof mark
        ls.close();
    }

    int dp = 2; // default number of decimals to write

    /**
     * @return the charset
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * @param charset
     *            the charset to set
     */
    public void setCharset(Charset charset) {
        this.charset = charset;
    }

}
