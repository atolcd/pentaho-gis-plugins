package org.geotools.dbffile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.vividsolutions.jump.io.EndianDataInputStream;

/**
 * This class represents a DBF (or DBase) file.
 * <p>
 * Construct it with a filename (including the .dbf) this causes the header and
 * field definitions to be read.
 * <p>
 * Later queries return rows or columns of the database.
 * <hr>
 * 
 * @author <a href="mailto:ian@geog.leeds.ac.uk">Ian Turton</a> Centre for
 *         Computaional Geography, University of Leeds, LS2 9JT, 1998.
 *
 */
@SuppressWarnings("unused")
public class DbfFile implements DbfConsts {
    static final boolean DEBUG = false;
    int dbf_id;
    int last_update_d;
    int last_update_m;
    int last_update_y;
    int last_rec;
    int data_offset;
    int rec_size;
    boolean hasmemo;
    public EndianDataInputStream dFile;
    RandomAccessFile rFile;
    int filesize;
    int numfields;
    Map<String, String> uniqueStrings;
    public DbfFieldDef[] fielddef;
    @SuppressWarnings("serial")
    public static final SimpleDateFormat DATE_PARSER = new SimpleDateFormat("yyyyMMdd") {
        {
            // DZ
            setLenient(true);
        }
    };

    private Charset charset = Charset.defaultCharset();

    protected DbfFile() {
        // for testing.
    }

    /**
     * For compatibilty reasons, this method is a wrapper to the new with
     * Charset functions.
     *
     * @param file
     * @throws java.io.IOException
     * @throws DbfFileException
     */
    public DbfFile(String file) throws java.io.IOException, DbfFileException {
        this(file, Charset.defaultCharset());
    }

    /**
     * Constructor, opens the file and reads the header infomation.
     * 
     * @param file
     *            The file to be opened, includes path and .dbf
     * @exception java.io.IOException
     *                If the file can't be opened.
     * @exception DbfFileException
     *                If there is an error reading header.
     */
    public DbfFile(String file, Charset charset) throws java.io.IOException, DbfFileException {
        this.charset = charset;
        if (DEBUG) {
            System.out.println("---->uk.ac.leeds.ccg.dbffile.DbfFile constructed. Will identify itself as DbFi>");
        }

        InputStream in = new FileInputStream(file);
        EndianDataInputStream sfile = new EndianDataInputStream(in);
        rFile = new RandomAccessFile(new File(file), "r");

        if (DEBUG) {
            System.out.println("Dbf file has initinalized");
        }

        init(sfile);
    }

