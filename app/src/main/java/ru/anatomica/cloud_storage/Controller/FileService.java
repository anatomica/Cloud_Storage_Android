package ru.anatomica.cloud_storage.Controller;

import android.app.AlertDialog;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import ru.anatomica.cloud_storage.MainActivity;
import ru.anatomica.cloud_storage.Protocol.ProtocolFiles;
import ru.anatomica.cloud_storage.R;

public class FileService {

    private static File directory = new File(Environment.getExternalStorageDirectory().toString());
    private static final String HOST_ADDRESS_PROP = "server.address";
    private static final String HOST_PORT_PROP = "server.port";
    static String hostAddress;
    static int hostPort;

    private AlertDialog.Builder alertDialog;
    private MainActivity mainActivity;
    private Callback authOkCallback;
    private Callback refreshCallback;
    private Callback reloadTableCallback;

    public FileService(MainActivity mainActivity, Callback authOkCallback, Callback refreshCallback, Callback reloadTableCallback) {
        this.mainActivity =  mainActivity;
        this.authOkCallback = authOkCallback;
        this.reloadTableCallback = reloadTableCallback;
        this.refreshCallback = refreshCallback;
        initialize();
    }

    private void initialize() {
        readProperties();
        authOkCallback = () -> {
            mainActivity.runOnUiThread(this::loginToApp);
            mainActivity.saveLoginPass();
            TimeUnit.MILLISECONDS.sleep(200);
            mainActivity.runOnUiThread(() -> mainActivity.refreshLocalFilesList("path"));
            TimeUnit.MILLISECONDS.sleep(200);
            mainActivity.runOnUiThread(() -> mainActivity.refreshCloudFilesList());
            System.out.println("Вход выполнен!");
        };
        refreshCallback = () -> {
            if (MainActivity.directory.toString().endsWith("Download")) mainActivity.runOnUiThread(() -> mainActivity.refreshLocalFilesList("path"));
            else mainActivity.runOnUiThread(() -> mainActivity.refreshLocalFilesList("getChild"));
            System.out.println("Обновлено!");
        };
        reloadTableCallback = () -> {
            mainActivity.runOnUiThread(() -> mainActivity.reloadLocalTable());
            TimeUnit.MILLISECONDS.sleep(200);
            mainActivity.runOnUiThread(() -> mainActivity.reloadCloudTable());
            System.out.println("Перерисовано!");
        };
    }

