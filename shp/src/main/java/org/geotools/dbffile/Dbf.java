package org.geotools.dbffile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

import com.vividsolutions.jump.io.EndianDataInputStream;

/**
 *
 * This class represents a DBF (or DBase) file.
 * <p>
 * Construct it with a URL or File (including the .dbf) this causes the header
 * and field definitions to be read.
 * <p>
 * Later queries return rows or columns of the database.
 * <p>
 * If a URL is specified then the whole file is read into memory<br>
 * if a file is specified then a randomAccess system is used.<br>
 * <hr>
 * 
 * @author <a href="mailto:ian@geog.leeds.ac.uk">Ian Turton</a> Centre for
 *         Computaional Geography, University of Leeds, LS2 9JT, 1998. <br>
 *         mod to getStringCol by James Macgill.
 */
public class Dbf implements DbfConsts {
    static final boolean DEBUG = false;
    static final String DBC = "Dbf->";
    int dbf_id;
    int last_update_d, last_update_m, last_update_y;
    int last_rec;
    int data_offset;
    int rec_size;
    StringBuffer records[];
    int position = 0;
    boolean hasmemo;
    boolean isFile = false;
    RandomAccessFile rFile;
    EndianDataInputStream dFile;
    int filesize, numfields;
    public DbfFieldDef fielddef[];

    /**
     * Constructor, opens the file and reads the header infomation.
     * 
     * @param url
     *            the url to be opened
     * @exception java.io.IOException
     *                If the file can't be opened.
     * @exception DbfFileException
     *                If there is an error reading header.
     */
    public Dbf(URL url) throws java.io.IOException, DbfFileException {
        if (DEBUG)
            System.out.println("---->uk.ac.leeds.ccg.dbffile.Dbf constructed. Will identify itself as " + DBC);
        URLConnection uc = url.openConnection();
        InputStream in = uc.getInputStream();
        EndianDataInputStream sfile = new EndianDataInputStream(in);
        init(sfile);
    }

    public Dbf(InputStream in) throws java.io.IOException, DbfFileException {
        if (DEBUG)
            System.out.println("---->uk.ac.leeds.ccg.dbffile.Dbf constructed. Will identify itself as " + DBC);
        EndianDataInputStream sfile = new EndianDataInputStream(in);
        init(sfile);
    }

    public Dbf(String name) throws java.io.IOException, DbfFileException {
        if (DEBUG)
            System.out.println("---->uk.ac.leeds.ccg.dbffile.Dbf constructed. Will identify itself as " + DBC);
        URL url = new URL(name);
        URLConnection uc = url.openConnection();
        InputStream in = uc.getInputStream();
        EndianDataInputStream sfile = new EndianDataInputStream(in);
        init(sfile);
    }

    public Dbf(File file) throws java.io.IOException, DbfFileException {
        if (DEBUG)
            System.out.println("---->uk.ac.leeds.ccg.dbffile.Dbf constructed. Will identify itself as " + DBC);
        InputStream in = new FileInputStream(file);
        EndianDataInputStream sfile = new EndianDataInputStream(in);
        rFile = new RandomAccessFile(file, "r");
        isFile = true;
        init(sfile);
    }

    /**
     * Returns the date of the last update of the file as a string.
     */
    public String getLastUpdate() {
        String date = last_update_d + "/" + (last_update_m + 1) + "/" + (1900 + last_update_y);
        return date;
    }

    /**
     * Returns the number of records in the database file.
     */
    public int getLastRec() {
        return last_rec;
    }

    /**
     * Returns the size of the records in the database file.
     */
    public int getRecSize() {
        return rec_size;
    }

    /**
     * Returns the number of fields in the records in the database file.
     */
    public int getNumFields() {
        return numfields;
    }

    /**
     * looks up the field number for the given named column
     * 
     * @param name
     *            A String for the name to look up
     * @return int The col number for the field, -1 if field could not be found
     */
    public int getFieldNumber(String name) {
        for (int i = 0; i < numfields; i++) {
            // System.out.println(i);
            if (name.equalsIgnoreCase(fielddef[i].fieldname.toString())) {
                return i;
            }
        }
        return -1;// not found
    }

    /**
     * Returns the size of the database file.
     */
    public int getFileSize() {
        return filesize;
    }

    public StringBuffer getFieldName(int col) {
        if (col >= numfields)
            throw new IllegalArgumentException(DBC + "column number specified is invalid. It's higher than the amount of columns available " + numfields);
        return fielddef[col].fieldname;
    }

    public char getFieldType(int col) {
        if (col >= numfields)
            throw new IllegalArgumentException(DBC + "column number specified is invalid. It's higher than the amount of columns available" + numfields);
        return fielddef[col].fieldtype;
    }

    /**
     * initailizer, allows the use of multiple constructers in later versions.
     */

