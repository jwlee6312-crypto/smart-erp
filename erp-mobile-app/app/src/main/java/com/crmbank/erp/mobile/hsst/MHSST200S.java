package com.crmbank.erp.mobile.hsst;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
import com.crmbank.erp.mobile.hsio.MHSIO650S;
import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.RetrofitClient;
import com.crmbank.erp.mobile.CodeDto;
import com.crmbank.erp.mobile.ItemDto;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

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

public class MHSST200S extends BaseActivity {

    private static final String TAG = "InventoryStatus";
    private Spinner spWarehouse;
    private EditText etItemNm;
    private ListView lvInventoryStatus;
    private ArrayAdapter<CodeDto> warehouseAdapter;
    private InventoryAdapter adapter;
    private List<InventoryItem> inventoryItems;
    private String selectedItemCd = "";
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsst200s);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        apiService = RetrofitClient.getApiService();

        spWarehouse = findViewById(R.id.spWarehouse);
        etItemNm = findViewById(R.id.etItemNm);
        lvInventoryStatus = findViewById(R.id.lvInventoryStatus);
        Button btnSearch = findViewById(R.id.btnSearch);

        etItemNm.setFocusable(false);
        etItemNm.setOnClickListener(v -> showItemSearchDialog());

        setupWarehouseSpinner();

        inventoryItems = new ArrayList<>();
        adapter = new InventoryAdapter();
        lvInventoryStatus.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> {
            if (validateInputs()) {
                searchInventory();
            }
        });

        // 🚀 수불 현황 화면으로 이동 (MHSIO650S)
        lvInventoryStatus.setOnItemClickListener((parent, view, position, id) -> {
            InventoryItem item = inventoryItems.get(position);
            CodeDto wh = (CodeDto) spWarehouse.getSelectedItem();
            
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            String todt = sdf.format(cal.getTime());
            
            cal.set(Calendar.DAY_OF_MONTH, 1);
            String fromdt = sdf.format(cal.getTime());

            Intent intent = new Intent(this, MHSIO650S.class);
            intent.putExtra("itemcd", item.itemcd);
            intent.putExtra("itemnm", item.itemNm);
            intent.putExtra("whcd", wh.getCodecd());
            intent.putExtra("fromdt", fromdt);
            intent.putExtra("todt", todt);
            intent.putExtra("astkind", "120");
            startActivity(intent);
        });
    }

    private boolean validateInputs() {
        if (spWarehouse.getSelectedItem() == null) {
            Toast.makeText(this, "창고를 선택해 주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void setupWarehouseSpinner() {
        warehouseAdapter = new ArrayAdapter<CodeDto>(this, R.layout.item_spinner, new ArrayList<>()) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                if (getItem(position) != null) tv.setText(getItem(position).getCodenm());
                return tv;
            }
            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                if (getItem(position) != null) {
                    tv.setText(getItem(position).getCodenm());
                    tv.setTextColor(Color.BLACK);
                    tv.setBackgroundColor(Color.WHITE);
                }
                return tv;
            }
        };
        spWarehouse.setAdapter(warehouseAdapter);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String cmpycd = prefs.getString("cmpycd", "HAIONNET");

        apiService.getCommonCode(cmpycd, "KOR", "030").enqueue(new Callback<List<CodeDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<CodeDto>> call, @NonNull Response<List<CodeDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    warehouseAdapter.addAll(response.body());
                    warehouseAdapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(@NonNull Call<List<CodeDto>> call, @NonNull Throwable t) {}
        });
    }

    private void showItemSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_item_search, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        EditText etSearchQuery = dialogView.findViewById(R.id.etSearchQuery);
        ListView lvList = dialogView.findViewById(R.id.lvItemList);
        Button btnClose = dialogView.findViewById(R.id.btnClose);

        final ArrayAdapter<ItemDto> itemAdapter = new ArrayAdapter<>(this, R.layout.item_popup_list, new ArrayList<>());
        lvList.setAdapter(itemAdapter);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String cmpycd = prefs.getString("cmpycd", "HAIONNET");

        Runnable doSearch = () -> {
            String keyword = etSearchQuery.getText().toString().trim();
            apiService.searchItems(cmpycd, "KOR", keyword, "120").enqueue(new Callback<List<ItemDto>>() {
                @Override
                public void onResponse(@NonNull Call<List<ItemDto>> call, @NonNull Response<List<ItemDto>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<ItemDto> list = response.body();
                        itemAdapter.clear();
                        if (list != null) {
                            itemAdapter.addAll(list);
                        }
                        itemAdapter.notifyDataSetChanged();
                    }
                }
                @Override public void onFailure(@NonNull Call<List<ItemDto>> call, @NonNull Throwable t) {
                    Log.e(TAG, "품목 검색 실패: " + t.getMessage());
                }
            });
        };

        dialog.setOnShowListener(d -> doSearch.run());

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                doSearch.run();
            }
        });

        lvList.setOnItemClickListener((parent, view1, position, id) -> {
            ItemDto selectedItem = itemAdapter.getItem(position);
            if (selectedItem != null) {
                etItemNm.setText(selectedItem.itemnm);
                selectedItemCd = selectedItem.itemcd;
                dialog.dismiss();
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void searchInventory() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        CodeDto selectedWh = (CodeDto) spWarehouse.getSelectedItem();
        
        Map<String, String> params = new HashMap<>();
        params.put("ymd", today);
        params.put("whcd", selectedWh.getCodecd());
        String itemCdParam = (selectedItemCd == null || selectedItemCd.isEmpty()) ? "0000000" : selectedItemCd;
        params.put("itemcd", itemCdParam);
        params.put("astkind", "120");

        apiService.getInventoryProductStatus(params).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> list = response.body();
                    inventoryItems.clear();
                    if (list.isEmpty()) {
                        Toast.makeText(MHSST200S.this, "조회된 자료가 없습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        for (Map<String, Object> map : list) {
                            inventoryItems.add(new InventoryItem(
                                    getStringValue(map, "itemcd"),
                                    getStringValue(map, "itemnm"),
                                    parseToDouble(map.get("stock")),
                                    parseToDouble(map.get("stkqty")),
                                    parseToDouble(map.get("stkamt"))
                            ));
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                Toast.makeText(MHSST200S.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
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

    private static class InventoryItem {
        String itemcd, itemNm;
        double properQty, stockQty, stockAmt;

        public InventoryItem(String itemcd, String itemNm, double properQty, double stockQty, double stockAmt) {
            this.itemcd = itemcd;
            this.itemNm = itemNm;
            this.properQty = properQty;
            this.stockQty = stockQty;
            this.stockAmt = stockAmt;
        }
    }

    private class InventoryAdapter extends BaseAdapter {
        @Override public int getCount() { return inventoryItems.size(); }
        @Override public Object getItem(int pos) { return inventoryItems.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MHSST200S.this).inflate(R.layout.item_mhsst200s, parent, false);
            }
            InventoryItem item = inventoryItems.get(position);

            ((TextView) convertView.findViewById(R.id.tvItemNm)).setText(item.itemNm);
            ((TextView) convertView.findViewById(R.id.tvProperQty)).setText(String.format(Locale.getDefault(), "%,d", (int)item.properQty));
            ((TextView) convertView.findViewById(R.id.tvStockQty)).setText(String.format(Locale.getDefault(), "%,d", (int)item.stockQty));
            ((TextView) convertView.findViewById(R.id.tvStockAmt)).setText(String.format(Locale.getDefault(), "%,d", (int)item.stockAmt));

            return convertView;
        }
    }

    @Override protected String getProgramTitle() { return "창고재고현황"; }
    @Override protected String getProgramId() { return "MHSST200S"; }
}
