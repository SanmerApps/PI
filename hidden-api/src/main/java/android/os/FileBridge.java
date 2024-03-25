package android.os;

import java.io.IOException;
import java.io.OutputStream;

public class FileBridge {

    public static class FileBridgeOutputStream extends OutputStream {

        public FileBridgeOutputStream(ParcelFileDescriptor clientPfd) {
            throw new RuntimeException("Stub!");
        }

        public void fsync() throws IOException {
            throw new RuntimeException("Stub!");
        }

        @Override
        public void write(int b) throws IOException {
            throw new RuntimeException("Stub!");
        }
    }
}