    /**
     * Returns the date of the last update of the file as a string.
     */
    public String getLastUpdate() {
        String date = last_update_d + "/" + last_update_m + "/" + last_update_y;

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

    public String getFieldName(int row) {
        return (fielddef[row].fieldname).toString();
    }

    public String getFieldType(int row) {
        char type = fielddef[row].fieldtype;
        String realtype = "";

        switch (type) {
        case 'C':
            realtype = "STRING";
            break;

        case 'N':
            if (fielddef[row].fieldnumdec == 0) {
                realtype = "INTEGER";
            } else {
                realtype = "DOUBLE";
            }
            break;

        case 'F':
            realtype = "DOUBLE";
            break;

        case 'D': // Added by [Jon Aquino]
            realtype = "DATE";
            break;

        default:
            realtype = "STRING";
            break;
        }

        return realtype;
    }

    /**
     * Returns the size of the database file.
     */
    public int getFileSize() {
        return filesize;
    }

    /**
     * initailizer, allows the use of multiple constructers in later versions.
     */
    private void init(EndianDataInputStream sfile) throws IOException, DbfFileException {
        DbfFileHeader head = new DbfFileHeader(sfile);
        // A map to store a unique reference for identical field value
        uniqueStrings = new HashMap<String, String>();
        int widthsofar;

        if (DEBUG) {
            System.out.println("Dbf file has initinalized");
        }

        dFile = sfile;

        fielddef = new DbfFieldDef[numfields];
        widthsofar = 1;

        for (int index = 0; index < numfields; index++) {
            fielddef[index] = new DbfFieldDef();
            fielddef[index].setup(widthsofar, dFile, charset);
            widthsofar += fielddef[index].fieldlen;
        }

        sfile.skipBytes(1); // end of field defs marker
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
        StringBuffer record = new StringBuffer(rec_size + numfields);

        for (int i = 0; i < rec_size; i++) {
            // we could do some checking here.
            record.append((char) rFile.readUnsignedByte());
        }

        return record;
    }

    /**
     * fetches the <i>row</i>th row of the file
     * 
     * @param row
     *            - the row to fetch
     * @exception java.io.IOException
     *                on read error.
     */
    // public StringBuffer GetDbfRec(int row) throws java.io.IOException {
    // StringBuffer record = new StringBuffer(rec_size + numfields); //[sstein
    // 9.Sept.08]
    public byte[] GetDbfRec(int row) throws java.io.IOException { // [sstein
                                                                  // 9.Sept.08]

        rFile.seek(data_offset + (rec_size * row));

        // Multi byte character modification thanks to Hisaji ONO
        byte[] strbuf = new byte[rec_size]; // <---- byte array buffer fo
                                            // storing string's byte data

        dFile.readByteLEnum(strbuf);
        // record.append(new String(strbuf)); // <- append byte array to String
        // Buffer //[sstein 9.Sept.08]

        // record.append(strbuf);
        // return record; //[sstein 9.Sept.08]
        return strbuf; // [sstein 9.Sept.08]
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

    // like public Vector ParseRecord(StringBuffer rec), but this
    // will try to minimize the number of object created to keep
    // memory usage down.
    //
    // Will return a String, Double, or Integer
    // not currently supporting Data or logical since we dont have any test
    // datasets

    // intern() function used to save heapspace is abandonned,
    // it is replaced by a mechanism used to keep a unique reference for each
    // string in the heapspace [michael michaud 2008-07-20]
    // ref : http://mindprod.com/jgloss/interned.html#MANUAL
    // static boolean useIntern = true;

    // public Object ParseRecordColumn(StringBuffer rec, int wantedCol)
    // //[sstein 9.Sept.08]
    public Object ParseRecordColumn(byte[] rec, int wantedCol) // [sstein
                                                               // 9.Sept.08]
            throws Exception {
        int start;
        int end;
        start = fielddef[wantedCol].fieldstart;
        int len = fielddef[wantedCol].fieldlen; // [sstein 9.Sept.08]
        end = start + fielddef[wantedCol].fieldlen;
        String s = null, masterString = null;
        switch (fielddef[wantedCol].fieldtype) {

        case 'C': // character
            while ((start < end) && (rec[end - 1] == ' ' || // [sstein
                                                            // 9.Sept.08]
                    rec[end - 1] == 0))
                // [mmichaud 16 june 2010]
                end--; // trim trailing spaces
            s = new String(rec, start, end - start, charset.name()); // [sstein
                                                                     // 9.Sept.08]
                                                                     // +
                                                                     // [Matthias
                                                                     // Scholz
                                                                     // 3.
                                                                     // Sept.10]
                                                                     // Charset
                                                                     // added
            masterString = uniqueStrings.get(s);
            if (masterString != null)
                return masterString;
            else {
                uniqueStrings.put(s, s);
                return s;
            }

        case 'F': // same as numeric, more or less
        case 'N': // numeric

            // fields of type 'F' are always represented as Doubles
            boolean isInteger = fielddef[wantedCol].fieldnumdec == 0 && fielddef[wantedCol].fieldtype == 'N';

            // The number field should be trimed from the start AND the end.
            // Added .trim() to 'String numb = rec.substring(start, end)'
            // instead. [Kevin Neufeld]
            // while ((start < end) && (rec.charAt(start) == ' '))
            // start++;

            // String numb = rec.substring(start, end).trim(); //[sstein
            // 9.Sept.08]
            String numb = new String(rec, start, len).trim(); // [sstein
                                                              // 9.Sept.08]
            if (isInteger) { // its an int

                try {
                    return new Integer(numb);
                } catch (java.lang.NumberFormatException e) {
                    return new Integer(0);
                }
            } else { // its a float

                try {
                    return new Double(numb);
                } catch (java.lang.NumberFormatException e) {
                    // dBase can have numbers that look like '********' !! This
                    // isn't ideal but at least reads them
                    return new Double(Double.NaN);
                }
            }

        case 'D': // date. Added by [Jon Aquino]
            // return parseDate(rec.substring(start, end)); //[sstein 9.Sept.08]
            return parseDate(new String(rec, start, len)); // [sstein 9.Sept.08]

        default:
            // s = rec.substring(start, end); //[sstein 9.Sept.08]
            s = new String(rec, start, len); // [sstein 9.Sept.08]
            masterString = uniqueStrings.get(s);
            if (masterString != null)
                return masterString;
            else {
                uniqueStrings.put(s, s);
                return s;
            }
        }
    }

    /**
     * Parses the record stored in the StringBuffer rec into a vector of objects
     * 
     * @param rec
     *            the record to be parsed.
     */
    // public Vector ParseRecord(StringBuffer rec) { //[sstein 9.Sept.08]
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Vector ParseRecord(byte[] rec) { // [sstein 9.Sept.08]
        Vector record = new Vector(numfields);
        String t;
        // Integer I = new Integer(0);
        // Double F = new Double(0.0);
        // t = rec.toString(); //[sstein 9.Sept.08]
        t = new String(rec); // [sstein 9.Sept.08]

        for (int i = 0; i < numfields; i++) {
            if (DEBUG) {
                System.out.println("DbFi>type " + fielddef[i].fieldtype);
            }

            if (DEBUG) {
                System.out.println("DbFi>start " + fielddef[i].fieldstart);
            }

            if (DEBUG) {
                System.out.println("DbFi>len " + fielddef[i].fieldlen);
            }

            if (DEBUG) {
                System.out.println(t.substring(fielddef[i].fieldstart, fielddef[i].fieldstart + fielddef[i].fieldlen));
            }

            switch (fielddef[i].fieldtype) {
            case 'C':
                record.addElement(t.substring(fielddef[i].fieldstart, fielddef[i].fieldstart + fielddef[i].fieldlen));
                break;

            case 'N':
                if (fielddef[i].fieldnumdec == 0) { // its an int
                    try {
                        String tt = t.substring(fielddef[i].fieldstart, fielddef[i].fieldstart + fielddef[i].fieldlen);
                        record.addElement(Integer.valueOf(tt.trim()));
                    } catch (java.lang.NumberFormatException e) {
                        record.addElement(new Integer(0));
                    }
                } else { // its a float

                    try {
                        record.addElement(Double.valueOf(t.substring(fielddef[i].fieldstart, fielddef[i].fieldstart + fielddef[i].fieldlen).trim()));
                    } catch (java.lang.NumberFormatException e) {
                        record.addElement(new Double(0.0));
                    }
                }

                break;

            case 'F':
                try {
                    record.addElement(Double.valueOf(t.substring(fielddef[i].fieldstart, fielddef[i].fieldstart + fielddef[i].fieldlen).trim()));
                } catch (java.lang.NumberFormatException e) {
                    record.addElement(new Double(0.0));
                }

                break;

            case 'D':
                // Date formats. This method doesn't seem to be called anywhere
                // in JUMP,
                // so I'm not going to spend time understanding this method.
                // [Jon Aquino]
                throw new UnsupportedOperationException();

            default:
                record.addElement(t.substring(fielddef[i].fieldstart, fielddef[i].fieldstart + fielddef[i].fieldlen));
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
        Integer[] column = new Integer[end - start];
        String record = new String();
        StringBuffer sb = new StringBuffer(numfields);
        int k = 0;
        int i = 0;

        if (col >= numfields) {
            throw new DbfFileException("DbFi>No Such Column in file: " + col);
        }

        if (fielddef[col].fieldtype != 'N') {
            throw new DbfFileException("DbFi>Column " + col + " is not Integer");
        }

        // move to start of data
        try {
            rFile.seek(data_offset + (rec_size * start));

            for (i = start; i < end; i++) {
                sb.setLength(0);

                for (k = 0; k < rec_size; k++)
                    sb.append((char) rFile.readUnsignedByte());

                record = sb.toString();

                try {
                    column[i - start] = new Integer(record.substring(fielddef[col].fieldstart, fielddef[col].fieldstart + fielddef[col].fieldlen));
                } catch (java.lang.NumberFormatException e) {
                    column[i - start] = new Integer(0);
                }
            }
        } catch (java.io.EOFException e) {
            System.err.println("DbFi>" + e);
            System.err.println("DbFi>record " + i + " byte " + k + " file pos " + rFile.getFilePointer());
        } catch (java.io.IOException e) {
            System.err.println("DbFi>" + e);
            System.err.println("DbFi>record " + i + " byte " + k + " file pos " + rFile.getFilePointer());
        }

        return column;
    }

    /**
     * Fetches a column of Double from the database file.
     * 
     * @param col
     *            - the column to fetch
     * @exception java.io.IOException
     *                - on read error
     * @exception DbfFileException
     *                - column is not an Integer.
     */
    public Double[] getFloatCol(int col) throws DbfFileException, java.io.IOException {
        return getFloatCol(col, 0, last_rec);
    }

    /**
     * Fetches a part column of Double from the database file.
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
    public Double[] getFloatCol(int col, int start, int end) throws DbfFileException, java.io.IOException {
        Double[] column = new Double[end - start];
        String record;
        String st;
        StringBuffer sb = new StringBuffer(rec_size);
        int k = 0;
        int i = 0;

        if (col >= numfields) {
            throw new DbfFileException("DbFi>No Such Column in file: " + col);
        }

        if (fielddef[col].fieldtype != 'F') {
            throw new DbfFileException("DbFi>Column " + col + " is not Double " + fielddef[col].fieldtype);
        }

        // move to start of data
        try {
            rFile.seek(data_offset + (rec_size * start));

            for (i = start; i < end; i++) {
                sb.setLength(0);

                // we should be able to skip to the start here.
                for (k = 0; k < rec_size; k++)
                    sb.append((char) rFile.readUnsignedByte());

                record = sb.toString();
                st = new String(record.substring(fielddef[col].fieldstart, fielddef[col].fieldstart + fielddef[col].fieldlen));

                if (st.indexOf('.') == -1) {
                    st = st + ".0";
                }

                try {
                    column[i - start] = new Double(st);
                } catch (java.lang.NumberFormatException e) {
                    column[i - start] = new Double(0.0);
                }
            }
        } catch (java.io.EOFException e) {
            System.err.println("DbFi>" + e);
            System.err.println("DbFi>record " + i + " byte " + k + " file pos " + rFile.getFilePointer());
        } catch (java.io.IOException e) {
            System.err.println("DbFi>" + e);
            System.err.println("DbFi>record " + i + " byte " + k + " file pos " + rFile.getFilePointer());
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
        String[] column = new String[end - start];
        String record = new String();

        // StringBuffer sb = new StringBuffer(numfields);
        int k = 0;

        // StringBuffer sb = new StringBuffer(numfields);
        int i = 0;

        if (col >= numfields) {
            throw new DbfFileException("DbFi>No Such Column in file: " + col);
        }

        if (fielddef[col].fieldtype != 'C') {
            throw new DbfFileException("DbFi>Column " + col + " is not a String");
        }

        // move to start of data
        try {
            rFile.seek(data_offset + (start * rec_size));

            for (i = start; i < end; i++) {
                // sb.setLength(0);
                // *** initialize buffer for record ***
                byte[] strbuf = new byte[rec_size];

                for (k = 0; k < rec_size; k++) {
                    strbuf[k] = rFile.readByte(); // *** get byte data
                }

                // sb.append((char)rFile.readUnsignedByte());
                // record=sb.toString();
                // *** convert buffer data to String ***
                record = new String(strbuf);

                // column[i-start]=new
                // String(record.substring(fielddef[col].fieldstart,fielddef[col].fieldstart+fielddef[col].fieldlen));
                // *** Extract string data from record
                column[i - start] = new String(strbuf, fielddef[col].fieldstart, fielddef[col].fieldlen);
            }
        } catch (java.io.EOFException e) {
            System.err.println("DbFi>" + e);
            System.err.println("DbFi>record " + i + " byte " + k + " file pos " + rFile.getFilePointer());
        } catch (java.io.IOException e) {
            System.err.println("DbFi>" + e);
            System.err.println("DbFi>record " + i + " byte " + k + " file pos " + rFile.getFilePointer());
        }

        return column;
    }

    public void close() throws IOException {
        dFile.close();
        rFile.close();
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

        private void getDbfFileHeader(EndianDataInputStream file) throws IOException {
            int len;
            dbf_id = (int) file.readUnsignedByteLE();

            if (DEBUG) {
                System.out.print("DbFi>Header id ");
            }

            if (DEBUG) {
                System.out.println(dbf_id);
            }

            if (dbf_id == 3) {
                hasmemo = false;
            } else {
                hasmemo = true;
            }

            last_update_y = (int) file.readUnsignedByteLE() + DBF_CENTURY;
            last_update_m = (int) file.readUnsignedByteLE();
            last_update_d = (int) file.readUnsignedByteLE();

            if (DEBUG) {
                System.out.print("DbFi>last update ");
            }

            if (DEBUG) {
                System.out.print(last_update_d);
            }

            if (DEBUG) {
                System.out.print("/");
            }

            if (DEBUG) {
                System.out.print(last_update_m);
            }

            if (DEBUG) {
                System.out.print("/");
            }

            if (DEBUG) {
                System.out.println(last_update_y);
            }

            last_rec = file.readIntLE();

            if (DEBUG) {
                System.out.print("DbFi>last rec ");
            }

            if (DEBUG) {
                System.out.println(last_rec);
            }

            data_offset = file.readShortLE();

            // data_offset=0;
            // System.out.println("x = "+file.readUnsignedByte()+" " +
            // file.readUnsignedByte());
            if (DEBUG) {
                System.out.print("DbFi>data offset ");
            }

            if (DEBUG) {
                System.out.println(data_offset);
            }

            rec_size = file.readShortLE();

            if (DEBUG) {
                System.out.print("DbFi>rec_size ");
            }

            if (DEBUG) {
                System.out.println(rec_size);
            }

            filesize = (rec_size * last_rec) + data_offset + 1;
            numfields = (data_offset - DBF_BUFFSIZE - 1) / DBF_BUFFSIZE;

            if (DEBUG) {
                System.out.print("DbFi>num fields ");
            }

            if (DEBUG) {
                System.out.println(numfields);
            }

            if (DEBUG) {
                System.out.print("DbFi>file size ");
            }

            if (DEBUG) {
                System.out.println(filesize);
            }

            file.skipBytes(20);
        }
    }

    protected Date parseDate(String s) throws ParseException {
        if (s.trim().length() == 0) {
            return null;
        }

        if (s.equals("00000000")) {
            // Not sure if Jan 1, 0001 is the most appropriate value.
            // Year 0000 gives me a ParseException. [Jon Aquino]
            return DATE_PARSER.parse("00010101");
        }
        try {
            return lastFormat.parse(s);
        } catch (ParseException pe) {
            // ignore
        }

        String[] patterns = new String[] { "yyyyMMdd", "yy/mm/dd" };

        for (int i = 0; i < patterns.length; i++) {
            DateFormat df = new SimpleDateFormat(patterns[i]);
            df.setLenient(true);
            try {
                Date d = df.parse(s);
                lastFormat = df;
                return d;
            } catch (ParseException pe) {
                // ignore
            }
        }

        return null;
    }

    private DateFormat lastFormat = DATE_PARSER;

    @SuppressWarnings("serial")
    public static void main(String[] args) throws Exception {
        System.out.println(new SimpleDateFormat("yyyymmdd") {
            {
                setLenient(false);
            }
        }.parse("00010101"));
    }
}
