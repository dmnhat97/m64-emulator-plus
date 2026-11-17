package paulscode.android.mupen64plusae.netplay.TcpMessage;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import paulscode.android.mupen64plusae.netplay.TcpServer;

public class PlayerRegistrationMessage implements TcpMessage {

    private static final int MESSAGE_SIZE = 8 - 1;
    TcpServer mTcpServer;
    ByteBuffer mReceiveBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
    ByteBuffer mSendBuffer = ByteBuffer.allocate(2);
    OutputStream mOutputStream;
    int mBufferTarget;

    int mPlayer;
    int mPlugin;
    boolean mRawInput;
    int mRegistrationId;

    public PlayerRegistrationMessage(TcpServer tcpServer, OutputStream outputStream) {
        mTcpServer = tcpServer;
        mOutputStream = outputStream;
        mBufferTarget = tcpServer.getBufferTarget();
        mReceiveBuffer.order(ByteOrder.BIG_ENDIAN);
        mReceiveBuffer.mark();
        mSendBuffer.order(ByteOrder.BIG_ENDIAN);
        mSendBuffer.mark();
    }

    @Override
    public void parse(InputStream stream) throws IOException {

        int offset = 0;
        mReceiveBuffer.reset();
        int bytesRead = 0;
        while (offset < MESSAGE_SIZE && bytesRead != -1) {
            bytesRead = stream.read(mReceiveBuffer.array(), offset, MESSAGE_SIZE - offset);
            offset += bytesRead != -1 ? bytesRead : 0;
        }

        mPlayer = mReceiveBuffer.get();
        mPlugin = mReceiveBuffer.get();
        mRawInput = mReceiveBuffer.get() != 0;
        mRegistrationId = mReceiveBuffer.getInt();
    }

    @Override
    public void process() throws IOException {

        TcpServer.PlayerData playerData = mTcpServer.getPlayerData(mPlayer);
        mSendBuffer.reset();

        if (playerData == null) {
            //Only P1 can use mempak
            if (mPlayer > 0 && mPlugin == 2) {
                mPlugin = 1;
            }

            playerData = new TcpServer.PlayerData();
            playerData.mRegId = mRegistrationId;
            playerData.mPlugin = mPlugin;
            playerData.mRaw = mRawInput;
            mTcpServer.addPlayerData(mPlayer, playerData);

            mSendBuffer.put((byte)1);
            mSendBuffer.put((byte)mBufferTarget);

        } else {
            if (playerData.mRegId == mRegistrationId) {
                mSendBuffer.put((byte)1);
            } else {
                // Same player slot is being re-registered with a different
                // regId. This is expected when the host's GameActivity is
                // recreated (e.g. orientation change) and the native core
                // restarts: NetplayService (foreground) outlives the
                // Activity and still remembers the original regId, but the
                // new native instance generates a fresh one. Treat this as
                // a successful re-registration and replace the stored regId
                // instead of locking the player slot, which would cause the
                // host's native side to skip netplay_set_controller(0) and
                // never send TCP_SEND_SETTINGS, leaving everyone stuck on
                // a black screen.
                Log.i("PlayerRegistrationMessage",
                        "Replacing regId for player " + mPlayer +
                                ": old=" + playerData.mRegId +
                                " new=" + mRegistrationId);
                playerData.mRegId = mRegistrationId;
                playerData.mPlugin = mPlugin;
                playerData.mRaw = mRawInput;
                mTcpServer.addPlayerData(mPlayer, playerData);
                mSendBuffer.put((byte)1);
            }

            mSendBuffer.put((byte)mTcpServer.getBufferTarget());
        }

        mOutputStream.write(mSendBuffer.array());
    }
}
