package com.crmbank.erp.mobile.hsst;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
// import com.crmbank.erp.mobile.SalesCustomerLedgerDtlActivity;
import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.RetrofitClient;
import com.crmbank.erp.mobile.ApiResponse;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

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

public class MHSST180S extends BaseActivity {

    private TextView tvSalesDate;
    private EditText etCustNm;
    private SalesReportAdapter adapter;
    private List<DailySalesItem> dataList;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsst180s);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        apiService = RetrofitClient.getApiService();

        tvSalesDate = findViewById(R.id.tvSalesDate);
        etCustNm = findViewById(R.id.etCustNm);
        ListView lvSalesReport = findViewById(R.id.lvSalesReport);
        Button btnSearch = findViewById(R.id.btnSearch);

        // 珥덇린 ?좎쭨 ?ㅼ젙
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvSalesDate.setText(today);
        tvSalesDate.setOnClickListener(v -> showDatePicker());

        dataList = new ArrayList<>();
        adapter = new SalesReportAdapter();
        lvSalesReport.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> searchData());

        // 由ъ뒪???대┃ ???곸꽭 ?댁뿭 議고쉶 (留ㅼ텧泥섏썝???곸꽭? ?숈씪 ?붾㈃ 怨듭쑀)
        lvSalesReport.setOnItemClickListener((parent, view, position, id) -> {
            DailySalesItem selected = dataList.get(position);
            String dateParam = tvSalesDate.getText().toString().replace("-", "");
            
//            Intent intent = new Intent(this, SalesCustomerLedgerDtlActivity.class);
//            intent.putExtra("fromdt", dateParam);
//            intent.putExtra("todt", dateParam); // ?붿껌?ы빆: ?쒖옉?쇱쓣 醫낅즺?쇰줈 ?꾨떖
//            intent.putExtra("custcd", selected.custcd);
//            intent.putExtra("custnm", selected.custNm);
//            startActivity(intent);
            Toast.makeText(this, "상세 내역 기능을 준비 중입니다.", Toast.LENGTH_SHORT).show();
        });

        searchData();
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            tvSalesDate.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void searchData() {
        String fromYmd = tvSalesDate.getText().toString().replace("-", "");
        String custNm = etCustNm.getText().toString();

        Map<String, String> params = new HashMap<>();
        params.put("fromymd", fromYmd);
        params.put("custnm", custNm);

        apiService.getDailySalesReport(params).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> list = response.body().getData();
                    dataList.clear();
                    if (list == null || list.isEmpty()) {
                        Toast.makeText(MHSST180S.this, "?대떦 ?먮즺媛 議댁옱?섏? ?딆뒿?덈떎.", Toast.LENGTH_SHORT).show();
                    } else {
                        for (Map<String, Object> map : list) {
                            dataList.add(new DailySalesItem(
                                    getStringValue(map, "custcd"),
                                    getStringValue(map, "custnm"),
                                    parseToDouble(map.get("pre_sale_amt")),
                                    parseToDouble(map.get("day_sale_amt")),
                                    parseToDouble(map.get("day_depo_amt")),
                                    parseToDouble(map.get("jan_sale_amt"))
                            ));
                        }
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(MHSST180S.this, "議고쉶 ?ㅽ뙣", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Throwable t) {
                Toast.makeText(MHSST180S.this, "?ㅽ듃?뚰겕 ?ㅻ쪟: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val) : "";
    }

    private double parseToDouble(Object value) {
        if (value == null) return 0.0;
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static class DailySalesItem {
        String custcd, custNm;
        double prevAmt, todaySales, todayDeposit, balanceAmt;

        public DailySalesItem(String custcd, String custNm, double prevAmt, double todaySales, double todayDeposit, double balanceAmt) {
            this.custcd = custcd;
            this.custNm = custNm;
            this.prevAmt = prevAmt;
            this.todaySales = todaySales;
            this.todayDeposit = todayDeposit;
            this.balanceAmt = balanceAmt;
        }
    }

    private class SalesReportAdapter extends BaseAdapter {
        @Override public int getCount() { return dataList.size(); }
        @Override public Object getItem(int pos) { return dataList.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MHSST180S.this).inflate(R.layout.item_mhsst180s, parent, false);
            }
            DailySalesItem item = dataList.get(position);

            ((TextView) convertView.findViewById(R.id.tvCustNm)).setText(item.custNm);
            ((TextView) convertView.findViewById(R.id.tvPrevAmt)).setText(String.format(Locale.getDefault(), "%,d", (int)item.prevAmt));
            ((TextView) convertView.findViewById(R.id.tvTodaySales)).setText(String.format(Locale.getDefault(), "%,d", (int)item.todaySales));
            ((TextView) convertView.findViewById(R.id.tvTodayDeposit)).setText(String.format(Locale.getDefault(), "%,d", (int)item.todayDeposit));
            ((TextView) convertView.findViewById(R.id.tvBalanceAmt)).setText(String.format(Locale.getDefault(), "%,d", (int)item.balanceAmt));

            return convertView;
        }
    }

    @Override protected String getProgramTitle() { return "영업담당별 판매현황"; }
    @Override protected String getProgramId() { return "MHSST180S"; }
}
