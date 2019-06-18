# News-Reader

### Management File XML
![Screen Shot 2019-06-18 at 17 23 36](https://user-images.githubusercontent.com/43386555/59674581-c6cb4e00-91ed-11e9-8615-b7861a098c44.png)

## XML
### 1. activity_article.xml
    <WebView
        android:id="@+id/webView"
        android:layout_height="match_parent"
        android:layout_width="match_parent">
    </WebView>
    
## 2. activity_main.xml    
    <ListView
        android:id="@+id/listView"
        android:layout_width="match_parent"
        android:layout_height="match_parent">
    </ListView>
    
### Management File Java
![Screen Shot 2019-06-18 at 17 19 08](https://user-images.githubusercontent.com/43386555/59674297-368d0900-91ed-11e9-8a5c-112f274087d8.png)

## Java
### MainActivity.java
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

            //onClick listview
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    Intent intent = new Intent(getApplicationContext(), ArticleActivity.class);
                    intent.putExtra("content", content.get(position));

                    startActivity(intent);

                }
            });
            //end onClick listview

            //creat database
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
    
    
    
### ArticleActivity.java
    public class ArticleActivity extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_article);

            WebView webView = (WebView) findViewById(R.id.webView);

            webView.getSettings().setJavaScriptEnabled(true);
            webView.setWebViewClient(new WebViewClient());

            Intent intent = getIntent();

            webView.loadData(intent.getStringExtra("content"), "text/html", "UTF-8");
        }
    }
    
    
### SettingApi
    public class SettingApi {

        public static final String IP_TOPSTORIES =
                "https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty";

        //menggantin ID = 8863., dengan article yang kita inginkan
    //    public static final String IP_ITEM =
    //            "https://hacker-news.firebaseio.com/v0/item/" + MainActivity.articleId + ".json?print=pretty";

        //menggantin ID = 8863., dengan article yang kita inginkan
        //https://hacker-news.firebaseio.com/v0/item/8863.json?print=pretty
        //public static final String IP_ITEM =
          //    "https://hacker-news.firebaseio.com/v0/item/";

        public static final String IP_ITEM =
              "https://newsapi.org/v2/top-headlines?";
    }