    private void readProperties() {
        Properties serverProperties = new Properties();
        try {
            serverProperties.load(mainActivity.serverAddressProp);
            hostAddress = serverProperties.getProperty(HOST_ADDRESS_PROP);
            hostPort = Integer.parseInt(serverProperties.getProperty(HOST_PORT_PROP));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read application.properties file", e);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port value", e);
        }
    }

    public void startConnectionToServer() throws InterruptedException {
        CountDownLatch networkStarter = new CountDownLatch(1);
        new Thread(() -> Network.getInstance().start(mainActivity, authOkCallback, refreshCallback, reloadTableCallback, networkStarter)).start();
        networkStarter.await();
    }

    public void receiveFile(String filename) {
        MainActivity.visibleProgress();
        ProtocolFiles.receiveFile(Paths.get(filename), Network.getInstance().getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                MainActivity.invisibleProgress();
                future.cause().printStackTrace();
            }
            if (future.isSuccess()) {
                System.out.println("Команда на получение передана");
            }
        });
    }

    public void sendFile(String fileName, String download) {
        try {
            if (fileName != null) {
                String pathToFile;
                if (download.equals("Download")) pathToFile = MainActivity.directory + "/" + fileName;
                else if (fileName.startsWith("/storage")) pathToFile = fileName;
                else if (fileName.startsWith("/external_files")) pathToFile = directory + fileName.substring(15);
                else if (fileName.startsWith("content://com.estrongs.files")) pathToFile = fileName.substring(28);
                else if (fileName.startsWith("file://")) pathToFile = fileName.substring(7);
                else if (fileName.startsWith("/raw")) pathToFile = fileName.substring(4);
                else {
                    messageToService(mainActivity.getString(R.string.help));
                    return;
                }
                MainActivity.visibleProgress();
                ProtocolFiles.sendFile(Paths.get(pathToFile), Network.getInstance().getCurrentChannel(), future -> {
                    if (!future.isSuccess()) {
                        MainActivity.invisibleProgress();
                        future.cause().printStackTrace();
                    }
                    if (future.isSuccess()) {
                        MainActivity.invisibleProgress();
                        ProtocolFiles.progress = 0;
                        System.out.println("Файл успешно передан");
                        messageToService(mainActivity.getString(R.string.successSend));
                        TimeUnit.MILLISECONDS.sleep(300);
                        mainActivity.runOnUiThread(() -> mainActivity.refreshCloudFilesList());
                    }
                });
            } else messageToService(mainActivity.getString(R.string.help));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteLocalFiles(String filename) {
        try {
            Files.delete(Paths.get(MainActivity.directory + "/" + filename).toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (MainActivity.directory.toString().endsWith("Download")) mainActivity.runOnUiThread(() -> mainActivity.refreshLocalFilesList("path"));
        else mainActivity.runOnUiThread(() -> mainActivity.refreshLocalFilesList("getChild"));
    }

    public void deleteCloudFiles(String filename) {
        ProtocolFiles.deleteFile(Paths.get(filename), Network.getInstance().getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
            }
            if (future.isSuccess()) {
                System.out.println("Команда на удаление передана!");
                TimeUnit.MILLISECONDS.sleep(300);
                mainActivity.runOnUiThread(() -> mainActivity.refreshCloudFilesList());
            }
        });
    }

    public void renameLocalFiles(String oldFilename) {
        alertDialog = new AlertDialog.Builder(mainActivity);
        alertDialog.setTitle("Переименование файла ...");
        alertDialog.setMessage("Введите новое имя:");
        final EditText input = new EditText(mainActivity);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);
        alertDialog.setPositiveButton("Переименовать!", (dialog, which) -> {
            String newFileName = input.getText().toString();
            if (newFileName.split("\\s+").length > 1) {
                messageToService("Имя должно быть без пробелов!");
                return;
            }
            if (newFileName.compareTo("") > 0 && !newFileName.equals("") && !newFileName.startsWith(" ") && newFileName != null) {
                try {
                    Files.move(Paths.get(MainActivity.directory + "/" + oldFilename), Paths.get(MainActivity.directory + "/" + newFileName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (MainActivity.directory.toString().endsWith("Download")) mainActivity.runOnUiThread(() -> mainActivity.refreshLocalFilesList("path"));
                else mainActivity.runOnUiThread(() -> mainActivity.refreshLocalFilesList("getChild"));
            } else messageToService("Пожалуйста, введите желаемое имя!");
        });
        alertDialog.setNegativeButton("Отмена!", (dialog, which) -> dialog.cancel());
        alertDialog.show();
    }

    public void changeToChoose() {
        mainActivity.changeLayout.setVisibility(View.VISIBLE);
        mainActivity.registerLayout.setVisibility(View.INVISIBLE);
        mainActivity.loginLayout.setVisibility(View.INVISIBLE);
        mainActivity.tabLayout.setVisibility(View.INVISIBLE);
        mainActivity.viewPagerLayout.setVisibility(View.INVISIBLE);
        mainActivity.fabGallery.setVisibility(View.INVISIBLE);
        mainActivity.fabES.setVisibility(View.INVISIBLE);
    }

    public void changeToReg() {
        mainActivity.changeLayout.setVisibility(View.INVISIBLE);
        mainActivity.registerLayout.setVisibility(View.VISIBLE);
        mainActivity.loginLayout.setVisibility(View.INVISIBLE);
        mainActivity.tabLayout.setVisibility(View.INVISIBLE);
        mainActivity.viewPagerLayout.setVisibility(View.INVISIBLE);
        mainActivity.fabGallery.setVisibility(View.INVISIBLE);
        mainActivity.fabES.setVisibility(View.INVISIBLE);
    }

    public void changeToLogin() {
        mainActivity.changeLayout.setVisibility(View.INVISIBLE);
        mainActivity.registerLayout.setVisibility(View.INVISIBLE);
        mainActivity.loginLayout.setVisibility(View.VISIBLE);
        mainActivity.tabLayout.setVisibility(View.INVISIBLE);
        mainActivity.viewPagerLayout.setVisibility(View.INVISIBLE);
        mainActivity.fabGallery.setVisibility(View.INVISIBLE);
        mainActivity.fabES.setVisibility(View.INVISIBLE);
    }

    public void loginToApp() {
        mainActivity.changeLayout.setVisibility(View.INVISIBLE);
        mainActivity.registerLayout.setVisibility(View.INVISIBLE);
        mainActivity.loginLayout.setVisibility(View.INVISIBLE);
        mainActivity.tabLayout.setVisibility(View.VISIBLE);
        mainActivity.viewPagerLayout.setVisibility(View.VISIBLE);
        mainActivity.fabGallery.setVisibility(View.VISIBLE);
        mainActivity.fabES.setVisibility(View.VISIBLE);
    }

    public void messageToService(String message) {
        mainActivity.runOnUiThread(() -> mainActivity.serviceMessage(message));
    }

    public void close() throws IOException {
        Network.getInstance().stop();
        System.exit(0);
    }
}