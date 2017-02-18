package it.communikein.waveonthego;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.crash.FirebaseCrash;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

public class NewsFragment extends Fragment {

    private RecyclerView mRecyclerView;

    private ArticleAdapter mAdapter;

    private final Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            ex.printStackTrace();
            FirebaseCrash.report(ex);
        }
    };

    public NewsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler(handler);
        return inflater.inflate(R.layout.fragment_news, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initUI(view);

        getNews();
    }

    private void initUI(View view) {
        ArrayList<Article> temp = new ArrayList<>();
        // specify an adapter (see also next example)
        mAdapter = new ArticleAdapter(temp);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.my_recycler_view);
        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void updateView(ArrayList<Article> news) {
        mAdapter.clear();
        mAdapter.addAll(news);
        mAdapter.notifyDataSetChanged();
    }

    private void getNews() {
        (new AsyncTask<Void, Integer, ArrayList<Article>>() {
            @Override
            protected ArrayList<Article> doInBackground(Void... params) {
                ArrayList<Article> ris;

                try {
                    ris = downloadNews();
                } catch (IOException | ParseException e) {
                    ris = new ArrayList<>();
                }

                return ris;
            }

            @Override
            protected void onPostExecute(ArrayList<Article> s) {
                super.onPostExecute(s);

                updateView(s);
            }
        }).execute();
    }


    public static ArrayList<Article> downloadNews() throws IOException, ParseException {
        ArrayList<Article> ris = new ArrayList<>();
        boolean errorOccured;
        HttpURLConnection result = getPage("http://www.parkourwave.com/en/blog/");
        String html;

        try {
            html = getHTML(result.getInputStream());
            errorOccured = false;
        } catch (IOException e) {
            html = getHTML(result.getErrorStream());
            errorOccured = true;
        }

        if (!errorOccured) {
            Document doc = Jsoup.parse(html);
            Elements els = doc.select("article.post");

            for (Element el : els) {
                Element post = el.select("div.post_text_inner").first();

                String title = post.select("h4").first().text();
                String URL = post.select("h4 a").first().attr("href");
                String summary = post.select("p.post_excerpt").first().text();
                String dateStr = post.select("div.date").first().text();
                dateStr = dateStr.substring(dateStr.indexOf("on") + 3);
                Date date = Article.dateFormat.parse(dateStr);

                ris.add(new Article(title, summary, URL, date));
            }
        }

        return ris;
    }

    public static HttpURLConnection getPage(String url) throws IOException {
        String USER_AGENT = System.getProperty("http.agent");

        HttpURLConnection con = (HttpURLConnection) (new URL(url)).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept", "text/html");
        con.setConnectTimeout(5000);

        // Send post request
        con.setDoOutput(true);
        con.getResponseCode();

        return con;
    }

    public static String getHTML(InputStream instream) throws IOException {
        BufferedReader rd;
        String ris;
        rd = new BufferedReader(new InputStreamReader(instream));

        StringBuilder result = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        ris = result.toString();

        return ris;
    }
}