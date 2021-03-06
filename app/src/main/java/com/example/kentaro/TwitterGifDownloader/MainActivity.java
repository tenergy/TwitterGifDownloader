package com.example.kentaro.TwitterGifDownloader;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSetText(intent);
            }
        }


        Button download = (Button) findViewById(R.id.download);
        download.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                EditText twitterUrl = (EditText) findViewById(R.id.UrleditText);
                String twitterId = twitterUrl.getText().toString().split("/")[5];

                new DownloadTask().execute(twitterId);

            }
        });
    }

    private static List<String> extractUrls(String input)
    {
        List<String> result = new ArrayList<String>();

        String[] words = input.split("\\s+");


        Pattern pattern = Patterns.WEB_URL;
        for(String word : words)
        {
            if(pattern.matcher(word).find())
            {
                if(!word.toLowerCase().contains("http://") && !word.toLowerCase().contains("https://"))
                {
                    word = "http://" + word;
                }
                result.add(word);
            }
        }

        return result;
    }

    void handleSetText(Intent intent){
        EditText text = (EditText) findViewById(R.id.UrleditText);
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        String list = extractUrls(sharedText).toString();
        String string = list.substring(1, list.length()).split("[?]")[0];

        if (sharedText != null) {
            text.setText(string);
        }
    }

    void deleteFiles(String folder, String ext)
    {
        File dir = new File(folder);
        if (!dir.exists())
            return;
        File[] files = dir.listFiles(new GenericExtFilter(ext));
        for (File file : files)
        {
            if (!file.isDirectory())
            {
                boolean result = file.delete();
                Log.d("TAG", "Deleted:" + result);
            }
        }
    }

    public  boolean haveStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.e("Permission error","You have permission");
                return true;
            } else {

                Log.e("Permission error","You have asked for permission");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //you dont need to worry about these stuff below api level 23
            Log.e("Permission error","You already have the permission");
            return true;
        }
    }

    private String convertToGif(String mediaId){

        String charset = "UTF-8";
        String baseUrl = "http://upload.giphy.com/v1/gifs";
        String gifId = null;

        File uploadFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + mediaId + ".mp4");

        try{
            MultipartUtility multipart = new MultipartUtility(baseUrl, charset);
            multipart.addFormField("api_key", "dc6zaTOxFJmzC");
            multipart.addFilePart("file", uploadFile);

            List<String> response = multipart.finish();

            JSONObject jsonobject = new JSONObject(response.get(2));
            gifId = jsonobject.getJSONObject("data").getString("id");


        }catch (IOException e){
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return gifId;
    }


    private String getMediaId(String tweetId){

        String mediaId = null;
        Status status;
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey("XqNwYplxMeHiDEl2iSNhKVB7J")
                .setOAuthConsumerSecret("a4rtEW9M7E0AuxeGmpfisDf4aTrjUPhgT1yyGFVBnGgTFoJDXy")
                .setOAuthAccessToken("796494214154649600-OjrAVh8SweH7aagpO48cnJmLksOhEg4")
                .setOAuthAccessTokenSecret("lTvNGcqdhgar7xEdYFb7Nzvufg9iaUjdCNClegJFnh59P");

        TwitterFactory tf = new TwitterFactory(cb.build());
        Twitter twitter = tf.getInstance();

        try {
            status = twitter.showStatus(Long.parseLong(tweetId));
            String[] url = status.getMediaEntities()[0].getMediaURL().split("/");
            mediaId = url[4].substring(0, url[4].length()-4);
        } catch (TwitterException e) {
//            e.printStackTrace();
        }

        return mediaId;
    }

    private String saveMp4(String tweetId) throws Exception{

        String mediaUrl = "https://video.twimg.com/tweet_video/";
        String mediaId = getMediaId(tweetId);
        mediaUrl += mediaId + ".mp4";
        Uri uri = Uri.parse(mediaUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, mediaId + ".mp4");
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);

        return mediaId;

    }

    private void saveGif(String id) throws Exception{

        deleteFiles("/storage/emulated/0/Download/", "mp4");

        Uri uri = Uri.parse(buildURL(id));
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, id + ".gif");
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);

    }

    private static String buildURL(String id){return "http://i.giphy.com/" + id + ".gif";}


    private class GenericExtFilter implements FilenameFilter {

        private String ext;

        public GenericExtFilter(String ext) {
            this.ext = ext;
        }

        public boolean accept(File dir, String name) {
            return (name.endsWith(ext));
        }
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            String twitterId = params[0];
            String fileName;
            String gifId = "";


            try {
                if(haveStoragePermission()){
                    fileName= saveMp4(twitterId);
                    Thread.sleep(1000);
                    gifId = convertToGif(fileName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return gifId;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                saveGif(result);
                Context context = getApplicationContext();
                Toast toast= Toast.makeText(context, "Download Finished", Toast.LENGTH_LONG);
                toast.show();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

}
