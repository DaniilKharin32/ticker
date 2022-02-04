package com.robinhood.ticker.sample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.core.provider.FontRequest;
import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.FontRequestEmojiCompatConfig;

import com.robinhood.ticker.LevenshteinUtils;
import com.robinhood.ticker.TickerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainActivity extends BaseActivity {
    private final CharSequence[] alphabetlist = LevenshteinUtils.toCharArrayOfArray("0123456789abcdefghiklmnop\uD83D\uDC77\uD83C\uDFFF\u200D♀️");

    private TickerView ticker1, ticker2, ticker3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        List<Integer> exceptionInts = new ArrayList<>(11);
        exceptionInts.add((int) '1');
        exceptionInts.add((int) '2');
        exceptionInts.add((int) '3');
        exceptionInts.add((int) '4');
        exceptionInts.add((int) '5');
        exceptionInts.add((int) '6');
        exceptionInts.add((int) '7');
        exceptionInts.add((int) '8');
        exceptionInts.add((int) '9');
        exceptionInts.add((int) '0');
        exceptionInts.add((int) '#');
        FontRequest fontRequest = new FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs);
        EmojiCompat.Config config = new FontRequestEmojiCompatConfig(this, fontRequest)
                .setReplaceAll(true)
                .setUseEmojiAsDefaultStyle(true, exceptionInts);
        EmojiCompat.init(config);
        setContentView(R.layout.activity_main);

        ticker1 = findViewById(R.id.ticker1);
        ticker2 = findViewById(R.id.ticker2);
        ticker3 = findViewById(R.id.ticker3);

        ticker3.setCharacterLists("0123456789");

        ticker1.setPreferredScrollingDirection(TickerView.ScrollingDirection.DOWN);
        ticker2.setPreferredScrollingDirection(TickerView.ScrollingDirection.UP);
        ticker3.setPreferredScrollingDirection(TickerView.ScrollingDirection.ANY);

        findViewById(R.id.perfBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PerfActivity.class));
            }
        });

        findViewById(R.id.slideBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SlideActivity.class));
            }
        });
    }

    @Override
    protected void onUpdate() {
        final int digits = RANDOM.nextInt(2) + 6;

        ticker1.setText(getRandomNumber(digits));
        final String currencyFloat = Float.toString(RANDOM.nextFloat() * 100);
        ticker2.setText("$" + currencyFloat.substring(0, Math.min(digits, currencyFloat.length())));
        ticker3.setText(generateChars(RANDOM, alphabetlist, digits));
    }

    private CharSequence generateChars(Random random, CharSequence[] list, int numDigits) {
        StringBuilder sb = new StringBuilder();
        CharSequence symbol;
        int pos;
        for (int i = 0; i < numDigits; i++) {
            pos = random.nextInt(list.length);
            symbol = list[pos];
            sb.append(symbol);
        }
        return EmojiCompat.get().process(sb.toString());
    }
}
