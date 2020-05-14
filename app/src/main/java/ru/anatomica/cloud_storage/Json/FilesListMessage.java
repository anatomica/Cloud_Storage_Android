package ru.anatomica.cloud_storage.Json;

import com.google.gson.Gson;
import java.util.List;
import ru.anatomica.cloud_storage.File.FileAbout;

public class FilesListMessage {

    public List<FileAbout> files;
    public String from;

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static FilesListMessage fromJson(String json) {
        return new Gson().fromJson(json, FilesListMessage.class);
    }
}
