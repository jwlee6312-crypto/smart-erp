package com.crmbank.erp.mobile.hsio;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.RetrofitClient;
import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.CodeDto;
import com.crmbank.erp.mobile.PopupAdapter;
import com.crmbank.erp.mobile.R;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 🚀 [MHSIO650S] 창고별 수불현황
 * 웹 HSIO650S.vue 로직 및 MHSIO052U 프로토타입 기반 완성 버전
 */
public class MHSIO650S extends BaseActivity {

    private TextView tvDateFrom, tvDateTo, tvFinalStock;
    private EditText etItemNm;
    private Spinner spWarehouse;
    private ListView lvHistory;
    private HistoryAdapter adapter;
    private final List<Map<String, Object>> historyList = new ArrayList<>();
    private final List<CodeDto> whcdList = new ArrayList<>();
    private ApiService apiService;
    private String cmpycd, userid;
    private String selectedItemCd = "";
    private final DecimalFormat df = new DecimalFormat("#,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsio650s);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        userid = prefs.getString("userId", "");

        apiService = RetrofitClient.getApiService();

        tvDateFrom = findViewById(R.id.tvDateFrom);
        tvDateTo = findViewById(R.id.tvDateTo);
        tvFinalStock = findViewById(R.id.tvFinalStock);
        etItemNm = findViewById(R.id.etItemNm);
        spWarehouse = findViewById(R.id.spWarehouse);
        lvHistory = findViewById(R.id.lvHistory);

        adapter = new HistoryAdapter();
        lvHistory.setAdapter(adapter);

        tvDateFrom.setOnClickListener(v -> showDatePicker(tvDateFrom));
        tvDateTo.setOnClickListener(v -> showDatePicker(tvDateTo));
        etItemNm.setOnClickListener(v -> openItemHelp());

        findViewById(R.id.btnSearch).setOnClickListener(v -> search());

        loadWarehouses();
        initialize();
    }

    private void initialize() {
        historyList.clear();
        selectedItemCd = "";
        
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(cal.getTime());
        tvDateTo.setText(today);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        tvDateFrom.setText(sdf.format(cal.getTime()));
        
        etItemNm.setText("");
        tvFinalStock.setText("0");

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void loadWarehouses() {
        Map<String, Object> p = new HashMap<>();
        p.put("gubun", "W0"); p.put("cmpycd", cmpycd);
        apiService.executeHs00Procedure("HS00_000S_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    whcdList.clear();
                    CodeDto all = new CodeDto();
                    all.codecd = "000"; all.codenm = "전체";
                    whcdList.add(all);
                    for (Map<String, Object> m : response.body()) {
                        CodeDto dto = new CodeDto();
                        dto.codecd = getStringVal(m, "whcd");
                        dto.codenm = getStringVal(m, "whnm");
                        whcdList.add(dto);
                    }
                    List<String> names = new ArrayList<>();
                    for (CodeDto dto : whcdList) names.add(dto.codenm);
                    spWarehouse.setAdapter(new ArrayAdapter<>(MHSIO650S.this, android.R.layout.simple_spinner_item, names));
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void showDatePicker(TextView tv) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> 
            tv.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d)), 
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openItemHelp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null);
        builder.setTitle("조회 품목 선택").setView(dialogView);
        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setText("품목 검색");

        EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<Map<String, Object>> list = new ArrayList<>();
        AlertDialog dialog = builder.create();

        PopupAdapter popupAdapter = new PopupAdapter(list, "ITEM", item -> {
            selectedItemCd = getStringVal(item, "itemcd");
            etItemNm.setText(getStringVal(item, "itemnm"));
            dialog.dismiss();
            search();
        });
        rv.setAdapter(popupAdapter);

        dialogView.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>();
            p.put("gubun", "I1"); p.put("cmpycd", cmpycd); p.put("gbncd", "2"); 
            p.put("codenm", etSearch.getText().toString().trim());

            apiService.executeHs00Procedure("HS00_000S_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
                @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> c, @NonNull Response<List<Map<String, Object>>> r) {
                    if (r.isSuccessful() && r.body() != null) {
                        list.clear(); list.addAll(r.body()); popupAdapter.notifyDataSetChanged();
                    }
                }
                @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> c, @NonNull Throwable t) {}
            });
        });

        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        dialogView.findViewById(R.id.btnSearch).performClick();
    }

    private void search() {
        if (selectedItemCd.isEmpty()) {
            Toast.makeText(this, "조회할 품목을 선택하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> p = new HashMap<>();
        p.put("cmpycd", cmpycd);
        p.put("whcd", whcdList.get(spWarehouse.getSelectedItemPosition()).codecd);
        p.put("fromdt", tvDateFrom.getText().toString().replace("-", ""));
        p.put("todt", tvDateTo.getText().toString().replace("-", ""));
        p.put("astkind", "120");
        p.put("itemcd", selectedItemCd);

        apiService.executeHsioProcedure("HSIO_650S_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> rawList = response.body();
                    historyList.clear();
                    double currentStock = 0;
                    for (Map<String, Object> item : rawList) {
                        double inQty = getDoubleVal(item, "inqty");
                        double outQty = getDoubleVal(item, "outqty");
                        currentStock = currentStock + inQty - outQty;
                        
                        Map<String, Object> row = new HashMap<>(item);
                        row.put("_stkqty", currentStock);
                        historyList.add(row);
                    }
                    adapter.notifyDataSetChanged();
                    tvFinalStock.setText(df.format(currentStock));
                    if (historyList.isEmpty()) Toast.makeText(MHSIO650S.this, "조회된 수불 내역이 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }

    private double getDoubleVal(Map<String, Object> map, String key) {
        try { return Double.parseDouble(getStringVal(map, key).replace(",", "")); } catch (Exception e) { return 0.0; }
    }

    @Override protected String getProgramTitle() { return "창고별 수불현황"; }
    @Override protected String getProgramId() { return "MHSIO650S"; }

    private class HistoryAdapter extends BaseAdapter {
        @Override public int getCount() { return historyList.size(); }
        @Override public Object getItem(int p) { return historyList.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup pr) {
            if (v == null) v = LayoutInflater.from(MHSIO650S.this).inflate(R.layout.item_mhsio650s, pr, false);
            Map<String, Object> item = historyList.get(p);

            String date = getStringVal(item, "ioymd");
            if (date.length() == 8) date = date.substring(4, 6) + "-" + date.substring(6, 8);
            ((TextView) v.findViewById(R.id.tvDate)).setText(date);

            String remark = getStringVal(item, "custnm");
            if (remark.isEmpty()) remark = getStringVal(item, "iotypenm");
            else remark += " / " + getStringVal(item, "iotypenm");
            ((TextView) v.findViewById(R.id.tvRemark)).setText(remark);

            ((TextView) v.findViewById(R.id.tvInQty)).setText(df.format(getDoubleVal(item, "inqty")));
            ((TextView) v.findViewById(R.id.tvOutQty)).setText(df.format(getDoubleVal(item, "outqty")));
            
            double stk = getDoubleVal(item, "_stkqty");
            TextView tvStk = v.findViewById(R.id.tvStockQty);
            tvStk.setText(df.format(stk));
            tvStk.setTextColor(stk < 0 ? Color.RED : Color.BLACK);

            return v;
        }
    }
}