    @SuppressWarnings("unused")
    private void init(EndianDataInputStream sfile) throws IOException, DbfFileException {
        DbfFileHeader head = new DbfFileHeader(sfile);
        int widthsofar;

        dFile = sfile;

        fielddef = new DbfFieldDef[numfields];
        widthsofar = 1;
        for (int index = 0; index < numfields; index++) {
            fielddef[index] = new DbfFieldDef();
            fielddef[index].setup(widthsofar, sfile);
            widthsofar += fielddef[index].fieldlen;
        }
        sfile.skipBytes(1); // end of field defs marker
        if (!isFile) {
            records = GrabFile();
        }
    }

    /**
     * Internal Class to hold information from the header of the file
     */
    class DbfFileHeader {

        /**
         * Reads the header of a dbf file.
         * 
         * @param LEDataInputStream
         *            file Stream attached to the input file
         * @exception IOException
         *                read error.
         */
        public DbfFileHeader(EndianDataInputStream file) throws IOException {
            getDbfFileHeader(file);
        }

        @SuppressWarnings("unused")
        private void getDbfFileHeader(EndianDataInputStream file) throws IOException {

            int len;
            dbf_id = (int) file.readUnsignedByteLE();
            if (DEBUG)
                System.out.println(DBC + "Header id " + dbf_id);
            if (dbf_id == 3)
                hasmemo = true;
            else
                hasmemo = false;

            last_update_y = (int) file.readUnsignedByteLE();
            last_update_m = (int) file.readUnsignedByteLE();
            last_update_d = (int) file.readUnsignedByteLE();
            if (DEBUG)
                System.out.print(DBC + "last update ");
            if (DEBUG)
                System.out.print(last_update_d);
            if (DEBUG)
                System.out.print("/");
            if (DEBUG)
                System.out.print(last_update_m);
            if (DEBUG)
                System.out.print("/");
            if (DEBUG)
                System.out.println(last_update_y);

            last_rec = file.readIntLE();
            if (DEBUG)
                System.out.print(DBC + "last rec ");
            if (DEBUG)
                System.out.println(last_rec);

            data_offset = file.readShortLE();
            // data_offset=0;
            // System.out.println("x = "+file.readUnsignedByte()+" " +
            // file.readUnsignedByte());
            if (DEBUG)
                System.out.print(DBC + "data offset ");
            if (DEBUG)
                System.out.println(data_offset);

            rec_size = file.readShortLE();
            if (DEBUG)
                System.out.print(DBC + "rec_size ");
            if (DEBUG)
                System.out.println(rec_size);

            filesize = (rec_size * last_rec) + data_offset + 1;
            numfields = (data_offset - DBF_BUFFSIZE - 1) / DBF_BUFFSIZE;

            if (DEBUG)
                System.out.print(DBC + "num fields ");
            if (DEBUG)
                System.out.println(numfields);
            if (DEBUG)
                System.out.print(DBC + "file size ");
            if (DEBUG)
                System.out.println(filesize);
            file.skipBytes(20);
        }

    }

    /**
     * gets the next record and returns it as a string. This method works on a
     * sequential stream and can not go backwards. Only useful if you want to
     * read the whole file in one.
     * 
     * @exception java.io.IOException
     *                on read error.
     */
    public StringBuffer GetNextDbfRec() throws java.io.IOException {
        return records[position++];
    }

    private StringBuffer GrabNextDbfRec() throws java.io.IOException {
        StringBuffer record = new StringBuffer(rec_size + numfields);

        // Modifed to use Hisaji ONO's approach for reading multi byte character
        // sets
        byte[] strbuf = new byte[rec_size]; // <---- byte array buffer fo
                                            // storing string's byte data
        for (int i = 0; i < rec_size; i++) {
            strbuf[i] = dFile.readByteLE(); // <---- read string's byte data
        }
        record.append(new String(strbuf)); // <- append byte array to String
                                           // Buffer

        return record;
    }

    private StringBuffer[] GrabFile() throws java.io.IOException {
        StringBuffer records[] = new StringBuffer[last_rec];
        for (int i = 0; i < last_rec; i++)
            records[i] = GrabNextDbfRec();
        return records;
    }

    /**
     * fetches the <i>row</i>th row of the file
     * 
     * @param row
     *            - the row to fetch
     * @exception java.io.IOException
     *                on read error.
     */
    public StringBuffer GetDbfRec(int row) throws java.io.IOException {
        StringBuffer record;// = new StringBuffer(rec_size);
        if (!isFile) {
            return record = new StringBuffer(records[row].toString());
        } else {
            record = new StringBuffer(rec_size + numfields);

            rFile.seek(data_offset + (rec_size * row));
            // Modifed to use Hisaji ONO's approach for reading multi byte
            // character sets
            byte[] strbuf = new byte[rec_size]; // <---- byte array buffer fo
                                                // storing string's byte data
            for (int i = 0; i < rec_size; i++) {
                strbuf[i] = dFile.readByteLE(); // <---- read string's byte data
            }
            record.append(new String(strbuf)); // <- append byte array to String
                                               // Buffer
            return record;
        }

    }

