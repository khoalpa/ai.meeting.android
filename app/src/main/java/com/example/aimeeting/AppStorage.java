package com.example.aimeeting;

import android.content.Context;

import java.io.File;

final class AppStorage {
    private AppStorage() {
    }

    static File externalFilesDir(Context context) {
        File external = context.getExternalFilesDir(null);
        return external != null ? external : context.getFilesDir();
    }

    static File modelDir(Context context, String type) {
        return new File(externalFilesDir(context), "models/" + type);
    }

    static File modelsRoot(Context context) {
        return new File(externalFilesDir(context), "models");
    }
}
