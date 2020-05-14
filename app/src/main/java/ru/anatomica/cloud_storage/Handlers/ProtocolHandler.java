package ru.anatomica.cloud_storage.Handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import ru.anatomica.cloud_storage.Controller.Callback;
import ru.anatomica.cloud_storage.Json.FilesListMessage;
import ru.anatomica.cloud_storage.MainActivity;
import ru.anatomica.cloud_storage.Protocol.ProtocolFiles;
import ru.anatomica.cloud_storage.R;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class ProtocolHandler extends ChannelInboundHandlerAdapter {

    public enum State {
        IDLE,
        NAME_LENGTH,
        NAME,
        FILE_LENGTH,
        FILE_RECEIVE,
        FILE_SEND,
        REFRESH,
        END
    }

    private State currentState = State.IDLE;
    private int sendFileFromServer = 0;
    private int refreshFile = 0;
    private int nextLength;
    public static int progress = 0;
    public static long fileLength;
    public static long receivedFileLength;
    private BufferedOutputStream out;
    private MainActivity mainActivity;
    private Callback refreshCallback;
    private Callback reloadTableCallback;

    public ProtocolHandler(MainActivity mainActivity, Callback refreshCallback, Callback reloadTableCallback) {
        this.mainActivity = mainActivity;
        this.refreshCallback = refreshCallback;
        this.reloadTableCallback = reloadTableCallback;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) return;
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            if (currentState == State.IDLE) {
                byte readByte = buf.readByte();
                if (readByte > 0) {
                    if (readByte == (byte) 7) {
                        currentState = State.NAME_LENGTH;
                        refreshFile = 1;
                        receivedFileLength = 0L;
                        AuthHandler.log.info("STATE: Start Refresh Files");
                    }
                    if (readByte == (byte) 10) {
                        currentState = State.END;
                        receivedFileLength = 0L;
                        AuthHandler.log.info("STATE: End of receiving file");
                    }
                    if (readByte == (byte) 15) {
                        currentState = State.NAME_LENGTH;
                        sendFileFromServer = 1;
                        receivedFileLength = 0L;
                        AuthHandler.log.info("STATE: Start file sending");
                    }
                    if (readByte == (byte) 25) {
                        currentState = State.NAME_LENGTH;
                        receivedFileLength = 0L;
                        AuthHandler.log.info("STATE: Start file receiving");
                    } else if (readByte != (byte) 7 && readByte != (byte) 10 && readByte != (byte) 15) {
                        AuthHandler.log.info("ERROR: Invalid first byte - " + readByte);
                    }
                }
            }

            if (currentState == State.NAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    System.out.println("STATE: Get filename length");
                    nextLength = buf.readInt();
                    currentState = State.NAME;
                }
            }

            if (currentState == State.NAME) {
                if (buf.readableBytes() >= nextLength) {
                    if (refreshFile == 1) currentState = State.REFRESH;
                    if (sendFileFromServer == 1) currentState = State.FILE_SEND;
                    if (sendFileFromServer == 0 && refreshFile == 0) {
                        byte[] fileName = new byte[nextLength];
                        buf.readBytes(fileName);
                        System.out.println("STATE: Filename received - " + new String(fileName, StandardCharsets.UTF_8));
                        out = new BufferedOutputStream(new FileOutputStream(MainActivity.directory + "/" + new String(fileName)));
                        currentState = State.FILE_LENGTH;
                    }
                }
            }

            if (currentState == State.FILE_LENGTH) {
                if (buf.readableBytes() >= 8) {
                    fileLength = buf.readLong();
                    System.out.println("STATE: File length received - " + fileLength);
                    currentState = State.FILE_RECEIVE;
                }
            }

            if (currentState == State.FILE_SEND) {
                sendFileFromServer = 0;
                byte[] fileName = new byte[nextLength];
                buf.readBytes(fileName);
                System.out.println("STATE: Filename what will be send - " + new String(fileName, StandardCharsets.UTF_8));
                if (Files.exists(Paths.get("server_storage/" + new String(fileName)))) {
                    ProtocolFiles.sendFile(Paths.get("server_storage/" + new String(fileName)), ctx.channel(), future -> {
                        currentState = State.IDLE;
                        if (!future.isSuccess()) {
                            future.cause().printStackTrace();
                        }
                        if (future.isSuccess()) {
                            System.out.println("File send successful!");
                            ByteBuf end = ByteBufAllocator.DEFAULT.directBuffer(1);
                            end.writeByte((byte) 10);
                            ctx.channel().writeAndFlush(end);
                        }
                    });
                }
                break;
            }

            if (currentState == State.FILE_RECEIVE) {
                if (progress == 0) {
                    progress = 1;
                    mainActivity.runOnUiThread(mainActivity::startProgressBar);
                }
                while (buf.readableBytes() > 0) {
                    out.write(buf.readByte());
                    receivedFileLength++;
                    if (fileLength == receivedFileLength) {
                        currentState = State.IDLE;
                        System.out.println("File receive successful!");
                        MainActivity.invisibleProgress();
                        mainActivity.runOnUiThread(() -> mainActivity.serviceMessage(mainActivity.getString(R.string.success)));
                        out.close();
                        progress = 0;
                        break;
                    }
                }
            }

            if (currentState == State.REFRESH) {
                refreshFile = 0;
                byte[] fileName = new byte[nextLength];
                buf.readBytes(fileName);
                System.out.println("STATE: Refresh string - " + new String(fileName, StandardCharsets.UTF_8));
                FilesListMessage flm = FilesListMessage.fromJson(new String(fileName, StandardCharsets.UTF_8));
                MainActivity.cloudFilesList.addAll(flm.files);
                TimeUnit.MILLISECONDS.sleep(200);
                mainActivity.runOnUiThread(() -> mainActivity.reloadCloudTable());
                currentState = State.IDLE;
                break;
            }

            if (currentState == State.END) {
                currentState = State.IDLE;
                if (MainActivity.directory.toString().endsWith("Download")) mainActivity.runOnUiThread(() -> mainActivity.refreshLocalFilesList("path"));
                else mainActivity.runOnUiThread(() -> mainActivity.refreshLocalFilesList("getChild"));
                break;
            }
        }
        if (buf.readableBytes() == 0) {
            buf.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
