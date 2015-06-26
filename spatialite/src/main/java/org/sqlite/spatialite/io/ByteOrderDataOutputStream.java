package org.sqlite.spatialite.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;

import com.vividsolutions.jts.io.ByteOrderValues;

public class ByteOrderDataOutputStream extends OutputStream {

    private final OutputStream delegate;
    private final int byteOrder;

    private byte[] buf1 = new byte[1];
    private byte[] buf4 = new byte[4];
    private byte[] buf8 = new byte[8];

    public ByteOrderDataOutputStream(OutputStream os, ByteOrder byteOrder) {
        this.delegate = os;
        if (byteOrder.equals(ByteOrder.BIG_ENDIAN)) {
            this.byteOrder = ByteOrderValues.BIG_ENDIAN;
        } else {
            this.byteOrder = ByteOrderValues.LITTLE_ENDIAN;
        }
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    public void writeByte(byte b) throws IOException {
        buf1[0] = b;
        delegate.write(buf1);
    }

    @Override
    public void write(int b) throws IOException {

        ByteOrderValues.putInt(b, buf4, byteOrder);
        delegate.write(buf4);

    }

    public void writeDouble(double d) throws IOException {

        ByteOrderValues.putDouble(d, buf8, byteOrder);
        delegate.write(buf8);

    }

    public void writeFloat(float f) throws IOException {

        int b = Float.floatToIntBits(f);
        write(b);

    }

}
