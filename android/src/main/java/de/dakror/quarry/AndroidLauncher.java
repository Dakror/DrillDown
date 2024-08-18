package de.dakror.quarry;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.util.DisplayMetrics;
import android.view.DisplayCutout;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidFileHandle;
import com.badlogic.gdx.backends.android.AndroidFiles;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.StreamUtils;

import net.spookygames.gdx.sfx.android.AndroidAudioDurationResolver;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.HashMap;

import de.dakror.common.libgdx.PlatformInterface;

public class AndroidLauncher extends AndroidApplication implements PlatformInterface {
    static HashMap<String, Long> lastModifiedCache = new HashMap<>();

    public class QuarryAndroidFileHandle extends FileHandle {
        DocumentFile file;
        DocumentFile parent;

        QuarryAndroidFileHandle(DocumentFile parent, DocumentFile file, String fileName, Files.FileType type) {
            super(fileName.replace('\\', '/'), type);
            this.file = file;
            this.parent = parent;
        }

        public InputStream read() {
            try {
                return getContentResolver().openInputStream(file.getUri());
            } catch (FileNotFoundException e) {
                throw new GdxRuntimeException("Error reading file: " + file + " (" + type + ")", e);
            }
        }

        public OutputStream write(boolean append) {
            try {
                if (!exists()) {
                    file = parent.createFile("application/octet-stream", name());
                }
                if (!exists()) {
                    throw new RuntimeException("Created file does not exist: " + file);
                }

                return getContentResolver().openOutputStream(file.getUri());
            } catch (FileNotFoundException e) {
                throw new GdxRuntimeException("Error writing file: " + file + " (" + type + ")", e);
            }
        }

        public ByteBuffer map(FileChannel.MapMode mode) {
            throw new GdxRuntimeException("Cannot map an external file: " + this);
        }

        @Override
        public void mkdirs() {
            if (!exists() && parent != null)
                file = parent.createDirectory(name());
        }

        @Override
        public boolean delete() {
            return file.delete();
        }

        @Override
        public void moveTo(FileHandle dest) {
            file.renameTo(dest.name());
        }

        public FileHandle[] list() {
            throw new GdxRuntimeException("Error listing children: " + file + " (" + type + ")");
        }

        public FileHandle[] list(FileFilter filter) {
            throw new GdxRuntimeException("Error listing children: " + file + " (" + type + ")");
        }

