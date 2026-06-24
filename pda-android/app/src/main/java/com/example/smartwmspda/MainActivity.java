package com.example.smartwmspda;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int BLUE = Color.rgb(23, 105, 170);
    private static final int GREEN = Color.rgb(46, 157, 98);
    private static final int RED = Color.rgb(220, 38, 38);
    private FrameLayout content;
    private ProgressBar loadingBar;
    private TextView userText;
    private Button logoutButton;
    private SharedPreferences prefs;

    private JSONObject user;
    private JSONObject currentJobView;
    private EditText jobInput;
    private EditText scanInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        content = findViewById(R.id.content);
        loadingBar = findViewById(R.id.loadingBar);
        userText = findViewById(R.id.userText);
        logoutButton = findViewById(R.id.logoutButton);
        prefs = getSharedPreferences("pda", MODE_PRIVATE);

        logoutButton.setOnClickListener(v -> logout());
        String savedUser = prefs.getString("user", null);
        if (savedUser != null) {
            try {
                user = new JSONObject(savedUser);
                showList();
                loadJobs();
            } catch (Exception ignored) {
                showLogin();
            }
        } else {
            showLogin();
        }
    }

    private void showLogin() {
        user = null;
        userText.setText("");
        logoutButton.setVisibility(View.GONE);
        content.removeAllViews();

        LinearLayout form = column(18);
        form.setGravity(Gravity.CENTER);
        form.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView title = text("Smart WMS PDA", 26, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);

        EditText username = input("账号");
        username.setText("admin");
        EditText password = input("密码");
        password.setText("123456");
        password.setInputType(0x00000081);
        Button login = primaryButton("登录");

        LinearLayout card = card();
        card.addView(text("PDA 发运扫码", 24, Color.rgb(23, 32, 42), true));
        card.addView(text("仓库现场扫码、分拣、上车确认", 14, Color.rgb(102, 112, 133), false));
        card.addView(username);
        card.addView(password);
        card.addView(login);
        card.addView(text("默认：admin / 123456", 12, Color.rgb(102, 112, 133), false));

        form.setBackgroundColor(Color.rgb(15, 23, 42));
        form.addView(card, new LinearLayout.LayoutParams(-1, -2));
        content.addView(form, new FrameLayout.LayoutParams(-1, -1));

        login.setOnClickListener(v -> {
            JSONObject body = new JSONObject();
            try {
                body.put("username", username.getText().toString().trim());
                body.put("password", password.getText().toString().trim());
            } catch (Exception ignored) {}
            request("POST", "/users/login", body, data -> {
                user = data;
                prefs.edit().putString("user", data.toString()).apply();
                showList();
                loadJobs();
            });
        });
    }

    private void showList() {
        currentJobView = null;
        logoutButton.setVisibility(View.VISIBLE);
        userText.setText(displayName());
        content.removeAllViews();

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = column(12);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        scroll.addView(root);

        LinearLayout searchCard = card();
        searchCard.addView(text("扫描 / 输入 Shipping Job", 14, Color.rgb(71, 84, 103), true));
        jobInput = input("SH-WHA-00000001");
        Button open = primaryButton("进入 Job");
        Button refresh = secondaryButton("刷新列表");
        searchCard.addView(jobInput);
        searchCard.addView(row(open, refresh));
        root.addView(searchCard);

        LinearLayout list = column(10);
        list.setId(View.generateViewId());
        root.addView(list);
        content.addView(scroll);

        open.setOnClickListener(v -> openJob(jobInput.getText().toString().trim()));
        refresh.setOnClickListener(v -> loadJobs());
        jobInput.setOnEditorActionListener((v, actionId, event) -> {
            openJob(jobInput.getText().toString().trim());
            return true;
        });
    }

    private void loadJobs() {
        requestArray("GET", "/pda/shipping-jobs", null, jobs -> {
            LinearLayout root = (LinearLayout) ((ScrollView) content.getChildAt(0)).getChildAt(0);
            LinearLayout list = (LinearLayout) root.getChildAt(1);
            list.removeAllViews();
            if (jobs.length() == 0) {
                list.addView(empty("暂无可分拣 Job"));
                return;
            }
            for (int i = 0; i < jobs.length(); i++) {
                JSONObject job = jobs.optJSONObject(i);
                if (job != null) list.addView(jobCard(job));
            }
        });
    }

    private View jobCard(JSONObject job) {
        LinearLayout card = card();
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout info = column(2);
        info.addView(text(job.optString("jobNo"), 18, Color.rgb(23, 32, 42), true));
        JSONArray orders = job.optJSONArray("orders");
        int orderCount = orders == null ? 0 : orders.length();
        info.addView(text((job.optString("truckNo", "未填写车辆")) + " / " + orderCount + " 单", 13, Color.rgb(102, 112, 133), false));

        TextView status = badge(statusLabel(job.optString("status")));
        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(status);
        card.setOnClickListener(v -> openJob(job.optString("jobNo")));
        return card;
    }

    private void openJob(String jobNo) {
        if (jobNo == null || jobNo.isEmpty()) return;
        request("GET", "/pda/shipping-jobs/" + encode(jobNo), null, data -> {
            currentJobView = data;
            showJob();
            toast("Job 已进入 Sorting，可开始扫码");
        });
    }

    private void showJob() {
        content.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = column(12);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        scroll.addView(root);

        JSONObject shippingJob = currentJobView.optJSONObject("shippingJob");
        if (shippingJob == null) {
            toast("Job 数据异常");
            showListThenLoad();
            return;
        }
        JSONArray orders = currentJobView.optJSONArray("orders");
        int required = total(orders, "requiredQuantity");
        int scanned = total(orders, "scannedQuantity");

        LinearLayout head = card();
        head.setOrientation(LinearLayout.HORIZONTAL);
        Button back = secondaryButton("返回");
        LinearLayout info = column(2);
        info.addView(text("当前 Job", 12, Color.rgb(102, 112, 133), false));
        info.addView(text(shippingJob.optString("jobNo"), 22, Color.rgb(23, 32, 42), true));
        head.addView(back);
        head.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(badge(statusLabel(shippingJob.optString("status"))));
        root.addView(head);

        LinearLayout progress = card();
        progress.addView(text("上车进度", 12, Color.rgb(102, 112, 133), false));
        progress.addView(text(scanned + " / " + required, 26, Color.rgb(23, 32, 42), true));
        root.addView(progress);

        LinearLayout scanCard = card();
        scanCard.addView(text("扫描商品 SKU / 条码", 14, Color.rgb(71, 84, 103), true));
        scanInput = input("扫描商品");
        Button scan = primaryButton("确认扫码");
        Button refresh = secondaryButton("刷新");
        scanCard.addView(scanInput);
        scanCard.addView(row(scan, refresh));
        root.addView(scanCard);

        Button allOnBoard = dangerButton("全部上车");
        allOnBoard.setEnabled(required > 0 && scanned == required);
        root.addView(allOnBoard, new LinearLayout.LayoutParams(-1, dp(58)));

        for (int i = 0; orders != null && i < orders.length(); i++) {
            JSONObject order = orders.optJSONObject(i);
            if (order != null) root.addView(orderCard(order));
        }

        content.addView(scroll);
        back.setOnClickListener(v -> showListThenLoad());
        refresh.setOnClickListener(v -> openJob(shippingJob.optString("jobNo")));
        scan.setOnClickListener(v -> scanItem());
        scanInput.setOnEditorActionListener((v, actionId, event) -> {
            scanItem();
            return true;
        });
        allOnBoard.setOnClickListener(v -> request("POST", "/pda/shipping-jobs/" + encode(shippingJob.optString("jobNo")) + "/complete", new JSONObject(), data -> {
            vibrate(120);
            toast("全部上车完成，Job 已 Shipped");
            showListThenLoad();
        }));
        scanInput.requestFocus();
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(scanInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void scanItem() {
        if (currentJobView == null || scanInput == null) return;
        String code = scanInput.getText().toString().trim();
        if (code.isEmpty()) return;
        JSONObject shippingJob = currentJobView.optJSONObject("shippingJob");
        if (shippingJob == null) return;
        String jobNo = shippingJob.optString("jobNo");
        JSONObject body = new JSONObject();
        try {
            body.put("code", code);
            body.put("quantity", 1);
            body.put("operatorName", displayName());
        } catch (Exception ignored) {}
        request("POST", "/pda/shipping-jobs/" + encode(jobNo) + "/scan", body, data -> {
            currentJobView = data.optJSONObject("job");
            if (currentJobView == null) {
                toast("扫码返回数据异常");
                return;
            }
            vibrate(50);
            toast(data.optString("message", "扫码成功"));
            showJob();
        });
    }

    private View orderCard(JSONObject order) {
        String status = order.optString("status");
        LinearLayout card = card();
        if ("DONE".equals(status)) card.setBackgroundColor(Color.rgb(240, 251, 245));
        if ("PARTIAL".equals(status)) card.setBackgroundColor(Color.rgb(255, 250, 240));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout info = column(2);
        info.addView(text(order.optString("orderNo"), 18, Color.rgb(23, 32, 42), true));
        info.addView(text(order.optString("receiverName", "-"), 13, Color.rgb(102, 112, 133), false));
        header.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(badge(status));
        card.addView(header);

        JSONArray items = order.optJSONArray("items");
        for (int i = 0; items != null && i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item != null) card.addView(itemRow(item));
        }
        return card;
    }

    private View itemRow(JSONObject item) {
        int required = item.optInt("requiredQuantity");
        int scanned = item.optInt("scannedQuantity");
        boolean done = required > 0 && scanned >= required;
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.setBackgroundColor(done ? Color.rgb(231, 248, 238) : Color.rgb(248, 250, 252));

        LinearLayout info = column(2);
        info.addView(text(item.optString("sku"), 15, Color.rgb(23, 32, 42), true));
        info.addView(text(item.optString("productName") + " / " + item.optString("locationCode", "-"), 12, Color.rgb(102, 112, 133), false));
        info.addView(text(item.optString("barcode", "-"), 12, Color.rgb(102, 112, 133), false));

        TextView qty = text(scanned + " / " + required, 18, done ? GREEN : BLUE, true);
        qty.setGravity(Gravity.RIGHT);
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(qty, new LinearLayout.LayoutParams(dp(76), -2));
        return row;
    }

    private void showListThenLoad() {
        showList();
        loadJobs();
    }

    private void logout() {
        prefs.edit().clear().apply();
        showLogin();
    }

    private void requestArray(String method, String path, JSONObject body, ArrayCallback callback) {
        requestRaw(method, path, body, data -> callback.onSuccess((JSONArray) data));
    }

    private void request(String method, String path, JSONObject body, ObjectCallback callback) {
        requestRaw(method, path, body, data -> callback.onSuccess((JSONObject) data));
    }

    private void requestRaw(String method, String path, JSONObject body, RawCallback callback) {
        loading(true);
        new AsyncTask<Void, Void, Result>() {
            @Override
            protected Result doInBackground(Void... voids) {
                try {
                    URL url = new URL(BuildConfig.API_BASE_URL + path);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(method);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(20000);
                    conn.setRequestProperty("Accept", "application/json");
                    if (body != null) {
                        conn.setDoOutput(true);
                        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                        try (OutputStream os = conn.getOutputStream()) {
                            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                        }
                    }
                    int code = conn.getResponseCode();
                    InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
                    String text = read(stream);
                    JSONObject wrapper = new JSONObject(text);
                    if (!wrapper.optBoolean("success")) {
                        return Result.fail(wrapper.optString("message", "请求失败"));
                    }
                    Object data = wrapper.opt("data");
                    return Result.ok(data);
                } catch (Exception e) {
                    return Result.fail(e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(Result result) {
                loading(false);
                if (result.error != null) {
                    vibrate(180);
                    toast(result.error);
                    return;
                }
                callback.onSuccess(result.data);
            }
        }.execute();
    }

    private String read(InputStream stream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) builder.append(line);
        return builder.toString();
    }

    private LinearLayout card() {
        LinearLayout view = column(10);
        view.setPadding(dp(14), dp(14), dp(14), dp(14));
        view.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(10));
        view.setLayoutParams(params);
        return view;
    }

    private LinearLayout column(int gapDp) {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        return view;
    }

    private LinearLayout row(View left, View right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(left, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1);
        p.setMargins(dp(8), 0, 0, 0);
        row.addView(right, p);
        return row;
    }

    private EditText input(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextSize(18);
        input.setPadding(dp(12), 0, dp(12), 0);
        return input;
    }

    private Button primaryButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(BLUE);
        return b;
    }

    private Button secondaryButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(BLUE);
        b.setBackgroundColor(Color.rgb(238, 246, 252));
        return b;
    }

    private Button dangerButton(String text) {
        Button b = primaryButton(text);
        b.setBackgroundColor(RED);
        b.setTextSize(18);
        return b;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value == null ? "" : value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, 1);
        return t;
    }

    private TextView badge(String value) {
        TextView t = text(value, 12, BLUE, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(10), dp(5), dp(10), dp(5));
        t.setBackgroundColor(Color.rgb(238, 246, 252));
        return t;
    }

    private TextView empty(String value) {
        TextView t = text(value, 15, Color.rgb(102, 112, 133), false);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(16), dp(48), dp(16), dp(48));
        return t;
    }

    private String statusLabel(String status) {
        if ("DRAFT".equals(status) || "IN_QUEUE".equals(status)) return "In Queue";
        if ("SCHEDULED".equals(status) || "READY_TO_SORT".equals(status)) return "Ready to Sort";
        if ("SORTING".equals(status)) return "Sorting";
        if ("SHIPPED".equals(status)) return "Shipped";
        if ("COMPLETED".equals(status)) return "Completed";
        if ("CANCELLED".equals(status)) return "Cancelled";
        return status;
    }

    private int total(JSONArray orders, String field) {
        int total = 0;
        for (int i = 0; orders != null && i < orders.length(); i++) {
            JSONObject order = orders.optJSONObject(i);
            if (order == null) continue;
            JSONArray items = order.optJSONArray("items");
            for (int j = 0; items != null && j < items.length(); j++) {
                JSONObject item = items.optJSONObject(j);
                if (item != null) total += item.optInt(field);
            }
        }
        return total;
    }

    private String displayName() {
        if (user == null) return "";
        String displayName = user.optString("displayName", "");
        return displayName.isEmpty() ? user.optString("username", "") : displayName;
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private void loading(boolean value) {
        loadingBar.setVisibility(value ? View.VISIBLE : View.GONE);
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }

    private void vibrate(long ms) {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(ms);
            }
        } catch (Exception ignored) {}
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    interface ObjectCallback { void onSuccess(JSONObject data); }
    interface ArrayCallback { void onSuccess(JSONArray data); }
    interface RawCallback { void onSuccess(Object data); }

    static class Result {
        Object data;
        String error;
        static Result ok(Object data) {
            Result r = new Result();
            r.data = data;
            return r;
        }
        static Result fail(String error) {
            Result r = new Result();
            r.error = error;
            return r;
        }
    }
}
