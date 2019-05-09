package com.ycm.demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ycm.demo.db.Book;
import com.ycm.demo.db.BookDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

;

public class ScrollingActivity extends AppCompatActivity {
    private LinearLayout container;
    private SwipeRefreshLayout swipeRefreshLayout;

    private BookDatabase mBookDatabase = new BookDatabase(ScrollingActivity.this);

    private int unloadedCount = 10;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == 1) {
                Book book = (Book) msg.obj;
                renderItem(book.getCover(), book.getName(), book.getDesc());
            } else {
                String name = (String) msg.obj;
                Toast.makeText(ScrollingActivity.this, "《" + name + "》下载封面失败", Toast.LENGTH_LONG).show();
            }


            unloadedCount--;
            if (unloadedCount <= 0) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        container = findViewById(R.id.scrolling_content);

        swipeRefreshLayout = findViewById(R.id.scrolling_refresh);
        swipeRefreshLayout.setSize(SwipeRefreshLayout.DEFAULT);
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_dark);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                refresh();
            }
        });

        List<Book> books = mBookDatabase.queryList(0, 10);
        for (Book book: books) {
            this.renderItem(book.getCover(), book.getName(), book.getDesc());
        }
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void renderItem(Bitmap icon, String title, String contet) {
        LayoutInflater inflater = LayoutInflater.from(ScrollingActivity.this);
        ConstraintLayout itemLayout = (ConstraintLayout)inflater.inflate(R.layout.scrolling_item, null);

        ImageView iconView = itemLayout.findViewById(R.id.scrolling_item_icon);
        if (icon != null) {
            iconView.setImageBitmap(icon);
        }

        TextView titleView = itemLayout.findViewById(R.id.scrolling_item_title);
        titleView.setText(title);

        TextView contentView = itemLayout.findViewById(R.id.scrolling_item_content);
        contentView.setText(contet);
        container.addView(itemLayout);
    }



    public void refresh() {
        container.removeAllViews();
        mBookDatabase.clear();

        initDataBase();
    }

    private void initDataBase() {
        unloadedCount = 4;

        List<String> names = new ArrayList<String>();
        names.add("吉时医到");
        names.add("宫样年华");
        names.add("碧血红妆");
        names.add("花醉三千.完结篇");
        List<String> descs = new ArrayList<String>();
        descs.add("　　《吉时医到》杨茉不知道自己从哪里来，但是她留在这里，成为了一个无依无靠的孤女杨茉兰，守着一扇永远迈不出去的大宅门。如今，她要做的是不再寄人篱下、仰人鼻息，而是用自己的双手编织出属于她的璀璨人生！");
        descs.add("　　 未经历过失去，便不明白曾经拥有的是多么珍贵。\n" +
                "　　 一场蓄谋的车祸，该称作姐夫的他危难之下护着她存活下来，而他却带着遗憾离开尘世。姐妹之间的战争一触即发，她始终不愿相信那该死的残忍事实。意外坠楼，她带着前生记忆穿越到陌生国度。心如止水的她在异世遇上貌似故人的他，一路追逐。无奈世事无常，他早已暗许芳心，佳人红袖添香。\n" +
                "　　 他乃古越国堂堂靖王，一身桀骜，行事张弛有度，魄力十足。而她不过异世一缕香魂，执著地期待他能回头。一个是救过他的人，一个是他爱的女子，而她什么都不是，却成了他的妃。龙天睿隐忍十年，谋划十年，将他深爱的女子隐于人后，不过是为日后风光比肩而立……\n" +
                "　　 梦落成空，花谢花开，十年离别，十年相思，十年守候，荆棘丛生的情路又将如何结局？穿越大剧《宫样年华》为您寻找答案。\n" +
                "　　 原来，爱早已落地生根，只道当时不寻常。");
        descs.add("女扮男装的将军易凤歌，与云国皇帝慕容云翔同生共死十三年。在慕容云翔神志不清的情况下，两人发生了性关系，易凤歌因此怀孕，却依然不泄露女子身份。慕容云翔怀疑易凤歌有谋逆之心，易凤歌刳腹产子，跳崖自尽。慕容云翔非常后悔，宰相关之洲回忆起与易凤歌交往的点点滴滴，陷入痛苦之中。两年后易凤歌醒来，身体还未曾恢复，就遇上叛军作乱。易凤歌化名易冷香，冒充自己，带领乡兵进京勤王。在战役中，易凤歌对关之洲产生了朦胧的感情。易凤歌重返边疆，皇帝的叔父慕容傲天利用慕容云翔对易凤歌的感情，设下重重阴谋，要绞杀易凤歌。得知消息的关之洲千里奔驰报讯，却迟了一步。而与此同时，慕容傲天发动宫廷政变，皇帝与易凤歌的儿子，都陷入危难之中……");
        descs.add("　　她是相府里有名无实的夫人，她是风月楼里妩媚的头牌，她是一计退兵十万的小兵，她是江湖上让人闻风丧胆的鬼娘。\n" +
                "　　她不想百变，她只想报心头之恨。\n" +
                "　　他是位高权重的相国，他是智计百出的谋士，他是龙行浅滩韬光养晦的皇脉。\n" +
                "　　他不想隐忍，他只想一招定乾坤。");

        List<String> coverUrls = new ArrayList<String>();
        coverUrls.add("https://img10.360buyimg.com/n1/jfs/t1039/190/140965789/102021/ee4b4875/55027d68N43ba1e85.jpg");
        coverUrls.add("https://img14.360buyimg.com/n1/g9/M03/05/04/rBEHaVBFUc8IAAAAAAF5BRnV1ecAABAMgKFa18AAXkd902.jpg");
        coverUrls.add("https://img10.360buyimg.com/n1/jfs/t2581/96/498437725/130147/fe8b200b/5716fc99N2ea42c94.jpg");
        coverUrls.add("https://img10.360buyimg.com/n1/jfs/t1669/352/1132999657/261795/e3b7fe9b/55e3ead7N8dbdc9a8.jpg");

        for (int i = 0; i < names.size(); i++) {
            this.saveBook(names.get(i), descs.get(i), coverUrls.get(i));
        }
    }

    private void saveBook(final String name, final String desc, String url) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Message msg = new Message();
                msg.what = 0;
                msg.obj = name;
                mHandler.sendMessage(msg);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){
                    //回调的方法执行在子线程。
                    byte [] cover = response.body().bytes();
                    long status = -1;
                    if (cover.length > 0) {
                        status = mBookDatabase.insert(name, desc, cover);
                    } else {
                        status = mBookDatabase.insert(name, desc, null);
                    }

                    if (status > -1) {
                        Message msg = new Message();
                        msg.what = 1;
                        Bitmap bitmap = null;
                        if (cover.length > 0) {
                            bitmap = BitmapFactory.decodeByteArray(cover, 0, cover.length);
                        }
                        msg.obj = new Book(0, name, desc, bitmap);
                        mHandler.sendMessage(msg);
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        mBookDatabase.close();
        mBookDatabase = null;

        super.onDestroy();
    }
}
