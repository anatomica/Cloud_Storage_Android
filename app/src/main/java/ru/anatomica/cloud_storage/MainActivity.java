package ru.anatomica.cloud_storage;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import ru.anatomica.cloud_storage.Controller.FileService;
import ru.anatomica.cloud_storage.Controller.Network;
import ru.anatomica.cloud_storage.File.*;
import ru.anatomica.cloud_storage.Handlers.ProtocolHandler;
import ru.anatomica.cloud_storage.Json.AuthMessage;
import ru.anatomica.cloud_storage.Json.Message;
import ru.anatomica.cloud_storage.Json.RegisterMessage;
import ru.anatomica.cloud_storage.Protocol.ProtocolFiles;
import ru.anatomica.cloud_storage.ui.main.SectionsPagerAdapter;

public class MainActivity extends AppCompatActivity {

    public CoordinatorLayout mainLayout;
    public ConstraintLayout changeLayout;
    public ConstraintLayout registerLayout;
    public ConstraintLayout loginLayout;
    public ViewPager viewPagerLayout;
    public FloatingActionButton fabGallery;
    public FloatingActionButton fabES;
    public TabLayout tabLayout;
    public ImageView imageView;

    public Button exit;
    public Button sendAuth;
    public EditText loginField;
    public EditText passField;
    public EditText nicknameReg;
    public EditText loginReg;
    public EditText passReg;
    public static ProgressBar progressBar;
    public static ProgressBar progressBar2;

    public FileOutputStream fosLogin;
    public FileInputStream fisLogin;
    public FileOutputStream fosPasswd;
    public FileInputStream fisPasswd;

    public static final int PICK_IMAGE = 1;
    private static final int PERMISSION_REQUEST_CODE = 123;
    public static File directory = new File(Environment.getExternalStorageDirectory(), "Download");
    public static ArrayList<FileAbout> localFilesList;
    public static ArrayList<FileAbout> cloudFilesList;
    public static ListView localListView;
    public static ListView cloudListView;
    public static Context context;

    public AlertDialog.Builder alertDialog;
    public InputStream serverAddressProp;
    private FileService fileService;

