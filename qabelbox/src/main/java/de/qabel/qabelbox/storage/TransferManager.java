package de.qabel.qabelbox.storage;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.communication.BlockServer;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class TransferManager {

    private static final Logger logger = LoggerFactory.getLogger(TransferManager.class.getName());
    private static final String TAG = "TransferManager";
    private final File tempDir;
    private final Map<Integer, CountDownLatch> latches;
    private final Map<Integer, Exception> errors;
    private final BlockServer blockServer = new BlockServer();
    private final Context context;

    public TransferManager(File tempDir) {

        this.tempDir = tempDir;
        latches = new ConcurrentHashMap<>();
        errors = new HashMap<>();

        context = QabelBoxApplication.getInstance().getApplicationContext();
    }

    public File createTempFile() {

        try {
            return File.createTempFile("download", "", tempDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create tempfile");
        }
    }

    /**
     * upload file to server
     *
     * @param prefix              prefix from identity
     * @param name                file name with path
     * @param file                file to upload
     * @param boxTransferListener listener
     * @return new download id
     */
    public int upload(String prefix, String name, final File file, @Nullable final BoxTransferListener boxTransferListener) {

        Log.d(TAG, "upload " + prefix + " " + name + " " + file.toString());
        final int id = blockServer.getNextId();
        latches.put(id, new CountDownLatch(1));
        blockServer.uploadFile(context, prefix, name, file, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                errors.put(id, e);
                Log.e(TAG, "error uploading to " + call.request(), e);
                if (boxTransferListener != null) {
                    boxTransferListener.onFinished();
                }
                latches.get(id).countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                latches.get(id).countDown();
                Log.d(TAG, "upload response " + response.code() + "(" + call.request() + ")");
                if (boxTransferListener != null) {
                    boxTransferListener.onFinished();
                }

                Log.d(TAG, "delete file " + file.getName());
                file.delete();
            }
        });

        return id;
    }

    /**
     * download file from server
     *
     * @param prefix              prefix from identity
     * @param name                file name with directory
     * @param file                destination file
     * @param boxTransferListener listener
     * @return new download id
     */
    public int download(String prefix, String name, final File file, @Nullable final BoxTransferListener boxTransferListener) {

        Log.d(TAG, "download " + prefix + " " + name + " " + file.toString());

        final int id = blockServer.getNextId();
        latches.put(id, new CountDownLatch(1));
        blockServer.downloadFile(context, prefix, name, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                if (boxTransferListener != null) {
                    boxTransferListener.onFinished();
                }
                errors.put(id, e);
                latches.get(id).countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                Log.d(TAG, "download response " + response.code() + " on " + call.request());
                if (response.code() == 200) {
                    readStreamFromServer(response, file, boxTransferListener);
                } else {
                    Log.d(TAG, "donwload failure");
                }
                latches.get(id).countDown();
                if (boxTransferListener != null) {
                    boxTransferListener.onFinished();
                }
            }
        });

        return id;
    }

    /**
     * read stream from server
     *
     * @param response
     * @param file
     * @param boxTransferListener
     * @throws IOException
     */
    private void readStreamFromServer(Response response, File file, @Nullable BoxTransferListener boxTransferListener) throws IOException {

        InputStream is = response.body().byteStream();
        BufferedInputStream input = new BufferedInputStream(is);
        OutputStream output = new FileOutputStream(file);

        Log.d(TAG, "Server response received. Reading stream with unknown size");
        final byte[] data = new byte[1024];
        long total = 0;
        int count;
        while ((count = input.read(data)) != -1) {
            total += count;
            output.write(data, 0, count);
        }

        Log.d(TAG, "download filesize after: " + total);
        if (boxTransferListener != null) {
            boxTransferListener.onProgressChanged(total, total);
        }
        output.flush();
        output.close();
        input.close();
    }

    /**
     * wait until server request finished.
     *
     * @param id id (getted from up/downbload
     * @return true if no error occurs
     */
    public boolean waitFor(int id) {

        logger.info("Waiting for " + id);
        try {
            latches.get(id).await();
            logger.info("Waiting for " + id + " finished");
            Exception e = errors.get(id);
            if (e != null) {
                logger.warn("Error found waiting for " + id, e);
            }
            return e == null;
        } catch (InterruptedException e) {
            return false;
        }
    }

    public void delete(String prefix, String name) {

        Log.d(TAG, "delete " + prefix + " " + name);

        final int id = blockServer.getNextId();
        latches.put(id, new CountDownLatch(1));
        blockServer.downloadFile(context, prefix, name, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                latches.get(id).countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                Log.d(TAG, "delete response " + response.code());

                latches.get(id).countDown();
            }
        });
    }

    public interface BoxTransferListener {

        void onProgressChanged(long bytesCurrent, long bytesTotal);

        void onFinished();
    }
}
