package com.ycm.demo;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ScrollingActivity extends AppCompatActivity {
    private LinearLayout container;
    private SwipeRefreshLayout swipeRefreshLayout;

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
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        swipeRefreshLayout.setRefreshing(true);
        refresh();
        swipeRefreshLayout.setRefreshing(false);
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void renderItem(Drawable icon, String title, String contet) {
        LayoutInflater inflater = LayoutInflater.from(ScrollingActivity.this);
        ConstraintLayout itemLayout = (ConstraintLayout)inflater.inflate(R.layout.scrolling_item, null);

        ImageView iconView = itemLayout.findViewById(R.id.scrolling_item_icon);
        iconView.setImageResource(R.drawable.ic_person_50dp);

        TextView titleView = itemLayout.findViewById(R.id.scrolling_item_title);
        titleView.setText(title);

        TextView contentView = itemLayout.findViewById(R.id.scrolling_item_content);
        contentView.setText(contet);
        container.addView(itemLayout);
    }

    public void refresh() {
        container.removeAllViews();

        List<String> titles = new ArrayList<String>();
        titles.add("簪中录");
        titles.add("揽溪传");
        titles.add("后宫·褒姒传");
        titles.add("嫡女风华");
        titles.add("大明皇妃·孙若微传");

        List<String> contents = new ArrayList<String>();
        contents.add("唐朝懿宗年间,名闻天下的女探黄梓瑕,一夜之间从破案才女变为毒杀全家的凶手，成为海捕文书上各地捉拿的通缉犯。李舒白贵为皇子，却身遭“鳏残孤独废疾”的诅咒，难以脱身。皇帝指婚之时，准王妃却形迹可疑，“鳏”的诅咒应验在即。");
        contents.add("一个短命皇子，一个失宠皇妃，他们所面对的敌手，实在太过可怕，是圣眷不衰的皇贵妃，甚至是重重帷幕后深不可测的皇帝。");
        contents.add("她为救父入宫，一朝受宠，却引万人欲杀而诛之！人人都一口咬定她就是童谣里预言的亡国妖女！");
        contents.add("杀人偿命，欠债还钱！杜蘅对天发誓，要以牙还牙，以眼还眼！");
        contents.add("以传奇皇妃孙若微为主线，写了她从八岁入宫历经六朝五帝的故事。既有她与青梅竹马的皇帝朱瞻基相伴一生、坚如磐石的爱情；也有她以柔肩力挽狂澜，驾驭两次震惊中外的皇宫政变，影响幼帝定都、废除殉葬制的政治作为。");

        for (int i = 0; i < 5; i++) {
            renderItem(null, titles.get(i), contents.get(i));
        }
        for (int i = 0; i < 5; i++) {
            renderItem(null, titles.get(i), contents.get(i));
        }
        for (int i = 0; i < 5; i++) {
            renderItem(null, titles.get(i), contents.get(i));
        }
    }
}