    protected String login = "Login.txt";
    protected String password = "Password.txt";
    protected String loginTextOnLogin;
    protected String passwordTextOnLogin;
    public static String pathToFileOfUser;
    private String rememberDirName;
    public String selectedFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy threadPolicy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.VmPolicy vmPolicy = new StrictMode.VmPolicy.Builder().build();
        StrictMode.setThreadPolicy(threadPolicy);
        StrictMode.setVmPolicy(vmPolicy);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        viewPagerLayout = findViewById(R.id.view_pager);
        viewPagerLayout.setAdapter(sectionsPagerAdapter);
        tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPagerLayout);
        fabGallery = findViewById(R.id.fabGallery);
        fabGallery.setOnClickListener(this::onClickFabGallery);
        fabES = findViewById(R.id.fabES);
        fabES.setOnClickListener(this::onClickFabES);

        MainActivity.context = getApplicationContext();
        mainLayout = findViewById(R.id.activity_main);
        changeLayout = findViewById(R.id.activity_change);
        registerLayout = findViewById(R.id.activity_register);
        loginLayout = findViewById(R.id.activity_login);
        imageView = findViewById(R.id.imageView);

        loginField = findViewById(R.id.login);
        loginField.setTextColor(Color.WHITE);
        passField = findViewById(R.id.password);
        passField.setTextColor(Color.WHITE);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setProgress(100, true);
        progressBar.setVisibility(View.INVISIBLE);
        progressBar2 = findViewById(R.id.progressBar2);
        progressBar2.setVisibility(View.INVISIBLE);

        nicknameReg = findViewById(R.id.nicknameReg);
        nicknameReg.setTextColor(Color.WHITE);
        loginReg = findViewById(R.id.loginReg);
        loginReg.setTextColor(Color.WHITE);
        passReg = findViewById(R.id.passwordReg);
        passReg.setTextColor(Color.WHITE);

        exit = findViewById(R.id.btn_exit);
        sendAuth = findViewById(R.id.btn_auth);

        initialize();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // shutdown();
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();
        if (registerLayout.getVisibility() == View.VISIBLE) {
            fileService.changeToChoose();
            return;
        }
        if (loginLayout.getVisibility() == View.VISIBLE) {
            fileService.changeToChoose();
            return;
        }
        if (changeLayout.getVisibility() == View.VISIBLE) {
            System.exit(0);
            return;
        }
        if (imageView.getVisibility() == View.VISIBLE) {
            imageView.setVisibility(View.INVISIBLE);
            return;
        }
        if (tabLayout.getVisibility() == View.VISIBLE) {
            shutdown();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_about:
                about();
                break;
            case R.id.btn_logout:
                logout();
                break;
            case R.id.btn_exit:
                shutdown();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onClick(View view) throws InterruptedException {
        switch (view.getId()) {
            case R.id.btn_register:
                fileService.changeToReg();
                break;
            case R.id.btn_login:
                fileService.changeToLogin();
                break;
            case R.id.btn_reg:
                register();
                break;
            case R.id.btn_auth:
                sendAuth();
                break;
        }
    }

    private void initialize() {
        try {
            localFilesList = new ArrayList<>();
            cloudFilesList = new ArrayList<>();
            serverAddressProp = getAssets().open("application.properties");
            this.fileService = new FileService(this, null, null, null);
            if (!hasPermissions()) requestPermissionWithRationale();
            auth();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshCloudFilesList() {
        cloudFilesList.clear();
        ProtocolFiles.refreshFile(pathToFileOfUser, Network.getInstance().getCurrentChannel());
    }

    public void refreshLocalFilesList(String whatPath) {
        try {
            localFilesList.clear();
            File parentDir = directory.getParentFile();
            if (whatPath.equals("getChild") && parentDir != null) {
                localFilesList.add(new FileAbout(parentDir));
                localFilesList.get(0).setName(String.format(getString(R.string.parentFolder), parentDir.getName()));
                rememberDirName = localFilesList.get(0).getName();
            }
            localFilesList.addAll(Files.list(Paths.get(directory.toString())).map(Path::toFile).sorted().map(FileAbout::new).collect(Collectors.toList()));
            reloadLocalTable();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadLocalTable() {
        List<String> localFileName = new ArrayList<>();
        for (int i = 0; i < localFilesList.size(); i++) {
            String tmp = localFilesList.get(i).getName();
            localFileName.add(tmp);
        }
        ArrayAdapter<String> localAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_expandable_list_item_1, localFileName);
        localListView.setAdapter(localAdapter);
        localListView.setOnItemClickListener((parent, view, position, id) -> {
            selectedFile = localListView.getAdapter().getItem(position).toString();
            workWithFileOrDir();
        });
        localListView.setOnItemLongClickListener((parent, view, position, id) -> {
            selectedFile = localListView.getAdapter().getItem(position).toString();
            localFileMenu();
            return true;
        });
    }

    public void reloadCloudTable() {
        List<String> cloudFileName = new ArrayList<>();
        for(int i = 0; i < cloudFilesList.size(); i++) {
            String tmp = cloudFilesList.get(i).getName();
            cloudFileName.add(tmp);
        }
        ArrayAdapter<String> cloudAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_expandable_list_item_1, cloudFileName);
        cloudListView.setAdapter(cloudAdapter);
        cloudListView.setOnItemLongClickListener((parent, view, position, id) -> {
            selectedFile = cloudListView.getAdapter().getItem(position).toString();
            cloudFileMenu();
            return true;
        });
    }

    private void workWithFileOrDir() {
        File parentDir = directory;
        if (directory.getParentFile() != null) parentDir = directory.getParentFile();
        boolean isDirectory;
        if (selectedFile.equals(rememberDirName)) isDirectory = new File(parentDir.toString()).isDirectory();
        else isDirectory = new File(directory + "/" + selectedFile).isDirectory();
        boolean isFile = new File(directory + "/" + selectedFile).isFile();
        if (isDirectory) {
            if (selectedFile.equals(rememberDirName)) {
                directory = directory.getParentFile();
                if (MainActivity.directory.toString().endsWith("Download")) refreshLocalFilesList("path");
                else refreshLocalFilesList("getChild");
            } else {
                directory = new File(directory + "/" + selectedFile);
                runOnUiThread(() -> this.refreshLocalFilesList("getChild"));
            }
        }
        else if (isFile) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("*/*");
            Uri pathToFile = Uri.fromFile(new File(directory + "/" + selectedFile));
            try {
                if (pathToFile.toString().endsWith(".jpg") || pathToFile.toString().endsWith(".jpeg")) {
                    int galleryRequestCode = 41;
                    Intent galleryIntent = new Intent(Intent.ACTION_VIEW);
                    galleryIntent.setPackage("com.miui.gallery");
                    galleryIntent.setDataAndTypeAndNormalize(pathToFile, "image/*");
                    if (galleryIntent.resolveActivity(getPackageManager()) != null) startActivityForResult(galleryIntent, galleryRequestCode);
                    else {
                        imageView.setVisibility(View.VISIBLE);
                        imageView.setImageBitmap(MediaStore.Images.Media.getBitmap(this.getContentResolver(), pathToFile));
                    } return;
                }
                if (pathToFile.toString().endsWith(".mp4") || pathToFile.toString().endsWith(".avi")) {
                    int vlcRequestCode = 42;
                    Intent vlcIntent = new Intent(Intent.ACTION_VIEW);
                    vlcIntent.setPackage("org.videolan.vlc");
                    vlcIntent.setDataAndTypeAndNormalize(pathToFile, "video/*");
                    if (vlcIntent.resolveActivity(getPackageManager()) != null) startActivityForResult(vlcIntent, vlcRequestCode);
                    else {
                        int galleryRequestCode = 41;
                        Intent galleryIntent = new Intent(Intent.ACTION_VIEW);
                        galleryIntent.setPackage("com.miui.gallery");
                        galleryIntent.setDataAndTypeAndNormalize(pathToFile, "video/*");
                        if (galleryIntent.resolveActivity(getPackageManager()) != null) startActivityForResult(galleryIntent, galleryRequestCode);
                        else {
                            imageView.setVisibility(View.VISIBLE);
                            imageView.setImageBitmap(MediaStore.Images.Media.getBitmap(this.getContentResolver(), pathToFile));
                        } return;
                    }
                } else {
                    shareIntent.putExtra(Intent.EXTRA_STREAM, pathToFile);
                    startActivity(shareIntent);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void onClickFabGallery(View view) {
        Intent getMiuiContent = new Intent(Intent.ACTION_GET_CONTENT);
        getMiuiContent.setPackage("com.miui.gallery");
        getMiuiContent.setType("*/*");
        if (getMiuiContent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(Intent.createChooser(getMiuiContent, "Select Picture"), PICK_IMAGE);
        else {
            Intent getContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
            getContentIntent.setType("*/*");
            if (getContentIntent.resolveActivity(getPackageManager()) != null)
                startActivityForResult(Intent.createChooser(getContentIntent, "Select Picture"), PICK_IMAGE);
        }
    }

    private void onClickFabES(View view) {
        Intent getEsContent = new Intent(Intent.ACTION_GET_CONTENT);
        getEsContent.setPackage("com.estrongs.android.pop");
        getEsContent.setType("*/*");
        if (getEsContent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(Intent.createChooser(getEsContent, "Select Picture"), PICK_IMAGE);
        else {
            Intent getContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
            getContentIntent.setType("*/*");
            if (getContentIntent.resolveActivity(getPackageManager()) != null)
                startActivityForResult(Intent.createChooser(getContentIntent, "Select Picture"), PICK_IMAGE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) fileService.sendFile(getRealPathFromUri(this, uri),"Not");
                return;
            }
        }
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // makeFolder();
        }
    }

    public static String getRealPathFromUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void localFileMenu() {
        File parentDir = directory;
        if (directory.getParentFile() != null) parentDir = directory.getParentFile();
        boolean isDirectory;
        if (selectedFile.equals(rememberDirName)) isDirectory = new File(parentDir.toString()).isDirectory();
        else isDirectory = new File(directory + "/" + selectedFile).isDirectory();
        boolean isFile = new File(directory + "/" + selectedFile).isFile();
        AtomicInteger number = new AtomicInteger();
        String[] choose = {getString(R.string.sendFile), getString(R.string.renameFile), getString(R.string.deleteFile)};
        choose[0] = String.format(getString(R.string.sendMod), selectedFile);
        alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(R.string.action);
        alertDialog.setSingleChoiceItems(choose, -1, (dialog, item) -> {
            if (isFile && choose[item].equals(String.format(getString(R.string.sendMod), selectedFile))) number.set(0);
            if (isDirectory && choose[item].equals(String.format(getString(R.string.sendMod), selectedFile))) number.set(0);
            if (choose[item].equals(getString(R.string.renameFile))) number.set(1);
            if (choose[item].equals(getString(R.string.deleteFile))) number.set(2);
        });
        alertDialog.setPositiveButton("OK", (dialog, item) -> {
            if (isFile && number.get() == 0) fileService.sendFile(selectedFile, "Download");
            if (isDirectory && number.get() == 0) serviceMessage(getString(R.string.notSend));
            if (number.get() == 1) fileService.renameLocalFiles(selectedFile);
            if (number.get() == 2) fileService.deleteLocalFiles(selectedFile);
        });
        alertDialog.setNegativeButton(getString(R.string.cancel), (dialog, item) ->
                Toast.makeText(getApplicationContext(), getString(R.string.cancel), Toast.LENGTH_SHORT).show());
        alertDialog.setCancelable(true);
        alertDialog.show();
    }

    private void cloudFileMenu() {
        AtomicInteger number = new AtomicInteger();
        String[] choose = {getString(R.string.sendFile), getString(R.string.renameFile), getString(R.string.deleteFile)};
        choose[0] = String.format(getString(R.string.download), selectedFile);
        alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(R.string.action);
        alertDialog.setSingleChoiceItems(choose, -1, (dialog, item) -> {
            if (choose[item].equals(String.format(getString(R.string.download), selectedFile))) number.set(0);
            if (choose[item].equals(getString(R.string.renameFile))) number.set(1);
            if (choose[item].equals(getString(R.string.deleteFile))) number.set(2);
        });
        alertDialog.setPositiveButton("OK", (dialog, item) -> {
            if (number.get() == 0) fileService.receiveFile(selectedFile);
            if (number.get() == 1) serviceMessage(getString(R.string.function));
            if (number.get() == 2) fileService.deleteCloudFiles(selectedFile);
        });
        alertDialog.setNegativeButton(getString(R.string.cancel), (dialog, item) ->
                Toast.makeText(getApplicationContext(), getString(R.string.cancel), Toast.LENGTH_SHORT).show());
        alertDialog.setCancelable(true);
        alertDialog.show();
    }

    private void register() throws InterruptedException {
        fileService.startConnectionToServer();
        String loginText = loginReg.getText().toString();
        String passwordText = passReg.getText().toString();
        String nicknameText = nicknameReg.getText().toString();
        if (nicknameText.equals("") || passwordText.equals("") ||
                passwordText.equals(" ") || loginText.equals("") || loginText.equals(" ")) {
            serviceMessage(getString(R.string.notEmpty));
            return;
        }
        if (nicknameText.split("\\s+").length > 1 || nicknameText.equals(" ") ||
                nicknameText.startsWith(" ") || nicknameText.endsWith(" ")) {
            serviceMessage(getString(R.string.nickNotEmpty));
            return;
        }
        RegisterMessage msg = new RegisterMessage();
        msg.nickname = nicknameText;
        msg.login = loginText;
        msg.password = passwordText;
        Message regMsg = Message.createRegister(msg);
        ProtocolFiles.regSend(regMsg.toJson(), Network.getInstance().getCurrentChannel());
    }

    public void auth() throws IOException, InterruptedException {
        fileService.startConnectionToServer();
        fosLogin = openFileOutput(login, Context.MODE_APPEND);
        fosPasswd = openFileOutput(password, Context.MODE_APPEND);
        fisLogin = openFileInput(login);
        fisPasswd = openFileInput(password);

        if (fisLogin.available() != 0 && fisLogin != null) {
            findViewById(R.id.activity_change).setVisibility(View.INVISIBLE);

            int available = fisLogin.available();
            byte[] bufLogin = new byte[available];
            fisLogin.read(bufLogin);
            String loginText = new String(bufLogin);
            pathToFileOfUser = loginText + "/";
            fisLogin.close();

            int available1 = fisPasswd.available();
            byte[] bufPass = new byte[available1];
            fisPasswd.read(bufPass);
            String passwdText = new String(bufPass);
            fisPasswd.close();

            AuthMessage msg = new AuthMessage();
            msg.login = loginText;
            msg.password = passwdText;
            Message authMsg = Message.createAuth(msg);
            ProtocolFiles.authSend(authMsg.toJson(), Network.getInstance().getCurrentChannel());
        }
        fosLogin.close();
        fosPasswd.close();
    }

    public void sendAuth () throws InterruptedException {
        fileService.startConnectionToServer();
        loginTextOnLogin = loginField.getText().toString();
        passwordTextOnLogin = passField.getText().toString();
        pathToFileOfUser = loginField.getText().toString() + "/";
        AuthMessage msg = new AuthMessage();
        msg.login = loginTextOnLogin;
        msg.password = passwordTextOnLogin;
        Message authMsg = Message.createAuth(msg);
        ProtocolFiles.authSend(authMsg.toJson(), Network.getInstance().getCurrentChannel());
    }

    public void saveLoginPass() {
        try {
            fosLogin = openFileOutput(login, Context.MODE_APPEND);
            fosPasswd = openFileOutput(password, Context.MODE_APPEND);
            fisLogin = openFileInput(login);
            if (fisLogin.available() == 0 && loginTextOnLogin != null && !loginTextOnLogin.equals("")) {
                fosLogin = openFileOutput(login, Context.MODE_PRIVATE);
                fosPasswd = openFileOutput(password, Context.MODE_PRIVATE);
                fosLogin.write(loginTextOnLogin.getBytes());
                fosPasswd.write(passwordTextOnLogin.getBytes());
            }
            fosLogin.close();
            fosPasswd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logout() {
        try {
            fosLogin = openFileOutput(login, Context.MODE_PRIVATE);
            fosPasswd = openFileOutput(password, Context.MODE_PRIVATE);
            fosLogin.write("".getBytes());
            fosPasswd.write("".getBytes());
            fosLogin.close();
            fosPasswd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        shutdown();
    }

    public void logoutAfterReg() throws InterruptedException {
        TimeUnit.SECONDS.sleep(5);
        logout();
    }

    public void shutdown() {
        try {
            fileService.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void about() {
        alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle(R.string.AboutApp);
        alertDialog.setMessage(R.string.Version);
        // alertDialog.setIcon(R.drawable.ic_stat_ic_notification);
        alertDialog.setPositiveButton("ОК", (dialog, which) -> { dialog.cancel();});
        alertDialog.show();
    }

    public void serviceMessage(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    public boolean hasPermissions() {
        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        for (String perms : permissions) {
            int res = checkCallingOrSelfPermission(perms);
            if (!(res == PackageManager.PERMISSION_GRANTED)) return false;
        }
        return true;
    }

    public void requestPermissionWithRationale() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            final String message = "Storage permission is needed to show files count";
            Snackbar.make(MainActivity.this.findViewById(R.id.activity_main), message, Snackbar.LENGTH_LONG)
                    .setAction("GRANT", view -> requestPerms()).show();
        } else {
            requestPerms();
        }
    }

    private void requestPerms() {
        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:ru.anatomica.cloud_storage")));
        requestPermissions(permissions, PERMISSION_REQUEST_CODE);
    }

    public static void visibleProgress() {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(100, true);
    }

    public static void invisibleProgress() {
        progressBar.setVisibility(View.INVISIBLE);
    }

    public void startProgressBar() {
        progressBar2.setVisibility(View.VISIBLE);
        progressBar2.setMax(Integer.parseInt(String.valueOf(ProtocolHandler.fileLength)));
        Thread progress = new Thread(() -> {
            while (true) {
                if (ProtocolHandler.receivedFileLength < ProtocolHandler.fileLength) {
                    progressBar2.setProgress(Integer.parseInt(String.valueOf(ProtocolHandler.receivedFileLength)));
                    try { TimeUnit.MILLISECONDS.sleep(100); }
                    catch (InterruptedException e) {
                        serviceMessage(e.getMessage());
                    }
                }
                if (ProtocolHandler.progress == 0) {
                    progressBar2.setVisibility(View.INVISIBLE);
                    break;
                }
            }
        });
        progress.start();
    }

    public static void startProgressBar(long staticLength) {
        progressBar2.setVisibility(View.VISIBLE);
        progressBar2.setMax(Integer.parseInt(String.valueOf(staticLength)));
        int length = (Integer.parseInt(String.valueOf(staticLength))) / 10;
        final int[] newValue = {length};
        Thread progress = new Thread(() -> {
            while (true) {
                if (0 < staticLength) {
                    progressBar2.setProgress(newValue[0]);
                    try { TimeUnit.MILLISECONDS.sleep(1000);
                        newValue[0] += length;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (ProtocolFiles.progress == 0) {
                    progressBar2.setVisibility(View.INVISIBLE);
                    break;
                }
            }
        });
        progress.start();
    }

}