        public FileHandle[] list(FilenameFilter filter) {
            throw new GdxRuntimeException("Error listing children: " + file + " (" + type + ")");
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public FileHandle[] list(String suffix) {
            Array<FileHandle> files = new Array<>();

            if (file != null) {
                Uri uri = file.getUri();
                final ContentResolver resolver = getContentResolver();
                final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri,
                        DocumentsContract.getDocumentId(uri));
                Cursor c = null;
                try {
                    c = resolver.query(childrenUri, new String[] {
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    }, null, null, null);
                    while (c.moveToNext()) {
                        final String documentId = c.getString(0);
                        final Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId);
                        String name = c.getString(1);
                        if (name.endsWith(suffix)) {
                            files.add(new QuarryAndroidFileHandle(file,
                                    DocumentFile.fromTreeUri(getApplicationContext(), documentUri), name,
                                    Files.FileType.External));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (c != null)
                        c.close();
                }
            }

            return files.toArray(FileHandle.class);
        }

        public boolean isDirectory() {
            return file != null && file.isDirectory();
        }

        public boolean exists() {
            return file != null && file.exists();
        }

        public long length() {
            try {
                return getContentResolver().openFileDescriptor(file.getUri(), "r").getStatSize();
            } catch (FileNotFoundException e) {
                throw new GdxRuntimeException("Error getting length: " + file + " (" + type + ")", e);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        public long lastModified() {
            if (!exists())
                return 0;
            String id = DocumentsContract.getDocumentId(file.getUri());
            if (!lastModifiedCache.containsKey(id)) {
                final Cursor cursor = getContentResolver().query(
                        DocumentsContract.buildChildDocumentsUriUsingTree(parent.getUri(),
                                DocumentsContract.getDocumentId(parent.getUri())),
                        new String[] {
                                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                DocumentsContract.Document.COLUMN_LAST_MODIFIED
                        }, null, null, null);
                while (cursor.moveToNext()) {
                    lastModifiedCache.put(cursor.getString(0), cursor.getLong(1));
                }
            }

            return lastModifiedCache.get(id);
        }

        public File file() {
            return null;
        }
    }

    public class QuarryAndroidFiles extends AndroidFiles {
        DocumentFile root;

        public QuarryAndroidFiles(DocumentFile root, AssetManager assets, String localpath) {
            super(assets, localpath);
            this.root = root;
        }

        @Override
        public FileHandle external(String path) {
            if (root == null) {
                return new QuarryAndroidFileHandle(null, null, "", FileType.External);
            }

            if (path.startsWith("TheQuarry/")) {
                path = path.substring("TheQuarry/".length());
            }
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            String origPath = path;

            String parent = null;
            DocumentFile parentUri = root;
            int last = path.lastIndexOf("/");
            if (last > -1) {
                parent = path.substring(0, last);
                parentUri = DocumentFile.fromTreeUri(getContext(),
                        Uri.parse(root.getUri().toString() + "%2F" + parent));
            }

            // yeah this is a hack, but so what
            path = root.getUri().toString() + "%2F" + path.replace("/", "%2F");

            return new QuarryAndroidFileHandle(
                    parentUri,
                    DocumentFile.fromTreeUri(getContext(), Uri.parse(path)),
                    last > -1 ? origPath.substring(last + 1) : origPath,
                    FileType.External);
        }
    }

    static final int WRITE_REQUEST_CODE = 0x1233;
    static final int TREE_REQUEST_CODE = 0x1234;

    private Quarry game;

    boolean errorDialogOpen;

    /**
     * left, top, right, bottom
     */
    int[] safeInsets;

    // A handler on the UI thread.
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidAudioDurationResolver.initialize();

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        safeInsets = new int[4];

        mHandler = new Handler();

        SharedPreferences prefs = getSharedPreferences("TheQuarry-Android", MODE_PRIVATE);
        DocumentFile dir = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            String str = prefs.getString("EXTERNAL_URI", null);
            if (str != null) {
                Uri uri = Uri.parse(str);
                dir = DocumentFile.fromTreeUri(this, uri);
                if (!dir.canRead() || !dir.canWrite()) {
                    str = null;
                }
            }

            if (str == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder
                        .setView(getLayoutInflater().inflate(R.layout.external_file_layout, null))
                        .setPositiveButton(R.string.select_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                                        Environment.getExternalStorageDirectory());
                                try {
                                    startActivityForResult(intent, TREE_REQUEST_CODE);
                                } catch (Exception e) {
                                    Toast.makeText(getApplicationContext(), R.string.no_filechooser_found,
                                            Toast.LENGTH_LONG).show();
                                }
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.skip_button, null)
                        .setCancelable(false)
                        .create()
                        .show();
            }
        }

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;
        config.maxSimultaneousSounds = 32;
        game = new Quarry(this, BuildConfig.FLAVOR.equals("full"), BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME,
                false, Build.VERSION.SDK_INT >= Build.VERSION_CODES.R, null);
        initialize(game, config);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            files = new QuarryAndroidFiles(dir, this.getAssets(), this.getFilesDir().getAbsolutePath());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, WRITE_REQUEST_CODE);
                return;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == WRITE_REQUEST_CODE) {
            game.message(Const.MSG_FILE_PERMISSION,
                    grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TREE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                SharedPreferences prefs = getSharedPreferences("TheQuarry-Android", MODE_PRIVATE);
                prefs.edit().putString("EXTERNAL_URI", uri.toString()).commit();
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                ((QuarryAndroidFiles) files).root = DocumentFile.fromTreeUri(this, uri);
            }
        }
    }

    @Override
    public Object message(int messageCode, final Object payload) {
        switch (messageCode) {
            case MSG_EXCEPTION: {
                StringWriter sw = new StringWriter();
                ((Exception) payload).printStackTrace(new PrintWriter(sw));

                if (!errorDialogOpen) {
                    Handler h = new Handler(Looper.getMainLooper());
                    h.post(() -> {
                        final AlertDialog dialog = new AlertDialog.Builder(this).create();
                        dialog.setTitle("Error!");
                        dialog.setMessage(Quarry.Q.i18n.get("ui.error") + "\n\nDetails:\n" + sw.toString());
                        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Copy", (d, which) -> {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboard.setPrimaryClip(ClipData.newPlainText("Drill Down Error", sw.toString()));
                            d.dismiss();
                        });
                        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "OK", (d, which) -> d.dismiss());

                        dialog.show();
                        errorDialogOpen = true;
                        dialog.setOnDismissListener(x -> {
                            errorDialogOpen = false;
                        });
                    });
                }
                break;
            }
            case MSG_BROWSER_INTENT: {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse((String) payload));
                startActivity(browserIntent);
                break;
            }
            case Const.MSG_PADDING:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    DisplayCutout displayCutout = getApplicationWindow().getDecorView().getRootWindowInsets()
                            .getDisplayCutout();
                    if (displayCutout != null) {
                        safeInsets[0] = displayCutout.getSafeInsetLeft();
                        safeInsets[1] = displayCutout.getSafeInsetTop();
                        safeInsets[2] = displayCutout.getSafeInsetRight();
                        safeInsets[3] = displayCutout.getSafeInsetBottom();
                    }
                    safeInsets[1] += 20;
                }

                return safeInsets;
            case Const.MSG_DPI: {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                return Math.max(1, metrics.scaledDensity / 3.0f);
            }
            case Const.MSG_FILE_PERMISSION: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                                WRITE_REQUEST_CODE);
                        return false;
                    }
                }

                return true;
            }
            case Const.MSG_PASTE: {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("Drill Down Blueprint", payload.toString()));
                break;
            }
            case Const.MSG_COPY: {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (!clipboard.hasPrimaryClip())
                    return null;
                if (clipboard.getPrimaryClip().getItemCount() == 0)
                    return null;
                return clipboard.getPrimaryClip().getItemAt(0).coerceToText(this);
            }
            case Const.MSG_SELECT_ROOT: {
                runOnUiThread(new Runnable() {
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(AndroidLauncher.this);
                        builder
                                .setView(getLayoutInflater().inflate(R.layout.external_file_layout, null))
                                .setPositiveButton(R.string.select_button, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                                                Environment.getExternalStorageDirectory());
                                        try {
                                            startActivityForResult(intent, TREE_REQUEST_CODE);
                                        } catch (Exception e) {
                                            Toast.makeText(getApplicationContext(), R.string.no_filechooser_found,
                                                    Toast.LENGTH_LONG).show();
                                        }
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton(R.string.skip_button, null)
                                .setCancelable(false)
                                .create()
                                .show();
                    }
                });
                break;
            }
        }

        return null;
    }
}
