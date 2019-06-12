package com.redudant.newsreader;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
{

    ListView listView;

    ArrayList<String> titles = new ArrayList<>();

    ArrayList<String> content = new ArrayList<>();

    ArrayAdapter arrayAdapter;

    String articleId;

    //variable global ditampilkan sesuai API
    String articleTitle;

    String articleURL;

    //database
    SQLiteDatabase articlesDB;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.listView);

        arrayAdapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, titles);

        listView.setAdapter(arrayAdapter);

        //creat
        articlesDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);

        //create table
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, content VARCHAR)");

        DownloadTask task = new DownloadTask();

        updateListView();

        try
        {

            task.execute(SettingApi.IP_TOPSTORIES);
        }

        catch (Exception e)

        {
            e.printStackTrace();
        }

    }

    //
    public void updateListView()
    {
        Cursor c = articlesDB.rawQuery("SELECT * FROM articles", null);

        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");

        if (c.moveToFirst()) {
            titles.clear();
            content.clear();

            do {
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
            } while (c.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }
    }

    public class DownloadTask extends AsyncTask<String, Void, String>
    {

        @Override
        protected String doInBackground(String... strings)
        {

            String result = "";

            URL url;

            HttpURLConnection urlConnection = null;

            try
            {
                url = new URL(strings[0]);

                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = urlConnection.getInputStream();

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);



                int data = inputStreamReader.read();

                while (data != -1)
                {
                    char current = (char) data;

                    result += current;

                    data = inputStreamReader.read();
                }

                //Log.i("URLContent", result);

                //Start jsonArray
                JSONArray jsonArray = new JSONArray(result);

                //dibataskan 20 item yang tampil
                int numberOfitems = 20;

                //melakukan pemeriksaan untuk menjaga2 jika terjadi crash (yang tampil tidak selalu 20)
                if (jsonArray.length() < 20)
                {
                    numberOfitems = jsonArray.length();
                }

                //delete article
                articlesDB.execSQL("DELETE FROM articles");

                for (int i = 0; i < jsonArray.length(); i++) {
                    //Log.i("JsonItem", jsonArray.getString(i));

                    //create new String articleId
                    articleId = jsonArray.getString(i);

                    //cara ke 1 dalam class yang sama
                    //url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");

                    //cara ke 2 harus varible nya public static String articleId dan bisa bebeda class
                    //MainActivity.articleId = jsonArray.getString(i);

                    //cara ke 3 bisa bebeda class
                    url = new URL(SettingApi.IP_ITEM + articleId + ".json?print=pretty");


                    urlConnection = (HttpURLConnection) url.openConnection();

                    inputStream = urlConnection.getInputStream();

                    inputStreamReader = new InputStreamReader(inputStream);

                    data = inputStreamReader.read();

                    String articleInfo = "";

                    while (data != -1) {
                        char current = (char) data;

                        articleInfo += current;

                        data = inputStreamReader.read();
                    }

                    //Log.i("ArticleInfo", articleInfo);

                    JSONObject jsonObject = new JSONObject(articleInfo);

                    //Log.i("infoArticle", jsonObject.toString());

                    //jika !jsonObject nya ada jadi bukan tidak ada, jika ada tampabhkan ! didapannya
                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url"))
                    {

                        //variable global
                        //title
                        articleTitle = jsonObject.getString("title");

                        //url
                        articleURL = jsonObject.getString("url");

                        //Log.i("Infoarticle", articleTitle + articleURL);

                        //
                        url = new URL(articleURL);

                        urlConnection = (HttpURLConnection) url.openConnection();

                        inputStream = urlConnection.getInputStream();

                        inputStreamReader = new InputStreamReader(inputStream);

                        data = inputStreamReader.read();

                        String articleContent = "";

                        while (data != -1)
                        {
                            char current = (char) data;

                            articleInfo += current;

                            data = inputStreamReader.read();
                        }

                        Log.i("articleContent", articleContent);

                        //query
                        String sql = "INSERT INTO articles (articleId, title, content) VALUES (?, ?, ?)";

                        SQLiteStatement statement = articlesDB.compileStatement(sql);

                        statement.bindString(1, articleId);
                        statement.bindString(2, articleTitle);
                        statement.bindString(3, articleContent);

                        statement.execute();
                    }

                }

            }

            catch (MalformedURLException e)

            {
                e.printStackTrace();
            }

            catch (IOException e)

            {
                e.printStackTrace();

            } catch (JSONException e)
            {

                e.printStackTrace();

            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
    }
}