    /**
     * fetches the <i>row</i>th row of the file and parses it into an vector of
     * objects.
     * 
     * @param row
     *            - the row to fetch
     * @exception java.io.IOException
     *                on read error.
     */
    @SuppressWarnings("rawtypes")
    public Vector ParseDbfRecord(int row) throws java.io.IOException {
        return ParseRecord(GetDbfRec(row));
    }

    /**
     * Parses the record stored in the StringBuffer rec into a vector of objects
     * 
     * @param rec
     *            the record to be parsed.
     */

    @SuppressWarnings({ "rawtypes", "unchecked", "static-access" })
    public Vector ParseRecord(StringBuffer rec) {
        Vector record = new Vector(numfields);
        String t;
        Integer I = new Integer(0);
        Float F = new Float(0.0);
        t = rec.toString();
        for (int i = 0; i < numfields; i++) {
            if (DEBUG)
                System.out.println(DBC + "type " + fielddef[i].fieldtype);
            if (DEBUG)
                System.out.println(DBC + "start " + fielddef[i].fieldstart);
            if (DEBUG)
                System.out.println(DBC + "len " + fielddef[i].fieldlen);
            if (DEBUG)
                System.out.println(DBC + "" + t.substring(fielddef[i].fieldstart, fielddef[i].fieldstart + fielddef[i].fieldlen));
            switch (fielddef[i].fieldtype) {
            case 'C':
                record.addElement(t.substring(fielddef[i].fieldstart, fielddef[i].fieldstart + fielddef[i].fieldlen));
                break;
            case 'N':
            case 'F':
                if (fielddef[i].fieldnumdec == 0)
                    try {
                        record.addElement(I.decode(t.substring(fielddef[i].fieldstart, fielddef[i].fieldstart + fielddef[i].fieldlen)));
                    } catch (java.lang.NumberFormatException e) {
                        record.addElement(new Integer(0));
                    }
                else
                    try {
                        record.addElement(F.valueOf(t.substring(fielddef[i].fieldstart, fielddef[i].fieldstart + fielddef[i].fieldlen)));
                    } catch (java.lang.NumberFormatException e) {
                        record.addElement(new Float(0.0));
                    }

                break;
            default:
                if (DEBUG)
                    System.out.println(DBC + "Oh - don't know how to parse " + fielddef[i].fieldtype);
            }
        }
        return record;
    }

    /**
     * Fetches a column of Integers from the database file.
     * 
     * @param col
     *            - the column to fetch
     * @exception java.io.IOException
     *                - on read error
     * @exception DbfFileException
     *                - column is not an Integer.
     */
    public Integer[] getIntegerCol(int col) throws java.io.IOException, DbfFileException {
        return getIntegerCol(col, 0, last_rec);
    }

    /**
     * Fetches a part column of Integers from the database file.
     * 
     * @param col
     *            - the column to fetch
     * @param start
     *            - the row to start fetching from
     * @param end
     *            - the row to stop fetching at.
     * @exception java.io.IOException
     *                - on read error
     * @exception DbfFileException
     *                - column is not an Integer.
     */
    public Integer[] getIntegerCol(int col, int start, int end) throws java.io.IOException, DbfFileException {
        Integer column[] = new Integer[end - start];
        String record = new String();
        StringBuffer sb = new StringBuffer(numfields);
        int k = 0, i = 0;
        if (col >= numfields)
            throw new DbfFileException(DBC + "No Such Column in file: " + col);
        if (fielddef[col].fieldtype != 'N')
            throw new DbfFileException(DBC + "Column " + col + " is not Integer " + fielddef[col].fieldtype);
        if (start < 0)
            throw new DbfFileException(DBC + "Start must be >= 0");
        if (end > last_rec)
            throw new DbfFileException(DBC + "End must be <= " + last_rec);
        // move to start of data
        try {
            for (i = start; i < end; i++) {
                sb.setLength(0);
                sb = GetDbfRec(i);
                record = sb.toString();
                if (DEBUG)
                    System.out.println(DBC + record.substring(fielddef[col].fieldstart, fielddef[col].fieldstart + fielddef[col].fieldlen).trim() + "*");
                column[i - start] = new Integer(record.substring(fielddef[col].fieldstart, fielddef[col].fieldstart + fielddef[col].fieldlen).trim());
            }
        } catch (java.lang.NumberFormatException nfe) {
            // throw new
            // DbfFileException(DBC+"Column "+col+" contains an non integer id number "+nfe);
            // be nicer
            column[i - start] = new Integer(0);
        } catch (java.io.EOFException e) {
            System.err.println(e);
            System.err.println("Dbf->record " + i + " byte " + k + " file pos ");
        } catch (java.io.IOException e) {
            System.err.println(e);
            System.err.println("Dbf->record " + i + " byte " + k + " file pos ");
        }
        return column;
    }

