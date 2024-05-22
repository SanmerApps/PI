package dev.sanmer.hidden.compat.stub;

interface IPackageInstallerSessionCompat {
    ParcelFileDescriptor openWrite(String name, long offsetBytes, long lengthBytes);
    ParcelFileDescriptor openRead(String name);
    void write(String name, long offsetBytes, long lengthBytes, in ParcelFileDescriptor fd);
    void close();
    void commit(in IntentSender statusReceiver, boolean forTransferred);
    void abandon();
}