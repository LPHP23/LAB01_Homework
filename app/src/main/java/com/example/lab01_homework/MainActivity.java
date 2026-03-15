package com.example.lab01_homework;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private final OkHttpClient client = new OkHttpClient();
    private EditText inputText;
    private TextView emojiView;
    private TextView headerTitle;
    private LinearLayout mainLayout;
    private Button btnSubmit;
    private ImageButton btnRefresh;
    
    // THAY KEY CỦA BẠN VÀO ĐÂY ĐỂ CHẠY THẬT
    private static final String API_KEY = "YOUR_API_KEY"; 

    // Colors
    private final String COLOR_DEFAULT_BG = "#DECCFF";
    private final String COLOR_DEFAULT_BTN = "#9D70FF";
    private final String COLOR_POSITIVE_BG = "#ADF7CB";
    private final String COLOR_POSITIVE_BTN = "#2E7D32";
    private final String COLOR_NEGATIVE_BG = "#B31E3C";
    private final String COLOR_NEGATIVE_BTN = "#7B1113";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputText = findViewById(R.id.inputText);
        emojiView = findViewById(R.id.emojiView);
        mainLayout = findViewById(R.id.mainLayout);
        headerTitle = findViewById(R.id.headerTitle);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnRefresh = findViewById(R.id.btnRefresh);

        btnSubmit.setOnClickListener(v -> {
            String text = inputText.getText().toString().trim();
            if (!text.isEmpty()) {
                analyzeSentiment(text);
            } else {
                Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show();
            }
        });

        btnRefresh.setOnClickListener(v -> {
            inputText.setText("");
            emojiView.setText("🙈");
            updateTheme(COLOR_DEFAULT_BG, COLOR_DEFAULT_BTN, Color.BLACK);
            applyFadeAnimation(emojiView);
        });
    }

    private void analyzeSentiment(String text) {
        RequestBody body = new FormBody.Builder()
                .add("key", API_KEY)
                .add("txt", text)
                .add("lang", "en")
                .build();

        Request request = new Request.Builder()
                .url("https://api.meaningcloud.com/sentiment-2.1")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String result = response.body().string();
                Log.d("API_RESPONSE", result); // Xem nội dung API trả về trong Logcat

                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(result);
                        
                        // Kiểm tra nếu API trả về mã lỗi trong JSON (status)
                        JSONObject status = json.optJSONObject("status");
                        if (status != null && !"0".equals(status.getString("code"))) {
                            final String msg = status.getString("msg");
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "API Error: " + msg, Toast.LENGTH_LONG).show());
                            return;
                        }

                        String sentiment = json.optString("score_tag", "NONE");
                        runOnUiThread(() -> updateUI(sentiment));

                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Error parsing JSON", e);
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Network Error", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("NETWORK_ERROR", "Failed to connect", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connection Failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateUI(String sentiment) {
        String emoji;
        String bgHex;
        String btnHex;
        int textColor;

        switch (sentiment) {
            case "P+":
            case "P":
                emoji = "😊";
                bgHex = COLOR_POSITIVE_BG;
                btnHex = COLOR_POSITIVE_BTN;
                textColor = Color.parseColor("#1B5E20");
                break;
            case "NEU":
                emoji = "😐";
                bgHex = "#F5F5F5";
                btnHex = "#757575";
                textColor = Color.DKGRAY;
                break;
            case "N":
            case "N+":
                emoji = "😡";
                bgHex = COLOR_NEGATIVE_BG;
                btnHex = COLOR_NEGATIVE_BTN;
                textColor = Color.WHITE;
                break;
            default:
                emoji = "🤔";
                bgHex = COLOR_DEFAULT_BG;
                btnHex = COLOR_DEFAULT_BTN;
                textColor = Color.BLACK;
                break;
        }

        updateTheme(bgHex, btnHex, textColor);
        emojiView.setText(emoji);
        applyFadeAnimation(emojiView);
    }

    private void updateTheme(String bgHex, String btnHex, int textColor) {
        mainLayout.setBackgroundColor(Color.parseColor(bgHex));
        btnSubmit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(btnHex)));
        headerTitle.setTextColor(textColor);
        btnRefresh.setImageTintList(ColorStateList.valueOf(textColor));
    }

    private void applyFadeAnimation(TextView view) {
        AlphaAnimation fade = new AlphaAnimation(0.0f, 1.0f);
        fade.setDuration(1000);
        view.startAnimation(fade);
    }
}