    /**
     * Fetches a column of Floats from the database file.
     * 
     * @param col
     *            - the column to fetch
     * @exception java.io.IOException
     *                - on read error
     * @exception DbfFileException
     *                - column is not an Integer.
     */
    public Float[] getFloatCol(int col) throws DbfFileException, java.io.IOException {
        return getFloatCol(col, 0, last_rec);
    }

    /**
     * Fetches a part column of Floats from the database file.
     * 
     * @param col
     *            - the column to fetch
     * @param start
     *            - the row to start fetching from
     * @param end
     *            - the row to stop fetching at.
     * @exception java.io.IOException
     *                - on read error
     * @exception DbfFileException
     *                - column is not an Integer.
     */
    public Float[] getFloatCol(int col, int start, int end) throws DbfFileException, java.io.IOException {
        Float column[] = new Float[end - start];
        String record, st;
        StringBuffer sb = new StringBuffer(rec_size);
        int k = 0, i = 0;
        if (col >= numfields)
            throw new DbfFileException("Dbf->No Such Column in file: " + col);
        if (fielddef[col].fieldtype != 'F' && fielddef[col].fieldtype != 'N')
            throw new DbfFileException("Dbf->Column " + col + " is not Float " + fielddef[col].fieldtype);
        if (start < 0)
            throw new DbfFileException("Dbf->Start must be >= 0");
        if (end > last_rec)
            throw new DbfFileException("Dbf->End must be <= " + last_rec);
        // move to start of data
        try {
            for (i = start; i < end; i++) {
                sb.setLength(0);
                sb = GetDbfRec(i);
                record = sb.toString();
                st = new String(record.substring(fielddef[col].fieldstart, fielddef[col].fieldstart + fielddef[col].fieldlen)).trim();
                if (st.indexOf('.') == -1) {
                    st = st + ".0";
                }
                try {
                    column[i - start] = new Float(st);
                } catch (java.lang.NumberFormatException e) {
                    column[i - start] = new Float(0.0);
                }
            }
        } catch (java.io.EOFException e) {
            System.err.println("Dbf->" + e);
            System.err.println("Dbf->record " + i + " byte " + k + " file pos ");
        } catch (java.io.IOException e) {
            System.err.println("Dbf->" + e);
            System.err.println("Dbf->record " + i + " byte " + k + " file pos ");
        }

        return column;
    }

    /**
     * Fetches a column of Strings from the database file.
     * 
     * @param col
     *            - the column to fetch
     * @exception java.io.IOException
     *                - on read error
     * @exception DbfFileException
     *                - column is not an Integer.
     */
    public String[] getStringCol(int col) throws DbfFileException, java.io.IOException {
        return getStringCol(col, 0, last_rec);
    }

    /**
     * Fetches a part column of Strings from the database file.
     * 
     * @param col
     *            - the column to fetch
     * @param start
     *            - the row to start fetching from
     * @param end
     *            - the row to stop fetching at.
     * @exception java.io.IOException
     *                - on read error
     * @exception DbfFileException
     *                - column is not an Integer.
     */
    public String[] getStringCol(int col, int start, int end) throws DbfFileException, java.io.IOException {
        String column[] = new String[end - start];
        String record = new String();
        StringBuffer sb = new StringBuffer(numfields);
        int k = 0, i = 0;
        if (col >= numfields)
            throw new DbfFileException("Dbf->No Such Column in file: " + col);
        // if(fielddef[col].fieldtype!='C')
        // throw new DbfFileException("Column "+col+" is not a String");
        if (start < 0)
            throw new DbfFileException("Dbf->Start must be >= 0");
        if (end > last_rec)
            throw new DbfFileException("Dbf->End must be <= " + last_rec);
        // move to start of data
        try {
            for (i = start; i < end; i++) {
                sb.setLength(0);
                sb = GetDbfRec(i);
                record = sb.toString();
                // column[i-start]=new
                // String(record.substring(fielddef[col].fieldstart,
                // fielddef[col].fieldstart+fielddef[col].fieldlen));
                // replaced to fix bug #547080
                column[i - start] = new String(record.getBytes(), fielddef[col].fieldstart, fielddef[col].fieldlen).trim();

            }
        } catch (java.io.EOFException e) {
            System.err.println("Dbf->" + e);
            System.err.println("Dbf->record " + i + " byte " + k + " file pos ");
        } catch (java.io.IOException e) {
            System.err.println("Dbf->" + e);
            System.err.println("Dbf->record " + i + " byte " + k + " file pos ");
        }
        return column;
    }
}
