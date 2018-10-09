package com.cheesycode.meldhet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.cheesycode.meldhet.IntroActivity.setWindowFlag;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;
    SharedPreferences prefs = null;

    private String mCurrentPhotoPath;
    private String currentIssueType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, true);

        if(intent.getData()!=null)
                onImageOrTextClick(intent.getData().toString());
    }
    public void onImageOrTextClick(View v){
        onImageOrTextClick((Object) v);
    }
    private void onImageOrTextClick(Object view) {
        if(view instanceof TextView){
            currentIssueType = ((TextView) view).getText().toString();
        }
        else if(view instanceof ImageView)
        {
            currentIssueType = ((ImageView) view).getContentDescription().toString();
        }
        if(view instanceof String){
            currentIssueType = view.toString();
        }

        if(currentIssueType.equals(getResources().getString(R.string.item8))){
            prefs = getSharedPreferences("com.cheesycode.MeldHet", MODE_PRIVATE);
            prefs.edit().putBoolean("firstrun", true).apply();

            Intent i = new Intent(this,IntroActivity.class);
            this.startActivity(i);
            return;
        }

        if(currentIssueType.equals(getResources().getString(R.string.item9))){
            Intent i = new Intent(this,ChatActivity.class);
            this.startActivity(i);
            return;
        }

        if(currentIssueType.equals(getResources().getString(R.string.item7))){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Wat voor probleem wil je melden");

            final EditText input = new EditText(this);
            input.setSingleLine();
            FrameLayout container = new FrameLayout(this);
            FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialoguemargin);
            params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialoguemargin);
            params.topMargin = getResources().getDimensionPixelSize(R.dimen.dialoguemargin);
            params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.dialoguemargin);
            input.setLayoutParams(params);
            container.addView(input);

            builder.setView(container);

            builder.setPositiveButton("Melden", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                   onImageOrTextClick(input.getText().toString());
                }
            });
            builder.setNegativeButton("Annuleren", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
            return;
        }


        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.d("FILEIO", ex.toString());
                Toast.makeText(this, "Failed to create picture, please reset permissions" , Toast.LENGTH_LONG).show();
            }
            if (photoFile != null) {

                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.cheesycode.meldhet.fileprovider",
                        photoFile);
                Toast.makeText(this, R.string.fotoinstructie , Toast.LENGTH_LONG).show();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
                Intent intent = new Intent(this, UploadActivity.class);
                intent.putExtra("filepath", mCurrentPhotoPath);
                intent.putExtra("issueType", currentIssueType);
                this.startActivity(intent);
            }
        }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMAN).format(new Date());
        String imageFileName = "MeldingOp" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir);
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
}

