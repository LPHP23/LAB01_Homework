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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
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

    // ĐỊA CHỈ API PHO-BERT CỦA BẠN: Thay X bằng IP máy tính chạy Flask (VD: 192.168.1.15)
    // Lưu ý: Đừng dùng localhost hoặc 127.0.0.1 vì máy ảo Android sẽ hiểu nhầm là chính nó.
    private static final String API_URL = "http://192.168.1.X:5000/predict";

    // Colors
    private final String COLOR_DEFAULT_BG = "#DECCFF";
    private final String COLOR_DEFAULT_BTN = "#9D70FF";
    private final String COLOR_POSITIVE_BG = "#ADF7CB";
    private final String COLOR_POSITIVE_BTN = "#2E7D32";
    private final String COLOR_NEGATIVE_BG = "#FFCDD2"; // Đổi màu đỏ nhạt xíu cho dịu mắt
    private final String COLOR_NEGATIVE_BTN = "#D32F2F";

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
                emojiView.setText("⏳"); // Hiệu ứng chờ
                analyzeSentiment(text);
            } else {
                Toast.makeText(this, "Vui lòng nhập câu tiếng Việt!", Toast.LENGTH_SHORT).show();
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
        // Đóng gói dữ liệu thành JSON để gửi tới Flask API PhoBERT
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("text", text);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("NETWORK_ERROR", "Failed to connect", e);
                runOnUiThread(() -> {
                    emojiView.setText("❌");
                    Toast.makeText(MainActivity.this, "Lỗi kết nối tới máy chủ Flask!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String result = response.body().string();
                Log.d("API_RESPONSE", result); // Xem JSON trả về trong Logcat

                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(result);

                        // Đọc nhãn cảm xúc từ JSON của PhoBERT (thường là "POSITIVE" hoặc "NEGATIVE")
                        // Bạn cần đảm bảo key "sentiment" khớp với file Python Flask của bạn trả về
                        String sentiment = json.optString("sentiment", "NEU");

                        runOnUiThread(() -> updateUI(sentiment));

                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Error parsing JSON", e);
                        runOnUiThread(() -> emojiView.setText("⚠️"));
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Network Error: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void updateUI(String sentiment) {
        String emoji;
        String bgHex;
        String btnHex;
        int textColor;

        // Cập nhật logic để bắt các nhãn trả về từ PhoBERT
        if (sentiment.equalsIgnoreCase("POSITIVE") || sentiment.equalsIgnoreCase("POS")) {
            emoji = "🥰";
            bgHex = COLOR_POSITIVE_BG;
            btnHex = COLOR_POSITIVE_BTN;
            textColor = Color.parseColor("#1B5E20");
        } else if (sentiment.equalsIgnoreCase("NEGATIVE") || sentiment.equalsIgnoreCase("NEG")) {
            emoji = "😡";
            bgHex = COLOR_NEGATIVE_BG;
            btnHex = COLOR_NEGATIVE_BTN;
            textColor = Color.WHITE;
        } else {
            // Trường hợp trung tính hoặc không xác định
            emoji = "😐";
            bgHex = "#FFF9C4"; // Vàng nhạt
            btnHex = "#FBC02D";
            textColor = Color.DKGRAY;
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