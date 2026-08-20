package com.crmbank.erp.mobile.hpio;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.RetrofitClient;
import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.ApiResponse;
import com.crmbank.erp.mobile.CodeDto;
import com.crmbank.erp.mobile.InboundRegisterAdapter;
import com.crmbank.erp.mobile.InoutRequest;
import com.crmbank.erp.mobile.BarcodeScanActivity;
import com.crmbank.erp.mobile.R;

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

public class MHPIO400U extends BaseActivity {

    private static final String TAG = "MHPIO_400U";
    private Spinner spWarehouse;
    private TextView tvInboundDate;
    private EditText etOrderNo, etCustomerName, etInboundNo, etRemark;
    private ListView lvRegisterList;
    private Button btnSave, btnReset;
    private ImageButton btnScan;
    private ArrayAdapter<CodeDto> warehouseAdapter;
    private InboundRegisterAdapter listAdapter;
    private List<Map<String, Object>> detailList = new ArrayList<>();
    private Map<String, Object> masterData = new HashMap<>();
    private ApiService apiService;

    private long lastClickTime = 0;

    private final ActivityResultLauncher<Intent> barcodeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String scannedValue = result.getData().getStringExtra("BARCODE_VALUE");
                    handleScannedBarcode(scannedValue);
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) openBarcodeScanner();
                else Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhpio400u);

        apiService = RetrofitClient.getApiService();

        spWarehouse = findViewById(R.id.spWarehouse);
        tvInboundDate = findViewById(R.id.tvInboundDate);
        btnScan = findViewById(R.id.btnScan);
        btnSave = findViewById(R.id.btnSave);
        btnReset = findViewById(R.id.btnReset);
        etOrderNo = findViewById(R.id.etOrderNo);
        etInboundNo = findViewById(R.id.etInboundNo);
        etCustomerName = findViewById(R.id.etCustomerName);
        etRemark = findViewById(R.id.etRemark);
        lvRegisterList = findViewById(R.id.lvRegisterList);
        
        listAdapter = new InboundRegisterAdapter(this, detailList);
        lvRegisterList.setAdapter(listAdapter);
        
        setupWarehouseSpinner();
        setupInboundDatePicker();
        resetFields();

        etOrderNo.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                long clickTime = System.currentTimeMillis();
                if (clickTime - lastClickTime < 500) {
                    showInboundOrderSearchDialog();
                    return true;
                }
                lastClickTime = clickTime;
            }
            return false;
        });

        if (btnScan != null) {
            btnScan.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    openBarcodeScanner();
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA);
                }
            });
        }

        findViewById(R.id.btnSearchOrder).setOnClickListener(v -> {
            String balno = etOrderNo.getText().toString().trim();
            if (!balno.isEmpty()) performOrderSearch(balno);
            else Toast.makeText(this, "발주번호를 입력하세요.", Toast.LENGTH_SHORT).show();
        });

        btnReset.setOnClickListener(v -> resetFields());
        btnSave.setOnClickListener(v -> saveInboundReceive());
    }

    @Override protected String getProgramTitle() { return "제품입고작업"; }
    @Override protected String getProgramId() { return "MHPIO_400U"; }

    private void openBarcodeScanner() {
        Intent intent = new Intent(this, BarcodeScanActivity.class);
        barcodeLauncher.launch(intent);
    }

    private void handleScannedBarcode(String value) {
        if (value == null || value.isEmpty()) return;
        final String cleanValue = value.trim();

        if (cleanValue.length() == 12) { 
            etOrderNo.setText(cleanValue);
            performOrderSearch(cleanValue);
        } else {
            if (detailList.isEmpty()) {
                Toast.makeText(this, "발주서를 먼저 조회하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            processProductBarcode(cleanValue);
        }
    }

    private void processProductBarcode(String barcode) {
        boolean found = false;
        for (int i = 0; i < detailList.size(); i++) {
            Map<String, Object> item = detailList.get(i);
            String itemCd = getStringValue(item, "ITEMCD");
            String itemBarcode = getStringValue(item, "BARCODE");
            
            if (barcode.equalsIgnoreCase(itemCd) || barcode.equalsIgnoreCase(itemBarcode)) {
                double currentQty = getDoubleValue(item, "ioqty");
                item.put("ioqty", currentQty + 1);
                listAdapter.notifyDataSetChanged();
                lvRegisterList.setSelection(i);
                Toast.makeText(this, "[" + getStringValue(item, "ITEMNM") + "] 수량 증가", Toast.LENGTH_SHORT).show();
                found = true;
                break;
            }
        }
        if (!found) Toast.makeText(this, "품목을 찾을 수 없습니다: " + barcode, Toast.LENGTH_LONG).show();
    }

    private void performOrderSearch(String balno) {
        etInboundNo.setText("");
        etCustomerName.setText("조회 중...");
        detailList.clear();
        listAdapter.notifyDataSetChanged();
        btnSave.setEnabled(true);
        btnSave.setAlpha(1.0f);
        searchOrderMst(balno);
        searchOrderDtl(balno);
    }

    private void showInboundOrderSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_purch_order_search, null);
        builder.setTitle("발주번호 검색").setView(dialogView);

        TextView tvPopStartDate = dialogView.findViewById(R.id.tvPopStartDate);
        TextView tvPopEndDate = dialogView.findViewById(R.id.tvPopEndDate);
        EditText etPopCustNm = dialogView.findViewById(R.id.etPopCustNm);
        Button btnPopSearch = dialogView.findViewById(R.id.btnPopSearch);
        ListView lv = dialogView.findViewById(R.id.lvPopOrderList);

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        tvPopEndDate.setText(sdf.format(cal.getTime()));
        cal.add(Calendar.MONTH, -1);
        tvPopStartDate.setText(sdf.format(cal.getTime()));

        List<Map<String, Object>> popList = new ArrayList<>();
        InboundOrderPopAdapter popAdapter = new InboundOrderPopAdapter(popList);
        lv.setAdapter(popAdapter);

        AlertDialog dialog = builder.create();
        btnPopSearch.setOnClickListener(v -> {
            apiService.getPurchaseOrderInMobileList(tvPopStartDate.getText().toString().replace("-",""), 
                tvPopEndDate.getText().toString().replace("-",""), etPopCustNm.getText().toString().trim())
                .enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Response<ApiResponse<List<Map<String, Object>>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            popList.clear();
                            if (response.body().getData() != null) popList.addAll(response.body().getData());
                            popAdapter.notifyDataSetChanged();
                        }
                    }
                    @Override public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Throwable t) {}
                });
        });

        lv.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, Object> selected = popList.get(position);
            String bNo = String.valueOf(selected.get("balno") != null ? selected.get("balno") : selected.get("BALNO"));
            etOrderNo.setText(bNo);
            dialog.dismiss();
            performOrderSearch(bNo);
        });

        dialogView.findViewById(R.id.btnPopClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        btnPopSearch.performClick();
    }

    private void resetFields() {
        etOrderNo.setText(""); etInboundNo.setText(""); etCustomerName.setText(""); etRemark.setText("");
        tvInboundDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        detailList.clear(); listAdapter.notifyDataSetChanged();
        btnSave.setEnabled(true); btnSave.setAlpha(1.0f);
    }

    private void searchOrderMst(String balno) {
        apiService.getPurchaseOrderInSearchMst(balno).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    masterData = response.body().getData();
                    if (masterData != null) {
                        Object nm = masterData.get("CUSTNM") != null ? masterData.get("CUSTNM") : masterData.get("custnm");
                        if (nm != null) etCustomerName.setText(nm.toString());
                    }
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void searchOrderDtl(String balno) {
        apiService.getPurchaseOrderInSearchDtl(balno).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    detailList.clear();
                    if (response.body().getData() != null) {
                        for (Map<String, Object> item : response.body().getData()) {
                            double jan = getDoubleValue(item, "JANQTY");
                            if (jan == 0) jan = getDoubleValue(item, "janqty");
                            item.put("ioqty", jan);
                            detailList.add(item);
                        }
                    }
                    listAdapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Throwable t) {}
        });
    }

    private void saveInboundReceive() {
        if (detailList.isEmpty() || masterData.isEmpty()) {
            Toast.makeText(this, "저장할 내역이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String sessionUserid = prefs.getString("userId", "");
        String ioymd = tvInboundDate.getText().toString().replace("-", "");

        masterData.put("cmpycd", "haionnet");
        masterData.put("ioymd", ioymd);
        masterData.put("balno", etOrderNo.getText().toString().trim());
        masterData.put("userid", sessionUserid);

        List<Map<String, Object>> validDetails = new ArrayList<>();
        for (Map<String, Object> item : detailList) {
            double ioqty = getDoubleValue(item, "ioqty");
            if (ioqty > 0) {
                item.put("userid", sessionUserid);
                validDetails.add(item);
            }
        }

        InoutRequest request = new InoutRequest(masterData, validDetails);
        apiService.receivePurchOrderIn(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<String>> call, @NonNull Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        Toast.makeText(MHPIO400U.this, "저장 성공", Toast.LENGTH_SHORT).show();
                        etInboundNo.setText(response.body().getData());
                        btnSave.setEnabled(false); btnSave.setAlpha(0.5f);
                    }
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<String>> call, @NonNull Throwable t) {}
        });
    }

    private double getDoubleValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return 0;
        try { return Double.parseDouble(String.valueOf(val)); } catch (Exception e) { return 0; }
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) val = map.get(key.toUpperCase());
        if (val == null) val = map.get(key.toLowerCase());
        return val != null ? String.valueOf(val) : "";
    }

    private void setupWarehouseSpinner() {
        warehouseAdapter = new ArrayAdapter<>(this, R.layout.item_popup_list, new ArrayList<>());
        spWarehouse.setAdapter(warehouseAdapter);
        apiService.getCommonCode("haionnet", "KOR", "030").enqueue(new Callback<List<CodeDto>>() {
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

    private void setupInboundDatePicker() {
        tvInboundDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> 
                tvInboundDate.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d)), 
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private static class InboundOrderPopAdapter extends BaseAdapter {
        private final List<Map<String, Object>> items;
        public InboundOrderPopAdapter(List<Map<String, Object>> items) { this.items = items; }
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int p) { return items.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup pr) {
            if (v == null) v = LayoutInflater.from(pr.getContext()).inflate(R.layout.item_mhpio400u_pop, pr, false);
            Map<String, Object> i = items.get(p);
            ((TextView) v.findViewById(R.id.tvPopCustNm)).setText(String.valueOf(i.get("CUSTNM")!=null?i.get("CUSTNM"):i.get("custnm")));
            ((TextView) v.findViewById(R.id.tvPopBalno)).setText(String.valueOf(i.get("BALNO")!=null?i.get("BALNO"):i.get("balno")));
            return v;
        }
    }
}
