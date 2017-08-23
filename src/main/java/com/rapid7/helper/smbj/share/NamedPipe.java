package com.rapid7.helper.smbj.share;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mserref.NtStatus;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.SMB2ImpersonationLevel;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.messages.SMB2CreateRequest;
import com.hierynomus.mssmb2.messages.SMB2CreateResponse;
import com.hierynomus.mssmb2.messages.SMB2IoctlRequest;
import com.hierynomus.mssmb2.messages.SMB2IoctlResponse;
import com.hierynomus.mssmb2.messages.SMB2ReadRequest;
import com.hierynomus.mssmb2.messages.SMB2ReadResponse;
import com.hierynomus.smbj.io.ArrayByteChunkProvider;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.PipeShare;
import com.rapid7.helper.smbj.io.SMB2SessionMessage;

public class NamedPipe extends SMB2SessionMessage implements Closeable {
    private final static int FSCTL_PIPE_TRANSCEIVE = 0x0011c017;
    private final static EnumSet<NtStatus> IOCTL_SUCCESS =
        EnumSet.of(NtStatus.STATUS_SUCCESS, NtStatus.STATUS_BUFFER_OVERFLOW);
    private final static EnumSet<NtStatus> READ_SUCCESS =
        EnumSet.of(NtStatus.STATUS_SUCCESS, NtStatus.STATUS_BUFFER_OVERFLOW, NtStatus.STATUS_END_OF_FILE);
    private final PipeShare share;
    private final SMB2FileId fileID;
    private final int transactBufferSize;
    private final int readBufferSize;

    public NamedPipe(final Session session, final PipeShare share, final String name) throws IOException {
        super(session);

        this.share = share;

        final SMB2CreateRequest createRequest = new SMB2CreateRequest(
            session.getConnection().getNegotiatedProtocol().getDialect(),
            session.getSessionId(),
            share.getTreeConnect().getTreeId(),
            SMB2ImpersonationLevel.Impersonation,
            EnumSet.of(AccessMask.MAXIMUM_ALLOWED),
            null,
            EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ, SMB2ShareAccess.FILE_SHARE_WRITE),
            SMB2CreateDisposition.FILE_OPEN_IF,
            null,
            name);
        final SMB2CreateResponse createResponse = sendAndRead(
            createRequest,
            EnumSet.of(NtStatus.STATUS_SUCCESS));

        fileID = createResponse.getFileId();
        transactBufferSize = Math.min(
            session.getConnection().getConfig().getTransactBufferSize(),
            session.getConnection().getNegotiatedProtocol().getMaxTransactSize());
        readBufferSize = Math.min(
            session.getConnection().getConfig().getReadBufferSize(),
            session.getConnection().getNegotiatedProtocol().getMaxReadSize());
    }

    public InputStream transact(final byte[] inBuffer) throws IOException {
        final SMB2IoctlResponse ioctlResponse = _ioctl(inBuffer);

        return new InputStream() {
            ByteArrayInputStream buffer = new ByteArrayInputStream(ioctlResponse.getOutputBuffer());
            NtStatus status = ioctlResponse.getHeader().getStatus();

            @Override
            public int read() throws IOException {
                int result = buffer.read();
                if (result == -1 && status.equals(NtStatus.STATUS_BUFFER_OVERFLOW)) {
                    final SMB2ReadResponse readResponse = _read();
                    buffer = new ByteArrayInputStream(readResponse.getData());
                    status = readResponse.getHeader().getStatus();
                    result = buffer.read();
                }

                return result;
            }

            @Override
            public void close() throws IOException {
                while (read() != -1);
            }
        };
    }

    @Override
    public void close() {
        share.closeFileId(fileID);
    }

    private SMB2IoctlResponse _ioctl(final byte[] inBuffer) throws IOException {
        final SMB2IoctlRequest ioctlRequest = new SMB2IoctlRequest(
            getDialect(),
            getSessionID(),
            share.getTreeConnect().getTreeId(),
            FSCTL_PIPE_TRANSCEIVE,
            fileID,
            new ArrayByteChunkProvider(inBuffer, 0, inBuffer.length, 0),
            true,
            transactBufferSize);
        final SMB2IoctlResponse ioctlResponse = sendAndRead(
            ioctlRequest,
            IOCTL_SUCCESS);
        return ioctlResponse;
    }

    private SMB2ReadResponse _read() throws IOException {
        final SMB2ReadRequest readRequest = new SMB2ReadRequest(
            getDialect(),
            fileID,
            getSessionID(),
            share.getTreeConnect().getTreeId(),
            0,
            readBufferSize);
        final SMB2ReadResponse readResponse = sendAndRead(
            readRequest,
            READ_SUCCESS);
        return readResponse;
    }
}